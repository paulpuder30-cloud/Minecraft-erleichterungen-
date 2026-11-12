package de.erleichterungen.gui;

import com.mojang.blaze3d.platform.InputConstants;
import de.erleichterungen.ErleichterungenClient;
import de.erleichterungen.config.ModConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Click-GUI (Rechte Shift-Taste): ein verschiebbares Fenster pro Kategorie.
 * <ul>
 *   <li>Linksklick auf ein Modul: an/aus</li>
 *   <li>Rechtsklick auf ein Modul: Einstellungen auf-/zuklappen</li>
 *   <li>Mittelklick auf ein Modul: Taste neu belegen</li>
 *   <li>Fenster am Titel ziehen, Rechtsklick auf den Titel klappt es ein</li>
 *   <li>Mausrad: Fenster scrollen bzw. über einem Regler fein einstellen</li>
 * </ul>
 */
public class ClickGuiScreen extends Screen {
	private static final int PANEL_W = 128;
	private static final int HEADER_H = 16;
	private static final int ROW_H = 14;
	private static final int SET_H = 13;
	private static final int SLIDER_SET_H = 21;
	private static final int TEXT_SET_H = 26;
	private static final int PAD = 5;

	/** Bleibt über das Schließen hinweg erhalten. */
	private static final Set<String> EXPANDED = new HashSet<>();
	private static boolean layoutReset;

	private final List<Panel> panels = new ArrayList<>();
	private final List<Hit> hits = new ArrayList<>();

	private Panel dragging;
	private int dragOffX, dragOffY;
	private Hit activeSlider;
	private Setting.Text focusedText;
	private StringBuilder textBuffer;
	private Setting.Color openColor;
	private Module waitingBind;
	private String tooltip;

	public ClickGuiScreen() {
		super(Component.literal("Erleichterungen"));
	}

	public static void requestLayoutReset() {
		layoutReset = true;
	}

	private static final class Panel {
		final Module.Category category;
		final List<Module> modules = new ArrayList<>();
		int x, y;
		boolean collapsed;
		int scroll;
		int contentHeight;
		int visibleHeight;

		Panel(Module.Category category) {
			this.category = category;
		}
	}

	private enum Kind {HEADER, MODULE, BIND, BOOL, SLIDER, COLOR, COLOR_CHANNEL, TEXT, LIST, ACTION, PANEL_AREA}

	private record Hit(Kind kind, int x, int y, int w, int h, Panel panel, Module module, Setting setting, int channel) {
	}

	// ------------------------------------------------------------------ Aufbau

	@Override
	protected void init() {
		if (!panels.isEmpty() && !layoutReset) return;
		layoutReset = false;
		panels.clear();
		ModConfig cfg = ModConfig.get();
		int i = 0;
		int x = 8, y = 8;
		for (Module.Category cat : Module.Category.values()) {
			Panel p = new Panel(cat);
			for (Module m : Modules.all()) if (m.category == cat) p.modules.add(m);
			if (p.modules.isEmpty()) continue;
			int[] saved = cfg.guiPanels.get(cat.name());
			if (saved != null && saved.length >= 3) {
				p.x = saved[0];
				p.y = saved[1];
				p.collapsed = saved[2] != 0;
			} else {
				if (x + PANEL_W > width - 4 && i > 0) {
					x = 8;
					y += 160;
				}
				p.x = x;
				p.y = y;
				x += PANEL_W + 8;
			}
			clampPanel(p);
			panels.add(p);
			i++;
		}
	}

	private void clampPanel(Panel p) {
		p.x = Mth.clamp(p.x, 0, Math.max(0, width - PANEL_W));
		p.y = Mth.clamp(p.y, 0, Math.max(0, height - HEADER_H));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		ModConfig cfg = ModConfig.get();
		for (Panel p : panels) {
			cfg.guiPanels.put(p.category.name(), new int[]{p.x, p.y, p.collapsed ? 1 : 0});
		}
		ModConfig.save();
	}

	// ------------------------------------------------------------------ Zeichnen

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, width, height, 0x60000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		hits.clear();
		tooltip = null;
		for (Panel p : panels) drawPanel(g, p, mouseX, mouseY);

