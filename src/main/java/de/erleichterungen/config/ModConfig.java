package de.erleichterungen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.erleichterungen.ErleichterungenClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

	// ---------- Tracer ----------
	public float tracerWidth = 1.5f;

	// ---------- Block ESP ----------
	public boolean blockEspEnabled = false;
	/** Maximale Entfernung (in Blöcken) bis zu der Blöcke markiert werden. */
	public int blockEspRange = 96;
	/** Maximale Anzahl gleichzeitig gezeichneter Boxen (Performance). */
	public int blockEspMaxBoxes = 1500;
	public boolean blockEspFill = true;
	public boolean blockEspBoxes = true;
	public boolean blockEspTracers = false;
	public float blockEspLineWidth = 2.0f;
	/** Block-ID -> Farbe (Hex, RGB oder ARGB). */
	public Map<String, String> blockEspBlocks = defaultBlocks();

	// ---------- Entity ESP ----------
	public boolean entityEspEnabled = false;
	public int entityEspRange = 128;
	public int entityEspMaxBoxes = 1000;
	/** Name bzw. Gegenstand im Item Frame + Entfernung anzeigen. */
	public boolean entityEspLabels = true;
	public boolean entityEspFill = false;
	public boolean entityEspBoxes = true;
	public boolean entityEspTracers = false;
	public float entityEspLineWidth = 2.0f;
	/** Entity-ID -> Farbe (Hex, RGB oder ARGB). */
	public Map<String, String> entityEspTypes = defaultEntities();

	// ---------- Player ESP ----------
	public boolean playerEspEnabled = false;
	public int playerEspRange = 256;
	public boolean playerEspNames = true;
	public boolean playerEspFill = false;
	public boolean playerEspBoxes = true;
	public boolean playerEspTracers = true;
	public float playerEspLineWidth = 2.5f;
	public String playerEspColor = "FF3B3B";
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
	public boolean antiVanishBoxes = true;
	public boolean antiVanishTracers = true;

	// ---------- Geoden-Tracker ----------
	public boolean geodeTrackerEnabled = false;
	public int geodeRange = 64;
	public boolean geodeShowBudding = true;
	public boolean geodeShowGrowing = true;
	public boolean geodeNotifyFullyGrown = true;
	public boolean geodeSound = true;
	public boolean geodeBoxes = true;
	/** Tracer zu erntebereiten Clustern. */
	public boolean geodeTracers = false;

	private static Map<String, String> defaultBlocks() {
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

	private static Map<String, String> defaultEntities() {
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

	public static ModConfig get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				if (loaded != null) {
					if (loaded.blockEspBlocks == null) loaded.blockEspBlocks = defaultBlocks();
					if (loaded.entityEspTypes == null) loaded.entityEspTypes = defaultEntities();
					instance = loaded;
				}
			} catch (Exception e) {
				ErleichterungenClient.LOGGER.error("Konnte Konfiguration nicht laden, verwende Standardwerte", e);
			}
		}
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

	public List<String> blockIds() {
		return new ArrayList<>(blockEspBlocks.keySet());
	}
}
