/*
 * This file is part of ERL-Client.
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.modules;

import de.erl.client.ErlExtras;
import de.erl.client.utils.ErlCombatUtils;
import de.erl.client.events.entity.player.DoAttackEvent;
import de.erl.client.events.game.GameLeftEvent;
import de.erl.client.events.world.TickEvent;
import de.erl.client.settings.*;
import de.erl.client.systems.modules.Module;
import de.erl.client.utils.misc.Keybind;
import de.erl.client.utils.player.InvUtils;
import de.erl.client.lib.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.HitResult;

/**
 * Wechselt auf einen Speer mit Lunge, führt einen Jab aus (Lunge-Dash) und wechselt zurück.
 *
 * <p>Vanilla 26.3 ({@code data/minecraft/enchantment/lunge.json}): Lunge wirkt beim Jab ({@code post_piercing_attack}),
 * stößt den Spieler um 0,458 × Stufe nach vorn, kostet 4 × Stufe Erschöpfung und 1 Haltbarkeit. Es wirkt nur mit
 * <b>mindestens 7 Hungerpunkten</b> (außer im Kreativmodus), nicht im Wasser, nicht beim Elytra-Flug und nicht auf Reittieren.
 *
 * <p>Ein Speer braucht volle Angriffsladung ({@code minimum_attack_charge = 1}). Der Server prüft die Ladung beim Jab
 * aber noch mit der vorherigen Waffe, weil er sie erst im nächsten Spieler-Tick zurücksetzt. Wechsel und Jab im
 * selben Tick gelingen deshalb, sobald die aktuelle Waffe voll geladen ist. Bis dahin wartet das Modul.
 */
public class LungeSwap extends Module {
    private static final int MAX_WAIT_TICKS = 40;
    private static final int VANILLA_MIN_FOOD = 7;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwap = settings.createGroup("Swap");

    private final Setting<Trigger> trigger = sgGeneral.add(new EnumSetting.Builder<Trigger>()
        .name("trigger")
        .description("Keybind = lunge when pressing the lunge key. On Attack = every left click (not on blocks) becomes a lunge.")
        .defaultValue(Trigger.Keybind)
        .build()
    );

