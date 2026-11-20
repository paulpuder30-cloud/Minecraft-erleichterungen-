package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

/**
 * Chunk-Borders: zeichnet das Chunk-Raster um den Spieler – senkrechte Linien an den
 * Chunk-Ecken sowie ein Bodengitter auf Spielerhöhe.
 */
public final class ChunkBorders {
	private ChunkBorders() {
	}

	public static void render(Minecraft mc, Vec3 cam) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.chunkBordersEnabled || mc.player == null) return;

		int radius = Math.max(0, cfg.chunkBordersRadius);
		float width = cfg.chunkBordersLineWidth;
		int color = ModConfig.parseColor(cfg.chunkBordersColor, 0xFFFFFF55);
		double height = cfg.chunkBordersHeight;

		int pcx = ((int) Math.floor(cam.x)) >> 4;
		int pcz = ((int) Math.floor(cam.z)) >> 4;
		double y0 = cam.y - height;
		double y1 = cam.y + height;
		double feet = mc.player.getY();

		int minX = (pcx - radius) * 16;
		int maxX = (pcx + radius + 1) * 16;
		int minZ = (pcz - radius) * 16;
		int maxZ = (pcz + radius + 1) * 16;

		// Senkrechte Linien an jedem Chunk-Eckpunkt + Bodengitter
		for (int cx = pcx - radius; cx <= pcx + radius + 1; cx++) {
			double wx = cx * 16.0;
			Gizmos.line(new Vec3(wx, feet, minZ), new Vec3(wx, feet, maxZ), color, width).setAlwaysOnTop();
			for (int cz = pcz - radius; cz <= pcz + radius + 1; cz++) {
				double wz = cz * 16.0;
				Gizmos.line(new Vec3(wx, y0, wz), new Vec3(wx, y1, wz), color, width).setAlwaysOnTop();
			}
		}
		for (int cz = pcz - radius; cz <= pcz + radius + 1; cz++) {
			double wz = cz * 16.0;
			Gizmos.line(new Vec3(minX, feet, wz), new Vec3(maxX, feet, wz), color, width).setAlwaysOnTop();
		}
	}
}
