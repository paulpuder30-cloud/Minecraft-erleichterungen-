/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erl.client.commands.Command;
import de.erl.client.mixin.ClientPacketListenerAccessor;
import de.erl.client.utils.misc.ErlStarscript;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.util.Crypt;
import de.erl.client.lib.starscript.Script;

import java.time.Instant;
import java.util.Optional;

public class SayCommand extends Command {
    public SayCommand() {
        super("say", "Sends messages in chat.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String msg = context.getArgument("message", String.class);
            Script script = ErlStarscript.compile(msg);

            if (script != null) {
                String message = ErlStarscript.run(script);

                if (message != null) {
                    Instant instant = Instant.now();
                    long l = Crypt.SaltSupplier.getLong();
                    ClientPacketListener handler = mc.getConnection();
                    LastSeenMessagesTracker.Update lastSeenMessages = ((ClientPacketListenerAccessor) handler).erl$getLastSeenMessages().generateAndApplyUpdate();
                    MessageSignature messageSignatureData = ((ClientPacketListenerAccessor) handler).erl$getSignedMessageEncoder().pack(new SignedMessageBody(message, instant, l, lastSeenMessages.lastSeen()));
                    handler.send(new ServerboundChatPacket(message, instant, l, Optional.ofNullable(messageSignatureData), lastSeenMessages.update()));
                }
            }

            return SINGLE_SUCCESS;
        }));
    }
}
