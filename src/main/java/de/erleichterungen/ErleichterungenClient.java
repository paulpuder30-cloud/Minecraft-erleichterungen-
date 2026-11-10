package de.erleichterungen;

import com.mojang.blaze3d.platform.InputConstants;
import de.erleichterungen.config.ModConfig;
import de.erleichterungen.feature.AntiVanish;
import de.erleichterungen.feature.BlockEsp;
import de.erleichterungen.feature.GeodeTracker;
import de.erleichterungen.feature.Hud;
import de.erleichterungen.feature.PlayerEsp;
import de.erleichterungen.scan.ScanManager;
import de.erleichterungen.util.Chat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class ErleichterungenClient implements ClientModInitializer {
	public static final String MOD_ID = "erleichterungen";
	public static final Logger LOGGER = LoggerFactory.getLogger("Erleichterungen");

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
	private static KeyMapping keyBlockEsp;
	private static KeyMapping keyPlayerEsp;
	private static KeyMapping keyGeode;
	private static KeyMapping keyAntiVanish;
	private static KeyMapping keyHud;

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ScanManager.init();
		BlockEsp.init();
		GeodeTracker.init();

		keyBlockEsp = key("block_esp", InputConstants.KEY_F6);
		keyPlayerEsp = key("player_esp", InputConstants.KEY_F7);
		keyGeode = key("geode_tracker", InputConstants.KEY_F8);
		keyAntiVanish = key("anti_vanish", InputConstants.KEY_F9);
		keyHud = key("hud", InputConstants.KEY_F10);

		ClientTickEvents.END_CLIENT_TICK.register(ErleichterungenClient::onTick);

		// Alle 3D-Markierungen als "Gizmos" mit always-on-top → durch Wände sichtbar
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.player == null) return;
			Vec3 cam = context.levelState().cameraRenderState.pos;
			float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
			try (Gizmos.TemporaryCollection ignored = context.levelRenderer().collectPerFrameRenderThreadGizmos()) {
				BlockEsp.render(mc, cam);
				GeodeTracker.render(mc, cam);
				PlayerEsp.render(mc, cam, partialTick);
			} catch (Exception e) {
				LOGGER.error("Fehler beim Rendern", e);
			}
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), Hud::render);

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) AntiVanish.onGameMessage(message);
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> AntiVanish.reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(AntiVanish::reset));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Commands.register(dispatcher));

		LOGGER.info("Erleichterungen geladen: Anti-Vanish, Block-ESP, Player-ESP, Geoden-Tracker");
	}

	private static KeyMapping key(String name, int glfwKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key." + MOD_ID + "." + name,
				InputConstants.Type.KEYBOARD, glfwKey, CATEGORY));
	}

	private static void onTick(Minecraft mc) {
		while (keyBlockEsp.consumeClick()) toggle("blockesp");
		while (keyPlayerEsp.consumeClick()) toggle("playeresp");
		while (keyGeode.consumeClick()) toggle("geode");
		while (keyAntiVanish.consumeClick()) toggle("antivanish");
		while (keyHud.consumeClick()) toggle("hud");

		ScanManager.tick(mc);
		AntiVanish.tick(mc);
		GeodeTracker.tick(mc);
	}

	/** Schaltet ein Modul um. @return neuer Zustand oder null bei unbekanntem Modul. */
	public static Boolean toggle(String module) {
		ModConfig c = ModConfig.get();
		boolean state;
		String label;
		switch (module.toLowerCase(Locale.ROOT)) {
			case "blockesp" -> { state = c.blockEspEnabled = !c.blockEspEnabled; label = "Block-ESP"; }
			case "playeresp" -> { state = c.playerEspEnabled = !c.playerEspEnabled; label = "Player-ESP"; }
			case "geode" -> { state = c.geodeTrackerEnabled = !c.geodeTrackerEnabled; label = "Geoden-Tracker"; }
			case "antivanish" -> {
				state = c.antiVanishEnabled = !c.antiVanishEnabled;
				label = "Anti-Vanish";
				if (!state) AntiVanish.reset();
			}
			case "hud" -> { state = c.hudEnabled = !c.hudEnabled; label = "HUD"; }
			default -> {
				return null;
			}
		}
		ModConfig.save();
		Chat.send("Erleichterungen", Component.literal(label + " ").withStyle(ChatFormatting.GRAY)
				.append(state ? Component.literal("AN").withStyle(ChatFormatting.GREEN)
						: Component.literal("AUS").withStyle(ChatFormatting.RED)));
		return state;
	}
}
