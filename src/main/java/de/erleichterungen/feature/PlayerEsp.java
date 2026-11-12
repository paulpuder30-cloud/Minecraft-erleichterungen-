package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.util.Tracers;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Zeichnet Boxen (und optional Name + Entfernung) um alle Spieler – durch Wände hindurch.
 * Spieler, die Anti-Vanish als versteckt erkannt hat, werden in einer eigenen Farbe markiert.
 */
public final class PlayerEsp {
	private static int lastRendered;

	private PlayerEsp() {
	}

	public static int lastRendered() {
		return lastRendered;
	}

	public static void render(Minecraft mc, Vec3 cam, float partialTick) {
		ModConfig cfg = ModConfig.get();
		lastRendered = 0;
		if (mc.level == null || mc.player == null) return;
		boolean all = cfg.playerEspEnabled;
		// Anti-Vanish markiert sichtbare Verdächtige auch ohne Player-ESP
		boolean suspects = cfg.antiVanishEnabled && cfg.antiVanishHighlight;
		if (!all && !suspects) return;

		int normal = ModConfig.parseColor(cfg.playerEspColor, 0xFFFF3B3B);
		int vanished = ModConfig.parseColor(cfg.playerEspVanishedColor, 0xFFFF00FF);
		double rangeSq = (double) cfg.playerEspRange * cfg.playerEspRange;

		for (Player player : mc.level.players()) {
			if (player == mc.player || player.isRemoved()) continue;
			Vec3 pos = player.getPosition(partialTick);
			double distSq = pos.distanceToSqr(cam);
			if (distSq > rangeSq) continue;

			boolean suspected = AntiVanish.isSuspected(player);
			if (!all && !suspected) continue;
			// Verdächtige nutzen den Anti-Vanish-Modus, alle anderen den Player-ESP-Modus
			boolean useVanish = suspected && suspects;
			boolean drawBox = useVanish ? cfg.antiVanishMode.box() : cfg.playerEspMode.box();
			boolean drawTracer = useVanish ? cfg.antiVanishMode.tracer() : cfg.playerEspMode.tracer();
			int tracerColor = ModConfig.parseColor(useVanish ? cfg.antiVanishTracerColor : cfg.playerEspTracerColor, 0xFFFFFFFF);
			int color = suspected ? vanished : normal;
			if (player.isInvisible() && !suspected) {
				color = (color & 0x00FFFFFF) | 0xA0000000; // unsichtbare Spieler etwas transparenter
			}

			// Bounding-Box an die interpolierte Position verschieben (flüssige Bewegung)
			AABB box = player.getBoundingBox().move(pos.subtract(player.position()));
			if (drawBox) {
				GizmoStyle style = cfg.playerEspFill
						? GizmoStyle.strokeAndFill(color, cfg.playerEspLineWidth, (color & 0x00FFFFFF) | 0x28000000)
						: GizmoStyle.stroke(color, cfg.playerEspLineWidth);
				Gizmos.cuboid(box, style).setAlwaysOnTop();
			}
			if (drawTracer) Tracers.line(box.getCenter(), tracerColor);

			if (cfg.playerEspNames && (drawBox || drawTracer)) {
				double dist = Math.sqrt(distSq);
				String label = player.getGameProfile().name()
						+ String.format(Locale.ROOT, " [%.0fm]", dist)
						+ (suspected ? " (Vanish?)" : "");
				// Text etwas mit der Entfernung skalieren, damit er weit weg lesbar bleibt
				float scale = (float) Math.min(4.0, Math.max(0.6, dist / 12.0)) * 0.5f;
				Vec3 top = new Vec3(pos.x, box.maxY + 0.35 + scale * 0.3, pos.z);
				Gizmos.billboardText(label, top, TextGizmo.Style.forColorAndCentered(color | 0xFF000000).withScale(scale))
						.setAlwaysOnTop();
			}
			lastRendered++;
		}
	}
}
