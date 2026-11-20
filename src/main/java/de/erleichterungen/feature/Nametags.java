package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Namensschilder: Name, Gesundheit und Entfernung über allen Spielern – durch Wände hindurch.
 * Eigenständig nutzbar (unabhängig vom Player-ESP).
 */
public final class Nametags {
	private Nametags() {
	}

	public static void render(Minecraft mc, Vec3 cam, float partialTick) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.nametagsEnabled || mc.level == null || mc.player == null) return;

		double rangeSq = (double) cfg.nametagsRange * cfg.nametagsRange;
		int color = ModConfig.parseColor(cfg.nametagsColor, 0xFFFFFFFF) | 0xFF000000;

		for (Player player : mc.level.players()) {
			if (player == mc.player || player.isRemoved()) continue;
			Vec3 pos = player.getPosition(partialTick);
			double distSq = pos.distanceToSqr(cam);
			if (distSq > rangeSq) continue;
			double dist = Math.sqrt(distSq);

			StringBuilder sb = new StringBuilder(player.getGameProfile().name());
			if (cfg.nametagsHealth) {
				sb.append(String.format(Locale.ROOT, "  %.0f/%.0f", player.getHealth(), player.getMaxHealth()));
			}
			if (cfg.nametagsDistance) {
				sb.append(String.format(Locale.ROOT, "  [%.0fm]", dist));
			}

			AABB box = player.getBoundingBox().move(pos.subtract(player.position()));
			float scale = (float) Math.min(4.0, Math.max(0.6, dist / 12.0)) * 0.5f * cfg.nametagsScale;
			Vec3 top = new Vec3(pos.x, box.maxY + 0.35 + scale * 0.3, pos.z);
			Gizmos.billboardText(sb.toString(), top,
					TextGizmo.Style.forColorAndCentered(color).withScale(scale)).setAlwaysOnTop();
		}
	}
}