		// Fußzeile mit Hilfe bzw. Beschreibung
		String help = waitingBind != null
				? "Taste für " + waitingBind.name + " drücken – Entf/Rücktaste = keine Taste, Esc = abbrechen"
				: "Links: an/aus · Rechts: Einstellungen · Mitte: Taste belegen · Titel ziehen zum Verschieben";
		String line = tooltip != null ? tooltip : help;
		int tw = Math.min(width - 20, font.width(line) + 10);
		int tx = (width - tw) / 2;
		int ty = height - 18;
		Theme.rect(g, tx, ty, tw, 14, 0xD0101014);
		Theme.rect(g, tx, ty + 13, tw, 1, Theme.accent());
		Theme.text(g, font, Theme.ellipsize(font, line, tw - 10), tx + 5, ty + 3, tooltip != null ? Theme.TEXT : Theme.TEXT_DIM);

		Theme.text(g, font, "Erleichterungen", width - font.width("Erleichterungen") - 6, height - 30, Theme.accent());
	}

	private void drawPanel(GuiGraphicsExtractor g, Panel p, int mouseX, int mouseY) {
		int accent = Theme.accent();
		// Titel
		Theme.rect(g, p.x, p.y, PANEL_W, HEADER_H, accent);
		Theme.text(g, font, p.category.title, p.x + PAD, p.y + 4, 0xFFFFFFFF);
		long active = p.modules.stream().filter(Module::isEnabled).count();
		Theme.textRight(g, font, (p.collapsed ? "+ " : "") + active + "/" + p.modules.stream().filter(Module::hasToggle).count(),
				p.x + PANEL_W - PAD, p.y + 4, 0xDDFFFFFF);
		hits.add(new Hit(Kind.HEADER, p.x, p.y, PANEL_W, HEADER_H, p, null, null, 0));
		if (p.collapsed) {
			Theme.rect(g, p.x, p.y + HEADER_H, PANEL_W, 1, Theme.withAlpha(accent, 0x80));
			return;
		}

		int top = p.y + HEADER_H;
		int maxBottom = height - 22;
		p.contentHeight = contentHeight(p);
		p.visibleHeight = Math.max(ROW_H, Math.min(p.contentHeight, maxBottom - top));
		p.scroll = Mth.clamp(p.scroll, 0, Math.max(0, p.contentHeight - p.visibleHeight));
		hits.add(new Hit(Kind.PANEL_AREA, p.x, top, PANEL_W, p.visibleHeight, p, null, null, 0));

		Theme.rect(g, p.x, top, PANEL_W, p.visibleHeight, Theme.PANEL_BG);
		g.enableScissor(p.x, top, p.x + PANEL_W, top + p.visibleHeight);
		int y = top - p.scroll;
		for (Module m : p.modules) {
			y = drawModule(g, p, m, y, top, top + p.visibleHeight, mouseX, mouseY);
		}
		g.disableScissor();
		Theme.rect(g, p.x, top + p.visibleHeight, PANEL_W, 1, Theme.withAlpha(accent, 0x80));

		// Scrollbalken
		if (p.contentHeight > p.visibleHeight) {
			int barH = Math.max(10, p.visibleHeight * p.visibleHeight / p.contentHeight);
			int barY = top + (p.visibleHeight - barH) * p.scroll / Math.max(1, p.contentHeight - p.visibleHeight);
			Theme.rect(g, p.x + PANEL_W - 2, barY, 2, barH, Theme.withAlpha(accent, 0xA0));
		}
	}

	private int contentHeight(Panel p) {
		int h = 0;
		for (Module m : p.modules) {
			h += ROW_H;
			if (EXPANDED.contains(m.id)) {
				if (m.key.get() != null) h += SET_H;
				for (Setting s : m.settings) h += settingHeight(s);
				h += 2;
			}
		}
		return h;
	}

	private int settingHeight(Setting s) {
		if (!s.visible.getAsBoolean()) return 0;
		return switch (s) {
			case Setting.Int ignored -> SLIDER_SET_H;
			case Setting.Dec ignored -> SLIDER_SET_H;
			case Setting.Text ignored -> TEXT_SET_H;
			case Setting.Color c -> SET_H + (openColor == c ? 3 * SET_H : 0);
			default -> SET_H;
		};
	}

	/** Trägt eine Klickfläche nur ein, wenn sie im sichtbaren Bereich liegt. */
	private void hit(Kind kind, int x, int y, int w, int h, int clipTop, int clipBottom, Panel p, Module m, Setting s, int channel) {
		int y1 = Math.max(y, clipTop), y2 = Math.min(y + h, clipBottom);
		if (y2 > y1) hits.add(new Hit(kind, x, y1, w, y2 - y1, p, m, s, channel));
	}

	private boolean hovered(int mx, int my, int x, int y, int w, int h, int clipTop, int clipBottom) {
		return my >= clipTop && my < clipBottom && Theme.inside(mx, my, x, y, w, h);
	}

	private int drawModule(GuiGraphicsExtractor g, Panel p, Module m, int y, int clipTop, int clipBottom, int mx, int my) {
		int accent = Theme.accent();
		boolean hover = hovered(mx, my, p.x, y, PANEL_W, ROW_H, clipTop, clipBottom);
		boolean expanded = EXPANDED.contains(m.id);
		Theme.rect(g, p.x, y, PANEL_W, ROW_H, Theme.ROW_BG);
		if (m.isEnabled()) {
			Theme.rect(g, p.x, y, PANEL_W, ROW_H, Theme.withAlpha(accent, 0x38));
			Theme.rect(g, p.x, y, 2, ROW_H, accent);
		}
		if (hover) {
			Theme.rect(g, p.x, y, PANEL_W, ROW_H, Theme.HOVER);
			tooltip = m.description;
		}
		int color = !m.hasToggle() ? Theme.TEXT : m.isEnabled() ? 0xFFFFFFFF : Theme.TEXT_OFF;
		Theme.text(g, font, m.name, p.x + PAD + 1, y + 3, color);
		if (!m.settings.isEmpty() || m.key.get() != null) {
			Theme.textRight(g, font, expanded ? "-" : "+", p.x + PANEL_W - PAD, y + 3, Theme.TEXT_DIM);
		}
		hit(Kind.MODULE, p.x, y, PANEL_W, ROW_H, clipTop, clipBottom, p, m, null, 0);
		y += ROW_H;

		if (!expanded) return y;
		int sx = p.x + 4;
		int sw = PANEL_W - 8;

		// Tastenbelegung
		KeyMapping key = m.key.get();
		if (key != null) {
			Theme.rect(g, p.x, y, PANEL_W, SET_H, Theme.SETTING_BG);
			if (hovered(mx, my, p.x, y, PANEL_W, SET_H, clipTop, clipBottom)) {
				Theme.rect(g, p.x, y, PANEL_W, SET_H, Theme.HOVER);
				tooltip = "Klicken, um die Taste neu zu belegen.";
			}
			Theme.text(g, font, "Taste", sx + 2, y + 2, Theme.TEXT_DIM);
			String keyName = waitingBind == m ? "[...]" : key.isUnbound() ? "keine" : key.getTranslatedKeyMessage().getString();
			Theme.textRight(g, font, keyName, sx + sw - 2, y + 2, waitingBind == m ? accent : Theme.TEXT);
			hit(Kind.BIND, p.x, y, PANEL_W, SET_H, clipTop, clipBottom, p, m, null, 0);
			y += SET_H;
		}

		for (Setting s : m.settings) {
			if (!s.visible.getAsBoolean()) continue;
			int h = settingHeight(s);
			Theme.rect(g, p.x, y, PANEL_W, h, Theme.SETTING_BG);
			Theme.rect(g, p.x, y, 1, h, Theme.withAlpha(accent, 0x60));
			boolean hov = hovered(mx, my, p.x, y, PANEL_W, h, clipTop, clipBottom);
			if (hov) tooltip = s.description;
			switch (s) {
				case Setting.Bool b -> {
					if (hov) Theme.rect(g, p.x, y, PANEL_W, h, Theme.HOVER);
					Theme.text(g, font, s.name, sx + 2, y + 2, Theme.TEXT);
					Theme.checkbox(g, sx + sw - 11, y + 2, b.get.get());
					hit(Kind.BOOL, p.x, y, PANEL_W, h, clipTop, clipBottom, p, m, s, 0);
				}
				case Setting.Int in -> {
					Theme.text(g, font, s.name, sx + 2, y + 2, Theme.TEXT);
					Theme.textRight(g, font, String.valueOf(in.get.get()), sx + sw - 2, y + 2, accent);
					double t = (in.get.get() - in.min) / (double) (in.max - in.min);
					Theme.slider(g, sx + 2, y + 14, sw - 4, t);
					hit(Kind.SLIDER, sx + 2, y + 11, sw - 4, 9, clipTop, clipBottom, p, m, s, 0);
				}
				case Setting.Dec d -> {
					Theme.text(g, font, s.name, sx + 2, y + 2, Theme.TEXT);
					Theme.textRight(g, font, String.format(Locale.ROOT, "%.1f", d.get.get()), sx + sw - 2, y + 2, accent);
					double t = (d.get.get() - d.min) / (double) (d.max - d.min);
					Theme.slider(g, sx + 2, y + 14, sw - 4, t);
					hit(Kind.SLIDER, sx + 2, y + 11, sw - 4, 9, clipTop, clipBottom, p, m, s, 0);
				}
				case Setting.Color c -> {
					if (hovered(mx, my, p.x, y, PANEL_W, SET_H, clipTop, clipBottom)) Theme.rect(g, p.x, y, PANEL_W, SET_H, Theme.HOVER);
					int col = ModConfig.parseColor(c.get.get(), 0xFFFFFFFF) | 0xFF000000;
					Theme.text(g, font, s.name, sx + 2, y + 2, Theme.TEXT);
					Theme.rect(g, sx + sw - 15, y + 2, 13, 9, 0xFF000000);
					Theme.rect(g, sx + sw - 14, y + 3, 11, 7, col);
					hit(Kind.COLOR, p.x, y, PANEL_W, SET_H, clipTop, clipBottom, p, m, s, 0);
					if (openColor == c) {
						String[] labels = {"R", "G", "B"};
						for (int ch = 0; ch < 3; ch++) {
							int cy = y + SET_H * (ch + 1);
							int value = (col >> (16 - ch * 8)) & 0xFF;
							Theme.text(g, font, labels[ch], sx + 4, cy + 2, Theme.TEXT_DIM);
							Theme.slider(g, sx + 14, cy + 5, sw - 40, value / 255.0);
							Theme.textRight(g, font, String.valueOf(value), sx + sw - 2, cy + 2, Theme.TEXT_DIM);
							hit(Kind.COLOR_CHANNEL, sx + 14, cy + 1, sw - 40, 11, clipTop, clipBottom, p, m, s, ch);
						}
					}
				}
				case Setting.Text t -> {
					Theme.text(g, font, s.name, sx + 2, y + 2, Theme.TEXT);
					boolean focused = focusedText == t;
					Theme.rect(g, sx + 2, y + 12, sw - 4, 12, focused ? 0xFF2E2E3A : Theme.SLIDER_BG);
					if (focused) Theme.rect(g, sx + 2, y + 23, sw - 4, 1, accent);
					String value = focused ? textBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "") : t.get.get();
					Theme.text(g, font, Theme.ellipsize(font, value == null ? "" : value, sw - 10), sx + 5, y + 14, Theme.TEXT);
					hit(Kind.TEXT, sx + 2, y + 12, sw - 4, 12, clipTop, clipBottom, p, m, s, 0);
				}
				case Setting.ListEdit l -> {
					if (hov) Theme.rect(g, p.x, y, PANEL_W, h, Theme.HOVER);
					Theme.text(g, font, s.name, sx + 2, y + 2, Theme.TEXT);
					Theme.textRight(g, font, l.map.get().size() + " >", sx + sw - 2, y + 2, accent);
					hit(Kind.LIST, p.x, y, PANEL_W, h, clipTop, clipBottom, p, m, s, 0);
				}
				case Setting.Action a -> {
					Theme.rect(g, sx + 2, y + 1, sw - 4, h - 2, Theme.withAlpha(accent, hov ? 0x90 : 0x50));
					g.centeredText(font, s.name, sx + sw / 2, y + 2, 0xFFFFFFFF);
					hit(Kind.ACTION, p.x, y, PANEL_W, h, clipTop, clipBottom, p, m, s, 0);
				}
			}
			y += h;
		}
		Theme.rect(g, p.x, y, PANEL_W, 2, Theme.SETTING_BG);
		return y + 2;
	}

	// ------------------------------------------------------------------ Eingabe

	private Hit hitAt(double mx, double my) {
		// Rückwärts suchen: Panels, die später gezeichnet wurden, liegen oben
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (h.kind == Kind.PANEL_AREA) continue;
			if (Theme.inside(mx, my, h.x, h.y, h.w, h.h)) return h;
		}
		return null;
	}

	private Panel panelAt(double mx, double my) {
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if ((h.kind == Kind.PANEL_AREA || h.kind == Kind.HEADER) && Theme.inside(mx, my, h.x, h.y, h.w, h.h)) return h.panel;
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		int button = event.button();
		commitText();
		if (waitingBind != null) {
			waitingBind = null;
			return true;
		}
		Hit h = hitAt(mx, my);
		if (h == null) return super.mouseClicked(event, doubleClick);

		// angeklicktes Panel nach vorne holen
		panels.remove(h.panel);
		panels.add(h.panel);

		switch (h.kind) {
			case HEADER -> {
				if (button == 1) {
					h.panel.collapsed = !h.panel.collapsed;
				} else if (button == 0) {
					dragging = h.panel;
					dragOffX = (int) mx - h.panel.x;
					dragOffY = (int) my - h.panel.y;
				}
			}
			case MODULE -> {
				Module m = h.module;
				if (button == 0 && m.hasToggle()) {
					m.toggle.run();
				} else if (button == 1 || (button == 0 && !m.hasToggle())) {
					if (!EXPANDED.remove(m.id)) EXPANDED.add(m.id);
				} else if (button == 2 && m.key.get() != null) {
					waitingBind = m;
				}
			}
			case BIND -> waitingBind = h.module;
			case BOOL -> {
				Setting.Bool b = (Setting.Bool) h.setting;
				b.set.accept(!b.get.get());
				b.onChange.run();
			}
			case SLIDER, COLOR_CHANNEL -> {
				activeSlider = h;
				applySlider(h, mx);
			}
			case COLOR -> openColor = openColor == h.setting ? null : (Setting.Color) h.setting;
			case TEXT -> {
				focusedText = (Setting.Text) h.setting;
				String v = focusedText.get.get();
				textBuffer = new StringBuilder(v == null ? "" : v);
			}
			case LIST -> minecraft.setScreenAndShow(new ListEditScreen(this, (Setting.ListEdit) h.setting));
			case ACTION -> {
				((Setting.Action) h.setting).action.run();
				h.setting.onChange.run();
				if (layoutReset) init();
			}
			default -> {
			}
		}
		return true;
	}

	private void applySlider(Hit h, double mx) {
		double t = Mth.clamp((mx - h.x) / (double) h.w, 0.0, 1.0);
		switch (h.setting) {
			case Setting.Int in -> in.set.accept((int) Math.round(in.min + t * (in.max - in.min)));
			case Setting.Dec d -> {
				float v = d.min + (float) t * (d.max - d.min);
				v = Math.round(v / d.step) * d.step;
				d.set.accept(Mth.clamp(v, d.min, d.max));
			}
			case Setting.Color c -> {
				int col = ModConfig.parseColor(c.get.get(), 0xFFFFFFFF);
				int shift = 16 - h.channel * 8;
				int value = (int) Math.round(t * 255);
				col = (col & ~(0xFF << shift)) | (value << shift);
				c.set.accept(String.format("%06X", col & 0xFFFFFF));
			}
			default -> {
				return;
			}
		}
		h.setting.onChange.run();
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			dragging.x = (int) event.x() - dragOffX;
			dragging.y = (int) event.y() - dragOffY;
			clampPanel(dragging);
			return true;
		}
		if (activeSlider != null) {
			applySlider(activeSlider, event.x());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		activeSlider = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		Hit h = hitAt(mx, my);
		if (h != null && h.kind == Kind.SLIDER) {
			int dir = scrollY > 0 ? 1 : -1;
			switch (h.setting) {
				case Setting.Int in -> {
					int step = Math.max(1, (in.max - in.min) / 200);
					in.set.accept(Mth.clamp(in.get.get() + dir * step, in.min, in.max));
				}
				case Setting.Dec d -> d.set.accept(Mth.clamp(Math.round((d.get.get() + dir * d.step) / d.step) * d.step, d.min, d.max));
				default -> {
				}
			}
			h.setting.onChange.run();
			return true;
		}
		Panel p = panelAt(mx, my);
		if (p != null) {
			p.scroll -= (int) Math.round(scrollY * 16);
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (waitingBind != null) {
			KeyMapping key = waitingBind.key.get();
			if (key != null && !event.isEscape()) {
				boolean clear = event.key() == InputConstants.KEY_BACKSPACE || event.key() == InputConstants.KEY_DELETE;
				key.setKey(clear ? InputConstants.UNKNOWN : InputConstants.getKey(event));
				KeyMapping.resetMapping();
				minecraft.options.save();
			}
			waitingBind = null;
			return true;
		}
		if (focusedText != null) {
			if (event.isEscape()) {
				focusedText = null;
			} else if (event.isConfirmation()) {
				commitText();
			} else if (event.key() == InputConstants.KEY_BACKSPACE && !textBuffer.isEmpty()) {
				textBuffer.setLength(textBuffer.length() - 1);
			}
			return true;
		}
		KeyMapping gui = ErleichterungenClient.keyFor("gui");
		if (gui != null && gui.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (focusedText != null && event.isAllowedChatCharacter() && textBuffer.length() < 64) {
			textBuffer.append(event.codepointAsString());
			return true;
		}
		return super.charTyped(event);
	}

	private void commitText() {
		if (focusedText == null) return;
		focusedText.set.accept(textBuffer.toString().trim());
		focusedText.onChange.run();
		focusedText = null;
	}
}
