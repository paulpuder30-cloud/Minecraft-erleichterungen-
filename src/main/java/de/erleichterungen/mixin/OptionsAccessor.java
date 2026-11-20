package de.erleichterungen.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Zugriff auf die privaten Options-Felder {@code gamma} (Helligkeit) und {@code fov} (Sichtfeld),
 * die kein öffentliches Getter haben. Wird von Fullbright und Zoom genutzt.
 */
@Mixin(Options.class)
public interface OptionsAccessor {
	@Accessor("gamma")
	OptionInstance<Double> erleichterungen$gamma();

	@Accessor("fov")
	OptionInstance<Integer> erleichterungen$fov();
}
