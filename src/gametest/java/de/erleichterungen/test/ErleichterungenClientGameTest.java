package de.erleichterungen.test;

import com.mojang.blaze3d.platform.InputConstants;
import de.erleichterungen.config.EspEntry;
import de.erleichterungen.config.ModConfig;
import de.erleichterungen.config.RenderMode;
import de.erleichterungen.feature.Excavator;
import de.erleichterungen.feature.Freecam;
import de.erleichterungen.gui.ClickGuiScreen;
import de.erleichterungen.gui.ListEditScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Startet eine Testwelt, platziert Blöcke/Entities und prüft ESP, Click-GUI und Freecam.
 * Screenshots landen im Run-Verzeichnis unter screenshots/.
 */
public class ErleichterungenClientGameTest implements FabricClientGameTest {
	/** Simulierte 3x3-Spitzhacke (wie ein Server-Plugin): baut die senkrechte 3x3-Fläche quer zur Blickrichtung ab. */
	private static volatile boolean hammer;
	private static boolean inHammer;
	private static volatile int hammerHits;

	private static void registerHammer() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
			if (!hammer || inHammer) return;
			inHammer = true;
			try {
				hammerHits++;
				Direction facing = player.getDirection();
				for (int a = -1; a <= 1; a++) {
					for (int b = -1; b <= 1; b++) {
						BlockPos p = facing.getAxis() == Direction.Axis.X ? pos.offset(0, a, b) : pos.offset(a, b, 0);
						if (p.equals(pos) || level.getBlockState(p).isAir() || level.getBlockState(p).getDestroySpeed(level, p) < 0) continue;
						level.destroyBlock(p, false, player, 512);
					}
				}
			} finally {
				inHammer = false;
			}
		});
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			sp.getServer().runCommand("time set day");
			sp.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 20");
			sp.getServer().runCommand("execute as @a at @s run setblock ~3 ~-2 ~6 minecraft:diamond_ore");
			sp.getServer().runCommand("execute as @a at @s run setblock ~-3 ~ ~7 minecraft:budding_amethyst");
			sp.getServer().runCommand("execute as @a at @s run setblock ~-3 ~1 ~7 minecraft:amethyst_cluster[facing=up]");
			sp.getServer().runCommand("execute as @a at @s run setblock ~-2 ~ ~7 minecraft:large_amethyst_bud[facing=east]");
			sp.getServer().runCommand("execute as @a at @s run summon minecraft:armor_stand ~1 ~ ~6");
			sp.getServer().runCommand("execute as @a at @s run setblock ~0 ~ ~9 minecraft:stone");
			sp.getServer().runCommand("execute as @a at @s run summon minecraft:item_frame ~0 ~ ~8 {Facing:2b,Item:{id:\"minecraft:elytra\",count:1}}");
			sp.getConnection().waitForChunksRender();

			ctx.runOnClient(mc -> {
				ModConfig c = ModConfig.get();
				c.blockEspEnabled = true;
				// Diamanterz: Box & Tracer mit eigener (gelber) Tracer-Farbe
				EspEntry diamond = c.blockEsp.get("minecraft:diamond_ore");
				diamond.mode = RenderMode.BOTH;
				diamond.tracerColor = "FFFF00";
				// Item Frame: nur Tracer (rot)
				EspEntry frame = c.entityEsp.get("minecraft:item_frame");
				frame.mode = RenderMode.TRACER;
				frame.tracerColor = "FF0000";
				c.entityEspEnabled = true;
				c.geodeTrackerEnabled = true;
				c.hudEnabled = true;
			});
			ctx.waitTicks(40);
			ctx.takeScreenshot("01-esp-und-tracer");

			// ---- Click-GUI
			TestInput in = ctx.getInput();
			in.pressKey(InputConstants.KEY_RSHIFT);
			ctx.waitForScreen(ClickGuiScreen.class);
			ctx.waitTicks(3);
			ctx.takeScreenshot("02-gui");

			double scale = ctx.computeOnClient(mc -> (double) mc.getWindow().getGuiScale());
			// Rechtsklick auf "Block-ESP" (erstes Modul im Render-Fenster) → Einstellungen
			in.setCursorPos(60 * scale, 31 * scale);
			in.pressMouse(1);
			ctx.waitTicks(3);
			ctx.takeScreenshot("03-gui-block-esp-einstellungen");

			// Klick auf "Blöcke" → Listen-Editor
			in.setCursorPos(60 * scale, 57 * scale);
			in.pressMouse(0);
			ctx.waitForScreen(ListEditScreen.class);
			// ersten Eintrag (Diamanterz) zum Bearbeiten auswählen
			int[] layout = ctx.computeOnClient(mc -> new int[]{mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()});
			int totalW = Math.min(440, layout[0] - 16);
			int leftX = (layout[0] - totalW) / 2;
			in.setCursorPos((leftX + 40) * scale, (50 + 6) * scale);
			in.pressMouse(0);
			in.typeChars("gold");
			ctx.waitTicks(3);
			ctx.takeScreenshot("04-liste-bearbeiten");
			in.pressKey(InputConstants.KEY_ESCAPE); // Suche leeren
			in.pressKey(InputConstants.KEY_ESCAPE); // zurück
			ctx.waitForScreen(ClickGuiScreen.class);

			// Suche in der Haupt-GUI
			in.typeChars("tracer");
			ctx.waitTicks(3);
			ctx.takeScreenshot("05-gui-suche");
			in.pressKey(InputConstants.KEY_ESCAPE); // Suche leeren
			in.pressKey(InputConstants.KEY_ESCAPE); // GUI schließen
			ctx.waitForScreen(null);

			// "Alle Tracer aus" muss jeden Modus auf Box setzen
			ctx.runOnClient(mc -> ModConfig.get().setAllTracers(false));
			boolean anyTracer = ctx.computeOnClient(mc -> ModConfig.get().blockEsp.values().stream().anyMatch(e -> e.mode().tracer())
					|| ModConfig.get().entityEsp.values().stream().anyMatch(e -> e.mode().tracer())
					|| ModConfig.get().playerEspMode.tracer());
			if (anyTracer) throw new AssertionError("Alle Tracer aus hat nicht alle Tracer entfernt");
			ctx.runOnClient(mc -> ModConfig.get().setAllTracers(true));

			// ---- Freecam
			Vec3 before = ctx.computeOnClient(mc -> mc.player.position());
			in.pressKey(InputConstants.KEY_G);
			ctx.waitTicks(2);
			if (!ctx.computeOnClient(mc -> Freecam.isActive())) throw new AssertionError("Freecam wurde nicht aktiviert");
			in.holdKeyFor(o -> o.keyDown, 25);
			in.holdKeyFor(o -> o.keyJump, 15);
			ctx.waitTicks(5);
			ctx.takeScreenshot("06-freecam");
			Vec3 after = ctx.computeOnClient(mc -> mc.player.position());
			if (before.distanceTo(after) > 0.5) {
				throw new AssertionError("Spieler hat sich in Freecam bewegt: " + before + " -> " + after);
			}
			in.pressKey(InputConstants.KEY_G);
			ctx.waitTicks(2);
			if (ctx.computeOnClient(mc -> Freecam.isActive())) throw new AssertionError("Freecam wurde nicht deaktiviert");

			// ---- Ausheben
			registerHammer();
			sp.getServer().runCommand("kill @e[type=!player]");
			sp.getServer().runCommand("gamemode survival @a");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("give @a minecraft:netherite_pickaxe[enchantments={efficiency:5}]");
			BlockPos base = ctx.computeOnClient(mc -> mc.player.blockPosition());
			int bx = base.getX(), by = base.getY() + 15, bz = base.getZ(); // über dem Grundgestein
			// Bereich: 5 breit (x), 6 tief (y), 7 lang (z), direkt vor dem Spieler (Blick nach Süden)
			BlockPos a1 = new BlockPos(bx - 2, by - 6, bz + 1), a2 = new BlockPos(bx + 2, by - 1, bz + 7);

			excavateRun(ctx, sp, bx, by, bz, a1, a2, Excavator.TOOL_NORMAL, "07-ausheben-normal");
			if (overDig(ctx, a1, a2) > 0) throw new AssertionError("Normal: außerhalb des Bereichs abgebaut: " + overDig(ctx, a1, a2));

			hammer = true;
			hammerHits = 0;
			excavateRun(ctx, sp, bx, by, bz, a1, a2, Excavator.TOOL_3X3, "08-ausheben-3x3");
			hammer = false;
			int over = overDig(ctx, a1, a2);
			System.out.println("[Erleichterungen-Test] 3x3: " + hammerHits + " Schläge, außerhalb abgebaut: " + over);
			if (over > 0) throw new AssertionError("3x3: außerhalb des Bereichs abgebaut: " + over);

			// Ausheben-Einstellungen in der GUI (Rechtsklick auf "Ausheben" im Welt-Fenster)
			in.pressKey(InputConstants.KEY_RSHIFT);
			ctx.waitForScreen(ClickGuiScreen.class);
			in.setCursorPos(190 * scale, 44 * scale);
			in.pressMouse(1);
			in.setCursorPos(420 * scale, 200 * scale);
			ctx.waitTicks(3);
			ctx.takeScreenshot("09-gui-ausheben");
			in.pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitForScreen(null);
		}
	}

	/** Boden aufbauen, Bereich auswählen, ausheben und prüfen, dass alles weg ist. */
	private static void excavateRun(ClientGameTestContext ctx, TestSingleplayerContext sp, int bx, int by, int bz,
			BlockPos a1, BlockPos a2, int tool, String screenshot) {
		sp.getServer().runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", bx - 6, by - 9, bz - 5, bx + 6, by - 1, bz + 13));
		sp.getServer().runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", bx - 6, by, bz - 5, bx + 6, by + 6, bz + 13));
		sp.getServer().runCommand(String.format("tp @a %d.5 %d %d.5 0 30", bx, by, bz));
		ctx.waitTicks(20);
		ctx.runOnClient(mc -> {
			ModConfig.get().excavateTool = tool;
			ModConfig.get().excavateAutoWalk = true;
			Excavator.setPos1(a1);
			Excavator.setPos2(a2);
			Excavator.start();
		});
		int volume = 5 * 6 * 7;
		boolean shot = false;
		for (int i = 0; i < 400 && ctx.computeOnClient(mc -> Excavator.isActive()); i++) {
			ctx.waitTicks(5);
			if (i % 40 == 0) {
				String st = ctx.computeOnClient(mc -> mc.player.position() + " " + Excavator.hudLine());
				System.out.println("[Erleichterungen-Test] " + remaining(ctx, a1, a2) + " übrig, " + st);
			}
			if (!shot && remaining(ctx, a1, a2) < volume / 2) {
				ctx.takeScreenshot(screenshot);
				shot = true;
			}
		}
		int left = remaining(ctx, a1, a2);
		if (ctx.computeOnClient(mc -> Excavator.isActive())) {
			ctx.runOnClient(mc -> Excavator.stop(false));
			throw new AssertionError("Ausheben (" + Excavator.TOOL_LABELS[tool] + ") nicht fertig geworden, übrig: " + left);
		}
		if (left > 0) throw new AssertionError("Ausheben (" + Excavator.TOOL_LABELS[tool] + ") hat " + left + " Blöcke übrig gelassen");
	}

	private static int remaining(ClientGameTestContext ctx, BlockPos a1, BlockPos a2) {
		return ctx.computeOnClient(mc -> {
			int n = 0;
			for (BlockPos p : BlockPos.betweenClosed(a1, a2)) if (!mc.level.getBlockState(p).isAir()) n++;
			return n;
		});
	}

	/** Fehlende Blöcke in der Hülle direkt um den Bereich (unterhalb der Oberfläche). */
	private static int overDig(ClientGameTestContext ctx, BlockPos a1, BlockPos a2) {
		return ctx.computeOnClient(mc -> {
			int n = 0;
			for (BlockPos p : BlockPos.betweenClosed(a1.offset(-1, -1, -1), new BlockPos(a2.getX() + 1, a2.getY(), a2.getZ() + 1))) {
				boolean inside = p.getX() >= a1.getX() && p.getX() <= a2.getX() && p.getY() >= a1.getY()
						&& p.getZ() >= a1.getZ() && p.getZ() <= a2.getZ();
				if (!inside && mc.level.getBlockState(p).isAir()) n++;
			}
			return n;
		});
	}
}
