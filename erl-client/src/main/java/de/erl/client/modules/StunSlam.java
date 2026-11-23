/*
 * This file is part of ERL-Client.
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.modules;

import de.erl.client.ErlExtras;
import de.erl.client.utils.ErlCombatUtils;
import de.erl.client.utils.ErlCombatUtils.AxePriority;
import de.erl.client.events.game.GameLeftEvent;
import de.erl.client.events.world.TickEvent;
import de.erl.client.settings.*;
import de.erl.client.systems.modules.Module;
import de.erl.client.utils.entity.SortPriority;
import de.erl.client.utils.entity.Target;
import de.erl.client.utils.entity.TargetUtils;
import de.erl.client.utils.player.InvUtils;
import de.erl.client.utils.player.Rotations;
import de.erl.client.lib.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Axt-Mace-Kombo: Schild des Gegners mit der Axt deaktivieren (Stun), sofort auf den Streitkolben wechseln und
 * aus dem Fall einen Smash-Angriff ausführen.
 *
 * <p>Vanilla 26.3: Ein komplett geblockter Treffer setzt beim Verteidiger {@code damageCooldownTime = 20} mit
 * {@code lastHurt = 0}. Der folgende Mace-Treffer verursacht deshalb trotz Unverwundbarkeitsfenster vollen Schaden.
 * Der Smash-Bonus ({@code MaceItem#getAttackDamageBonus}) skaliert nicht mit der Angriffsladung, der Wechsel kostet also
 * kaum Schaden. Voraussetzung für den Smash: {@code fallDistance > 1.5} und kein Elytra-Flug.
 */
