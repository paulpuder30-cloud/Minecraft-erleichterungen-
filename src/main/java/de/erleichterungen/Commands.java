package de.erleichterungen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.erleichterungen.config.EspEntry;
import de.erleichterungen.config.ModConfig;
import de.erleichterungen.config.RenderMode;
import de.erleichterungen.feature.AntiVanish;
import de.erleichterungen.feature.BlockEsp;
import de.erleichterungen.feature.EntityEsp;
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
			SharedSuggestionProvider.suggest(ModConfig.get().blockEsp.keySet(), builder);

	private static final SuggestionProvider<FabricClientCommandSource> ENTITY_IDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString), builder);

	private static final SuggestionProvider<FabricClientCommandSource> CONFIGURED_ENTITIES = (ctx, builder) ->
			SharedSuggestionProvider.suggest(ModConfig.get().entityEsp.keySet(), builder);

	private static final String[] MODULES = {"blockesp", "entityesp", "playeresp", "geode", "antivanish", "hud"};

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("erl")
				.executes(Commands::help)
				.then(literal("help").executes(Commands::help))
				.then(literal("status").executes(Commands::status))
				.then(literal("reload").executes(ctx -> {
					ModConfig.load();
					BlockEsp.rebuildFilter();
					EntityEsp.rebuildFilter();
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
							ModConfig.get().blockEsp.clear();
							ModConfig.save();
							BlockEsp.rebuildFilter();
							return ok(ctx, "Block-Liste geleert.");
						}))
						.then(intSetting("range", 8, 512, (c, v) -> c.blockEspRange = v))
						.then(intSetting("max", 10, 20000, (c, v) -> c.blockEspMaxBoxes = v))
						.then(boolSetting("fill", c -> c.blockEspFill = !c.blockEspFill, c -> c.blockEspFill))
						.then(listModeCommand("block", CONFIGURED_BLOCKS, () -> ModConfig.get().blockEsp))
						.then(listTracerColorCommand("block", CONFIGURED_BLOCKS, () -> ModConfig.get().blockEsp)))
				.then(literal("entityesp")
						.then(literal("add")
								.then(argument("entity", StringArgumentType.greedyString()).suggests(ENTITY_IDS)
										.executes(ctx -> addEntity(ctx, null))))
						.then(literal("color")
								.then(argument("farbe", StringArgumentType.word())
										.then(argument("entity", StringArgumentType.greedyString()).suggests(CONFIGURED_ENTITIES)
												.executes(ctx -> addEntity(ctx, StringArgumentType.getString(ctx, "farbe"))))))
						.then(literal("remove")
								.then(argument("entity", StringArgumentType.greedyString()).suggests(CONFIGURED_ENTITIES)
										.executes(Commands::removeEntity)))
						.then(literal("list").executes(Commands::listEntities))
						.then(literal("clear").executes(ctx -> {
							ModConfig.get().entityEsp.clear();
							ModConfig.save();
							EntityEsp.rebuildFilter();
							return ok(ctx, "Entity-Liste geleert.");
						}))
						.then(intSetting("range", 8, 512, (c, v) -> c.entityEspRange = v))
						.then(intSetting("max", 10, 20000, (c, v) -> c.entityEspMaxBoxes = v))
						.then(boolSetting("labels", c -> c.entityEspLabels = !c.entityEspLabels, c -> c.entityEspLabels))
						.then(boolSetting("fill", c -> c.entityEspFill = !c.entityEspFill, c -> c.entityEspFill))
						.then(listModeCommand("entity", CONFIGURED_ENTITIES, () -> ModConfig.get().entityEsp))
						.then(listTracerColorCommand("entity", CONFIGURED_ENTITIES, () -> ModConfig.get().entityEsp)))
				.then(literal("playeresp")
						.then(intSetting("range", 8, 1024, (c, v) -> c.playerEspRange = v))
						.then(boolSetting("names", c -> c.playerEspNames = !c.playerEspNames, c -> c.playerEspNames))
						.then(boolSetting("fill", c -> c.playerEspFill = !c.playerEspFill, c -> c.playerEspFill))
						.then(modeCommand((c, m) -> c.playerEspMode = m))
						.then(tracerColorCommand((c, v) -> c.playerEspTracerColor = v)))
				.then(literal("geode")
						.then(intSetting("range", 8, 512, (c, v) -> c.geodeRange = v))
						.then(boolSetting("budding", c -> c.geodeShowBudding = !c.geodeShowBudding, c -> c.geodeShowBudding))
						.then(boolSetting("growing", c -> c.geodeShowGrowing = !c.geodeShowGrowing, c -> c.geodeShowGrowing))
						.then(boolSetting("notify", c -> c.geodeNotifyFullyGrown = !c.geodeNotifyFullyGrown, c -> c.geodeNotifyFullyGrown))
						.then(boolSetting("sound", c -> c.geodeSound = !c.geodeSound, c -> c.geodeSound))
						.then(modeCommand((c, m) -> c.geodeMode = m))
						.then(tracerColorCommand((c, v) -> c.geodeTracerColor = v))
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
						.then(boolSetting("sound", c -> c.antiVanishSound = !c.antiVanishSound, c -> c.antiVanishSound))
						.then(boolSetting("highlight", c -> c.antiVanishHighlight = !c.antiVanishHighlight, c -> c.antiVanishHighlight))
						.then(modeCommand((c, m) -> c.antiVanishMode = m))
						.then(tracerColorCommand((c, v) -> c.antiVanishTracerColor = v)))
				.then(literal("tracers")
						.then(literal("all").then(argument("an", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(ctx -> {
							boolean on = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "an");
							ModConfig.get().setAllTracers(on);
							ModConfig.save();
							return ok(ctx, "Alle Tracer: " + onOff(on));
						})))
						.then(literal("width").then(argument("breite", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.5f, 10f)).executes(ctx -> {
							ModConfig.get().tracerWidth = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "breite");
							ModConfig.save();
							return ok(ctx, "Tracer-Breite = " + ModConfig.get().tracerWidth);
						})))));
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

	private static final SuggestionProvider<FabricClientCommandSource> MODES = (ctx, b) ->
			SharedSuggestionProvider.suggest(new String[]{"box", "beide", "tracer"}, b);

	private static String hexOf(String color) {
		String hex = color.replace("#", "").toUpperCase(Locale.ROOT);
		return hex.matches("[0-9A-F]{6}") ? hex : null;
	}

	/** /erl <modul> mode <box|beide|tracer> */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> modeCommand(
			BiConsumer<ModConfig, RenderMode> setter) {
		return literal("mode").then(argument("modus", StringArgumentType.word()).suggests(MODES).executes(ctx -> {
			RenderMode m = RenderMode.parse(StringArgumentType.getString(ctx, "modus"));
			if (m == null) return err(ctx, "Modus muss box, beide oder tracer sein.");
			setter.accept(ModConfig.get(), m);
			ModConfig.save();
			return ok(ctx, "Modus: " + m.label);
		}));
	}

	/** /erl <modul> tracercolor <hex> */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> tracerColorCommand(
			BiConsumer<ModConfig, String> setter) {
		return literal("tracercolor").then(argument("farbe", StringArgumentType.word()).executes(ctx -> {
			String hex = hexOf(StringArgumentType.getString(ctx, "farbe"));
			if (hex == null) return err(ctx, "Farbe als RRGGBB angeben, z.B. FF0000.");
			setter.accept(ModConfig.get(), hex);
			ModConfig.save();
			return ok(ctx, "Tracer-Farbe: #" + hex);
		}));
	}

	/** /erl blockesp mode <modus> [block] – ohne Block für alle Einträge. */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> listModeCommand(
			String argName, SuggestionProvider<FabricClientCommandSource> configured, java.util.function.Supplier<Map<String, EspEntry>> map) {
		return literal("mode").then(argument("modus", StringArgumentType.word()).suggests(MODES)
				.executes(ctx -> {
					RenderMode m = RenderMode.parse(StringArgumentType.getString(ctx, "modus"));
					if (m == null) return err(ctx, "Modus muss box, beide oder tracer sein.");
					map.get().values().forEach(e -> e.mode = m);
					ModConfig.save();
					return ok(ctx, "Alle Einträge: " + m.label);
				})
				.then(argument(argName, StringArgumentType.greedyString()).suggests(configured).executes(ctx -> {
					RenderMode m = RenderMode.parse(StringArgumentType.getString(ctx, "modus"));
					if (m == null) return err(ctx, "Modus muss box, beide oder tracer sein.");
					String id = BlockEsp.normalize(StringArgumentType.getString(ctx, argName).trim());
					EspEntry e = map.get().get(id);
					if (e == null) return err(ctx, id + " ist nicht in der Liste.");
					e.mode = m;
					ModConfig.save();
					return ok(ctx, id + ": " + m.label);
				})));
	}

	/** /erl blockesp tracercolor <hex> <block> */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> listTracerColorCommand(
			String argName, SuggestionProvider<FabricClientCommandSource> configured, java.util.function.Supplier<Map<String, EspEntry>> map) {
		return literal("tracercolor").then(argument("farbe", StringArgumentType.word())
				.then(argument(argName, StringArgumentType.greedyString()).suggests(configured).executes(ctx -> {
					String hex = hexOf(StringArgumentType.getString(ctx, "farbe"));
					if (hex == null) return err(ctx, "Farbe als RRGGBB angeben, z.B. FF0000.");
					String id = BlockEsp.normalize(StringArgumentType.getString(ctx, argName).trim());
					EspEntry e = map.get().get(id);
					if (e == null) return err(ctx, id + " ist nicht in der Liste.");
					e.tracerColor = hex;
					ModConfig.save();
					return ok(ctx, id + " Tracer-Farbe: #" + hex);
				})));
	}

	private static int addEntry(CommandContext<FabricClientCommandSource> ctx, String argName, Map<String, EspEntry> map,
			boolean valid, String id, String color, Runnable rebuild) {
		if (!valid) return err(ctx, "Unbekannt: " + id);
		String hex = color != null ? hexOf(color) : String.format("%06X", BlockEsp.defaultColor(id) & 0xFFFFFF);
		if (hex == null) return err(ctx, "Farbe als RRGGBB angeben, z.B. FF0000.");
		EspEntry existing = map.get(id);
		if (existing != null) {
			existing.color = hex;
		} else {
			map.put(id, EspEntry.of(hex));
		}
		ModConfig.save();
		rebuild.run();
		return ok(ctx, id + (existing != null ? " Box-Farbe: #" : " hinzugefügt (Farbe #") + hex + (existing != null ? "" : ")"));
	}

	private static int addBlock(CommandContext<FabricClientCommandSource> ctx, String color) {
		String id = BlockEsp.normalize(StringArgumentType.getString(ctx, "block").trim());
		return addEntry(ctx, "block", ModConfig.get().blockEsp, BlockEsp.resolve(id) != null, id, color, BlockEsp::rebuildFilter);
	}

	private static int addEntity(CommandContext<FabricClientCommandSource> ctx, String color) {
		String id = BlockEsp.normalize(StringArgumentType.getString(ctx, "entity").trim());
		return addEntry(ctx, "entity", ModConfig.get().entityEsp, EntityEsp.resolve(id) != null, id, color, EntityEsp::rebuildFilter);
	}

	private static int removeBlock(CommandContext<FabricClientCommandSource> ctx) {
		String id = BlockEsp.normalize(StringArgumentType.getString(ctx, "block").trim());
		if (ModConfig.get().blockEsp.remove(id) == null) return err(ctx, id + " ist nicht in der Liste.");
		ModConfig.save();
		BlockEsp.rebuildFilter();
		return ok(ctx, id + " entfernt.");
	}

	private static int removeEntity(CommandContext<FabricClientCommandSource> ctx) {
		String id = BlockEsp.normalize(StringArgumentType.getString(ctx, "entity").trim());
		if (ModConfig.get().entityEsp.remove(id) == null) return err(ctx, id + " ist nicht in der Liste.");
		ModConfig.save();
		EntityEsp.rebuildFilter();
		return ok(ctx, id + " entfernt.");
	}

	private static int listEntries(CommandContext<FabricClientCommandSource> ctx, String title, Map<String, EspEntry> map) {
		if (map.isEmpty()) return ok(ctx, "Die Liste ist leer.");
		ctx.getSource().sendFeedback(Component.literal(title).withStyle(ChatFormatting.LIGHT_PURPLE));
		for (Map.Entry<String, EspEntry> e : map.entrySet()) {
			EspEntry en = e.getValue();
			ctx.getSource().sendFeedback(Component.literal(" ■").withColor(en.boxColor() & 0xFFFFFF)
					.append(Component.literal("━ ").withColor(en.tracerColor() & 0xFFFFFF))
					.append(Component.literal(e.getKey() + " [" + en.mode().label + "]").withStyle(ChatFormatting.GRAY)));
		}
		return 1;
	}

	private static int listBlocks(CommandContext<FabricClientCommandSource> ctx) {
		return listEntries(ctx, "Block-ESP Blöcke:", ModConfig.get().blockEsp);
	}

	private static int listEntities(CommandContext<FabricClientCommandSource> ctx) {
		return listEntries(ctx, "Entity-ESP Typen:", ModConfig.get().entityEsp);
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
		ok(ctx, "Block-ESP: " + onOff(c.blockEspEnabled) + " | Entity-ESP: " + onOff(c.entityEspEnabled) + " | Player-ESP: " + onOff(c.playerEspEnabled)
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
				"/erl toggle <blockesp|entityesp|playeresp|geode|antivanish|hud>",
				"/erl status",
				"/erl blockesp add <block> | color <hex> <block> | remove <block> | list | clear",
				"/erl blockesp range <n> | max <n> | fill",
				"/erl blockesp mode <box|beide|tracer> [block] | tracercolor <hex> <block>",
				"/erl entityesp add <entity> | color <hex> <entity> | remove <entity> | list | clear",
				"/erl entityesp range <n> | max <n> | labels | fill",
				"/erl entityesp mode <box|beide|tracer> [entity] | tracercolor <hex> <entity>",
				"/erl playeresp range <n> | names | fill | mode <box|beide|tracer> | tracercolor <hex>",
				"/erl geode range <n> | budding | growing | notify | sound | mode <..> | tracercolor <hex> | stats | reset",
				"/erl antivanish list | clear | command <cmd> | interval <s> | timeout <ms>",
				"/erl antivanish leave | completion | entity | spectator | sound | highlight | mode <..> | tracercolor <hex>",
				"/erl tracers all <true|false> | width <breite>",
				"/erl reload",
				"Tasten: Rechte Shift = GUI, G Freecam, F4 Entity-ESP, F6 Block-ESP, F7 Player-ESP, F8 Geoden, F9 Anti-Vanish, F10 HUD"
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
