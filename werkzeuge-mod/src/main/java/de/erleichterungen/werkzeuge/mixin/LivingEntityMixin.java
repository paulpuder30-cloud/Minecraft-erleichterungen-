package de.erleichterungen.werkzeuge.mixin;

import de.erleichterungen.werkzeuge.HyperionItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Wither-Schild der Hyperion: 10 % weniger Schaden, solange er aktiv ist. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float werkzeuge$witherShield(float amount) {
		if ((Object) this instanceof Player p && HyperionItem.shieldActive(p)) return amount * HyperionItem.SHIELD_DAMAGE_FACTOR;
		return amount;
	}
}
