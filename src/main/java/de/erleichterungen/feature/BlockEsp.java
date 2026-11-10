package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.scan.BlockTracker;
import de.erleichterungen.scan.ScanManager;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Markiert ausgewählte Blöcke (Erze, Spawner, ...) durch Wände hindurch.
 */
public final class BlockEsp {
	/** Block -> ARGB-Farbe. Wird aus der Config aufgebaut. */
	private static final Map<Block, Integer> COLORS = new IdentityHashMap<>();
	public static final BlockTracker TRACKER = new BlockTracker(state -> COLORS.containsKey(state.getBlock()));

	private static int lastRendered;

	private BlockEsp() {
	}

	public static void init() {
		ScanManager.register(TRACKER);
		rebuildFilter();
	}

	/** Liest die Blockliste aus der Config neu ein und scannt alle Chunks neu. */
	public static void rebuildFilter() {
		COLORS.clear();
		for (Map.Entry<String, String> e : ModConfig.get().blockEspBlocks.entrySet()) {
			Block block = resolve(e.getKey());
			if (block != null) {
				COLORS.put(block, ModConfig.parseColor(e.getValue(), defaultColor(e.getKey())));
			}
		}
		TRACKER.clear();
		ScanManager.rescanAll();
	}

	public static Block resolve(String id) {
		Identifier ident = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
		if (ident == null || !BuiltInRegistries.BLOCK.containsKey(ident)) return null;
		return BuiltInRegistries.BLOCK.getValue(ident);
	}

	public static String normalize(String id) {
		return id.contains(":") ? id : "minecraft:" + id;
	}

	/** Deterministische Farbe aus der Block-ID, falls keine angegeben wurde. */
	public static int defaultColor(String id) {
		float hue = (id.hashCode() & 0xFFFF) / (float) 0xFFFF;
		return 0xFF000000 | (net.minecraft.util.Mth.hsvToRgb(hue, 0.8f, 1.0f) & 0xFFFFFF);
	}

	public static int lastRendered() {
		return lastRendered;
	}

	public static void render(Minecraft mc, Vec3 cam) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.blockEspEnabled || mc.level == null) {
			lastRendered = 0;
			return;
		}
		int range = Math.max(8, cfg.blockEspRange);
		double rangeSq = (double) range * range;
		int chunkRadius = (range >> 4) + 1;
		int camCx = ((int) Math.floor(cam.x)) >> 4;
		int camCz = ((int) Math.floor(cam.z)) >> 4;
		int max = cfg.blockEspMaxBoxes;
		float width = cfg.blockEspLineWidth;
		boolean fill = cfg.blockEspFill;
		LongArrayList stale = new LongArrayList();
		int[] count = {0};

		TRACKER.forEachNear(camCx, camCz, chunkRadius, (pos, state) -> {
			if (count[0] >= max) return;
			int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
			double dx = x + 0.5 - cam.x, dy = y + 0.5 - cam.y, dz = z + 0.5 - cam.z;
			if (dx * dx + dy * dy + dz * dz > rangeSq) return;
			BlockPos bp = new BlockPos(x, y, z);
			// Sicherheitsnetz: falls ein Update verpasst wurde
			if (mc.level.getBlockState(bp) != state) {
				stale.add(pos);
				return;
			}
			Integer color = COLORS.get(state.getBlock());
			if (color == null) return;
			GizmoStyle style = fill
					? GizmoStyle.strokeAndFill(color, width, (color & 0x00FFFFFF) | 0x30000000)
					: GizmoStyle.stroke(color, width);
			Gizmos.cuboid(bp, style).setAlwaysOnTop();
			count[0]++;
		});

		for (int i = 0; i < stale.size(); i++) {
			long p = stale.getLong(i);
			TRACKER.invalidate(p, mc.level.getBlockState(BlockPos.of(p)));
		}
		lastRendered = count[0];
	}
}
