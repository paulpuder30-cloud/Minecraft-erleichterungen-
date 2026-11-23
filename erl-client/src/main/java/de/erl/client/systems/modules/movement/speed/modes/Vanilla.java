/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.movement.speed.modes;

import de.erl.client.events.entity.player.PlayerMoveEvent;
import de.erl.client.mixininterface.IVec3;
import de.erl.client.systems.modules.Modules;
import de.erl.client.systems.modules.movement.Anchor;
import de.erl.client.systems.modules.movement.speed.SpeedMode;
import de.erl.client.systems.modules.movement.speed.SpeedModes;
import de.erl.client.utils.player.PlayerUtils;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public class Vanilla extends SpeedMode {
    public Vanilla() {
        super(SpeedModes.Vanilla);
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Vec3 vel = PlayerUtils.getHorizontalVelocity(settings.vanillaSpeed.get());
        double velX = vel.x();
        double velZ = vel.z();

        if (mc.player.hasEffect(MobEffects.SPEED)) {
            double value = (mc.player.getEffect(MobEffects.SPEED).getAmplifier() + 1) * 0.205;
            velX += velX * value;
            velZ += velZ * value;
        }

        Anchor anchor = Modules.get().get(Anchor.class);
        if (anchor.isActive() && anchor.controlMovement) {
            velX = anchor.deltaX;
            velZ = anchor.deltaZ;
        }

        ((IVec3) event.movement).erl$set(velX, event.movement.y, velZ);
    }
}
