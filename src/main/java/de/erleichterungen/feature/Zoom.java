package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.mixin.OptionsAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Zoom: solange die Zoom-Taste gehalten wird, wird das Sichtfeld (FOV) verringert.
 * Beim Loslassen wird das ursprüngliche FOV wiederhergestellt.
 */
public final class Zoom {
	private static boolean zooming;
	private static int saved;

	private Zoom() {
	}

	public static void tick(Minecraft mc, KeyMapping key) {
		OptionInstance<Integer> fov = ((OptionsAccessor) mc.options).erleichterungen$fov();
		boolean held = key.isDown() && mc.gui.screen() == null;
		if (held) {
			if (!zooming) {
				saved = fov.get();
				zooming = true;
			}
			fov.set(ModConfig.get().zoomFov);
		} else if (zooming) {
			fov.set(saved);
			zooming = false;
		}
	}
}
