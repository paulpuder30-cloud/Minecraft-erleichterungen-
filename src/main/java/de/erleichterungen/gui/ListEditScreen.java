package de.erleichterungen.gui;

import com.mojang.blaze3d.platform.InputConstants;
import de.erleichterungen.config.ModConfig;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bearbeitet eine ID→Farbe-Liste (Block-ESP-Blöcke bzw. Entity-ESP-Typen).
 * Links: ausgewählte Einträge (Klick = Farbe bearbeiten, x = entfernen).
 * Rechts: Suche über alle verfügbaren IDs (Klick = hinzufügen).
 */
public class ListEditScreen extends Screen {
	private static final int ROW = 13;

	private final Screen parent;
	private final Setting.ListEdit setting;
	private final Collection<String> allOptions;
	private final StringBuilder search = new StringBuilder();

	private String selected;
	private int scrollLeft, scrollRight;
	private int dragChannel = -1;

	// Layout (in extractRenderState berechnet)
	private int colW, leftX, rightX, listTop, listBottom, colorTop;

	public ListEditScreen(Screen parent, Setting.ListEdit setting) {
		super(Component.literal(setting.name));
		this.parent = parent;
		this.setting = setting;
		this.allOptions = setting.options.get();
	}

	private Map<String, String> map() {
		return setting.map.get();
	}

	private List<String> selectedList() {
		return new ArrayList<>(map().keySet());
	}

	private List<String> filtered() {
		String q = search.toString().toLowerCase(Locale.ROOT).trim();
		List<String> out = new ArrayList<>();
		for (String id : allOptions) {
			if (map().containsKey(id)) continue;
			if (q.isEmpty() || id.contains(q)) out.add(id);
		}
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

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int accent = Theme.accent();
		int totalW = Math.min(420, width - 20);
		colW = (totalW - 10) / 2;
		leftX = (width - totalW) / 2;
		rightX = leftX + colW + 10;
		int top = 20;
		listTop = top + 34;
		colorTop = height - 30 - 3 * ROW - 6;
		listBottom = height - 30;

		// Titel
		g.centeredText(font, setting.name + " bearbeiten", width / 2, 6, accent);

		// ---- linke Spalte: ausgewählt
		Theme.rect(g, leftX, top, colW, 16, accent);
		Theme.text(g, font, "Ausgewählt (" + map().size() + ")", leftX + 5, top + 4, 0xFFFFFFFF);
		Theme.rect(g, leftX, top + 16, colW, listBottom - top - 16, Theme.PANEL_BG);
		Theme.text(g, font, "Klick = Farbe · x = entfernen", leftX + 5, top + 21, Theme.TEXT_DIM);

		int leftBottom = selected != null ? colorTop - 2 : listBottom;
		List<String> sel = selectedList();
		scrollLeft = Mth.clamp(scrollLeft, 0, Math.max(0, sel.size() * ROW - (leftBottom - listTop)));
		g.enableScissor(leftX, listTop, leftX + colW, leftBottom);
		int y = listTop - scrollLeft;
		for (String id : sel) {
			boolean hov = Theme.inside(mouseX, mouseY, leftX, y, colW, ROW) && mouseY >= listTop && mouseY < leftBottom;
			if (id.equals(selected)) Theme.rect(g, leftX, y, colW, ROW, Theme.withAlpha(accent, 0x50));
			else if (hov) Theme.rect(g, leftX, y, colW, ROW, Theme.HOVER);
			int col = ModConfig.parseColor(map().get(id), 0xFFFFFFFF) | 0xFF000000;
			Theme.rect(g, leftX + 4, y + 3, 7, 7, col);
			Theme.text(g, font, Theme.ellipsize(font, shortId(id), colW - 30), leftX + 15, y + 2, Theme.TEXT);
			boolean hovX = Theme.inside(mouseX, mouseY, leftX + colW - 12, y, 12, ROW);
			Theme.text(g, font, "x", leftX + colW - 9, y + 2, hovX ? 0xFFFF5555 : Theme.TEXT_DIM);
			y += ROW;
		}
		g.disableScissor();

		// Farbregler für den ausgewählten Eintrag
		if (selected != null && map().containsKey(selected)) {
			int col = ModConfig.parseColor(map().get(selected), 0xFFFFFFFF);
			Theme.rect(g, leftX, colorTop - 2, colW, 1, Theme.SEPARATOR);
			String[] labels = {"R", "G", "B"};
			for (int ch = 0; ch < 3; ch++) {
				int cy = colorTop + ch * ROW;
				int value = (col >> (16 - ch * 8)) & 0xFF;
				Theme.text(g, font, labels[ch], leftX + 5, cy + 2, Theme.TEXT_DIM);
				Theme.slider(g, leftX + 16, cy + 5, colW - 50, value / 255.0);
				Theme.textRight(g, font, String.valueOf(value), leftX + colW - 5, cy + 2, Theme.TEXT_DIM);
			}
		} else {
			selected = null;
		}

		// ---- rechte Spalte: hinzufügen
		Theme.rect(g, rightX, top, colW, 16, accent);
		Theme.text(g, font, "Hinzufügen", rightX + 5, top + 4, 0xFFFFFFFF);
		Theme.rect(g, rightX, top + 16, colW, listBottom - top - 16, Theme.PANEL_BG);
		// Suchfeld
		Theme.rect(g, rightX + 4, top + 19, colW - 8, 12, 0xFF2E2E3A);
		Theme.rect(g, rightX + 4, top + 30, colW - 8, 1, accent);
		String shown = search.isEmpty() ? "Suchen..." : search + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
		Theme.text(g, font, Theme.ellipsize(font, shown, colW - 14), rightX + 7, top + 21, search.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT);

		List<String> opts = filtered();
		scrollRight = Mth.clamp(scrollRight, 0, Math.max(0, opts.size() * ROW - (listBottom - listTop)));
		g.enableScissor(rightX, listTop, rightX + colW, listBottom);
		y = listTop - scrollRight;
		int first = Math.max(0, scrollRight / ROW);
		y += first * ROW;
		for (int i = first; i < opts.size() && y < listBottom; i++) {
			String id = opts.get(i);
			boolean hov = Theme.inside(mouseX, mouseY, rightX, y, colW, ROW) && mouseY >= listTop && mouseY < listBottom;
			if (hov) Theme.rect(g, rightX, y, colW, ROW, Theme.HOVER);
			Theme.text(g, font, Theme.ellipsize(font, shortId(id), colW - 20), rightX + 6, y + 2, Theme.TEXT_OFF);
			if (hov) Theme.text(g, font, "+", rightX + colW - 10, y + 2, accent);
			y += ROW;
		}
		g.disableScissor();

		// Fertig-Knopf
		int bw = 80, bx = (width - bw) / 2, by = height - 24;
		boolean hovDone = Theme.inside(mouseX, mouseY, bx, by, bw, 16);
		Theme.rect(g, bx, by, bw, 16, Theme.withAlpha(accent, hovDone ? 0xFF : 0xB0));
		g.centeredText(font, "Fertig", width / 2, by + 4, 0xFFFFFFFF);
	}

