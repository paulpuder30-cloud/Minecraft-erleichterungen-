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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * KillAura: erfasst automatisch nahe Lebewesen in Reichweite, dreht optional zu ihnen
 * und greift an. Zielauswahl per Entfernung (oder niedrigster Gesundheit), mit Filtern
 * nach Zieltyp und einstellbarer Reichweite/Verzögerung.
 */
public final class KillAura {
	private static int delay;

	private KillAura() {
	}

	public static void tick(Minecraft mc) {
		if (delay > 0) delay--;
		ModConfig cfg = ModConfig.get();
		if (!cfg.killAuraEnabled) return;
		if (mc.player == null || mc.level == null || mc.gui.screen() != null
				|| mc.player.isSpectator() || !mc.player.isAlive()) return;
		if (cfg.killAuraRequireCharged && mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;
		if (delay > 0) return;

		double rangeSq = cfg.killAuraRange * cfg.killAuraRange;
		Vec3 eye = mc.player.getEyePosition(1.0f);

		List<LivingEntity> targets = new ArrayList<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity le)) continue;
			if (le == mc.player || !le.isAlive() || le.isRemoved() || !le.isAttackable()) continue;
			if (mc.player.isAlliedTo(le)) continue;
			if (!matchesFilter(cfg, le)) continue;
			if (eye.distanceToSqr(le.getBoundingBox().getCenter()) > rangeSq) continue;
			targets.add(le);
		}
		if (targets.isEmpty()) return;

		targets.sort((a, b) -> {
			if (cfg.killAuraPrioritizeLowHealth) {
				int c = Float.compare(a.getHealth(), b.getHealth());
				if (c != 0) return c;
			}
			return Double.compare(eye.distanceToSqr(a.getBoundingBox().getCenter()),
					eye.distanceToSqr(b.getBoundingBox().getCenter()));
		});

		int max = Math.max(1, cfg.killAuraMaxTargets);
		int attacked = 0;
		for (LivingEntity target : targets) {
			if (attacked >= max) break;
			if (cfg.killAuraRotate && attacked == 0) {
				rotateTo(mc, eye, target.getBoundingBox().getCenter());
			}
			mc.gameMode.attack(mc.player, target);
			mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			attacked++;
		}
		if (attacked > 0) delay = cfg.killAuraDelayTicks;
	}

	private static void rotateTo(Minecraft mc, Vec3 from, Vec3 to) {
		double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
		double distXZ = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));
		mc.player.setYRot(yaw);
		mc.player.setXRot(pitch);
	}

	private static boolean matchesFilter(ModConfig cfg, LivingEntity t) {
		if (t instanceof Player) return cfg.killAuraPlayers;
		if (t instanceof Monster) return cfg.killAuraHostile;
		if (t instanceof Animal) return cfg.killAuraPassive;
		return cfg.killAuraOther;
	}
}
