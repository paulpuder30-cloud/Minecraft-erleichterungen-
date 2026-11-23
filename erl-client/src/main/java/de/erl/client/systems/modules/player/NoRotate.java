/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.player;

import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import de.erl.client.mixin.ClientPacketListenerMixin;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Module;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @see ClientPacketListenerMixin#onHandleMovePlayerHead(ClientboundPlayerPositionPacket, CallbackInfo, LocalFloatRef, LocalFloatRef)
 */
public class NoRotate extends Module {
    public NoRotate() {
        super(Categories.Player, "no-rotate", "Attempts to block rotations sent from server to client.");
    }
}