	private static String shortId(String id) {
		return id.startsWith("minecraft:") ? id.substring(10) : id;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		// Fertig
		int bw = 80, bx = (width - bw) / 2, by = height - 24;
		if (Theme.inside(mx, my, bx, by, bw, 16)) {
			onClose();
			return true;
		}
		// Farbregler
		if (selected != null) {
			for (int ch = 0; ch < 3; ch++) {
				if (Theme.inside(mx, my, leftX + 16, colorTop + ch * ROW, colW - 50, ROW)) {
					dragChannel = ch;
					applyColor(mx);
					return true;
				}
			}
		}
		// linke Liste
		int leftBottom = selected != null ? colorTop - 2 : listBottom;
		if (Theme.inside(mx, my, leftX, listTop, colW, leftBottom - listTop)) {
			int idx = (int) ((my - listTop + scrollLeft) / ROW);
			List<String> sel = selectedList();
			if (idx >= 0 && idx < sel.size()) {
				String id = sel.get(idx);
				if (mx >= leftX + colW - 12) {
					map().remove(id);
					if (id.equals(selected)) selected = null;
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
				map().put(id, String.format("%06X", BlockEsp.defaultColor(id) & 0xFFFFFF));
				selected = id;
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void applyColor(double mx) {
		if (selected == null || dragChannel < 0) return;
		double t = Mth.clamp((mx - (leftX + 16)) / (double) (colW - 50), 0, 1);
		int col = ModConfig.parseColor(map().get(selected), 0xFFFFFFFF);
		int shift = 16 - dragChannel * 8;
		col = (col & ~(0xFF << shift)) | ((int) Math.round(t * 255) << shift);
		map().put(selected, String.format("%06X", col & 0xFFFFFF));
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
			onClose();
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
			search.append(event.codepointAsString().toLowerCase(Locale.ROOT));
			scrollRight = 0;
			return true;
		}
		return super.charTyped(event);
	}
}
