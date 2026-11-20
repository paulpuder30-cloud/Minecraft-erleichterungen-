package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Auto-Respawn: belebt den Spieler nach dem Tod automatisch wieder (sendet das
 * Respawn-Kommando), nach einer einstellbaren Verzögerung.
 */
public final class AutoRespawn {
	private static long lastRespawn;

	private AutoRespawn() {
	}

	public static void tick(Minecraft mc) {
		if (!ModConfig.get().autoRespawnEnabled) return;
		LocalPlayer player = mc.player;
		if (player == null) return;

		if (player.isDeadOrDying() || player.getHealth() <= 0.0f) {
			long now = System.currentTimeMillis();
			if (now - lastRespawn < ModConfig.get().autoRespawnDelayMs) return;
			lastRespawn = now;
			player.respawn();
		}
	}
}
