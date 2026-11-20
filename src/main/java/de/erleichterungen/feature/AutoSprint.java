package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;

/**
 * Auto-Sprint: lässt den Spieler automatisch sprinten, sobald er vorwärts läuft
 * (nicht beim Schleichen oder während ein Gegenstand benutzt wird).
 */
public final class AutoSprint {
	private AutoSprint() {
	}

	public static void tick(Minecraft mc) {
		if (!ModConfig.get().autoSprintEnabled) return;
		if (mc.player == null || mc.gui.screen() != null || !mc.player.isAlive()) return;

		boolean forward = mc.options.keyUp.isDown();
		boolean sneaking = mc.options.keyShift.isDown();
		if (forward && !sneaking && !mc.player.isUsingItem()) {
			mc.player.setSprinting(true);
		}
	}
}
