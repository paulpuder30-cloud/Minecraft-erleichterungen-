package de.erleichterungen.mixin;

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
}
