package de.erleichterungen.gui;

import com.mojang.blaze3d.platform.InputConstants;
import de.erleichterungen.config.EspEntry;
import de.erleichterungen.config.ModConfig;
import de.erleichterungen.config.RenderMode;
import de.erleichterungen.feature.BlockEsp;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bearbeitet eine Block- bzw. Entity-Liste.
 * <ul>
 *   <li>Links: ausgewählte Einträge. Klick = bearbeiten, x = entfernen, Klick auf B/B+T/T = Modus wechseln.</li>
 *   <li>Unten links (für den gewählten Eintrag): Modus (Box / Box & Tracer / Nur Tracer), Box- und Tracer-Farbe.</li>
 *   <li>Rechts: Suche über alle Blöcke/Entities (deutscher Name oder ID), Klick = hinzufügen.</li>
 * </ul>
 */
public class ListEditScreen extends Screen {
	private static final int ROW = 13;
	private static final int EDITOR_H = 2 * ROW + 3 * ROW + 8;

	private final Screen parent;
	private final Setting.ListEdit setting;
	private final Collection<String> allOptions;
	private final Map<String, String> names = new HashMap<>();
	private final Map<String, String> searchKeys = new HashMap<>();
	private final StringBuilder search = new StringBuilder();

	private String selected;
	/** Welche Farbe die Regler gerade bearbeiten: false = Box, true = Tracer. */
	private boolean editTracerColor;
	private int scrollLeft, scrollRight;
	private int dragChannel = -1;

	// Layout (in extractRenderState berechnet)
	private int colW, leftX, rightX, listTop, listBottom, editorTop, clearX, clearY;

	public ListEditScreen(Screen parent, Setting.ListEdit setting) {
		super(Component.literal(setting.name));
		this.parent = parent;
		this.setting = setting;
		this.allOptions = setting.options.get();
		for (String id : allOptions) cacheName(id);
	}

	private Map<String, EspEntry> map() {
		return setting.map.get();
	}

	private String nameOf(String id) {
		String n = names.get(id);
		return n != null ? n : cacheName(id);
	}

	private String cacheName(String id) {
		String n;
		try {
			n = setting.displayName.apply(id);
		} catch (Exception e) {
			n = id;
		}
		names.put(id, n);
		searchKeys.put(id, (n + " " + id + " " + shortId(id).replace('_', ' ')).toLowerCase(Locale.ROOT));
		return n;
	}

