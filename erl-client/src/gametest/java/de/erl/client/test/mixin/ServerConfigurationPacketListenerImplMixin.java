/*
 * This file is part of ERL-Client.
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.test.mixin;

import de.erl.client.test.ServerTrace;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zeichnet die Liste der Daten-Packs auf, die der Client dem Server als bekannt meldet. */
@Mixin(ServerConfigurationPacketListenerImpl.class)
public class ServerConfigurationPacketListenerImplMixin {
    @Inject(method = "handleSelectKnownPacks", at = @At("HEAD"))
    private void erlTest$record(ServerboundSelectKnownPacks packet, CallbackInfo ci) {
        ServerTrace.RECEIVED.add("known_packs -> " + packet.knownPacks());
    }
}
