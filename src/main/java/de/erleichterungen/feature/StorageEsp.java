package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.config.RenderMode;
import de.erleichterungen.scan.BlockTracker;
import de.erleichterungen.scan.ScanManager;
import de.erleichterungen.util.Tracers;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Storage-ESP: markiert Behälter (Truhen, Fässer, Shulker, Öfen, Hopper, ...) durch Wände –
 * ideal zum Auffinden von Basen. Nutzt die Scan-Infrastruktur ({@link BlockTracker}).
 * Die Farbe richtet sich nach der Behälter-Art.
 */
public final class StorageEsp {
	/** Behälter-Block -> Farbe (ARGB). */
	private static final Map<Block, Integer> COLORS = new IdentityHashMap<>();
	public static final BlockTracker TRACKER = new BlockTracker(state -> COLORS.containsKey(state.getBlock()));

	private static int lastRendered;

	private StorageEsp() {
	}

	public static void init() {
		ScanManager.register(TRACKER);
		buildSet();
	}

	private static void add(String path, int color) {
		Identifier id = Identifier.tryParse("minecraft:" + path);
		if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
			COLORS.put(BuiltInRegistries.BLOCK.getValue(id), color);
		}
	}

	private static void buildSet() {
		COLORS.clear();
		int chest = 0xFFFFC14D, furnace = 0xFFFF8844, redstone = 0xFFBBBBBB,
				ender = 0xFF00E0C0, brew = 0xFFFF77CC, crafter = 0xFF5C9CFF, pot = 0xFFD2B48C,
				shulker = 0xFFB060FF;
		add("chest", chest);
		add("trapped_chest", chest);
		add("barrel", chest);
		add("furnace", furnace);
		add("blast_furnace", furnace);
		add("smoker", furnace);
		add("hopper", redstone);
		add("dropper", redstone);
		add("dispenser", redstone);
		add("ender_chest", ender);
		add("brewing_stand", brew);
		add("crafter", crafter);
		add("decorated_pot", pot);
		// alle Shulkerkisten (gefärbt + ungefärbt) automatisch aus der Registry
		for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
			if (id.toString().endsWith("shulker_box")) {
				COLORS.put(BuiltInRegistries.BLOCK.getValue(id), shulker);
			}
		}
		TRACKER.clear();
		ScanManager.rescanAll();
	}

	public static int lastRendered() {
		return lastRendered;
	}

	public static void render(Minecraft mc, Vec3 cam) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.storageEspEnabled || mc.level == null) {
			lastRendered = 0;
			return;
		}
		int range = Math.max(8, cfg.storageEspRange);
		double rangeSq = (double) range * range;
		int chunkRadius = (range >> 4) + 1;
		int camCx = ((int) Math.floor(cam.x)) >> 4;
		int camCz = ((int) Math.floor(cam.z)) >> 4;
		int max = cfg.storageEspMaxBoxes;
		RenderMode mode = cfg.storageEspMode;
		boolean fill = cfg.storageEspFill;
		float width = cfg.storageEspLineWidth;
		boolean labels = cfg.storageEspLabels;
		LongArrayList stale = new LongArrayList();
		int[] count = {0};

		TRACKER.forEachNear(camCx, camCz, chunkRadius, (pos, state) -> {
			if (count[0] >= max) return;
			int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
			double dx = x + 0.5 - cam.x, dy = y + 0.5 - cam.y, dz = z + 0.5 - cam.z;
			double distSq = dx * dx + dy * dy + dz * dz;
			if (distSq > rangeSq) return;
			BlockPos bp = new BlockPos(x, y, z);
			if (mc.level.getBlockState(bp) != state) {
				stale.add(pos);
				return;
			}
			Integer color = COLORS.get(state.getBlock());
			if (color == null) return;
			if (mode.box()) {
				GizmoStyle style = fill
						? GizmoStyle.strokeAndFill(color, width, (color & 0x00FFFFFF) | 0x30000000)
						: GizmoStyle.stroke(color, width);
				Gizmos.cuboid(bp, style).setAlwaysOnTop();
			}
			if (mode.tracer()) Tracers.line(Vec3.atCenterOf(bp), color);
			if (labels) {
				String name = state.getBlock().getName().getString();
				float scale = (float) Math.min(2.5, Math.max(0.5, Math.sqrt(distSq) / 16.0)) * 0.4f;
				Gizmos.billboardText(name, new Vec3(x + 0.5, y + 1.1, z + 0.5),
						TextGizmo.Style.forColorAndCentered(color | 0xFF000000).withScale(scale)).setAlwaysOnTop();
			}
			count[0]++;
		});

		for (int i = 0; i < stale.size(); i++) {
			long p = stale.getLong(i);
			TRACKER.invalidate(p, mc.level.getBlockState(BlockPos.of(p)));
		}
		lastRendered = count[0];
	}
}