public class StunSlam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwap = settings.createGroup("Swap");

    private final Setting<Boolean> requireFall = sgGeneral.add(new BoolSetting.Builder()
        .name("require-fall")
        .description("Only triggers while falling. When off, the combo fires as soon as a target blocks; while falling the mace hit becomes a full smash.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> minFallHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-fall-height")
        .description("Minimum fall distance in blocks before the combo triggers. The mace smash itself needs more than 1.5.")
        .defaultValue(3)
        .min(1.6)
        .sliderRange(1.6, 20)
        .visible(requireFall::get)
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

    private final Setting<Boolean> onlyWhenBlocking = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-blocking")
        .description("Only triggers when the target is blocking with a shield. When off, unshielded targets get a plain mace smash.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("only-players")
        .description("Only target players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the target. When off, only the entity under your crosshair is targeted.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> hitGap = sgSwap.add(new IntSetting.Builder()
        .name("tick-gap")
        .description("Ticks between the axe hit and the mace hit. 0 = both in the same tick (fastest).")
        .defaultValue(0)
        .range(0, 10)
        .sliderRange(0, 5)
        .build()
    );

    private final Setting<AxePriority> axePriority = sgSwap.add(new EnumSetting.Builder<AxePriority>()
        .name("axe-priority")
        .description("Which axe to use: Best = Netherite > Diamond > Iron > Copper > Stone > Gold > Wood.")
        .defaultValue(AxePriority.Best)
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
        .description("Swaps back to the previously selected slot after the smash.")
        .defaultValue(true)
        .build()
    );

    private enum State {
        Idle,
        WaitingForMace,
        WaitingForSwapBack,
        WaitingForLanding
    }

    private State state = State.Idle;
    private LivingEntity target;
    private int timer;
    private int previousSlot = -1;
    private int cooldown;

    public StunSlam() {
        super(ErlExtras.CATEGORY, "stun-slam", "Axe + mace combo: breaks the target's shield with an axe and instantly follows up with a mace hit (full smash while falling).");
    }

    @Override
    public void onDeactivate() {
        if (state == State.WaitingForMace || state == State.WaitingForSwapBack) restoreSlot();
        reset();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        reset();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (cooldown > 0) cooldown--;

        switch (state) {
            case Idle -> tryStart();
            case WaitingForMace -> {
                if (--timer > 0) return;

                if ((!requireFall.get() || canSmash()) && isTargetValid(target, false)) maceHit(true);
                else {
                    // Gelandet oder Ziel weg: Kombo abbrechen
                    restoreSlot();
                    reset();
                    state = State.WaitingForLanding;
                }
            }
            case WaitingForSwapBack -> {
                if (--timer > 0) return;
                restoreSlot();
                previousSlot = -1;
                state = State.WaitingForLanding;
            }
            case WaitingForLanding -> {
                // Pro Fall nur eine Kombo
                if (mc.player.onGround() || mc.player.isInWater() || mc.player.fallDistance < 0.5) {
                    restoreSlot();
                    reset();
                }
            }
        }
    }

    private void tryStart() {
        if (cooldown > 0 || mc.player.isDeadOrDying() || mc.player.isSpectator()) return;

        boolean falling = canSmash() && (!requireFall.get() || mc.player.fallDistance >= minFallHeight.get());
        if (requireFall.get() && !falling) return;

        int maceSlot = findMace();
        if (maceSlot == -1) return;

        LivingEntity newTarget = findTarget();
        if (newTarget == null) return;

        boolean blocking = ErlCombatUtils.isBlockingTowardsUs(newTarget);
        // Im Stehen nur gegen blockende Ziele (sonst wäre es nur Dauer-Klicken mit dem Mace)
        if (!blocking && !falling) return;

        target = newTarget;
        previousSlot = mc.player.getInventory().getSelectedSlot();
        cooldown = 10;

        if (blocking) {
            int axeSlot = ErlCombatUtils.findShieldDisabler(axePriority.get(), true);
            if (axeSlot == -1) {
                reset();
                return;
            }

            // 1) Stun: Axt-Treffer deaktiviert den Schild
            if (axeSlot != previousSlot) InvUtils.swap(axeSlot, false);
            LivingEntity stunTarget = target;
            int gap = hitGap.get();

            // Bis der (ggf. rotierte) Treffer läuft, keine neue Kombo starten
            state = State.WaitingForLanding;

            runRotated(stunTarget, () -> {
                ErlCombatUtils.attack(stunTarget);

                if (gap == 0) {
                    // Gleicher Tick: wir sind bereits zum Ziel rotiert
                    maceHit(false);
                } else {
                    timer = gap;
                    state = State.WaitingForMace;
                }
            });
        } else {
            // Kein Schild: direkt Smash (nur wenn erlaubt)
            maceHit(true);
        }
    }

    private void maceHit(boolean rotateFirst) {
        int maceSlot = findMace();
        if (maceSlot == -1 || target == null) {
            restoreSlot();
            reset();
            state = State.WaitingForLanding;
            return;
        }

        if (maceSlot != mc.player.getInventory().getSelectedSlot()) InvUtils.swap(maceSlot, false);

        LivingEntity smashTarget = target;
        if (rotateFirst) runRotated(smashTarget, () -> ErlCombatUtils.attack(smashTarget));
        else ErlCombatUtils.attack(smashTarget);

        if (swapBack.get() && previousSlot != -1 && previousSlot != maceSlot) {
            timer = 1;
            state = State.WaitingForSwapBack;
        } else {
            previousSlot = -1;
            state = State.WaitingForLanding;
        }
    }

    private void runRotated(LivingEntity entity, Runnable action) {
        if (rotate.get()) Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity, Target.Body), 60, action);
        else action.run();
    }

    /** Gleiche Bedingung wie {@code MaceItem#canSmashAttack}. */
    private boolean canSmash() {
        return mc.player.fallDistance > MaceItem.SMASH_ATTACK_FALL_THRESHOLD && !mc.player.isFallFlying() && !mc.player.onGround();
    }

    private int findMace() {
        return ErlCombatUtils.findHotbarSlot(stack -> {
            if (!(stack.getItem() instanceof MaceItem)) return -1;
            return preferDensity.get() ? ErlCombatUtils.enchantmentLevel(stack, Enchantments.DENSITY) : 0;
        }, true);
    }

    private LivingEntity findTarget() {
        if (mc.hitResult instanceof EntityHitResult hit && isTargetValid(hit.getEntity(), true)) {
            return (LivingEntity) hit.getEntity();
        }

        if (!rotate.get()) return null;

        Entity entity = TargetUtils.get(e -> isTargetValid(e, true), SortPriority.ClosestAngle);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean isTargetValid(Entity entity, boolean checkBlocking) {
        if (entity == null || !ErlCombatUtils.isValidTarget(entity, onlyPlayers.get())) return false;
        if (!ErlCombatUtils.isInRange(entity, range.get())) return false;
        return !checkBlocking || !onlyWhenBlocking.get() || ErlCombatUtils.isBlockingTowardsUs((LivingEntity) entity);
    }

    private void restoreSlot() {
        if (swapBack.get() && previousSlot != -1 && mc.player != null) InvUtils.swap(previousSlot, false);
    }

    private void reset() {
        state = State.Idle;
        target = null;
        timer = 0;
        previousSlot = -1;
    }

    @Override
    public String getInfoString() {
        if (state == State.Idle && mc.player != null) {
            return findMace() == -1 ? "no mace" : null;
        }
        return state == State.WaitingForLanding ? null : state.name();
    }
}
