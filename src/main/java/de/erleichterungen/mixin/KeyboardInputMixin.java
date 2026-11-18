package de.erleichterungen.mixin;

import de.erleichterungen.feature.Excavator;
import de.erleichterungen.feature.Freecam;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bei aktiver Freecam bewegt sich der Spieler nicht – die Tasten steuern die Kamera.
 * Beim Ausheben mit "Automatisch laufen" wird nach vorne gelaufen.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void erleichterungen$blockMovement(CallbackInfo ci) {
		if (Freecam.isActive()) {
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
		} else if (Excavator.isWalking()) {
			// Ausheben läuft selbstständig zur nächsten Stelle
			this.keyPresses = new Input(true, false, false, false, Excavator.wantsJump(), false, false);
			this.moveVector = new Vec2(0.0f, 1.0f);
		}
	}
}
