package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.util.Chat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Anti-Vanish: erkennt Spieler (meist Teammitglieder), die sich per Vanish unsichtbar machen.
 * <p>
 * Verwendete Methoden:
 * <ol>
 *   <li><b>Leave-Nachricht:</b> Ein Spieler verschwindet aus der Tabliste, aber es kommt keine
 *   "hat das Spiel verlassen"-Nachricht → sehr wahrscheinlich Vanish.</li>
 *   <li><b>Befehls-Vorschläge:</b> Der Server wird regelmäßig nach Tab-Vervollständigungen
 *   z.B. für {@code /msg } gefragt. Namen, die dort auftauchen, aber nicht in der Tabliste
 *   stehen, sind versteckt online.</li>
 *   <li><b>Entities:</b> Spieler-Entities in der Nähe, die nicht in der Tabliste stehen.</li>
 *   <li><b>Spectator:</b> Wechsel in den Zuschauermodus (wird oft als "Vanish" genutzt).</li>
 * </ol>
 */
public final class AntiVanish {
	/** Eigene ID für Vorschlags-Anfragen, damit die Antwort nicht in den Chat-Vorschlägen landet. */
	public static final int SUGGESTION_ID = 0x5A4E_1337;

	/** Name (lowercase) -> Zeitpunkt, bis zu dem auf eine Leave-Nachricht gewartet wird. */
	private static final Map<String, Long> PENDING_LEAVE = new HashMap<>();
	private static final Map<String, String> PENDING_DISPLAY = new HashMap<>();
	/** Aktuell als "vermutlich im Vanish" erkannte Spieler: lowercase-Name -> Anzeigename. */
	private static final Map<String, String> VANISHED = new LinkedHashMap<>();
	private static final Set<UUID> VANISHED_UUIDS = new HashSet<>();
	private static final Set<UUID> REPORTED_ENTITIES = new HashSet<>();
	/** Namen, die nur über die Vorschlags-Methode erkannt wurden. */
	private static final Set<String> FROM_COMPLETION = new HashSet<>();
	/** Letzte System-Nachrichten (Leave-Nachricht kann VOR dem Tablisten-Remove kommen). */
	private static final java.util.ArrayDeque<Map.Entry<Long, String>> RECENT_MESSAGES = new java.util.ArrayDeque<>();

	private static long lastCompletionRequest;
	private static int tickCounter;

	private AntiVanish() {
	}

	public static Map<String, String> vanished() {
		return Collections.unmodifiableMap(VANISHED);
	}

	public static boolean isSuspected(Player player) {
		return VANISHED_UUIDS.contains(player.getUUID())
				|| VANISHED.containsKey(player.getGameProfile().name().toLowerCase(Locale.ROOT));
	}

	public static void reset() {
		PENDING_LEAVE.clear();
		PENDING_DISPLAY.clear();
		VANISHED.clear();
		VANISHED_UUIDS.clear();
		REPORTED_ENTITIES.clear();
		FROM_COMPLETION.clear();
		RECENT_MESSAGES.clear();
		lastCompletionRequest = 0;
	}

	// ------------------------------------------------------------------ Packet-Hooks (Main-Thread)

