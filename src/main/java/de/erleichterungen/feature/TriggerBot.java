package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * TriggerBot: greift automatisch das Lebewesen unter dem Fadenkreuz an, sobald die
 * Angriffsleiste (nahezu) voll ist. Es wird ausschließlich das bereits anvisierte Ziel
 * angegriffen – es findet keine automatische Zielerfassung oder Kameradrehung statt.
 */
public final class TriggerBot {
	private static int delay;

	private TriggerBot() {
	}

	public static void tick(Minecraft mc) {
		if (delay > 0) delay--;
		ModConfig cfg = ModConfig.get();
		if (!cfg.triggerBotEnabled) return;
		if (mc.player == null || mc.gui.screen() != null || mc.player.isSpectator() || !mc.player.isAlive()) return;

		Entity e = mc.crosshairPickEntity;
		if (!(e instanceof LivingEntity target)) return;
		if (!target.isAlive() || target == mc.player || !target.isAttackable()) return;
		if (mc.player.isAlliedTo(target)) return;
		if (!matchesFilter(cfg, target)) return;

		if (cfg.triggerBotRequireCharged && mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;
		if (delay > 0) return;

		mc.gameMode.attack(mc.player, target);
		mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		delay = cfg.triggerBotDelayTicks;
	}

	private static boolean matchesFilter(ModConfig cfg, LivingEntity target) {
		if (target instanceof Player) return cfg.triggerBotPlayers;
		if (target instanceof Monster) return cfg.triggerBotHostile;
		if (target instanceof Animal) return cfg.triggerBotPassive;
		return cfg.triggerBotOther;
	}
}
