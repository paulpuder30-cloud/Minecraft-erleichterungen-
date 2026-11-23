package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;

import java.util.Random;

/**
 * Shieldbreaker: solange die Taste gehalten wird, werden schnelle Angriffe auf das anvisierte
 * Ziel ausgeführt (nutzt die Mace ab, um ein hochgehaltenes Schild an Haltbarkeit zu verlieren).
 * Die Abstände zwischen den Klicks werden zufällig zwischen Min- und Max-Klickrate gewählt,
 * damit die Klicks nicht gleichförmig wirken.
 */
public final class Shieldbreaker {
	private static final Random RANDOM = new Random();
	private static long nextAttack;

	private Shieldbreaker() {
	}

	public static void tick(Minecraft mc, KeyMapping key) {
		if (mc.player == null || mc.level == null || mc.gui.screen() != null || mc.player.isSpectator()) return;
		if (!key.isDown()) return;

		ModConfig cfg = ModConfig.get();
		if (cfg.shieldbreakerRequireMace && mc.player.getMainHandItem().getItem() != Items.MACE) return;

		Entity e = mc.crosshairPickEntity;
		if (!(e instanceof LivingEntity target) || !target.isAlive() || !target.isAttackable()) return;

		long now = System.currentTimeMillis();
		if (now < nextAttack) return;

		mc.gameMode.attack(mc.player, target);
		mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);

		int min = Math.max(1, cfg.shieldbreakerMinCps);
		int max = Math.max(min, cfg.shieldbreakerMaxCps);
		double cps = min + RANDOM.nextDouble() * (max - min);
		nextAttack = now + Math.round(1000.0 / cps);
	}
}
