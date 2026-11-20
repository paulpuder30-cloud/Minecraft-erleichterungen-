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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Portal-ESP: markiert Nether-Portale, End-Portale, End-Portalrahmen und End-Gateways
 * durch Wände. Nutzt die Scan-Infrastruktur ({@link BlockTracker}).
 */
public final class PortalEsp {
	private static final Set<Block> BLOCKS = Collections.newSetFromMap(new IdentityHashMap<>());
	public static final BlockTracker TRACKER = new BlockTracker(state -> BLOCKS.contains(state.getBlock()));

	private static int lastRendered;

	private PortalEsp() {
	}

	public static void init() {
		ScanManager.register(TRACKER);
		buildSet();
	}

	private static void add(String path) {
		Identifier id = Identifier.tryParse("minecraft:" + path);
		if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
			BLOCKS.add(BuiltInRegistries.BLOCK.getValue(id));
		}
	}

	private static void buildSet() {
		BLOCKS.clear();
		add("nether_portal");
		add("end_portal");
		add("end_portal_frame");
		add("end_gateway");
		TRACKER.clear();
		ScanManager.rescanAll();
	}

	public static int lastRendered() {
		return lastRendered;
	}

	public static void render(Minecraft mc, Vec3 cam) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.portalEspEnabled || mc.level == null) {
			lastRendered = 0;
			return;
		}
		int range = Math.max(8, cfg.portalEspRange);
		double rangeSq = (double) range * range;
		int chunkRadius = (range >> 4) + 1;
		int camCx = ((int) Math.floor(cam.x)) >> 4;
		int camCz = ((int) Math.floor(cam.z)) >> 4;
		int max = cfg.portalEspMaxBoxes;
		RenderMode mode = cfg.portalEspMode;
		boolean fill = cfg.portalEspFill;
		float width = cfg.portalEspLineWidth;
		int color = ModConfig.parseColor(cfg.portalEspColor, 0xFFB14DFF);
		int tracerColor = ModConfig.parseColor(cfg.portalEspTracerColor, 0xFFB14DFF);
		LongArrayList stale = new LongArrayList();
		int[] count = {0};

		TRACKER.forEachNear(camCx, camCz, chunkRadius, (pos, state) -> {
			if (count[0] >= max) return;
			int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
			double dx = x + 0.5 - cam.x, dy = y + 0.5 - cam.y, dz = z + 0.5 - cam.z;
			if (dx * dx + dy * dy + dz * dz > rangeSq) return;
			BlockPos bp = new BlockPos(x, y, z);
			if (mc.level.getBlockState(bp) != state) {
				stale.add(pos);
				return;
			}
			if (mode.box()) {
				GizmoStyle style = fill
						? GizmoStyle.strokeAndFill(color, width, (color & 0x00FFFFFF) | 0x30000000)
						: GizmoStyle.stroke(color, width);
				Gizmos.cuboid(bp, style).setAlwaysOnTop();
			}
			if (mode.tracer()) Tracers.line(Vec3.atCenterOf(bp), tracerColor);
			count[0]++;
		});

		for (int i = 0; i < stale.size(); i++) {
			long p = stale.getLong(i);
			TRACKER.invalidate(p, mc.level.getBlockState(BlockPos.of(p)));
		}
		lastRendered = count[0];
	}
}
