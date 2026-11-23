/*
 * This file is part of ERL-Client, a fork of Meteor Client (https://github.com/MeteorDevelopment/meteor-client).
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.modules;

import de.erl.client.ErlClient;
import de.erl.client.utils.ErlCombatUtils;
import de.erl.client.utils.ErlCombatUtils.AxePriority;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Erkennt ein Ziel, das mit dem Schild in unsere Richtung blockt, wechselt auf eine Axt, schlägt zu
 * (Vanilla 26.3: Schild 5 s deaktiviert) und wechselt wieder zurück.
 */
public class ShieldBreaker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwap = settings.createGroup("Swap");

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
        .description("Only target players. In vanilla only players lose their shield when hit with an axe.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the target. When off, only the entity under your crosshair is targeted.")
        .defaultValue(true)
        .build()
    );

    private final Setting<AxePriority> axePriority = sgSwap.add(new EnumSetting.Builder<AxePriority>()
        .name("axe-priority")
        .description("Which axe to use: Best = Netherite > Diamond > Iron > Copper > Stone > Gold > Wood.")
        .defaultValue(AxePriority.Best)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgSwap.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Ignores axes with 10 or less durability left.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgSwap.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between swapping to the axe and hitting. 0 = same tick.")
        .defaultValue(0)
        .range(0, 20)
        .sliderRange(0, 10)
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
        .range(0, 20)
        .sliderRange(0, 10)
        .visible(swapBack::get)
        .build()
    );

    private enum State {
        Idle,
        WaitingForHit,
        WaitingForSwapBack
    }

    private State state = State.Idle;
    private LivingEntity target;
    private int timer;
    private int previousSlot = -1;
    private int cooldown;

    public ShieldBreaker() {
        super(ErlClient.CATEGORY, "shield-breaker", "Swaps to an axe and hits targets that are blocking with a shield, then swaps back.");
    }

    @Override
    public void onDeactivate() {
        if (state != State.Idle) restoreSlot();
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
            case Idle -> {
                if (cooldown > 0 || mc.player.isDeadOrDying() || mc.player.isSpectator()) return;

                LivingEntity newTarget = findTarget();
                if (newTarget == null) return;

                int axeSlot = ErlCombatUtils.findShieldDisabler(axePriority.get(), antiBreak.get());
                if (axeSlot == -1) return;

                target = newTarget;
                previousSlot = mc.player.getInventory().getSelectedSlot();
                if (axeSlot != previousSlot) InvUtils.swap(axeSlot, false);

                if (delay.get() == 0) hit();
                else {
                    timer = delay.get();
                    state = State.WaitingForHit;
                }
            }
            case WaitingForHit -> {
                if (--timer > 0) return;

                if (isStillValid(target) && ErlCombatUtils.disablesShields(mc.player.getMainHandItem())) hit();
                else finish();
            }
            case WaitingForSwapBack -> {
                if (--timer > 0) return;
                finish();
            }
        }
    }

    private LivingEntity findTarget() {
        // Ziel unter dem Fadenkreuz hat Vorrang
        if (mc.hitResult instanceof EntityHitResult hit && isStillValid(hit.getEntity())) {
            return (LivingEntity) hit.getEntity();
        }

        if (!rotate.get()) return null;

        Entity entity = TargetUtils.get(this::isStillValid, SortPriority.ClosestAngle);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean isStillValid(Entity entity) {
        if (entity == null || !ErlCombatUtils.isValidTarget(entity, onlyPlayers.get())) return false;
        if (!ErlCombatUtils.isInRange(entity, range.get())) return false;
        return ErlCombatUtils.isBlockingTowardsUs((LivingEntity) entity);
    }

    private void hit() {
        LivingEntity hitTarget = target;
        cooldown = 10;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(hitTarget), Rotations.getPitch(hitTarget, Target.Body), 50, () -> ErlCombatUtils.attack(hitTarget));
        } else {
            ErlCombatUtils.attack(hitTarget);
        }

        if (swapBack.get() && previousSlot != -1 && previousSlot != mc.player.getInventory().getSelectedSlot()) {
            // Zurückwechseln frühestens im nächsten Tick, damit der Treffer (ggf. erst im Rotations-Callback) sicher mit der Axt passiert
            timer = Math.max(1, swapBackDelay.get());
            state = State.WaitingForSwapBack;
        } else {
            previousSlot = -1;
            reset();
        }
    }

    private void finish() {
        restoreSlot();
        reset();
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
        return state == State.Idle ? null : state.name();
    }
}
