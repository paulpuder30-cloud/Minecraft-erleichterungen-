package de.erleichterungen.mixin;

import de.erleichterungen.feature.AntiVanish;
import de.erleichterungen.scan.ScanManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hängt sich in die Paketverarbeitung ein. Die handle*-Methoden werden zuerst auf dem Netzwerk-Thread
 * aufgerufen und dann auf den Main-Thread umgeleitet – deshalb wird hier jeweils geprüft, ob wir
 * schon auf dem Main-Thread sind (TAIL wird nur dort erreicht).
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleBlockUpdate", at = @At("TAIL"))
	private void erleichterungen$onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
		ScanManager.onBlockChanged(packet.getPos(), packet.getBlockState());
	}

	@Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
	private void erleichterungen$onSectionUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
		packet.runUpdates(ScanManager::onBlockChanged);
	}

	@Inject(method = "handlePlayerInfoRemove", at = @At("HEAD"))
	private void erleichterungen$onPlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
		if (Minecraft.getInstance().isSameThread()) {
			AntiVanish.onPlayerInfoRemove((ClientPacketListener) (Object) this, packet);
		}
	}

	@Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
	private void erleichterungen$onPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
		AntiVanish.onPlayerInfoUpdate((ClientPacketListener) (Object) this, packet);
	}

	@Inject(method = "handleCommandSuggestions", at = @At("HEAD"), cancellable = true)
	private void erleichterungen$onCommandSuggestions(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
		if (packet.id() != AntiVanish.SUGGESTION_ID) return;
		ci.cancel();
		ClientPacketListener self = (ClientPacketListener) (Object) this;
		Minecraft.getInstance().execute(() -> AntiVanish.onCommandSuggestions(self, packet));
	}
}
