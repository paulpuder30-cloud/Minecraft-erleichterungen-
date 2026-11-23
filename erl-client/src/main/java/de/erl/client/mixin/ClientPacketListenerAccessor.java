/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
    @Accessor("serverChunkRadius")
    int erl$getServerChunkRadius();

    @Accessor("signedMessageEncoder")
    SignedMessageChain.Encoder erl$getSignedMessageEncoder();

    @Accessor("lastSeenMessages")
    LastSeenMessagesTracker erl$getLastSeenMessages();

    @Accessor("registryAccess")
    RegistryAccess.Frozen erl$getRegistryAccess();

    @Accessor("enabledFeatures")
    FeatureFlagSet erl$getEnabledFeatures();

    @Accessor("COMMAND_NODE_BUILDER")
    static ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> erl$getCommandNodeFactory() {
        return null;
    }
}
