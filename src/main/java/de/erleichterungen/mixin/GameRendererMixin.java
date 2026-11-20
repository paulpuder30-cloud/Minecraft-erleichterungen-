package de.erleichterungen.mixin;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.feature.Freecam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blendet bei aktiver Freecam die eigene Hand aus. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void erleichterungen$hideHandInFreecam(CallbackInfo ci) {
		if (Freecam.isActive()) ci.cancel();
	}

	/** NoHurtCam: unterdrückt das Kamera-Wackeln beim Schadennehmen. */
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void erleichterungen$noHurtCam(CallbackInfo ci) {
		if (ModConfig.get().noHurtCamEnabled) ci.cancel();
	}
}
