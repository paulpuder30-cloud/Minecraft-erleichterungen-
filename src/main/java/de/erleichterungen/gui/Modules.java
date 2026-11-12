package de.erleichterungen.gui;

import de.erleichterungen.ErleichterungenClient;
import de.erleichterungen.config.ModConfig;
import de.erleichterungen.feature.AntiVanish;
import de.erleichterungen.feature.BlockEsp;
import de.erleichterungen.feature.EntityEsp;
import de.erleichterungen.feature.Freecam;
import de.erleichterungen.feature.GeodeTracker;
import de.erleichterungen.gui.Module.Category;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Alle Module mit ihren Einstellungen – Grundlage für die Click-GUI.
 */
public final class Modules {
	private static List<Module> all;

	private Modules() {
	}

	private static ModConfig c() {
		return ModConfig.get();
	}

	private static Module toggleable(String id, String name, Category cat, String desc, java.util.function.BooleanSupplier enabled) {
		return new Module(id, name, cat, desc, enabled, () -> ErleichterungenClient.toggle(id, false),
				() -> ErleichterungenClient.keyFor(id));
	}

	private static Collection<String> blockIds() {
		List<String> ids = new ArrayList<>();
		for (Identifier id : BuiltInRegistries.BLOCK.keySet()) ids.add(id.toString());
		ids.sort(null);
		return ids;
	}