	private List<String> filtered() {
		String q = search.toString().toLowerCase(Locale.ROOT).trim();
		String[] words = q.isEmpty() ? new String[0] : q.split("\\s+");
		List<String> out = new ArrayList<>();
		outer:
		for (String id : allOptions) {
			if (map().containsKey(id)) continue;
			String key = searchKeys.get(id);
			for (String w : words) if (key == null || !key.contains(w)) continue outer;
			out.add(id);
		}
		// Treffer, deren Name mit dem Suchbegriff beginnt, zuerst
		if (!q.isEmpty()) out.sort(Comparator.comparing((String id) -> !nameOf(id).toLowerCase(Locale.ROOT).startsWith(q)));
		return out;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		setting.onChange.run();
		ModConfig.save();
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, width, height, 0x80000000);
	}

	// ------------------------------------------------------------------ Zeichnen

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int accent = Theme.accent();
		int totalW = Math.min(440, width - 16);
		colW = (totalW - 8) / 2;
		leftX = (width - totalW) / 2;
		rightX = leftX + colW + 8;
		int top = 18;
		listTop = top + 32;
		listBottom = height - 28;
		editorTop = listBottom - EDITOR_H;
		if (selected != null && !map().containsKey(selected)) selected = null;

		g.centeredText(font, setting.name + " bearbeiten", width / 2, 5, accent);

		// ---------------- linke Spalte: ausgewählt
		Theme.rect(g, leftX, top, colW, 16, accent);
		Theme.text(g, font, "Ausgewählt (" + map().size() + ")", leftX + 5, top + 4, 0xFFFFFFFF);
		Theme.rect(g, leftX, top + 16, colW, listBottom - top - 16, Theme.PANEL_BG);
		Theme.text(g, font, Theme.ellipsize(font, "Klick = bearbeiten · Modus anklicken · x", colW - 10), leftX + 5, top + 20, Theme.TEXT_DIM);

		int leftBottom = selected != null ? editorTop - 2 : listBottom;
		List<String> sel = new ArrayList<>(map().keySet());
		scrollLeft = Mth.clamp(scrollLeft, 0, Math.max(0, sel.size() * ROW - (leftBottom - listTop)));
		g.enableScissor(leftX, listTop, leftX + colW, leftBottom);
		int y = listTop - scrollLeft;
		for (String id : sel) {
			EspEntry e = map().get(id);
			boolean rowHover = mouseY >= listTop && mouseY < leftBottom && Theme.inside(mouseX, mouseY, leftX, y, colW, ROW);
			if (id.equals(selected)) Theme.rect(g, leftX, y, colW, ROW, Theme.withAlpha(accent, 0x50));
			else if (rowHover) Theme.rect(g, leftX, y, colW, ROW, Theme.HOVER);
			// Box-Farbe (Quadrat) und Tracer-Farbe (Strich)
			Theme.rect(g, leftX + 4, y + 3, 7, 7, e.boxColor() | 0xFF000000);
			Theme.rect(g, leftX + 13, y + 6, 6, 2, e.tracerColor() | 0xFF000000);
			String tag = e.mode().shortLabel();
			int tagW = 24;
			int tagX = leftX + colW - 14 - tagW;
			boolean tagHover = rowHover && Theme.inside(mouseX, mouseY, tagX, y, tagW, ROW);
			Theme.rect(g, tagX, y + 1, tagW, ROW - 2, Theme.withAlpha(accent, tagHover ? 0xA0 : 0x50));
			g.centeredText(font, tag, tagX + tagW / 2, y + 2, 0xFFFFFFFF);
			drawNameAndId(g, id, leftX + 22, y + 2, tagX - leftX - 26, Theme.TEXT);
			boolean hovX = rowHover && Theme.inside(mouseX, mouseY, leftX + colW - 12, y, 12, ROW);
			Theme.text(g, font, "x", leftX + colW - 9, y + 2, hovX ? 0xFFFF5555 : Theme.TEXT_DIM);
			y += ROW;
		}
		g.disableScissor();

		if (selected != null) drawEditor(g, mouseX, mouseY, accent);

		// ---------------- rechte Spalte: hinzufügen
		Theme.rect(g, rightX, top, colW, 16, accent);
		Theme.text(g, font, "Hinzufügen", rightX + 5, top + 4, 0xFFFFFFFF);
		Theme.rect(g, rightX, top + 16, colW, listBottom - top - 16, Theme.PANEL_BG);

		List<String> opts = filtered();
		int fy = top + 18;
		Theme.rect(g, rightX + 4, fy, colW - 8, 12, 0xFF2E2E3A);
		Theme.rect(g, rightX + 4, fy + 11, colW - 8, 1, accent);
		boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
		String count = String.valueOf(opts.size());
		clearX = rightX + colW - 15;
		clearY = fy;
		int textMax = clearX - 4 - font.width(count) - (rightX + 7);
		if (search.isEmpty()) {
			Theme.text(g, font, blink ? "_" : " ", rightX + 7, fy + 2, Theme.TEXT);
			Theme.text(g, font, Theme.ellipsize(font, "Name oder ID tippen...", textMax - 8), rightX + 13, fy + 2, Theme.TEXT_DIM);
			Theme.textRight(g, font, count, rightX + colW - 7, fy + 2, Theme.TEXT_DIM);
		} else {
			String txt = search.toString();
			while (font.width(txt + "_") > textMax && txt.length() > 1) txt = txt.substring(1);
			Theme.text(g, font, txt + (blink ? "_" : ""), rightX + 7, fy + 2, Theme.TEXT);
			Theme.textRight(g, font, count, clearX - 3, fy + 2, Theme.TEXT_DIM);
			boolean hovClear = Theme.inside(mouseX, mouseY, clearX, clearY, 11, 12);
			Theme.text(g, font, "x", clearX + 3, fy + 2, hovClear ? 0xFFFF5555 : Theme.TEXT_DIM);
		}

		scrollRight = Mth.clamp(scrollRight, 0, Math.max(0, opts.size() * ROW - (listBottom - listTop)));
		g.enableScissor(rightX, listTop, rightX + colW, listBottom);
		int first = Math.max(0, scrollRight / ROW);
		y = listTop - scrollRight + first * ROW;
		for (int i = first; i < opts.size() && y < listBottom; i++) {
			String id = opts.get(i);
			boolean hov = mouseY >= listTop && mouseY < listBottom && Theme.inside(mouseX, mouseY, rightX, y, colW, ROW);
			if (hov) Theme.rect(g, rightX, y, colW, ROW, Theme.HOVER);
			drawNameAndId(g, id, rightX + 6, y + 2, colW - 20, hov ? Theme.TEXT : Theme.TEXT_OFF);
			if (hov) Theme.text(g, font, "+", rightX + colW - 10, y + 2, accent);
			y += ROW;
		}
		if (opts.isEmpty()) g.centeredText(font, "Keine Treffer", rightX + colW / 2, listTop + 6, Theme.TEXT_DIM);
		g.disableScissor();

		// Fertig-Knopf
		int bw = 80, bx = (width - bw) / 2, by = height - 22;
		boolean hovDone = Theme.inside(mouseX, mouseY, bx, by, bw, 16);
		Theme.rect(g, bx, by, bw, 16, Theme.withAlpha(accent, hovDone ? 0xFF : 0xB0));
		g.centeredText(font, "Fertig", width / 2, by + 4, 0xFFFFFFFF);
	}

	/** Editor für den gewählten Eintrag: Modus-Knöpfe, Farbwahl (Box/Tracer) und RGB-Regler. */
	private void drawEditor(GuiGraphicsExtractor g, int mouseX, int mouseY, int accent) {
		EspEntry e = map().get(selected);
		int x = leftX, w = colW, y = editorTop;
		Theme.rect(g, x, y - 2, w, 1, Theme.SEPARATOR);
		Theme.text(g, font, Theme.ellipsize(font, nameOf(selected), w - 10), x + 5, y + 2, accent);
		y += ROW;

		// Modus-Knöpfe
		RenderMode[] modes = RenderMode.values();
		int bw = (w - 8 - 2 * (modes.length - 1)) / modes.length;
		for (int i = 0; i < modes.length; i++) {
			int bx = x + 4 + i * (bw + 2);
			boolean active = e.mode() == modes[i];
			boolean hov = Theme.inside(mouseX, mouseY, bx, y, bw, ROW - 1);
			Theme.rect(g, bx, y, bw, ROW - 1, active ? accent : (hov ? 0xFF3A3A48 : Theme.SLIDER_BG));
			g.centeredText(font, Theme.ellipsize(font, modes[i].buttonLabel(), bw), bx + bw / 2, y + 2, active ? 0xFFFFFFFF : Theme.TEXT_OFF);
		}
		y += ROW + 1;

		// Farbwahl: Box / Tracer
		int half = (w - 10) / 2;
		drawColorTab(g, x + 4, y, half, "Box", e.boxColor(), !editTracerColor, mouseX, mouseY, accent);
		drawColorTab(g, x + 6 + half, y, half, "Tracer", e.tracerColor(), editTracerColor, mouseX, mouseY, accent);
		y += ROW + 1;

		// RGB-Regler
		int col = editTracerColor ? e.tracerColor() : e.boxColor();
		String[] labels = {"R", "G", "B"};
		for (int ch = 0; ch < 3; ch++) {
			int cy = y + ch * ROW;
			int value = (col >> (16 - ch * 8)) & 0xFF;
			Theme.text(g, font, labels[ch], x + 5, cy + 2, Theme.TEXT_DIM);
			Theme.slider(g, x + 16, cy + 5, w - 50, value / 255.0);
			Theme.textRight(g, font, String.valueOf(value), x + w - 5, cy + 2, Theme.TEXT_DIM);
		}
	}

	private void drawColorTab(GuiGraphicsExtractor g, int x, int y, int w, String label, int color, boolean active,
			int mouseX, int mouseY, int accent) {
		boolean hov = Theme.inside(mouseX, mouseY, x, y, w, ROW - 1);
		Theme.rect(g, x, y, w, ROW - 1, active ? Theme.withAlpha(accent, 0x90) : (hov ? 0xFF3A3A48 : Theme.SLIDER_BG));
		Theme.rect(g, x + 3, y + 2, 9, 8, 0xFF000000);
		Theme.rect(g, x + 4, y + 3, 7, 6, color | 0xFF000000);
		Theme.text(g, font, label + "-Farbe", x + 15, y + 2, active ? 0xFFFFFFFF : Theme.TEXT_OFF);
	}

	/** Übersetzter Name und dahinter (grau) die ID, soweit Platz ist. */
	private void drawNameAndId(GuiGraphicsExtractor g, String id, int x, int y, int maxW, int color) {
		String name = nameOf(id);
		String sid = shortId(id);
		if (name.equalsIgnoreCase(sid) || name.equals(id)) {
			Theme.text(g, font, Theme.ellipsize(font, sid, maxW), x, y, color);
			return;
		}
		String n = Theme.ellipsize(font, name, maxW);
		Theme.text(g, font, n, x, y, color);
		int rest = maxW - font.width(n) - 6;
		if (rest > 30) Theme.text(g, font, Theme.ellipsize(font, sid, rest), x + font.width(n) + 6, y, 0xFF6A6A76);
	}

	private static String shortId(String id) {
		return id.startsWith("minecraft:") ? id.substring(10) : id;
	}

	// ------------------------------------------------------------------ Eingabe

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		int button = event.button();

		int bw = 80, bx = (width - bw) / 2, by = height - 22;
		if (Theme.inside(mx, my, bx, by, bw, 16)) {
			onClose();
			return true;
		}
		if (!search.isEmpty() && Theme.inside(mx, my, clearX, clearY, 11, 12)) {
			search.setLength(0);
			scrollRight = 0;
			return true;
		}

		// Editor des gewählten Eintrags
		if (selected != null && my >= editorTop && my < listBottom && mx >= leftX && mx < leftX + colW) {
			EspEntry e = map().get(selected);
			int y = editorTop + ROW;
			RenderMode[] modes = RenderMode.values();
			int mbw = (colW - 8 - 2 * (modes.length - 1)) / modes.length;
			for (int i = 0; i < modes.length; i++) {
				if (Theme.inside(mx, my, leftX + 4 + i * (mbw + 2), y, mbw, ROW - 1)) {
					e.mode = modes[i];
					return true;
				}
			}
			y += ROW + 1;
			int half = (colW - 10) / 2;
			if (Theme.inside(mx, my, leftX + 4, y, half, ROW - 1)) {
				editTracerColor = false;
				return true;
			}
			if (Theme.inside(mx, my, leftX + 6 + half, y, half, ROW - 1)) {
				editTracerColor = true;
				return true;
			}
			y += ROW + 1;
			for (int ch = 0; ch < 3; ch++) {
				if (Theme.inside(mx, my, leftX + 10, y + ch * ROW, colW - 40, ROW)) {
					dragChannel = ch;
					applyColor(mx);
					return true;
				}
			}
			return true;
		}

		// linke Liste
		int leftBottom = selected != null ? editorTop - 2 : listBottom;
		if (Theme.inside(mx, my, leftX, listTop, colW, leftBottom - listTop)) {
			int idx = (int) ((my - listTop + scrollLeft) / ROW);
			List<String> sel = new ArrayList<>(map().keySet());
			if (idx >= 0 && idx < sel.size()) {
				String id = sel.get(idx);
				EspEntry e = map().get(id);
				int tagX = leftX + colW - 14 - 24;
				if (mx >= leftX + colW - 12) {
					map().remove(id);
					if (id.equals(selected)) selected = null;
				} else if (mx >= tagX && mx < tagX + 24) {
					e.mode = button == 1 ? e.mode().prev() : e.mode().next();
					selected = id;
				} else {
					selected = id.equals(selected) ? null : id;
				}
			}
			return true;
		}

		// rechte Liste
		if (Theme.inside(mx, my, rightX, listTop, colW, listBottom - listTop)) {
			int idx = (int) ((my - listTop + scrollRight) / ROW);
			List<String> opts = filtered();
			if (idx >= 0 && idx < opts.size()) {
				String id = opts.get(idx);
				map().put(id, EspEntry.of(String.format("%06X", BlockEsp.defaultColor(id) & 0xFFFFFF)));
				selected = id;
				editTracerColor = false;
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void applyColor(double mx) {
		if (selected == null || dragChannel < 0) return;
		EspEntry e = map().get(selected);
		if (e == null) return;
		double t = Mth.clamp((mx - (leftX + 16)) / (double) (colW - 50), 0, 1);
		int col = editTracerColor ? e.tracerColor() : e.boxColor();
		int shift = 16 - dragChannel * 8;
		col = (col & ~(0xFF << shift)) | ((int) Math.round(t * 255) << shift);
		String hex = String.format("%06X", col & 0xFFFFFF);
		if (editTracerColor) e.tracerColor = hex;
		else e.color = hex;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragChannel >= 0) {
			applyColor(event.x());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragChannel = -1;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		int amount = (int) Math.round(scrollY * ROW * 2);
		if (mx < rightX) scrollLeft -= amount;
		else scrollRight -= amount;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isEscape()) {
			if (!search.isEmpty()) {
				search.setLength(0);
				scrollRight = 0;
			} else {
				onClose();
			}
			return true;
		}
		if (event.key() == InputConstants.KEY_DELETE) {
			search.setLength(0);
			scrollRight = 0;
			return true;
		}
		if (event.key() == InputConstants.KEY_BACKSPACE) {
			if (!search.isEmpty()) search.setLength(search.length() - 1);
			scrollRight = 0;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (event.isAllowedChatCharacter() && search.length() < 48) {
			if (search.isEmpty() && event.codepointAsString().isBlank()) return true;
			search.append(event.codepointAsString());
			scrollRight = 0;
			return true;
		}
		return super.charTyped(event);
	}
}
