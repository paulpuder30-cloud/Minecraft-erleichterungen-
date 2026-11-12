package de.erleichterungen.test;

import com.mojang.blaze3d.platform.InputConstants;
import de.erleichterungen.config.EspEntry;
import de.erleichterungen.config.ModConfig;
import de.erleichterungen.config.RenderMode;
import de.erleichterungen.feature.Freecam;
import de.erleichterungen.gui.ClickGuiScreen;
import de.erleichterungen.gui.ListEditScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.phys.Vec3;

/**
 * Startet eine Testwelt, platziert Blöcke/Entities und prüft ESP, Click-GUI und Freecam.
 * Screenshots landen im Run-Verzeichnis unter screenshots/.
 */
public class ErleichterungenClientGameTest implements FabricClientGameTest {
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
		}
	}
}
