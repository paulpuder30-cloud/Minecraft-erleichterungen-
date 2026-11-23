package de.erleichterungen.gui;

import de.erleichterungen.ErleichterungenClient;
import de.erleichterungen.config.EspEntry;
import de.erleichterungen.config.ModConfig;
import de.erleichterungen.config.RenderMode;
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

	private static String blockName(String id) {
		Identifier ident = Identifier.tryParse(id);
		if (ident == null || !BuiltInRegistries.BLOCK.containsKey(ident)) return id;
		return BuiltInRegistries.BLOCK.getValue(ident).getName().getString();
	}

	private static String entityName(String id) {
		Identifier ident = Identifier.tryParse(id);
		if (ident == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(ident)) return id;
		return BuiltInRegistries.ENTITY_TYPE.getValue(ident).getDescription().getString();
	}

	public static List<Module> all() {
		if (all != null) return all;
		List<Module> list = new ArrayList<>();

		// ------------------------------------------------------------------ Kampf
		list.add(toggleable("autototem", "AutoTotem", Category.COMBAT,
				"Legt automatisch ein Totem der Unsterblichkeit in die Nebenhand.", () -> c().autoTotemEnabled)
				.add(new Setting.Bool("Nur bei leerer Nebenhand", "Nur tauschen, wenn die Nebenhand leer ist (sonst wird z.B. ein Schild ersetzt).",
						() -> c().autoTotemOnlyWhenEmpty, v -> c().autoTotemOnlyWhenEmpty = v)));

		list.add(toggleable("targetinfo", "Ziel-Info", Category.COMBAT,
				"Zeigt Name, Gesundheit und Entfernung des anvisierten Gegners unter dem Fadenkreuz.", () -> c().targetInfoEnabled));

		list.add(toggleable("killaura", "KillAura", Category.COMBAT,
				"Erfasst nahe Ziele automatisch, dreht (optional) zu ihnen und greift an.", () -> c().killAuraEnabled)
				.add(new Setting.Dec("Reichweite", "Angriffsreichweite in Blöcken (über ~3 kann Anticheat auslösen).", 1f, 6f, 0.5f,
						() -> c().killAuraRange, v -> c().killAuraRange = v))
				.add(new Setting.Bool("Monster", "Feindliche Mobs angreifen.",
						() -> c().killAuraHostile, v -> c().killAuraHostile = v))
				.add(new Setting.Bool("Tiere", "Passive Tiere angreifen.",
						() -> c().killAuraPassive, v -> c().killAuraPassive = v))
				.add(new Setting.Bool("Spieler", "Andere Spieler angreifen.",
						() -> c().killAuraPlayers, v -> c().killAuraPlayers = v))
				.add(new Setting.Bool("Sonstige", "Andere Lebewesen angreifen (z.B. Dorfbewohner).",
						() -> c().killAuraOther, v -> c().killAuraOther = v))
				.add(new Setting.Bool("Rotation", "Zum Ziel drehen (Kamera schwenkt).",
						() -> c().killAuraRotate, v -> c().killAuraRotate = v))
				.add(new Setting.Bool("Nur voll aufgeladen", "Nur angreifen, wenn die Angriffsleiste voll ist.",
						() -> c().killAuraRequireCharged, v -> c().killAuraRequireCharged = v))
				.add(new Setting.Bool("Niedrigste HP zuerst", "Ziel mit der geringsten Gesundheit bevorzugen.",
						() -> c().killAuraPrioritizeLowHealth, v -> c().killAuraPrioritizeLowHealth = v))
				.add(new Setting.Int("Max. Ziele", "Wie viele Ziele pro Tick angegriffen werden.", 1, 10,
						() -> c().killAuraMaxTargets, v -> c().killAuraMaxTargets = v))
				.add(new Setting.Int("Verzögerung (Ticks)", "Zusätzliche Pause zwischen Angriffen.", 0, 40,
						() -> c().killAuraDelayTicks, v -> c().killAuraDelayTicks = v)));

		list.add(toggleable("triggerbot", "TriggerBot", Category.COMBAT,
				"Greift automatisch das anvisierte Ziel an, wenn die Angriffsleiste voll ist.", () -> c().triggerBotEnabled)
				.add(new Setting.Bool("Monster", "Feindliche Mobs angreifen.",
						() -> c().triggerBotHostile, v -> c().triggerBotHostile = v))
				.add(new Setting.Bool("Tiere", "Passive Tiere angreifen.",
						() -> c().triggerBotPassive, v -> c().triggerBotPassive = v))
				.add(new Setting.Bool("Spieler", "Andere Spieler angreifen.",
						() -> c().triggerBotPlayers, v -> c().triggerBotPlayers = v))
				.add(new Setting.Bool("Sonstige", "Andere Lebewesen angreifen (z.B. Dorfbewohner).",
						() -> c().triggerBotOther, v -> c().triggerBotOther = v))
				.add(new Setting.Bool("Nur voll aufgeladen", "Nur angreifen, wenn die Angriffsleiste voll ist.",
						() -> c().triggerBotRequireCharged, v -> c().triggerBotRequireCharged = v))
				.add(new Setting.Int("Verzögerung (Ticks)", "Zusätzliche Pause zwischen Angriffen.", 0, 40,
						() -> c().triggerBotDelayTicks, v -> c().triggerBotDelayTicks = v)));

		list.add(toggleable("antiknockback", "AntiKnockback", Category.COMBAT,
				"Unterdrückt den vom Server geschickten Rückstoß (z.B. durch Treffer).", () -> c().antiKnockbackEnabled));

		list.add(new Module("lungeswap", "Auto-LungeSwap", Category.COMBAT,
				"Wirft auf Tastendruck eine Wind Charge und wechselt sofort auf den Speer (Items automatisch aus der Hotbar).",
				null, null, () -> ErleichterungenClient.keyFor("lungeswap")));

		list.add(new Module("shieldbreaker", "Shieldbreaker", Category.COMBAT,
				"Taste halten: schnelle, zufällig getimte Mace-Angriffe auf das anvisierte Ziel (nutzt Schilde ab).",
				null, null, () -> ErleichterungenClient.keyFor("shieldbreaker"))
				.add(new Setting.Bool("Nur mit Mace", "Nur auslösen, wenn du eine Mace in der Hand hast.",
						() -> c().shieldbreakerRequireMace, v -> c().shieldbreakerRequireMace = v))
				.add(new Setting.Int("Min. Klicks/s", "Untere Grenze der zufälligen Klickrate.", 1, 20,
						() -> c().shieldbreakerMinCps, v -> c().shieldbreakerMinCps = v))
				.add(new Setting.Int("Max. Klicks/s", "Obere Grenze der zufälligen Klickrate.", 1, 20,
						() -> c().shieldbreakerMaxCps, v -> c().shieldbreakerMaxCps = v)));

		list.add(new Module("stunslam", "Auto-StunSlam", Category.COMBAT,
				"Du schießt dich selbst hoch; auf Tastendruck: Axt-Schlag (Schild-Stun) → Mace-Wechsel → Mace-Slam auf den nächsten Gegner.",
				null, null, () -> ErleichterungenClient.keyFor("stunslam"))
				.add(new Setting.Dec("Reichweite", "Reichweite der Zielsuche in Blöcken.", 1f, 6f, 0.5f,
						() -> c().autoStunSlamRange, v -> c().autoStunSlamRange = v))
				.add(new Setting.Int("Lücke (Ticks)", "Ticks zwischen Axt-Stun und Mace-Slam.", 0, 10,
						() -> c().autoStunSlamGapTicks, v -> c().autoStunSlamGapTicks = v))
				.add(new Setting.Bool("Rotation", "Vor jedem Schlag zum Ziel drehen.",
						() -> c().autoStunSlamRotate, v -> c().autoStunSlamRotate = v))
				.add(new Setting.Bool("Nur Spieler", "Nur Spieler als Ziel erfassen.",
						() -> c().autoStunSlamPlayersOnly, v -> c().autoStunSlamPlayersOnly = v)));

		// ------------------------------------------------------------------ Render
		list.add(toggleable("blockesp", "Block-ESP", Category.RENDER,
				"Markiert ausgewählte Blöcke (Erze, Spawner, ...) durch Wände.", () -> c().blockEspEnabled)
				.add(new Setting.ListEdit("Blöcke", "Welche Blöcke markiert werden (mit Farbe).",
						() -> c().blockEsp, Modules::blockIds, Modules::blockName).onChange(BlockEsp::rebuildFilter))
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 512,
						() -> c().blockEspRange, v -> c().blockEspRange = v))
				.add(new Setting.Int("Max. Boxen", "Obergrenze gleichzeitig gezeichneter Blöcke.", 10, 20000,
						() -> c().blockEspMaxBoxes, v -> c().blockEspMaxBoxes = v))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().blockEspFill, v -> c().blockEspFill = v))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().blockEspLineWidth, v -> c().blockEspLineWidth = v))
				.add(bulkMode("Alle: Box", RenderMode.BOX, () -> c().blockEsp))
				.add(bulkMode("Alle: Box & Tracer", RenderMode.BOTH, () -> c().blockEsp))
				.add(bulkMode("Alle: Nur Tracer", RenderMode.TRACER, () -> c().blockEsp)));

		list.add(toggleable("entityesp", "Entity-ESP", Category.RENDER,
				"Markiert Base-typische Entities: Item Frames, Rüstungsständer, Loren, ...", () -> c().entityEspEnabled)
				.add(new Setting.ListEdit("Entities", "Welche Entity-Typen markiert werden (mit Farbe).",
						() -> c().entityEsp, Modules::entityIds, Modules::entityName).onChange(EntityEsp::rebuildFilter))
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 512,
						() -> c().entityEspRange, v -> c().entityEspRange = v))
				.add(new Setting.Int("Max. Boxen", "Obergrenze gleichzeitig gezeichneter Entities.", 10, 20000,
						() -> c().entityEspMaxBoxes, v -> c().entityEspMaxBoxes = v))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().entityEspFill, v -> c().entityEspFill = v))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().entityEspLineWidth, v -> c().entityEspLineWidth = v))
				.add(new Setting.Bool("Beschriftung", "Name bzw. Gegenstand im Item Frame + Entfernung anzeigen.",
						() -> c().entityEspLabels, v -> c().entityEspLabels = v))
				.add(bulkMode("Alle: Box", RenderMode.BOX, () -> c().entityEsp))
				.add(bulkMode("Alle: Box & Tracer", RenderMode.BOTH, () -> c().entityEsp))
				.add(bulkMode("Alle: Nur Tracer", RenderMode.TRACER, () -> c().entityEsp)));

		list.add(toggleable("playeresp", "Player-ESP", Category.RENDER,
				"Boxen, Namen und Entfernung aller Spieler durch Wände.", () -> c().playerEspEnabled)
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 1024,
						() -> c().playerEspRange, v -> c().playerEspRange = v))
				.add(new Setting.Mode("Modus", "Box, Box & Tracer oder nur Tracer.",
						() -> c().playerEspMode, v -> c().playerEspMode = v))
				.add(new Setting.Color("Box-Farbe", "Farbe der Boxen und Namen.",
						() -> c().playerEspColor, v -> c().playerEspColor = v).visibleIf(() -> c().playerEspMode.box()))
				.add(new Setting.Color("Tracer-Farbe", "Farbe der Tracer-Linien.",
						() -> c().playerEspTracerColor, v -> c().playerEspTracerColor = v).visibleIf(() -> c().playerEspMode.tracer()))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().playerEspFill, v -> c().playerEspFill = v).visibleIf(() -> c().playerEspMode.box()))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().playerEspLineWidth, v -> c().playerEspLineWidth = v).visibleIf(() -> c().playerEspMode.box()))
				.add(new Setting.Bool("Namen", "Name und Entfernung über dem Spieler.",
						() -> c().playerEspNames, v -> c().playerEspNames = v)));

		list.add(new Module("tracers", "Tracer", Category.RENDER,
				"Gemeinsame Einstellungen für alle Tracer-Linien.", null, null, () -> null)
				.add(new Setting.Dec("Linienbreite", "Breite aller Tracer-Linien.", 0.5f, 10f, 0.5f,
						() -> c().tracerWidth, v -> c().tracerWidth = v))
				.add(new Setting.Action("Alle Tracer an", "Box → Box & Tracer, für alle Module und Listeneinträge.", () -> c().setAllTracers(true)))
				.add(new Setting.Action("Alle Tracer aus", "Alles auf nur Box – entfernt sämtliche Tracer.", () -> c().setAllTracers(false))));

		list.add(new Module("zoom", "Zoom", Category.RENDER,
				"Verringert das Sichtfeld, solange die Zoom-Taste gehalten wird.", null, null,
				() -> ErleichterungenClient.keyFor("zoom"))
				.add(new Setting.Int("Zoom-FOV", "Sichtfeld beim Zoomen (kleiner = stärker herangezoomt).", 30, 110,
						() -> c().zoomFov, v -> c().zoomFov = v)));

		list.add(toggleable("fullbright", "Fullbright", Category.RENDER,
				"Setzt die Helligkeit (Gamma) auf das Maximum.", () -> c().fullbrightEnabled));

		list.add(toggleable("nametags", "Nametags", Category.RENDER,
				"Name, Gesundheit und Entfernung über allen Spielern.", () -> c().nametagsEnabled)
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 16, 512,
						() -> c().nametagsRange, v -> c().nametagsRange = v))
				.add(new Setting.Bool("Gesundheit", "Aktuelle/maximale HP anzeigen.",
						() -> c().nametagsHealth, v -> c().nametagsHealth = v))
				.add(new Setting.Bool("Entfernung", "Entfernung in Metern anzeigen.",
						() -> c().nametagsDistance, v -> c().nametagsDistance = v))
				.add(new Setting.Dec("Größe", "Skalierung der Schrift.", 0.5f, 3f, 0.1f,
						() -> c().nametagsScale, v -> c().nametagsScale = v))
				.add(new Setting.Color("Farbe", "Textfarbe.",
						() -> c().nametagsColor, v -> c().nametagsColor = v)));

		list.add(toggleable("itemesp", "Item-ESP", Category.RENDER,
				"Markiert am Boden liegende Gegenstände mit Box, Name und Anzahl.", () -> c().itemEspEnabled)
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 512,
						() -> c().itemEspRange, v -> c().itemEspRange = v))
				.add(new Setting.Mode("Modus", "Box, Box & Tracer oder nur Tracer.",
						() -> c().itemEspMode, v -> c().itemEspMode = v))
				.add(new Setting.Color("Box-Farbe", "Farbe der Boxen und Beschriftung.",
						() -> c().itemEspColor, v -> c().itemEspColor = v).visibleIf(() -> c().itemEspMode.box()))
				.add(new Setting.Color("Tracer-Farbe", "Farbe der Tracer-Linien.",
						() -> c().itemEspTracerColor, v -> c().itemEspTracerColor = v).visibleIf(() -> c().itemEspMode.tracer()))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().itemEspFill, v -> c().itemEspFill = v).visibleIf(() -> c().itemEspMode.box()))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().itemEspLineWidth, v -> c().itemEspLineWidth = v).visibleIf(() -> c().itemEspMode.box()))
				.add(new Setting.Bool("Beschriftung", "Name und Anzahl anzeigen.",
						() -> c().itemEspLabels, v -> c().itemEspLabels = v)));

		list.add(toggleable("blockhighlight", "Block-Highlight", Category.RENDER,
				"Hebt den Block hervor, auf den du schaust.", () -> c().blockHighlightEnabled)
				.add(new Setting.Color("Farbe", "Farbe der Hervorhebung.",
						() -> c().blockHighlightColor, v -> c().blockHighlightColor = v))
				.add(new Setting.Bool("Füllung", "Block halbtransparent füllen.",
						() -> c().blockHighlightFill, v -> c().blockHighlightFill = v))
				.add(new Setting.Bool("Durch Wände", "Auch durch Wände sichtbar.",
						() -> c().blockHighlightThroughWalls, v -> c().blockHighlightThroughWalls = v))
				.add(new Setting.Dec("Linienbreite", "Breite der Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().blockHighlightLineWidth, v -> c().blockHighlightLineWidth = v)));

		list.add(toggleable("nohurtcam", "NoHurtCam", Category.RENDER,
				"Unterdrückt das Kamera-Wackeln beim Schadennehmen.", () -> c().noHurtCamEnabled));

		// ------------------------------------------------------------------ Bewegung
		list.add(toggleable("autosprint", "Auto-Sprint", Category.MOVEMENT,
				"Sprintet automatisch, sobald du vorwärts läufst.", () -> c().autoSprintEnabled));

		list.add(toggleable("autowalk", "Auto-Walk", Category.MOVEMENT,
				"Hält automatisch die Vorwärtstaste gedrückt.", () -> c().autoWalkEnabled));

		// ------------------------------------------------------------------ Welt
		list.add(toggleable("geode", "Geoden-Tracker", Category.WORLD,
				"Zeigt Amethyst-Knospen nach Wachstumsstufe und meldet fertige Cluster.", () -> c().geodeTrackerEnabled)
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 8, 512,
						() -> c().geodeRange, v -> c().geodeRange = v))
				.add(new Setting.Mode("Modus", "Boxen um Knospen; Tracer führen zu erntebereiten Clustern.",
						() -> c().geodeMode, v -> c().geodeMode = v))
				.add(new Setting.Color("Tracer-Farbe", "Farbe der Tracer zu fertigen Clustern.",
						() -> c().geodeTracerColor, v -> c().geodeTracerColor = v).visibleIf(() -> c().geodeMode.tracer()))
				.add(new Setting.Bool("Knospender Amethyst", "Knospende Amethystblöcke (lila) anzeigen.",
						() -> c().geodeShowBudding, v -> c().geodeShowBudding = v).visibleIf(() -> c().geodeMode.box()))
				.add(new Setting.Bool("Wachsende Knospen", "Auch noch nicht fertige Knospen anzeigen.",
						() -> c().geodeShowGrowing, v -> c().geodeShowGrowing = v).visibleIf(() -> c().geodeMode.box()))
				.add(new Setting.Bool("Benachrichtigung", "Chat-Nachricht, wenn ein Cluster ausgewachsen ist.",
						() -> c().geodeNotifyFullyGrown, v -> c().geodeNotifyFullyGrown = v))
				.add(new Setting.Bool("Sound", "Ton bei ausgewachsenem Cluster.",
						() -> c().geodeSound, v -> c().geodeSound = v))
				.add(new Setting.Action("Statistik zurücksetzen", "Setzt die Wachstums-Statistik zurück.", GeodeTracker::reset)));

		list.add(toggleable("storageesp", "Storage-ESP", Category.WORLD,
				"Markiert Behälter (Truhen, Shulker, Öfen, Hopper, ...) – ideal zum Basen-Finden.", () -> c().storageEspEnabled)
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 16, 512,
						() -> c().storageEspRange, v -> c().storageEspRange = v))
				.add(new Setting.Int("Max. Boxen", "Obergrenze gleichzeitig gezeichneter Behälter.", 10, 20000,
						() -> c().storageEspMaxBoxes, v -> c().storageEspMaxBoxes = v))
				.add(new Setting.Mode("Modus", "Box, Box & Tracer oder nur Tracer.",
						() -> c().storageEspMode, v -> c().storageEspMode = v))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().storageEspFill, v -> c().storageEspFill = v).visibleIf(() -> c().storageEspMode.box()))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().storageEspLineWidth, v -> c().storageEspLineWidth = v).visibleIf(() -> c().storageEspMode.box()))
				.add(new Setting.Bool("Beschriftung", "Behälter-Namen anzeigen.",
						() -> c().storageEspLabels, v -> c().storageEspLabels = v)));

		list.add(toggleable("portalesp", "Portal-ESP", Category.WORLD,
				"Markiert Nether-/End-Portale, Portalrahmen und Gateways.", () -> c().portalEspEnabled)
				.add(new Setting.Int("Reichweite", "Maximale Entfernung in Blöcken.", 16, 512,
						() -> c().portalEspRange, v -> c().portalEspRange = v))
				.add(new Setting.Mode("Modus", "Box, Box & Tracer oder nur Tracer.",
						() -> c().portalEspMode, v -> c().portalEspMode = v))
				.add(new Setting.Color("Box-Farbe", "Farbe der Boxen.",
						() -> c().portalEspColor, v -> c().portalEspColor = v).visibleIf(() -> c().portalEspMode.box()))
				.add(new Setting.Color("Tracer-Farbe", "Farbe der Tracer-Linien.",
						() -> c().portalEspTracerColor, v -> c().portalEspTracerColor = v).visibleIf(() -> c().portalEspMode.tracer()))
				.add(new Setting.Bool("Füllung", "Boxen halbtransparent füllen.",
						() -> c().portalEspFill, v -> c().portalEspFill = v).visibleIf(() -> c().portalEspMode.box()))
				.add(new Setting.Dec("Linienbreite", "Breite der Box-Umrisse.", 0.5f, 6f, 0.5f,
						() -> c().portalEspLineWidth, v -> c().portalEspLineWidth = v).visibleIf(() -> c().portalEspMode.box())));

		list.add(toggleable("chunkborders", "Chunk-Borders", Category.WORLD,
				"Zeichnet das Chunk-Raster um dich herum.", () -> c().chunkBordersEnabled)
				.add(new Setting.Int("Radius (Chunks)", "Wie viele Chunks in jede Richtung.", 0, 8,
						() -> c().chunkBordersRadius, v -> c().chunkBordersRadius = v))
				.add(new Setting.Int("Höhe", "Halbe Höhe der senkrechten Linien (Blöcke).", 16, 512,
						() -> c().chunkBordersHeight, v -> c().chunkBordersHeight = v))
				.add(new Setting.Dec("Linienbreite", "Breite der Linien.", 0.5f, 6f, 0.5f,
						() -> c().chunkBordersLineWidth, v -> c().chunkBordersLineWidth = v))
				.add(new Setting.Color("Farbe", "Farbe der Rasterlinien.",
						() -> c().chunkBordersColor, v -> c().chunkBordersColor = v)));

		// ------------------------------------------------------------------ Spieler
		list.add(new Module("freecam", "Freecam", Category.PLAYER,
				"Kamera frei bewegen, der Spieler bleibt stehen. WASD, Leertaste/Shift hoch/runter, Sprint = schneller.",
				Freecam::isActive, () -> ErleichterungenClient.toggle("freecam", false), () -> ErleichterungenClient.keyFor("freecam"))
				.add(new Setting.Dec("Geschwindigkeit", "Blöcke pro Tick (mit Sprint x2.5).", 0.1f, 5f, 0.1f,
						() -> c().freecamSpeed, v -> c().freecamSpeed = v)));

		list.add(toggleable("autorespawn", "Auto-Respawn", Category.PLAYER,
				"Belebt dich nach dem Tod automatisch wieder.", () -> c().autoRespawnEnabled)
				.add(new Setting.Int("Verzögerung (ms)", "Wartezeit nach dem Tod bis zum Wiederbeleben.", 0, 5000,
						() -> c().autoRespawnDelayMs, v -> c().autoRespawnDelayMs = v)));

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
				.add(new Setting.Bool("Verdächtige markieren", "Sichtbare Verdächtige markieren (auch ohne Player-ESP).",
						() -> c().antiVanishHighlight, v -> c().antiVanishHighlight = v))
				.add(new Setting.Mode("Modus", "Box, Box & Tracer oder nur Tracer für Verdächtige.",
						() -> c().antiVanishMode, v -> c().antiVanishMode = v).visibleIf(() -> c().antiVanishHighlight))
				.add(new Setting.Color("Box-Farbe", "Farbe der Box um Verdächtige.",
						() -> c().playerEspVanishedColor, v -> c().playerEspVanishedColor = v)
						.visibleIf(() -> c().antiVanishHighlight && c().antiVanishMode.box()))
				.add(new Setting.Color("Tracer-Farbe", "Farbe der Tracer zu Verdächtigen.",
						() -> c().antiVanishTracerColor, v -> c().antiVanishTracerColor = v)
						.visibleIf(() -> c().antiVanishHighlight && c().antiVanishMode.tracer()))
				.add(new Setting.Action("Liste leeren", "Vergisst alle erkannten Vanish-Spieler.", AntiVanish::reset)));

		// ------------------------------------------------------------------ Client
		list.add(toggleable("hud", "HUD", Category.CLIENT,
				"Info-Overlay oben links.", () -> c().hudEnabled)
				.add(new Setting.Bool("Koordinaten", "XYZ-Position anzeigen.",
						() -> c().hudCoords, v -> c().hudCoords = v))
				.add(new Setting.Bool("Blickrichtung", "Himmelsrichtung anzeigen.",
						() -> c().hudDirection, v -> c().hudDirection = v))
				.add(new Setting.Bool("FPS", "Bilder pro Sekunde anzeigen.",
						() -> c().hudFps, v -> c().hudFps = v)));
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

	private static Setting bulkMode(String name, RenderMode mode, java.util.function.Supplier<java.util.Map<String, EspEntry>> map) {
		return new Setting.Action(name, "Setzt den Modus aller Einträge in der Liste auf \"" + mode.label + "\".",
				() -> map.get().values().forEach(e -> e.mode = mode));
	}
}