	private static Collection<String> entityIds() {
		List<String> ids = new ArrayList<>();
		for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) ids.add(id.toString());
		ids.sort(null);
		return ids;
	}

	public static List<Module> all() {
		if (all != null) return all;
		List<Module> list = new ArrayList<>();

		// ------------------------------------------------------------------ Render
		list.add(toggleable("blockesp", "Block-ESP", Category.RENDER,
				"Markiert ausgewählte Blöcke (Erze, Spawner, ...) durch Wände.", () -> c().blockEspEnabled)
				.add(new Setting.ListEdit("Blöcke", "Welche Blöcke markiert werden (mit Farbe).",
						() -> c().blockEspBlocks, Modules::blockIds).onChange(BlockEsp::rebuildFilter))
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 512,
						() -> c().blockEspRange, v -> c().blockEspRange = v))
				.add(new Setting.Int("Max. Boxen", "Obergrenze gleichzeitig gezeichneter Blöcke.", 10, 20000,
						() -> c().blockEspMaxBoxes, v -> c().blockEspMaxBoxes = v))
				.add(new Setting.Bool("Boxen", "Umrisse um die Blöcke zeichnen.",
						() -> c().blockEspBoxes, v -> c().blockEspBoxes = v))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().blockEspFill, v -> c().blockEspFill = v).visibleIf(() -> c().blockEspBoxes))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().blockEspLineWidth, v -> c().blockEspLineWidth = v).visibleIf(() -> c().blockEspBoxes))
				.add(new Setting.Bool("Tracer", "Linien vom Fadenkreuz zu den Blöcken.",
						() -> c().blockEspTracers, v -> c().blockEspTracers = v)));

		list.add(toggleable("entityesp", "Entity-ESP", Category.RENDER,
				"Markiert Base-typische Entities: Item Frames, Rüstungsständer, Loren, ...", () -> c().entityEspEnabled)
				.add(new Setting.ListEdit("Entities", "Welche Entity-Typen markiert werden (mit Farbe).",
						() -> c().entityEspTypes, Modules::entityIds).onChange(EntityEsp::rebuildFilter))
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 512,
						() -> c().entityEspRange, v -> c().entityEspRange = v))
				.add(new Setting.Int("Max. Boxen", "Obergrenze gleichzeitig gezeichneter Entities.", 10, 20000,
						() -> c().entityEspMaxBoxes, v -> c().entityEspMaxBoxes = v))
				.add(new Setting.Bool("Boxen", "Umrisse um die Entities zeichnen.",
						() -> c().entityEspBoxes, v -> c().entityEspBoxes = v))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().entityEspFill, v -> c().entityEspFill = v).visibleIf(() -> c().entityEspBoxes))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().entityEspLineWidth, v -> c().entityEspLineWidth = v).visibleIf(() -> c().entityEspBoxes))
				.add(new Setting.Bool("Beschriftung", "Name bzw. Gegenstand im Item Frame + Entfernung anzeigen.",
						() -> c().entityEspLabels, v -> c().entityEspLabels = v))
				.add(new Setting.Bool("Tracer", "Linien vom Fadenkreuz zu den Entities.",
						() -> c().entityEspTracers, v -> c().entityEspTracers = v)));

		list.add(toggleable("playeresp", "Player-ESP", Category.RENDER,
				"Boxen, Namen und Entfernung aller Spieler durch Wände.", () -> c().playerEspEnabled)
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 1024,
						() -> c().playerEspRange, v -> c().playerEspRange = v))
				.add(new Setting.Bool("Boxen", "Umrisse um die Spieler zeichnen.",
						() -> c().playerEspBoxes, v -> c().playerEspBoxes = v))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().playerEspFill, v -> c().playerEspFill = v).visibleIf(() -> c().playerEspBoxes))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().playerEspLineWidth, v -> c().playerEspLineWidth = v).visibleIf(() -> c().playerEspBoxes))
				.add(new Setting.Bool("Namen", "Name und Entfernung über dem Spieler.",
						() -> c().playerEspNames, v -> c().playerEspNames = v))
				.add(new Setting.Bool("Tracer", "Linien vom Fadenkreuz zu den Spielern.",
						() -> c().playerEspTracers, v -> c().playerEspTracers = v))
				.add(new Setting.Color("Farbe", "Farbe für normale Spieler.",
						() -> c().playerEspColor, v -> c().playerEspColor = v))
				.add(new Setting.Color("Vanish-Farbe", "Farbe für Spieler, die Anti-Vanish als versteckt erkannt hat.",
						() -> c().playerEspVanishedColor, v -> c().playerEspVanishedColor = v)));

		list.add(new Module("tracers", "Tracer", Category.RENDER,
				"Gemeinsame Einstellungen für alle Tracer-Linien.", null, null, () -> null)
				.add(new Setting.Dec("Linienbreite", "Breite aller Tracer-Linien.", 0.5f, 10f, 0.5f,
						() -> c().tracerWidth, v -> c().tracerWidth = v))
				.add(new Setting.Action("Alle an", "Schaltet die Tracer aller Module ein.", () -> setAllTracers(true)))
				.add(new Setting.Action("Alle aus", "Schaltet die Tracer aller Module aus.", () -> setAllTracers(false))));

		// ------------------------------------------------------------------ Welt
		list.add(toggleable("geode", "Geoden-Tracker", Category.WORLD,
				"Zeigt Amethyst-Knospen nach Wachstumsstufe und meldet fertige Cluster.", () -> c().geodeTrackerEnabled)
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 512,
						() -> c().geodeRange, v -> c().geodeRange = v))
				.add(new Setting.Bool("Boxen", "Knospen und Cluster umranden.",
						() -> c().geodeBoxes, v -> c().geodeBoxes = v))
				.add(new Setting.Bool("Knospender Amethyst", "Knospende Amethystblöcke (lila) anzeigen.",
						() -> c().geodeShowBudding, v -> c().geodeShowBudding = v).visibleIf(() -> c().geodeBoxes))
				.add(new Setting.Bool("Wachsende Knospen", "Auch noch nicht fertige Knospen anzeigen.",
						() -> c().geodeShowGrowing, v -> c().geodeShowGrowing = v).visibleIf(() -> c().geodeBoxes))
				.add(new Setting.Bool("Tracer", "Linien zu erntebereiten Clustern.",
						() -> c().geodeTracers, v -> c().geodeTracers = v))
				.add(new Setting.Bool("Benachrichtigung", "Chat-Nachricht, wenn ein Cluster ausgewachsen ist.",
						() -> c().geodeNotifyFullyGrown, v -> c().geodeNotifyFullyGrown = v))
				.add(new Setting.Bool("Sound", "Ton bei ausgewachsenem Cluster.",
						() -> c().geodeSound, v -> c().geodeSound = v))
				.add(new Setting.Action("Statistik zurücksetzen", "Setzt die Wachstums-Statistik zurück.", GeodeTracker::reset)));

		// ------------------------------------------------------------------ Spieler
		list.add(new Module("freecam", "Freecam", Category.PLAYER,
				"Kamera frei bewegen, der Spieler bleibt stehen. WASD, Leertaste/Shift hoch/runter, Sprint = schneller.",
				Freecam::isActive, () -> ErleichterungenClient.toggle("freecam", false), () -> ErleichterungenClient.keyFor("freecam"))
				.add(new Setting.Dec("Geschwindigkeit", "Blöcke pro Tick (mit Sprint x2.5).", 0.1f, 5f, 0.1f,
						() -> c().freecamSpeed, v -> c().freecamSpeed = v)));

		// ------------------------------------------------------------------ Sonstiges
		list.add(toggleable("antivanish", "Anti-Vanish", Category.MISC,
				"Erkennt Spieler, die sich per Vanish verstecken.", () -> c().antiVanishEnabled)
				.add(new Setting.Bool("Leave-Nachricht", "Melden, wenn jemand ohne Leave-Nachricht aus der Tabliste verschwindet.",
						() -> c().antiVanishLeaveMessageCheck, v -> c().antiVanishLeaveMessageCheck = v))
				.add(new Setting.Int("Leave-Timeout (ms)", "Wie lange auf die Leave-Nachricht gewartet wird.", 250, 30000,
						() -> c().antiVanishLeaveTimeoutMs, v -> c().antiVanishLeaveTimeoutMs = v)
						.visibleIf(() -> c().antiVanishLeaveMessageCheck))
				.add(new Setting.Bool("Tab-Vervollständigung", "Befehls-Vorschläge mit der Tabliste vergleichen.",
						() -> c().antiVanishCompletionCheck, v -> c().antiVanishCompletionCheck = v))
				.add(new Setting.Text("Befehl", "Befehl, dessen Vorschläge abgefragt werden (ohne /).",
						() -> c().antiVanishCompletionCommand, v -> c().antiVanishCompletionCommand = v)
						.visibleIf(() -> c().antiVanishCompletionCheck))
				.add(new Setting.Int("Intervall (s)", "Abstand zwischen den Abfragen.", 2, 600,
						() -> c().antiVanishCompletionIntervalSec, v -> c().antiVanishCompletionIntervalSec = v)
						.visibleIf(() -> c().antiVanishCompletionCheck))
				.add(new Setting.Bool("Entity-Prüfung", "Sichtbare Spieler, die in der Tabliste fehlen.",
						() -> c().antiVanishEntityCheck, v -> c().antiVanishEntityCheck = v))
				.add(new Setting.Bool("Zuschauermodus", "Melden, wenn jemand in den Spectator-Modus wechselt.",
						() -> c().antiVanishSpectatorCheck, v -> c().antiVanishSpectatorCheck = v))
				.add(new Setting.Bool("Sound", "Ton bei Erkennung.",
						() -> c().antiVanishSound, v -> c().antiVanishSound = v))
				.add(new Setting.Bool("Boxen", "Box um sichtbare Verdächtige (auch ohne Player-ESP).",
						() -> c().antiVanishBoxes, v -> c().antiVanishBoxes = v))
				.add(new Setting.Bool("Tracer", "Linien zu sichtbaren Verdächtigen.",
						() -> c().antiVanishTracers, v -> c().antiVanishTracers = v))
				.add(new Setting.Action("Liste leeren", "Vergisst alle erkannten Vanish-Spieler.", AntiVanish::reset)));

		// ------------------------------------------------------------------ Client
		list.add(toggleable("hud", "HUD", Category.CLIENT,
				"Info-Overlay oben links.", () -> c().hudEnabled));
		list.add(new Module("gui", "GUI", Category.CLIENT,
				"Aussehen dieser Oberfläche.", null, null, () -> ErleichterungenClient.keyFor("gui"))
				.add(new Setting.Color("Akzentfarbe", "Hauptfarbe der Oberfläche.",
						() -> c().guiAccent, v -> c().guiAccent = v))
				.add(new Setting.Action("Positionen zurücksetzen", "Ordnet alle Fenster neu an.", () -> {
					c().guiPanels.clear();
					ClickGuiScreen.requestLayoutReset();
				})));

		all = list;
		return all;
	}

	private static void setAllTracers(boolean on) {
		ModConfig c = c();
		c.blockEspTracers = c.entityEspTracers = c.playerEspTracers = c.geodeTracers = c.antiVanishTracers = on;
	}
}
