package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.mixin.OptionsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Fullbright: setzt die Helligkeit (Gamma) auf das Maximum, solange aktiv,
 * und stellt den vorherigen Wert beim Abschalten wieder her.
 */
public final class Fullbright {
	private static boolean applied;
	private static double saved;

	private Fullbright() {
	}

	public static void tick(Minecraft mc) {
		OptionInstance<Double> gamma = ((OptionsAccessor) mc.options).erleichterungen$gamma();
		if (ModConfig.get().fullbrightEnabled) {
			if (!applied) {
				saved = gamma.get();
				applied = true;
			}
			if (gamma.get() < 1.0) gamma.set(1.0);
		} else if (applied) {
			gamma.set(saved);
			applied = false;
		}
	}
}
