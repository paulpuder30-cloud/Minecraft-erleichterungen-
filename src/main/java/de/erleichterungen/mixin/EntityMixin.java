package de.erleichterungen.mixin;

import de.erleichterungen.feature.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mausbewegungen drehen bei aktiver Freecam die Kamera statt des Spielers. */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void erleichterungen$freecamTurn(double dx, double dy, CallbackInfo ci) {
		if (Freecam.isActive() && (Object) this == Minecraft.getInstance().player) {
			Freecam.turn(dx, dy);
			ci.cancel();
		}
	}
}
