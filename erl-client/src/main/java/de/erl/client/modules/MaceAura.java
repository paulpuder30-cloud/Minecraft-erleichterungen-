/*
 * This file is part of ERL-Client.
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.modules;

import de.erl.client.ErlExtras;
import de.erl.client.events.game.GameLeftEvent;
import de.erl.client.events.world.TickEvent;
import de.erl.client.lib.orbit.EventHandler;
import de.erl.client.settings.*;
import de.erl.client.systems.modules.Module;
import de.erl.client.utils.ErlCombatUtils;
import de.erl.client.utils.entity.SortPriority;
import de.erl.client.utils.entity.Target;
import de.erl.client.utils.entity.TargetUtils;
import de.erl.client.utils.player.InvUtils;
import de.erl.client.utils.player.Rotations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Schlägt automatisch mit dem Streitkolben zu, sobald man tief genug fällt und ein Gegner in Reichweite ist.
 *
 * <p>Vanilla 26.3 ({@code MaceItem}): Smash ab {@code fallDistance > 1,5}, nicht beim Elytra-Flug. Bonusschaden
 * 4/Block für die ersten 3 Blöcke, 2/Block bis 8, danach 1/Block, plus Density. Der Bonus skaliert nicht mit der
 * Angriffsladung, deshalb kostet der Wechsel direkt vor dem Schlag keinen Smash-Schaden. Schilde blocken den Smash
 * ({@code mace_smash} ist nicht in {@code #bypasses_shield}); dafür gibt es StunSlam.
 */
public class MaceAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwap = settings.createGroup("Swap");

    private final Setting<Double> minFallHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-fall-height")
        .description("Minimum fall distance in blocks before hitting. The mace smash needs more than 1.5.")
        .defaultValue(3)
        .min(1.6)
        .sliderRange(1.6, 30)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Maximum distance to the target's hitbox. Vanilla survival reach is 3.")
        .defaultValue(3)
        .min(0.5)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<Boolean> onlyPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("only-players")
        .description("Only target players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> skipBlocking = sgGeneral.add(new BoolSetting.Builder()
        .name("skip-blocking")
        .description("Ignores targets whose shield faces you (the smash would be blocked). Use Stun Slam for those.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the target. When off, only the entity under your crosshair is targeted.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> preferDensity = sgSwap.add(new BoolSetting.Builder()
        .name("prefer-density")
        .description("Prefers the mace with the highest Density level if you carry several.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgSwap.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to the previously selected slot after the hit.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swapBackDelay = sgSwap.add(new IntSetting.Builder()
        .name("swap-back-delay")
        .description("Ticks to wait after the hit before swapping back.")
        .defaultValue(1)
        .range(1, 20)
        .sliderRange(1, 10)
        .visible(swapBack::get)
        .build()
    );

    private boolean hitThisFall;
    private int backTimer;
    private int previousSlot = -1;

    public MaceAura() {
        super(ErlExtras.CATEGORY, "mace-aura", "Automatically smashes nearby enemies with your mace once you have fallen far enough.");
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

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (backTimer > 0 && --backTimer == 0) {
            restoreSlot();
            previousSlot = -1;
        }

        // Neuer Fall erst nach dem Landen
        if (mc.player.onGround() || mc.player.isInWater() || mc.player.isFallFlying()) {
            hitThisFall = false;
            return;
        }

        if (hitThisFall || backTimer > 0 || mc.player.isDeadOrDying() || mc.player.isSpectator()) return;
        if (!canSmash() || mc.player.fallDistance < minFallHeight.get()) return;

        int maceSlot = findMace();
        if (maceSlot == -1) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        hitThisFall = true;
        int selected = mc.player.getInventory().getSelectedSlot();
        if (maceSlot != selected) {
            previousSlot = selected;
            InvUtils.swap(maceSlot, false);
        }

        if (rotate.get()) Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body), 60, () -> ErlCombatUtils.attack(target));
        else ErlCombatUtils.attack(target);

        if (swapBack.get() && previousSlot != -1) backTimer = swapBackDelay.get();
        else previousSlot = -1;
    }

    /** Gleiche Bedingung wie {@code MaceItem#canSmashAttack}. */
    private boolean canSmash() {
        return mc.player.fallDistance > MaceItem.SMASH_ATTACK_FALL_THRESHOLD && !mc.player.isFallFlying();
    }

    private int findMace() {
        return ErlCombatUtils.findHotbarSlot(stack -> {
            if (!(stack.getItem() instanceof MaceItem) || ErlCombatUtils.isAboutToBreak(stack)) return -1;
            return preferDensity.get() ? ErlCombatUtils.enchantmentLevel(stack, Enchantments.DENSITY) : 0;
        }, true);
    }

    private LivingEntity findTarget() {
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && isValid(hit.getEntity())) {
            return (LivingEntity) hit.getEntity();
        }

        if (!rotate.get()) return null;

        Entity entity = TargetUtils.get(this::isValid, SortPriority.ClosestAngle);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean isValid(Entity entity) {
        if (entity == null || !ErlCombatUtils.isValidTarget(entity, onlyPlayers.get())) return false;
        if (!ErlCombatUtils.isInRange(entity, range.get())) return false;
        return !skipBlocking.get() || !ErlCombatUtils.isBlockingTowardsUs((LivingEntity) entity);
    }

    private void restoreSlot() {
        if (previousSlot != -1 && mc.player != null) InvUtils.swap(previousSlot, false);
    }

    private void reset() {
        hitThisFall = false;
        backTimer = 0;
        previousSlot = -1;
    }

    @Override
    public String getInfoString() {
        if (mc.player == null) return null;
        return findMace() == -1 ? "no mace" : null;
    }
}
