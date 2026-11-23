/*
 * This file is part of ERL-Client.
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.test;

import com.mojang.blaze3d.platform.InputConstants;
import de.erl.client.ErlExtras;
import de.erl.client.modules.LungeSwap;
import de.erl.client.modules.ShieldBreaker;
import de.erl.client.modules.StunSlam;
import de.erl.client.utils.ErlCombatUtils;
import de.erl.client.utils.misc.input.KeyBinds;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import de.erl.client.ErlClient;
import de.erl.client.gui.GuiThemes;
import de.erl.client.gui.tabs.TabScreen;
import de.erl.client.settings.Setting;
import de.erl.client.systems.modules.Module;
import de.erl.client.systems.modules.Modules;
import de.erl.client.utils.misc.Keybind;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Startet eine Testwelt und prüft ERL-Client: Branding, Click-GUI, Keybinds und die drei ERL-Module gegen einen
 * echten, Schild blockenden Gegner. Screenshots landen im Run-Verzeichnis unter screenshots/.
 *
 * <p>Ausführen: {@code ./gradlew runClientGameTest} (ohne Bildschirm: {@code xvfb-run -a ./gradlew runClientGameTest}).
 */
public class ErlClientGameTest implements FabricClientGameTest {
    // Hotbar-Belegung im Test
    private static final int SWORD = 0, IRON_AXE = 1, NETHERITE_AXE = 2, MACE = 3, SPEAR = 4;

    private BlockPos base;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        // ---- Branding & Registrierung
        ctx.waitTicks(40);
        ctx.takeScreenshot("00-titelbildschirm");

        ctx.runOnClient(mc -> {
            check(ErlClient.MOD_ID.equals("erl-client"), "Mod-ID ist nicht erl-client: " + ErlClient.MOD_ID);
            check(ErlClient.NAME.equals("ERL-Client"), "Name ist nicht ERL-Client: " + ErlClient.NAME);
            check(ErlClient.FOLDER.getName().equals("erl-client"), "Config-Ordner: " + ErlClient.FOLDER);
            check(mc.getWindow() != null, "kein Fenster");

            // Anticheat-relevant: Server können per Übersetzungs-Trick prüfen, ob bekannte Client-Schlüssel existieren
            check(KeyBinds.OPEN_GUI.getName().equals("key.erl-client.open-gui"), "Open-GUI-Schlüssel: " + KeyBinds.OPEN_GUI.getName());
            check(KeyBinds.OPEN_COMMANDS.getName().equals("key.erl-client.open-commands"), "Open-Commands-Schlüssel: " + KeyBinds.OPEN_COMMANDS.getName());
            check(I18n.get(KeyBinds.OPEN_GUI.getName()).equals("Open GUI"), "Open GUI nicht übersetzt");
            check(I18n.get(KeyBinds.OPEN_GUI.getCategory().label().getString()).equals("ERL-Client")
                || KeyBinds.OPEN_GUI.getCategory().label().getString().equals("ERL-Client"), "Tasten-Kategorie: " + KeyBinds.OPEN_GUI.getCategory().label().getString());
            for (String key : List.of("key.meteor-client.open-gui", "key.meteor-client.open-commands", "key.category.meteor-client.meteor-client")) {
                check(!Language.getInstance().has(key), "Alter Schlüssel noch übersetzbar: " + key);
            }
            log("Tasten: '" + I18n.get(KeyBinds.OPEN_GUI.getName()) + "' in Kategorie '" + KeyBinds.OPEN_GUI.getCategory().label().getString() + "'");

            boolean categoryRegistered = false;
            for (var category : Modules.loopCategories()) if (category == ErlExtras.CATEGORY) categoryRegistered = true;
            check(categoryRegistered, "Kategorie ERL fehlt");

            for (Class<? extends Module> type : List.of(ShieldBreaker.class, StunSlam.class, LungeSwap.class)) {
                Module module = Modules.get().get(type);
                check(module != null, type.getSimpleName() + " nicht registriert");
                check(module.category == ErlExtras.CATEGORY, type.getSimpleName() + " nicht in Kategorie ERL");
                check(Modules.get().getGroup(ErlExtras.CATEGORY).contains(module), type.getSimpleName() + " fehlt in der GUI-Gruppe ERL");
            }
        });

        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            TestServerContext server = sp.getServer();
            sp.getConnection().waitForChunksRender();
            setUpArena(server);
            sp.getConnection().waitForChunksRender();

