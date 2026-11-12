package de.erleichterungen.gui;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Farben und kleine Zeichenhilfen für die GUI.
 */
public final class Theme {
	public static final int PANEL_BG = 0xE5121218;
	public static final int ROW_BG = 0xE51A1A22;
	public static final int SETTING_BG = 0xE5141419;
	public static final int HOVER = 0x22FFFFFF;
	public static final int TEXT = 0xFFE8E8EE;
	public static final int TEXT_DIM = 0xFF8E8E9A;
	public static final int TEXT_OFF = 0xFFB4B4BE;
	public static final int SLIDER_BG = 0xFF2A2A34;
	public static final int SEPARATOR = 0x40FFFFFF;

	private Theme() {
	}

	public static int accent() {
		return ModConfig.parseColor(ModConfig.get().guiAccent, 0xFF8A5CFF);
	}

	public static int withAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | (alpha << 24);
	}

	public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + h, color);
	}

	public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
		g.text(font, s, x, y, color, false);
	}

	public static void textRight(GuiGraphicsExtractor g, Font font, String s, int rightX, int y, int color) {
		g.text(font, s, rightX - font.width(s), y, color, false);
	}

	/** Kürzt einen Text auf die gegebene Breite (mit "..."). */
	public static String ellipsize(Font font, String s, int maxWidth) {
		if (font.width(s) <= maxWidth) return s;
		String dots = "...";
		int w = maxWidth - font.width(dots);
		StringBuilder sb = new StringBuilder();
		for (char ch : s.toCharArray()) {
			if (font.width(sb.toString() + ch) > w) break;
			sb.append(ch);
		}
		return sb + dots;
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Checkbox im Meteor-Stil: kleines Quadrat, gefüllt wenn aktiv. */
	public static void checkbox(GuiGraphicsExtractor g, int x, int y, boolean on) {
		rect(g, x, y, 9, 9, SLIDER_BG);
		if (on) rect(g, x + 2, y + 2, 5, 5, accent());
	}

	public static void slider(GuiGraphicsExtractor g, int x, int y, int w, double t) {
		rect(g, x, y, w, 3, SLIDER_BG);
		int fw = (int) Math.round(w * Math.max(0, Math.min(1, t)));
		rect(g, x, y, fw, 3, accent());
		rect(g, x + fw - 1, y - 2, 3, 7, TEXT);
	}
}
