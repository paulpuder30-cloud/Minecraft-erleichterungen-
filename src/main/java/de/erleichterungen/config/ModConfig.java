package de.erleichterungen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.erleichterungen.ErleichterungenClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Einfache JSON-Konfiguration unter {@code config/erleichterungen.json}.
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("erleichterungen.json");

	private static ModConfig instance = new ModConfig();

	// ---------- HUD ----------
	public boolean hudEnabled = true;
	public boolean hudCoords = true;
	public boolean hudDirection = true;
	public boolean hudFps = true;

	// ---------- GUI ----------
	public String guiAccent = "8A5CFF";
	/** Panel-Positionen der GUI: Kategorie -> {x, y, eingeklappt(0/1)}. */
	public Map<String, int[]> guiPanels = new LinkedHashMap<>();

	// ---------- Freecam ----------
	public float freecamSpeed = 1.0f;

	// ---------- Zoom ----------
	/** Sichtfeld (FOV) beim Zoomen – kleiner = stärker herangezoomt. */
	public int zoomFov = 30;

	// ---------- Fullbright ----------
	public boolean fullbrightEnabled = false;

	// ---------- Auto-Sprint ----------
	public boolean autoSprintEnabled = false;

	// ---------- Auto-Walk ----------
	public boolean autoWalkEnabled = false;

	// ---------- Auto-Respawn ----------
	public boolean autoRespawnEnabled = false;
	/** Wartezeit (ms) nach dem Tod bis zum automatischen Wiederbeleben. */
	public int autoRespawnDelayMs = 100;

	// ---------- Nametags ----------
	public boolean nametagsEnabled = false;
	public int nametagsRange = 128;
	public String nametagsColor = "FFFFFF";
	public boolean nametagsHealth = true;
	public boolean nametagsDistance = true;
	public float nametagsScale = 1.0f;

	// ---------- Item-ESP ----------
	public boolean itemEspEnabled = false;
	public int itemEspRange = 96;
	public int itemEspMaxBoxes = 1000;
	public RenderMode itemEspMode = RenderMode.BOX;
	public String itemEspColor = "55FFFF";
	public String itemEspTracerColor = "55FFFF";
	public boolean itemEspFill = false;
	public float itemEspLineWidth = 2.0f;
	public boolean itemEspLabels = true;

	// ---------- Block-Highlight ----------
	public boolean blockHighlightEnabled = false;
	public String blockHighlightColor = "FFFFFF";
	public boolean blockHighlightFill = false;
	public boolean blockHighlightThroughWalls = false;
	public float blockHighlightLineWidth = 2.0f;

	// ---------- NoHurtCam ----------
	public boolean noHurtCamEnabled = false;

	// ---------- AutoTotem ----------
	public boolean autoTotemEnabled = false;
	public boolean autoTotemOnlyWhenEmpty = false;

	// ---------- Ziel-Info ----------
	public boolean targetInfoEnabled = false;

	// ---------- KillAura ----------
	public boolean killAuraEnabled = false;
	public float killAuraRange = 3.0f;
	public boolean killAuraHostile = true;
	public boolean killAuraPassive = false;
	public boolean killAuraPlayers = false;
	public boolean killAuraOther = false;
	public boolean killAuraRotate = true;
	public boolean killAuraRequireCharged = true;
	public boolean killAuraPrioritizeLowHealth = false;
	public int killAuraMaxTargets = 1;
	public int killAuraDelayTicks = 0;

	// ---------- TriggerBot ----------
	public boolean triggerBotEnabled = false;
	public boolean triggerBotHostile = true;
	public boolean triggerBotPassive = false;
	public boolean triggerBotPlayers = false;
	public boolean triggerBotOther = false;
	public boolean triggerBotRequireCharged = true;
	public int triggerBotDelayTicks = 0;

	// ---------- AntiKnockback ----------
	public boolean antiKnockbackEnabled = false;

	// ---------- Shieldbreaker ----------
	public boolean shieldbreakerRequireMace = true;
	public int shieldbreakerMinCps = 8;
	public int shieldbreakerMaxCps = 14;

	// ---------- Auto-StunSlam ----------
	public float autoStunSlamRange = 5.0f;
	public int autoStunSlamGapTicks = 2;
	public boolean autoStunSlamRotate = true;
	public boolean autoStunSlamPlayersOnly = true;

	// ---------- Storage-ESP ----------
	public boolean storageEspEnabled = false;
	public int storageEspRange = 128;
	public int storageEspMaxBoxes = 2000;
	public RenderMode storageEspMode = RenderMode.BOX;
	public boolean storageEspFill = true;
	public float storageEspLineWidth = 2.0f;
	public boolean storageEspLabels = false;

	// ---------- Portal-ESP ----------
	public boolean portalEspEnabled = false;
	public int portalEspRange = 256;
	public int portalEspMaxBoxes = 2000;
	public RenderMode portalEspMode = RenderMode.BOTH;
	public String portalEspColor = "B14DFF";
	public String portalEspTracerColor = "B14DFF";
	public boolean portalEspFill = true;
	public float portalEspLineWidth = 2.0f;

	// ---------- Chunk-Borders ----------
	public boolean chunkBordersEnabled = false;
	public String chunkBordersColor = "FFFF55";
	public int chunkBordersRadius = 1;
	public float chunkBordersLineWidth = 1.5f;
	public int chunkBordersHeight = 128;

	// ---------- Tracer ----------
	public float tracerWidth = 1.5f;

	// ---------- Block ESP ----------
	public boolean blockEspEnabled = false;
	/** Maximale Entfernung (in Blöcken) bis zu der Blöcke markiert werden. */
	public int blockEspRange = 96;
	/** Maximale Anzahl gleichzeitig gezeichneter Boxen (Performance). */
	public int blockEspMaxBoxes = 1500;
	public boolean blockEspFill = true;
	public float blockEspLineWidth = 2.0f;
	/** Block-ID -> Darstellung (Modus, Box-Farbe, Tracer-Farbe). */
	public Map<String, EspEntry> blockEsp;

	// ---------- Entity ESP ----------
	public boolean entityEspEnabled = false;
	public int entityEspRange = 128;
	public int entityEspMaxBoxes = 1000;
	/** Name bzw. Gegenstand im Item Frame + Entfernung anzeigen. */
	public boolean entityEspLabels = true;
	public boolean entityEspFill = false;
	public float entityEspLineWidth = 2.0f;
	/** Entity-ID -> Darstellung (Modus, Box-Farbe, Tracer-Farbe). */
	public Map<String, EspEntry> entityEsp;

	// ---------- Player ESP ----------
	public boolean playerEspEnabled = false;
	public int playerEspRange = 256;
	public boolean playerEspNames = true;
	public boolean playerEspFill = false;
	public RenderMode playerEspMode = RenderMode.BOTH;
	public float playerEspLineWidth = 2.5f;
	public String playerEspColor = "FF3B3B";
	public String playerEspTracerColor = "FF3B3B";
	public String playerEspVanishedColor = "FF00FF";

	// ---------- Anti Vanish ----------
	public boolean antiVanishEnabled = true;
	/** Meldet Spieler, die aus der Tabliste verschwinden, ohne dass eine Leave-Nachricht kam. */
	public boolean antiVanishLeaveMessageCheck = true;
	/** Zeit in ms, die auf eine Leave-Nachricht gewartet wird. */
	public int antiVanishLeaveTimeoutMs = 2500;
	/** Vergleicht Befehls-Vorschläge (z.B. /msg) mit der Tabliste. */
	public boolean antiVanishCompletionCheck = true;
	public String antiVanishCompletionCommand = "msg";
	public int antiVanishCompletionIntervalSec = 10;
	/** Meldet Spieler-Entities in Sichtweite, die nicht in der Tabliste stehen. */
	public boolean antiVanishEntityCheck = true;
	/** Meldet, wenn ein Spieler in den Zuschauermodus wechselt. */
	public boolean antiVanishSpectatorCheck = true;
	public boolean antiVanishSound = true;
	/** Box/Tracer zu sichtbaren Vanish-Verdächtigen (auch wenn Player-ESP aus ist). */
	public boolean antiVanishHighlight = true;
	public RenderMode antiVanishMode = RenderMode.BOTH;
	public String antiVanishTracerColor = "FF00FF";

	// ---------- Geoden-Tracker ----------
	public boolean geodeTrackerEnabled = false;
	public int geodeRange = 64;
	public boolean geodeShowBudding = true;
	public boolean geodeShowGrowing = true;
	public boolean geodeNotifyFullyGrown = true;
	public boolean geodeSound = true;
	/** Box für alle angezeigten Knospen, Tracer nur zu erntebereiten Clustern. */
	public RenderMode geodeMode = RenderMode.BOX;
	public String geodeTracerColor = "3BFF5A";

	private static Map<String, String> defaultBlockColors() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("minecraft:diamond_ore", "00E5FF");
		map.put("minecraft:deepslate_diamond_ore", "00E5FF");
		map.put("minecraft:ancient_debris", "8B4513");
		map.put("minecraft:emerald_ore", "00FF55");
		map.put("minecraft:deepslate_emerald_ore", "00FF55");
		map.put("minecraft:spawner", "FF2222");
		map.put("minecraft:trial_spawner", "FF8800");
		return map;
	}

	private static Map<String, String> defaultEntityColors() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("minecraft:item_frame", "FFAA00");
		map.put("minecraft:glow_item_frame", "00FFCC");
		map.put("minecraft:armor_stand", "FFFFFF");
		map.put("minecraft:painting", "C080FF");
		map.put("minecraft:chest_minecart", "FFD700");
		map.put("minecraft:hopper_minecart", "A0A0A0");
		map.put("minecraft:villager", "55FF55");
		map.put("minecraft:leash_knot", "C8A060");
		map.put("minecraft:end_crystal", "FF55FF");
		for (String wood : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak"}) {
			map.put("minecraft:" + wood + "_chest_boat", "DDAA55");
		}
		map.put("minecraft:bamboo_chest_raft", "DDAA55");
		return map;
	}

	private static Map<String, EspEntry> toEntries(Map<String, String> colors, RenderMode mode) {
		Map<String, EspEntry> out = new LinkedHashMap<>();
		colors.forEach((id, color) -> out.put(id, new EspEntry(color, color, mode)));
		return out;
	}

	/** Liest ein altes boxes/tracers-Paar aus Version 1.2/1.3 als Modus. */
	private static RenderMode legacyMode(JsonObject json, String boxesKey, String tracersKey, RenderMode fallback) {
		if (!json.has(boxesKey) && !json.has(tracersKey)) return fallback;
		boolean boxes = !json.has(boxesKey) || json.get(boxesKey).getAsBoolean();
		boolean tracers = json.has(tracersKey) && json.get(tracersKey).getAsBoolean();
		if (boxes && tracers) return RenderMode.BOTH;
		if (tracers) return RenderMode.TRACER;
		return RenderMode.BOX;
	}

	private static Map<String, String> legacyColors(JsonObject json, String key) {
		Map<String, String> out = new LinkedHashMap<>();
		JsonElement e = json.get(key);
		if (e != null && e.isJsonObject()) {
			e.getAsJsonObject().entrySet().forEach(en -> out.put(en.getKey(), en.getValue().getAsString()));
		}
		return out;
	}

	/** Ergänzt fehlende Werte und übernimmt Einstellungen aus älteren Versionen. */
	private void fillDefaults(JsonObject json) {
		if (blockEsp == null) {
			if (json.has("blockEspBlocks")) {
				blockEsp = toEntries(legacyColors(json, "blockEspBlocks"), legacyMode(json, "blockEspBoxes", "blockEspTracers", RenderMode.BOX));
			} else {
				blockEsp = toEntries(defaultBlockColors(), RenderMode.BOX);
			}
		}
		if (entityEsp == null) {
			if (json.has("entityEspTypes")) {
				entityEsp = toEntries(legacyColors(json, "entityEspTypes"), legacyMode(json, "entityEspBoxes", "entityEspTracers", RenderMode.BOX));
			} else {
				entityEsp = toEntries(defaultEntityColors(), RenderMode.BOX);
			}
		}
		if (!json.has("playerEspMode")) playerEspMode = legacyMode(json, "playerEspBoxes", "playerEspTracers", RenderMode.BOTH);
		if (!json.has("antiVanishMode")) antiVanishMode = legacyMode(json, "antiVanishBoxes", "antiVanishTracers", RenderMode.BOTH);
		if (!json.has("geodeMode")) geodeMode = legacyMode(json, "geodeBoxes", "geodeTracers", RenderMode.BOX);
		if (playerEspMode == null) playerEspMode = RenderMode.BOTH;
		if (antiVanishMode == null) antiVanishMode = RenderMode.BOTH;
		if (geodeMode == null) geodeMode = RenderMode.BOX;
		if (guiPanels == null) guiPanels = new LinkedHashMap<>();
		blockEsp.values().forEach(EspEntry::normalize);
		entityEsp.values().forEach(EspEntry::normalize);
	}

	/** Schaltet die Tracer aller Module und Listeneinträge an bzw. aus. */
	public void setAllTracers(boolean on) {
		java.util.function.UnaryOperator<RenderMode> f = m -> on ? (m == RenderMode.BOX ? RenderMode.BOTH : m) : RenderMode.BOX;
		blockEsp.values().forEach(e -> e.mode = f.apply(e.mode()));
		entityEsp.values().forEach(e -> e.mode = f.apply(e.mode()));
		playerEspMode = f.apply(playerEspMode);
		antiVanishMode = f.apply(antiVanishMode);
		geodeMode = f.apply(geodeMode);
	}

	public static ModConfig get() {
		return instance;
	}

	public static void load() {
		JsonObject json = new JsonObject();
		ModConfig loaded = null;
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				JsonElement root = JsonParser.parseReader(reader);
				if (root != null && root.isJsonObject()) {
					json = root.getAsJsonObject();
					loaded = GSON.fromJson(json, ModConfig.class);
				}
			} catch (Exception e) {
				ErleichterungenClient.LOGGER.error("Konnte Konfiguration nicht laden, verwende Standardwerte", e);
				json = new JsonObject();
			}
		}
		instance = loaded != null ? loaded : new ModConfig();
		instance.fillDefaults(json);
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			ErleichterungenClient.LOGGER.error("Konnte Konfiguration nicht speichern", e);
		}
	}

	/** Wandelt "RRGGBB" / "AARRGGBB" / "#..." in einen ARGB-Int (Alpha default FF). */
	public static int parseColor(String hex, int fallback) {
		if (hex == null) return fallback;
		String s = hex.trim();
		if (s.startsWith("#")) s = s.substring(1);
		if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
		try {
			long v = Long.parseLong(s, 16);
			if (s.length() <= 6) v |= 0xFF000000L;
			return (int) v;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

}