            testClickGuiAndKeybinds(ctx);
            testShieldBreaker(ctx, server);
            testStunSlam(ctx, server);
            testLungeSwap(ctx, server);
        }
    }

    // ------------------------------------------------------------------ Aufbau

    private void setUpArena(TestServerContext server) {
        base = server.computeOnServer(s -> {
            ServerPlayer p = player(s);
            return new BlockPos(p.getBlockX(), 200, p.getBlockZ());
        });

        int x = base.getX(), y = base.getY(), z = base.getZ();
        server.runCommand("gamemode survival @a");
        server.runCommand("difficulty easy");
        server.runCommand("time set day");
        server.runCommand("weather clear");
        server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 12, x + 12, y + 14, z + 12));
        server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 12, y - 1, z - 12, x + 12, y - 1, z + 12));
        server.runCommand("tp @a %d.5 %d %d.5 0 0".formatted(x, y, z));
        server.runCommand("clear @a");
        server.runCommand("item replace entity @a hotbar.0 with minecraft:diamond_sword");
        server.runCommand("item replace entity @a hotbar.1 with minecraft:iron_axe");
        server.runCommand("item replace entity @a hotbar.2 with minecraft:netherite_axe");
        server.runCommand("item replace entity @a hotbar.3 with minecraft:mace");
        server.runCommand("item replace entity @a hotbar.4 with minecraft:iron_spear[minecraft:enchantments={\"minecraft:lunge\":3}]");
        server.runCommand("effect give @a minecraft:resistance infinite 4 true");
    }

    // ------------------------------------------------------------------ Click-GUI & Keybinds

    private void testClickGuiAndKeybinds(ClientGameTestContext ctx) {
        TestInput in = ctx.getInput();

        in.pressKey(InputConstants.KEY_RSHIFT);
        ctx.waitForScreen(TabScreen.class);
        ctx.waitTicks(5);
        ctx.takeScreenshot("01-click-gui");
        in.pressKey(InputConstants.KEY_ESCAPE);
        ctx.waitForScreen(null);

        for (Class<? extends Module> type : List.of(ShieldBreaker.class, StunSlam.class, LungeSwap.class)) {
            String name = type.getSimpleName().toLowerCase();
            ctx.setScreen(() -> GuiThemes.get().moduleScreen(Modules.get().get(type)));
            ctx.waitTicks(5);
            ctx.takeScreenshot("02-einstellungen-" + name);
        }
        ctx.setScreen(() -> null);

        // Modul-Keybind: K schaltet ShieldBreaker an und wieder aus
        ctx.runOnClient(mc -> Modules.get().get(ShieldBreaker.class).keybind.set(Keybind.fromKey(InputConstants.KEY_K)));
        in.pressKey(InputConstants.KEY_K);
        ctx.waitTicks(2);
        check(ctx.computeOnClient(mc -> Modules.get().isActive(ShieldBreaker.class)), "Keybind hat ShieldBreaker nicht aktiviert");
        in.pressKey(InputConstants.KEY_K);
        ctx.waitTicks(2);
        check(!ctx.computeOnClient(mc -> Modules.get().isActive(ShieldBreaker.class)), "Keybind hat ShieldBreaker nicht deaktiviert");
    }

    // ------------------------------------------------------------------ ShieldBreaker

    private void testShieldBreaker(ClientGameTestContext ctx, TestServerContext server) {
        resetPlayer(ctx, server, base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        ShieldDummy dummy = spawnDummy(ctx, server, "ShieldDummy", base.getX() + 0.5, base.getZ() + 2.5);

        // Axt-Priorität: "Best" muss Netherite (Slot 3) vor Eisen (Slot 2) wählen, "Worst" umgekehrt
        check(ctx.computeOnClient(mc -> ErlCombatUtils.findShieldDisabler(ErlCombatUtils.AxePriority.Best, true)) == NETHERITE_AXE, "Axt-Priorität Best wählt nicht Netherite");
        check(ctx.computeOnClient(mc -> ErlCombatUtils.findShieldDisabler(ErlCombatUtils.AxePriority.Worst, true)) == IRON_AXE, "Axt-Priorität Worst wählt nicht Eisen");

        setActive(ctx, ShieldBreaker.class, true);
        int ticks = server.waitFor(s -> dummy.isShieldDisabled(), 60);
        ctx.waitTicks(4);
        ctx.takeScreenshot("03-shieldbreaker");
        setActive(ctx, ShieldBreaker.class, false);

        check(ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == SWORD, "ShieldBreaker hat nicht auf das Schwert zurückgewechselt");
        // Geblockte Treffer kosten in Vanilla keine Haltbarkeit – der deaktivierte Schild belegt den Axt-Treffer
        check(ticks >= 0 && server.computeOnServer(srv -> dummy.isShieldDisabled()), "ShieldBreaker: Schild nicht deaktiviert");
        log("ShieldBreaker: Schild nach " + ticks + " Ticks deaktiviert, zurück auf Slot 1");

        removeDummy(ctx, server, dummy);
    }

    // ------------------------------------------------------------------ StunSlam

    private void testStunSlam(ClientGameTestContext ctx, TestServerContext server) {
        resetPlayer(ctx, server, base.getX() + 0.5, base.getY(), base.getZ() - 4.5);
        ShieldDummy dummy = spawnDummy(ctx, server, "SlamDummy", base.getX() + 0.5, base.getZ() + 2.5);
        float healthBefore = server.computeOnServer(s -> dummy.getHealth());

        setActive(ctx, StunSlam.class, true);
        // Über und vor den Dummy (in Schildrichtung) teleportieren und fallen lassen
        server.runCommand("tp @a %d.5 %d %d.0 0 60".formatted(base.getX(), base.getY() + 11, base.getZ() + 1));
        ctx.waitFor(mc -> mc.player.getY() > base.getY() + 8, 40);
        ctx.waitFor(mc -> mc.player.onGround() && mc.player.getY() < base.getY() + 1, 100);
        ctx.waitTicks(5);
        ctx.takeScreenshot("04-stunslam");
        setActive(ctx, StunSlam.class, false);

        float healthAfter = server.computeOnServer(s -> dummy.getHealth());
        boolean shieldDisabled = server.computeOnServer(s -> dummy.isShieldDisabled());
        log("StunSlam: Dummy-Leben " + healthBefore + " -> " + healthAfter + ", Schild deaktiviert: " + shieldDisabled);

        check(shieldDisabled, "StunSlam: Schild wurde nicht mit der Axt deaktiviert");
        check(healthBefore - healthAfter >= 10, "StunSlam: Mace-Smash hat zu wenig Schaden gemacht (" + (healthBefore - healthAfter) + ")");
        check(ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == SWORD, "StunSlam hat nicht zurückgewechselt");

        removeDummy(ctx, server, dummy);
    }

    // ------------------------------------------------------------------ LungeSwap

    @SuppressWarnings("unchecked")
    private void testLungeSwap(ClientGameTestContext ctx, TestServerContext server) {
        TestInput in = ctx.getInput();
        ctx.runOnClient(mc -> {
            LungeSwap lunge = Modules.get().get(LungeSwap.class);
            ((Setting<LungeSwap.Trigger>) lunge.settings.get("trigger")).set(LungeSwap.Trigger.Keybind);
            ((Setting<Keybind>) lunge.settings.get("lunge-key")).set(Keybind.fromKey(InputConstants.KEY_R));
        });
        setActive(ctx, LungeSwap.class, true);

        // 1) Keybind mit genug Hunger: Dash nach vorn, danach wieder Schwert
        double moved = lungeOnce(ctx, server, 20, () -> in.pressKey(InputConstants.KEY_R));
        ctx.takeScreenshot("05-lungeswap");
        log("LungeSwap (Keybind): " + moved + " Blöcke");
        check(moved > 1.5, "LungeSwap: kein Lunge-Dash per Keybind (" + moved + ")");

        // 2) Hunger unter dem Mindestwert: Modul pausiert, kein Wechsel, kein Dash
        double movedHungry = lungeOnce(ctx, server, 5, () -> in.pressKey(InputConstants.KEY_R));
        log("LungeSwap (Hunger 5): " + movedHungry + " Blöcke");
        check(movedHungry < 0.3, "LungeSwap hätte bei Hunger 5 pausieren müssen (" + movedHungry + ")");

        // 3) Auslöser "bei Angriff": Linksklick in die Luft
        ctx.runOnClient(mc -> ((Setting<LungeSwap.Trigger>) Modules.get().get(LungeSwap.class).settings.get("trigger")).set(LungeSwap.Trigger.OnAttack));
        double movedAttack = lungeOnce(ctx, server, 20, () -> in.pressMouse(InputConstants.MOUSE_BUTTON_LEFT));
        log("LungeSwap (bei Angriff): " + movedAttack + " Blöcke");
        check(movedAttack > 1.5, "LungeSwap: kein Lunge-Dash bei Angriff (" + movedAttack + ")");

        setActive(ctx, LungeSwap.class, false);
    }

    /** Setzt Hunger, wartet auf volle Ladung, löst aus und misst die horizontale Strecke. */
    private double lungeOnce(ClientGameTestContext ctx, TestServerContext server, int food, Runnable trigger) {
        resetPlayer(ctx, server, base.getX() + 0.5, base.getY(), base.getZ() - 8.5);
        server.runOnServer(s -> {
            player(s).getFoodData().setFoodLevel(food);
            player(s).getFoodData().setSaturation(food);
        });
        ctx.waitFor(mc -> mc.player.getFoodData().getFoodLevel() == food, 40);
        ctx.waitTicks(30); // volle Angriffsladung mit dem Schwert

        Vec3 before = ctx.computeOnClient(mc -> mc.player.position());
        trigger.run();
        ctx.waitTicks(15);
        Vec3 after = ctx.computeOnClient(mc -> mc.player.position());

        check(ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == SWORD, "LungeSwap hat nicht auf das Schwert zurückgewechselt");
        return Math.hypot(after.x - before.x, after.z - before.z);
    }

    // ------------------------------------------------------------------ Hilfen

    private ShieldDummy spawnDummy(ClientGameTestContext ctx, TestServerContext server, String name, double x, double z) {
        // Yaw 180 = Blick nach Norden (-Z), also zum Spieler
        ShieldDummy dummy = server.computeOnServer(s -> ShieldDummy.spawn(s.overworld(), name, x, base.getY(), z, 180));
        int id = server.computeOnServer(s -> dummy.getId());
        ctx.waitFor(mc -> mc.level.getEntity(id) instanceof LivingEntity le && le.isBlocking(), 100);
        return dummy;
    }

    private void removeDummy(ClientGameTestContext ctx, TestServerContext server, ShieldDummy dummy) {
        int id = server.computeOnServer(s -> dummy.getId());
        server.runOnServer(s -> dummy.despawn());
        ctx.waitFor(mc -> mc.level.getEntity(id) == null, 40);
    }

    private void resetPlayer(ClientGameTestContext ctx, TestServerContext server, double x, double y, double z) {
        server.runCommand("tp @a %s %s %s 0 0".formatted(x, y, z));
        server.runOnServer(s -> {
            ServerPlayer p = player(s);
            p.setHealth(p.getMaxHealth());
            p.resetFallDistance();
            p.setDeltaMovement(Vec3.ZERO);
        });
        ctx.getInput().pressKey(o -> o.keyHotbarSlots[SWORD]);
        ctx.waitFor(mc -> mc.player.onGround() && mc.player.getInventory().getSelectedSlot() == SWORD, 60);
        ctx.waitTicks(5);
    }

    private static void setActive(ClientGameTestContext ctx, Class<? extends Module> type, boolean active) {
        ctx.runOnClient(mc -> {
            Module module = Modules.get().get(type);
            if (module.isActive() != active) module.toggle();
        });
    }

    private static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().getFirst();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void log(String message) {
        ErlClient.LOG.info("[ERL-Test] {}", message);
    }
}
