package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kleines Info-Overlay oben links: aktive Module, Geoden-Statistik, Vanish-Verdächtige.
 */
public final class Hud {
	private Hud() {
	}

	private record Line(String text, int color) {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		ModConfig cfg = ModConfig.get();
		Minecraft mc = Minecraft.getInstance();
		if (!cfg.hudEnabled || mc.player == null || mc.getDebugOverlay().showDebugScreen()) return;

		List<Line> lines = new ArrayList<>();
		lines.add(new Line("Erleichterungen", 0xFFD58CFF));
		if (cfg.blockEspEnabled) {
			lines.add(new Line("Block-ESP: " + BlockEsp.lastRendered() + " / " + BlockEsp.TRACKER.size(), 0xFF7FE8FF));
		}
		if (cfg.entityEspEnabled) {
			lines.add(new Line("Entity-ESP: " + EntityEsp.lastRendered(), 0xFFFFC04D));
			for (String s : EntityEsp.topCounts(4)) lines.add(new Line("  " + s, 0xFFDDDDDD));
		}
		if (cfg.playerEspEnabled) {
			lines.add(new Line("Player-ESP: " + PlayerEsp.lastRendered() + " Spieler", 0xFFFF7F7F));
		}
		if (cfg.geodeTrackerEnabled) {
			lines.add(new Line("Geoden (" + cfg.geodeRange + "m):", 0xFFB25CFF));
			lines.add(new Line("  Knospend: " + GeodeTracker.count(GeodeTracker.STAGE_BUDDING)
					+ "  freie Seiten: " + GeodeTracker.freeFaces(), 0xFFCCCCCC));
			lines.add(new Line("  Fertig: " + GeodeTracker.count(GeodeTracker.STAGE_CLUSTER),
					GeodeTracker.stageColor(GeodeTracker.STAGE_CLUSTER)));
			lines.add(new Line("  Wachsend: K " + GeodeTracker.count(GeodeTracker.STAGE_SMALL)
					+ " / M " + GeodeTracker.count(GeodeTracker.STAGE_MEDIUM)
					+ " / G " + GeodeTracker.count(GeodeTracker.STAGE_LARGE), 0xFFFFC870));
			long last = GeodeTracker.lastGrowthMillis();
			String ago = last == 0 ? "-" : formatAgo(System.currentTimeMillis() - last);
			lines.add(new Line("  Wachstum (10 min): " + GeodeTracker.growthLast10Min() + "  zuletzt: " + ago, 0xFFCCCCCC));
		}
		if (cfg.antiVanishEnabled) {
			Map<String, String> vanished = AntiVanish.vanished();
			if (vanished.isEmpty()) {
				lines.add(new Line("Anti-Vanish: keine", 0xFF9AFF9A));
			} else {
				lines.add(new Line("Anti-Vanish: " + vanished.size() + " versteckt", 0xFFFF5555));
				int shown = 0;
				for (String name : vanished.values()) {
					if (shown++ >= 6) {
						lines.add(new Line("  ...", 0xFFFF9999));
						break;
					}
					lines.add(new Line("  " + name, 0xFFFF9999));
				}
			}
		}
		if (lines.size() == 1) return;

		Font font = mc.font;
		int x = 4, y = 4;
		int width = 0;
		for (Line l : lines) width = Math.max(width, font.width(l.text()));
		graphics.fill(x - 2, y - 2, x + width + 2, y + lines.size() * (font.lineHeight + 1) + 1, 0x80000000);
		for (Line l : lines) {
			graphics.text(font, l.text(), x, y, l.color(), true);
			y += font.lineHeight + 1;
		}
	}

	private static String formatAgo(long ms) {
		long s = ms / 1000;
		if (s < 60) return s + "s";
		long m = s / 60;
		if (m < 60) return m + "m " + (s % 60) + "s";
		return (m / 60) + "h " + (m % 60) + "m";
	}
}