    private final Setting<Keybind> lungeKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("lunge-key")
        .description("Key that performs a lunge while the module is active.")
        .defaultValue(Keybind.none())
        .visible(() -> trigger.get() == Trigger.Keybind)
        .action(this::requestLunge)
        .build()
    );

    private final Setting<Boolean> preferHighestLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("prefer-highest-level")
        .description("Uses the spear with the highest Lunge level (farther dash). When off, the lowest level is used (less hunger).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minHunger = sgGeneral.add(new IntSetting.Builder()
        .name("min-hunger")
        .description("Pauses while your food level is below this value. Vanilla Lunge does nothing below 7.")
        .defaultValue(VANILLA_MIN_FOOD)
        .range(VANILLA_MIN_FOOD, 20)
        .sliderRange(VANILLA_MIN_FOOD, 20)
        .build()
    );

    private final Setting<Boolean> swapBack = sgSwap.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to the previous weapon after the jab.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swapBackDelay = sgSwap.add(new IntSetting.Builder()
        .name("swap-back-delay")
        .description("Ticks to wait after the jab before swapping back.")
        .defaultValue(1)
        .range(1, 20)
        .sliderRange(1, 10)
        .visible(swapBack::get)
        .build()
    );

    private boolean pending;
    private int waitTicks;
    private int backTimer;
    private int previousSlot = -1;
    private boolean warnedHunger;

    public LungeSwap() {
        super(ErlExtras.CATEGORY, "lunge-swap", "Swaps to a Lunge spear, jabs to get the lunge dash and swaps back.");
    }

    @Override
    public void onDeactivate() {
        if (backTimer > 0) restoreSlot();
        reset();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        reset();
    }

    /** Wird vom Lunge-Keybind aufgerufen. */
    private void requestLunge() {
        if (mc.player == null || mc.gui.screen() != null || trigger.get() != Trigger.Keybind) return;
        pending = true;
        waitTicks = 0;
    }

    @EventHandler
    private void onAttack(DoAttackEvent event) {
        if (trigger.get() != Trigger.OnAttack || mc.player == null || backTimer > 0) return;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return;
        if (!canLunge(false) || findLungeSpear() == -1) return;

        // Ohne volle Ladung lehnt der Server den Jab ab: dann normaler Vanilla-Angriff
        if (!isCharged()) return;

        if (performLunge()) event.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.gameMode == null) return;

        if (backTimer > 0 && --backTimer == 0) {
            restoreSlot();
            previousSlot = -1;
        }

        if (!pending) return;

        if (!canLunge(true)) {
            pending = false;
            return;
        }

        if (findLungeSpear() == -1) {
            warning("No spear with Lunge in your hotbar.");
            pending = false;
            return;
        }

        if (!isCharged()) {
            if (++waitTicks > MAX_WAIT_TICKS) pending = false;
            return;
        }

        pending = false;
        performLunge();
    }

    private boolean performLunge() {
        int spearSlot = findLungeSpear();
        if (spearSlot == -1) return false;

        int selected = mc.player.getInventory().getSelectedSlot();
        if (spearSlot != selected) {
            previousSlot = selected;
            InvUtils.swap(spearSlot, false);
        }

        ItemStack spear = mc.player.getMainHandItem();
        PiercingWeapon piercing = spear.get(DataComponents.PIERCING_WEAPON);
        if (piercing == null) {
            restoreSlot();
            previousSlot = -1;
            return false;
        }

        // Exakt der Vanilla-Pfad aus Minecraft#startAttack für Piercing-Waffen (sendet STAB)
        mc.gameMode.piercingAttack(spear.getAttackAnimation(), piercing);

        if (swapBack.get() && previousSlot != -1) backTimer = swapBackDelay.get();
        else previousSlot = -1;

        return true;
    }

    /** Lunge-Bedingungen aus lunge.json plus einstellbare Hunger-Grenze. */
    private boolean canLunge(boolean notify) {
        if (mc.player.isPassenger() || mc.player.isFallFlying() || mc.player.isInWater()) return false;

        if (!mc.player.isCreative() && mc.player.getFoodData().getFoodLevel() < minHunger.get()) {
            if (notify && !warnedHunger) {
                warning("Paused: food level %d is below %d.", mc.player.getFoodData().getFoodLevel(), minHunger.get());
                warnedHunger = true;
            }
            return false;
        }

        warnedHunger = false;
        return true;
    }

    /** Volle Angriffsladung mit der aktuell gehaltenen Waffe (siehe Klassenkommentar). */
    private boolean isCharged() {
        return mc.player.getAttackStrengthScale(0) >= 1;
    }

    private int findLungeSpear() {
        return ErlCombatUtils.findHotbarSlot(stack -> {
            if (!stack.is(ItemTags.SPEARS) || ErlCombatUtils.isAboutToBreak(stack)) return -1;
            int level = ErlCombatUtils.enchantmentLevel(stack, Enchantments.LUNGE);
            return level > 0 ? level : -1;
        }, preferHighestLevel.get());
    }

    private void restoreSlot() {
        if (previousSlot != -1 && mc.player != null) InvUtils.swap(previousSlot, false);
    }

    private void reset() {
        pending = false;
        waitTicks = 0;
        backTimer = 0;
        previousSlot = -1;
        warnedHunger = false;
    }

    @Override
    public String getInfoString() {
        if (mc.player == null) return null;
        if (!mc.player.isCreative() && mc.player.getFoodData().getFoodLevel() < minHunger.get()) return "paused";
        return trigger.get().name();
    }

    public enum Trigger {
        Keybind,
        OnAttack
    }
}
