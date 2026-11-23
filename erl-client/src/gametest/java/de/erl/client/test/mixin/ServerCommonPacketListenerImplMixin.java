/*
 * This file is part of ERL-Client.
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.test.mixin;

import de.erl.client.test.ServerTrace;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zeichnet serverseitig jedes Custom-Payload des Clients auf (Brand, Kanal-Registrierungen, Mod-Pakete). */
@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void erlTest$record(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        ServerTrace.RECEIVED.add("payload " + packet.payload().type().id() + " -> " + packet.payload());
    }
}
