package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

/**
 * Ziel-Info: zeigt Name, Gesundheit und Entfernung des aktuell anvisierten Lebewesens
 * (unter dem Fadenkreuz) mittig knapp unter der Bildmitte an.
 */
public final class TargetInfo {
	private TargetInfo() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		ModConfig cfg = ModConfig.get();
		if (!cfg.targetInfoEnabled || mc.player == null) return;
		if (mc.gui.screen() instanceof de.erleichterungen.gui.ClickGuiScreen
				|| mc.gui.screen() instanceof de.erleichterungen.gui.ListEditScreen) return;
		if (!(mc.crosshairPickEntity instanceof LivingEntity target) || !target.isAlive()) return;

		double dist = mc.player.distanceTo(target);
		String name = target.getName().getString();
		String stats = String.format(Locale.ROOT, "HP %.0f/%.0f   %.1f m",
				target.getHealth(), target.getMaxHealth(), dist);

		Font font = mc.font;
		int cx = mc.getWindow().getGuiScaledWidth() / 2;
		int top = mc.getWindow().getGuiScaledHeight() / 2 + 14;
		int boxW = Math.max(font.width(name), font.width(stats)) + 8;
		int x = cx - boxW / 2;
		int lineH = font.lineHeight + 1;
		graphics.fill(x, top, x + boxW, top + 2 * lineH + 3, 0x90000000);

		int accent = ModConfig.parseColor(cfg.guiAccent, 0xFF8A5CFF);
		graphics.text(font, name, cx - font.width(name) / 2, top + 2, accent, true);
		int hpColor = healthColor(target.getHealth() / Math.max(1.0f, target.getMaxHealth()));
		graphics.text(font, stats, cx - font.width(stats) / 2, top + 2 + lineH, hpColor, true);
	}

	private static int healthColor(float frac) {
		if (frac > 0.6f) return 0xFF66FF66;
		if (frac > 0.3f) return 0xFFFFD24D;
		return 0xFFFF5555;
	}
}