	/** Vor dem Entfernen aus der Tabliste aufgerufen, damit der Name noch auflösbar ist. */
	public static void onPlayerInfoRemove(ClientPacketListener listener, ClientboundPlayerInfoRemovePacket packet) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.antiVanishEnabled || !cfg.antiVanishLeaveMessageCheck) return;
		Minecraft mc = Minecraft.getInstance();
		long deadline = System.currentTimeMillis() + Math.max(250, cfg.antiVanishLeaveTimeoutMs);
		for (UUID id : packet.profileIds()) {
			if (mc.player != null && id.equals(mc.player.getUUID())) continue;
			PlayerInfo info = listener.getPlayerInfo(id);
			if (info == null) continue;
			String name = info.getProfile().name();
			String key = name.toLowerCase(Locale.ROOT);
			if (sawRecentMessageWith(key)) continue; // Leave-Nachricht kam bereits vorher
			PENDING_LEAVE.put(key, deadline);
			PENDING_DISPLAY.put(key, name);
		}
	}

	/** Nach dem Verarbeiten von Tablisten-Updates: Wieder-Auftauchen und Spectator erkennen. */
	public static void onPlayerInfoUpdate(ClientPacketListener listener, ClientboundPlayerInfoUpdatePacket packet) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.antiVanishEnabled) return;
		for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
			PlayerInfo info = listener.getPlayerInfo(entry.profileId());
			if (info == null) continue;
			String name = info.getProfile().name();
			String key = name.toLowerCase(Locale.ROOT);
			PENDING_LEAVE.remove(key);
			if (packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) && VANISHED.remove(key) != null) {
				VANISHED_UUIDS.remove(entry.profileId());
				alert(Component.literal(name).withStyle(ChatFormatting.YELLOW)
						.append(Component.literal(" ist wieder sichtbar (Vanish beendet?)").withStyle(ChatFormatting.GRAY)), false);
			}
			if (cfg.antiVanishSpectatorCheck
					&& packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)
					&& !packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)
					&& entry.gameMode() == GameType.SPECTATOR) {
				alert(Component.literal(name).withStyle(ChatFormatting.YELLOW)
						.append(Component.literal(" ist in den Zuschauermodus gewechselt").withStyle(ChatFormatting.GRAY)), true);
			}
		}
	}

	/** Antwort auf unsere Vorschlags-Anfrage. @return true, wenn das Paket uns gehörte. */
	public static boolean onCommandSuggestions(ClientPacketListener listener, ClientboundCommandSuggestionsPacket packet) {
		if (packet.id() != SUGGESTION_ID) return false;
		ModConfig cfg = ModConfig.get();
		if (!cfg.antiVanishEnabled || !cfg.antiVanishCompletionCheck) return true;

		Set<String> tab = new HashSet<>();
		for (PlayerInfo info : listener.getOnlinePlayers()) {
			tab.add(info.getProfile().name().toLowerCase(Locale.ROOT));
		}
		Set<String> hiddenNow = new HashSet<>();
		for (ClientboundCommandSuggestionsPacket.Entry entry : packet.suggestions()) {
			String text = entry.text();
			if (!isPlayerName(text)) continue;
			String key = text.toLowerCase(Locale.ROOT);
			if (tab.contains(key)) continue;
			hiddenNow.add(key);
			if (!VANISHED.containsKey(key)) {
				VANISHED.put(key, text);
				FROM_COMPLETION.add(key);
				alert(Component.literal(text).withStyle(ChatFormatting.RED)
						.append(Component.literal(" ist online, aber versteckt (Tab-Vervollständigung)").withStyle(ChatFormatting.GRAY)), true);
			}
		}
		// Über Vorschläge erkannte Spieler, die nicht mehr auftauchen, sind offline gegangen
		Iterator<String> it = FROM_COMPLETION.iterator();
		while (it.hasNext()) {
			String key = it.next();
			if (!hiddenNow.contains(key)) {
				it.remove();
				String display = VANISHED.remove(key);
				if (display != null && !tab.contains(key)) {
					Chat.send("Anti-Vanish", Component.literal(display).withStyle(ChatFormatting.YELLOW)
							.append(Component.literal(" ist nicht mehr (versteckt) online").withStyle(ChatFormatting.GRAY)));
				}
			}
		}
		return true;
	}

	public static void onGameMessage(Component message) {
		String text = message.getString().toLowerCase(Locale.ROOT);
		long now = System.currentTimeMillis();
		RECENT_MESSAGES.addLast(Map.entry(now, text));
		while (RECENT_MESSAGES.size() > 50 || (!RECENT_MESSAGES.isEmpty() && now - RECENT_MESSAGES.peekFirst().getKey() > 5000)) {
			RECENT_MESSAGES.pollFirst();
		}
		if (!PENDING_LEAVE.isEmpty()) PENDING_LEAVE.keySet().removeIf(name -> containsName(text, name));
	}

	private static boolean sawRecentMessageWith(String name) {
		long now = System.currentTimeMillis();
		for (Map.Entry<Long, String> e : RECENT_MESSAGES) {
			if (now - e.getKey() <= 5000 && containsName(e.getValue(), name)) return true;
		}
		return false;
	}

	/** Prüft, ob {@code name} als ganzes Wort in {@code text} vorkommt. */
	private static boolean containsName(String text, String name) {
		int idx = text.indexOf(name);
		while (idx >= 0) {
			boolean startOk = idx == 0 || !isNameChar(text.charAt(idx - 1));
			int end = idx + name.length();
			boolean endOk = end >= text.length() || !isNameChar(text.charAt(end));
			if (startOk && endOk) return true;
			idx = text.indexOf(name, idx + 1);
		}
		return false;
	}

	private static boolean isNameChar(char c) {
		return c == '_' || (c < 128 && Character.isLetterOrDigit(c));
	}

	// ------------------------------------------------------------------ Tick

	public static void tick(Minecraft mc) {
		ModConfig cfg = ModConfig.get();
		ClientPacketListener connection = mc.getConnection();
		if (!cfg.antiVanishEnabled || connection == null || mc.level == null || mc.player == null) return;

		long now = System.currentTimeMillis();

		// 1) Abgelaufene Leave-Prüfungen
		Iterator<Map.Entry<String, Long>> it = PENDING_LEAVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Long> e = it.next();
			if (e.getValue() > now) continue;
			it.remove();
			String display = PENDING_DISPLAY.getOrDefault(e.getKey(), e.getKey());
			if (!VANISHED.containsKey(e.getKey())) {
				VANISHED.put(e.getKey(), display);
				alert(Component.literal(display).withStyle(ChatFormatting.RED)
						.append(Component.literal(" ist ohne Leave-Nachricht aus der Tabliste verschwunden → vermutlich Vanish").withStyle(ChatFormatting.GRAY)), true);
			}
		}

		// 2) Vorschlags-Abfrage
		if (cfg.antiVanishCompletionCheck && cfg.antiVanishCompletionCommand != null && !cfg.antiVanishCompletionCommand.isBlank()) {
			long interval = Math.max(2, cfg.antiVanishCompletionIntervalSec) * 1000L;
			if (now - lastCompletionRequest >= interval) {
				lastCompletionRequest = now;
				String cmd = cfg.antiVanishCompletionCommand.trim();
				if (cmd.startsWith("/")) cmd = cmd.substring(1);
				connection.send(new ServerboundCommandSuggestionPacket(SUGGESTION_ID, "/" + cmd + " "));
			}
		}

		// 3) Entities ohne Tablisten-Eintrag (alle 10 Ticks)
		if (cfg.antiVanishEntityCheck && ++tickCounter % 10 == 0) {
			for (Player player : mc.level.players()) {
				if (player == mc.player) continue;
				UUID id = player.getUUID();
				// NPC-Plugins verwenden meist UUID-Version 2 – nur echte Spieler (v4/v3) prüfen
				if (id.version() != 4 && id.version() != 3) continue;
				if (connection.getPlayerInfo(id) != null) continue;
				VANISHED_UUIDS.add(id);
				if (REPORTED_ENTITIES.add(id)) {
					String name = player.getGameProfile().name();
					VANISHED.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
					alert(Component.literal(name).withStyle(ChatFormatting.RED)
							.append(Component.literal(String.format(Locale.ROOT, " ist sichtbar in der Nähe (%.0f m), fehlt aber in der Tabliste",
									player.distanceTo(mc.player))).withStyle(ChatFormatting.GRAY)), true);
				}
			}
		}
	}

	private static boolean isPlayerName(String s) {
		if (s.length() < 3 || s.length() > 16) return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (!(Character.isLetterOrDigit(c) || c == '_') || c > 127) return false;
		}
		return true;
	}

	private static void alert(Component message, boolean important) {
		Chat.send("Anti-Vanish", message);
		if (important && ModConfig.get().antiVanishSound) {
			Chat.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f);
		}
	}
}
