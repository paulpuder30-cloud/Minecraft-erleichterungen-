/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.movement;

import de.erl.client.events.world.TickEvent;
import de.erl.client.mixininterface.IVec3;
import de.erl.client.settings.DoubleSetting;
import de.erl.client.settings.EnumSetting;
import de.erl.client.settings.Setting;
import de.erl.client.settings.SettingGroup;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Module;
import de.erl.client.lib.orbit.EventHandler;

public class AutoJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The method of jumping.")
        .defaultValue(Mode.Jump)
        .build()
    );

    private final Setting<JumpWhen> jumpIf = sgGeneral.add(new EnumSetting.Builder<JumpWhen>()
        .name("jump-if")
        .description("Jump if.")
        .defaultValue(JumpWhen.Always)
        .build()
    );

    private final Setting<Double> velocityHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("velocity-height")
        .description("The distance that velocity mode moves you.")
        .defaultValue(0.25)
        .min(0)
        .sliderMax(2)
        .build()
    );

    public AutoJump() {
        super(Categories.Movement, "auto-jump", "Automatically jumps.");
    }

    private boolean jump() {
        return switch (jumpIf.get()) {
            case Sprinting -> mc.player.isSprinting() && (mc.player.zza != 0 || mc.player.xxa != 0);
            case Walking -> mc.player.zza != 0 || mc.player.xxa != 0;
            case Always -> true;
        };
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.onGround() || mc.player.isShiftKeyDown() || !jump()) return;

        if (mode.get() == Mode.Jump) mc.player.jumpFromGround();
        else ((IVec3) mc.player.getDeltaMovement()).erl$setY(velocityHeight.get());
    }

    public enum JumpWhen {
        Sprinting,
        Walking,
        Always
    }

    public enum Mode {
        Jump,
        LowHop
    }
}
