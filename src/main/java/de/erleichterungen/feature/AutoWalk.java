package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;

/**
 * Auto-Walk: hält automatisch die Vorwärtstaste gedrückt. Beim Öffnen eines Menüs
 * oder beim Abschalten wird die Taste wieder freigegeben.
 */
public final class AutoWalk {
	private static boolean forced;

	private AutoWalk() {
	}

	public static void tick(Minecraft mc) {
		boolean want = ModConfig.get().autoWalkEnabled && mc.player != null && mc.gui.screen() == null;
		if (want) {
			mc.options.keyUp.setDown(true);
			forced = true;
		} else if (forced) {
			mc.options.keyUp.setDown(false);
			forced = false;
		}
	}
}
