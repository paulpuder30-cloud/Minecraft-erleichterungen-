/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.world;

import de.erl.client.events.packets.PacketEvent;
import de.erl.client.mixin.BlockHitResultAccessor;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;

public class BuildHeight extends Module {
    public BuildHeight() {
        super(Categories.World, "build-height", "Allows you to interact with objects at the build limit.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundUseItemOnPacket p)) return;
        if (mc.level == null) return;
        if (p.hitResult().getLocation().y >= mc.level.getHeight() && p.hitResult().getDirection() == Direction.UP) {
            ((BlockHitResultAccessor) p.hitResult()).erl$setDirection(Direction.DOWN);
        }
    }
}
