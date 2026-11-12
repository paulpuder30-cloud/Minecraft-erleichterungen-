package de.erleichterungen.mixin;

import de.erleichterungen.feature.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Setzt bei aktiver Freecam Position und Blickrichtung der Kamera. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void setPosition(Vec3 pos);

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void erleichterungen$freecam(float partialTicks, CallbackInfo ci) {
		if (!Freecam.isActive()) return;
		setRotation(Freecam.yaw(), Freecam.pitch());
		setPosition(Freecam.position(partialTicks));
		detached = true; // eigener Spieler wird gerendert, Hand ausgeblendet
	}
}
