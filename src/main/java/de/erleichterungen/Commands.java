package de.erleichterungen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.erleichterungen.config.ModConfig;
import de.erleichterungen.feature.AntiVanish;
import de.erleichterungen.feature.BlockEsp;
import de.erleichterungen.feature.GeodeTracker;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Client-Befehl {@code /erl} zum Konfigurieren im Spiel.
 */
public final class Commands {
	private Commands() {
	}

	private static final SuggestionProvider<FabricClientCommandSource> BLOCK_IDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::toString), builder);

	private static final SuggestionProvider<FabricClientCommandSource> CONFIGURED_BLOCKS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(ModConfig.get().blockEspBlocks.keySet(), builder);

	private static final String[] MODULES = {"blockesp", "playeresp", "geode", "antivanish", "hud"};

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("erl")
				.executes(Commands::help)
				.then(literal("help").executes(Commands::help))
				.then(literal("status").executes(Commands::status))
				.then(literal("reload").executes(ctx -> {
					ModConfig.load();
					BlockEsp.rebuildFilter();
					return ok(ctx, "Konfiguration neu geladen.");
				}))
				.then(literal("toggle")
						.then(argument("modul", StringArgumentType.word())
								.suggests((ctx, b) -> SharedSuggestionProvider.suggest(MODULES, b))
								.executes(ctx -> {
									String m = StringArgumentType.getString(ctx, "modul").toLowerCase(Locale.ROOT);
									Boolean state = ErleichterungenClient.toggle(m);
									if (state == null) return err(ctx, "Unbekanntes Modul: " + m);
									return 1;
								})))
				.then(literal("blockesp")
						.then(literal("add")
								.then(argument("block", StringArgumentType.greedyString()).suggests(BLOCK_IDS)
										.executes(ctx -> addBlock(ctx, null))))
						.then(literal("color")
								.then(argument("farbe", StringArgumentType.word())
										.then(argument("block", StringArgumentType.greedyString()).suggests(CONFIGURED_BLOCKS)
												.executes(ctx -> addBlock(ctx, StringArgumentType.getString(ctx, "farbe"))))))
						.then(literal("remove")
								.then(argument("block", StringArgumentType.greedyString()).suggests(CONFIGURED_BLOCKS)
										.executes(Commands::removeBlock)))
						.then(literal("list").executes(Commands::listBlocks))
						.then(literal("clear").executes(ctx -> {
							ModConfig.get().blockEspBlocks.clear();
							ModConfig.save();
							BlockEsp.rebuildFilter();
							return ok(ctx, "Block-Liste geleert.");
						}))
						.then(intSetting("range", 8, 512, (c, v) -> c.blockEspRange = v))
						.then(intSetting("max", 10, 20000, (c, v) -> c.blockEspMaxBoxes = v))
						.then(boolSetting("fill", c -> c.blockEspFill = !c.blockEspFill, c -> c.blockEspFill)))
				.then(literal("playeresp")
						.then(intSetting("range", 8, 1024, (c, v) -> c.playerEspRange = v))
						.then(boolSetting("names", c -> c.playerEspNames = !c.playerEspNames, c -> c.playerEspNames))
						.then(boolSetting("fill", c -> c.playerEspFill = !c.playerEspFill, c -> c.playerEspFill)))
				.then(literal("geode")
						.then(intSetting("range", 8, 512, (c, v) -> c.geodeRange = v))
						.then(boolSetting("budding", c -> c.geodeShowBudding = !c.geodeShowBudding, c -> c.geodeShowBudding))
						.then(boolSetting("growing", c -> c.geodeShowGrowing = !c.geodeShowGrowing, c -> c.geodeShowGrowing))
						.then(boolSetting("notify", c -> c.geodeNotifyFullyGrown = !c.geodeNotifyFullyGrown, c -> c.geodeNotifyFullyGrown))
						.then(boolSetting("sound", c -> c.geodeSound = !c.geodeSound, c -> c.geodeSound))
						.then(literal("stats").executes(Commands::geodeStats))
						.then(literal("reset").executes(ctx -> {
							GeodeTracker.reset();
							return ok(ctx, "Geoden-Statistik zurückgesetzt.");
						})))
				.then(literal("antivanish")
						.then(literal("list").executes(Commands::vanishList))
						.then(literal("clear").executes(ctx -> {
							AntiVanish.reset();
							return ok(ctx, "Anti-Vanish-Liste geleert.");
						}))
						.then(literal("command")
								.then(argument("befehl", StringArgumentType.word()).executes(ctx -> {
									ModConfig.get().antiVanishCompletionCommand = StringArgumentType.getString(ctx, "befehl");
									ModConfig.save();
									return ok(ctx, "Vorschlags-Befehl: /" + ModConfig.get().antiVanishCompletionCommand);
								})))
						.then(intSetting("interval", 2, 600, (c, v) -> c.antiVanishCompletionIntervalSec = v))
						.then(intSetting("timeout", 250, 30000, (c, v) -> c.antiVanishLeaveTimeoutMs = v))
						.then(boolSetting("leave", c -> c.antiVanishLeaveMessageCheck = !c.antiVanishLeaveMessageCheck, c -> c.antiVanishLeaveMessageCheck))
						.then(boolSetting("completion", c -> c.antiVanishCompletionCheck = !c.antiVanishCompletionCheck, c -> c.antiVanishCompletionCheck))
						.then(boolSetting("entity", c -> c.antiVanishEntityCheck = !c.antiVanishEntityCheck, c -> c.antiVanishEntityCheck))
						.then(boolSetting("spectator", c -> c.antiVanishSpectatorCheck = !c.antiVanishSpectatorCheck, c -> c.antiVanishSpectatorCheck))
						.then(boolSetting("sound", c -> c.antiVanishSound = !c.antiVanishSound, c -> c.antiVanishSound))));
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> intSetting(
			String name, int min, int max, BiConsumer<ModConfig, Integer> setter) {
		return literal(name).then(argument("wert", IntegerArgumentType.integer(min, max)).executes(ctx -> {
			int v = IntegerArgumentType.getInteger(ctx, "wert");
			setter.accept(ModConfig.get(), v);
			ModConfig.save();
			return ok(ctx, name + " = " + v);
		}));
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> boolSetting(
			String name, java.util.function.Consumer<ModConfig> toggler, Function<ModConfig, Boolean> getter) {
		return literal(name).executes(ctx -> {
			toggler.accept(ModConfig.get());
			ModConfig.save();
			return ok(ctx, name + ": " + (getter.apply(ModConfig.get()) ? "an" : "aus"));
		});
	}

	private static int addBlock(CommandContext<FabricClientCommandSource> ctx, String color) {
		String id = BlockEsp.normalize(StringArgumentType.getString(ctx, "block").trim());
		if (BlockEsp.resolve(id) == null) return err(ctx, "Unbekannter Block: " + id);
		String hex = color != null ? color.replace("#", "") : String.format("%06X", BlockEsp.defaultColor(id) & 0xFFFFFF);
		if (color != null && ModConfig.parseColor(hex, 0) == 0 && !hex.matches("0+")) return err(ctx, "Ungültige Farbe: " + color);
		ModConfig.get().blockEspBlocks.put(id, hex);
		ModConfig.save();
		BlockEsp.rebuildFilter();
		return ok(ctx, id + " hinzugefügt (Farbe #" + hex + ").");
	}

	private static int removeBlock(CommandContext<FabricClientCommandSource> ctx) {
		String id = BlockEsp.normalize(StringArgumentType.getString(ctx, "block").trim());
		if (ModConfig.get().blockEspBlocks.remove(id) == null) return err(ctx, id + " ist nicht in der Liste.");
		ModConfig.save();
		BlockEsp.rebuildFilter();
		return ok(ctx, id + " entfernt.");
	}

	private static int listBlocks(CommandContext<FabricClientCommandSource> ctx) {
		Map<String, String> blocks = ModConfig.get().blockEspBlocks;
		if (blocks.isEmpty()) return ok(ctx, "Keine Blöcke in der Liste. /erl blockesp add <block>");
		ctx.getSource().sendFeedback(Component.literal("Block-ESP Blöcke:").withStyle(ChatFormatting.LIGHT_PURPLE));
		for (Map.Entry<String, String> e : blocks.entrySet()) {
			int color = ModConfig.parseColor(e.getValue(), 0xFFFFFFFF);
			ctx.getSource().sendFeedback(Component.literal(" ■ ").withColor(color & 0xFFFFFF)
					.append(Component.literal(e.getKey() + " #" + e.getValue()).withStyle(ChatFormatting.GRAY)));
		}
		return 1;
	}

	private static int geodeStats(CommandContext<FabricClientCommandSource> ctx) {
		FabricClientCommandSource src = ctx.getSource();
		src.sendFeedback(Component.literal("Geoden-Statistik (Sitzung):").withStyle(ChatFormatting.LIGHT_PURPLE));
		for (int s = GeodeTracker.STAGE_SMALL; s <= GeodeTracker.STAGE_CLUSTER; s++) {
			src.sendFeedback(Component.literal(" " + GeodeTracker.stageName(s) + ": " + GeodeTracker.grownTo(s) + "x gewachsen")
					.withColor(GeodeTracker.stageColor(s) & 0xFFFFFF));
		}
		src.sendFeedback(Component.literal(" Wachstums-Ereignisse in den letzten 10 min: " + GeodeTracker.growthLast10Min())
				.withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int vanishList(CommandContext<FabricClientCommandSource> ctx) {
		Map<String, String> v = AntiVanish.vanished();
		if (v.isEmpty()) return ok(ctx, "Keine versteckten Spieler erkannt.");
		return ok(ctx, "Vermutlich im Vanish: " + String.join(", ", v.values()));
	}

	private static int status(CommandContext<FabricClientCommandSource> ctx) {
		ModConfig c = ModConfig.get();
		ok(ctx, "Block-ESP: " + onOff(c.blockEspEnabled) + " | Player-ESP: " + onOff(c.playerEspEnabled)
				+ " | Geoden: " + onOff(c.geodeTrackerEnabled) + " | Anti-Vanish: " + onOff(c.antiVanishEnabled)
				+ " | HUD: " + onOff(c.hudEnabled));
		return 1;
	}

	private static String onOff(boolean b) {
		return b ? "an" : "aus";
	}

	private static int help(CommandContext<FabricClientCommandSource> ctx) {
		FabricClientCommandSource s = ctx.getSource();
		s.sendFeedback(Component.literal("Erleichterungen – Befehle:").withStyle(ChatFormatting.LIGHT_PURPLE));
		String[] lines = {
				"/erl toggle <blockesp|playeresp|geode|antivanish|hud>",
				"/erl status",
				"/erl blockesp add <block> | color <hex> <block> | remove <block> | list | clear",
				"/erl blockesp range <n> | max <n> | fill",
				"/erl playeresp range <n> | names | fill",
				"/erl geode range <n> | budding | growing | notify | sound | stats | reset",
				"/erl antivanish list | clear | command <cmd> | interval <s> | timeout <ms>",
				"/erl antivanish leave | completion | entity | spectator | sound",
				"/erl reload",
				"Tasten (änderbar in Steuerung): F6 Block-ESP, F7 Player-ESP, F8 Geoden, F9 Anti-Vanish, F10 HUD"
		};
		for (String l : lines) s.sendFeedback(Component.literal(" " + l).withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int ok(CommandContext<FabricClientCommandSource> ctx, String msg) {
		ctx.getSource().sendFeedback(Component.literal(msg).withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int err(CommandContext<FabricClientCommandSource> ctx, String msg) {
		ctx.getSource().sendError(Component.literal(msg));
		return 0;
	}
}
