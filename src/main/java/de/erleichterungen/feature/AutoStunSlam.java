package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Auto-StunSlam: Der Spieler schießt sich selbst hoch (manuell per Wind Charge/Pearl); auf
 * Tastendruck automatisiert dieses Modul nur den Slam: Axt-Schlag auf den nächsten Gegner
 * (Schild-Stun) → Wechsel auf die Mace → Mace-Schlag (Fall-Smash). Das Ziel wird beim
 * Auslösen erfasst und bei beiden Schlägen angegriffen, auch wenn das Fadenkreuz abdriftet.
 */
public final class AutoStunSlam {
	private static final Set<Item> AXES = Set.of(
			Items.WOODEN_AXE, Items.STONE_AXE, Items.COPPER_AXE, Items.GOLDEN_AXE,
			Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);

	private enum State { IDLE, AXE, WAIT, MACE }

	private static State state = State.IDLE;
	private static int wait;
	private static LivingEntity target;

	private AutoStunSlam() {
	}

	/** Startet die Sequenz (auf Tastendruck). */
	public static void start(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.gui.screen() != null || mc.player.isSpectator()) return;
		if (ComboUtil.findHotbar(mc, AXES) < 0 || ComboUtil.findHotbar(mc, Items.MACE) < 0) return;

		LivingEntity t = acquire(mc);
		if (t == null) return;
		target = t;
		state = State.AXE;
	}

	/** Treibt die laufende Sequenz pro Tick voran. */
	public static void tick(Minecraft mc) {
		if (state == State.IDLE) return;
		if (mc.player == null || mc.level == null || target == null
				|| !target.isAlive() || mc.player.isSpectator()) {
			reset();
			return;
		}
		ModConfig cfg = ModConfig.get();
		switch (state) {
			case AXE -> {
				int axe = ComboUtil.findHotbar(mc, AXES);
				if (axe < 0) { reset(); return; }
				ComboUtil.selectSlot(mc, axe);
				if (cfg.autoStunSlamRotate) ComboUtil.rotateTo(mc, target.getBoundingBox().getCenter());
				mc.gameMode.attack(mc.player, target);
				mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
				wait = Math.max(0, cfg.autoStunSlamGapTicks);
				state = State.WAIT;
			}
			case WAIT -> {
				if (wait-- <= 0) state = State.MACE;
			}
			case MACE -> {
				int mace = ComboUtil.findHotbar(mc, Items.MACE);
				if (mace < 0) { reset(); return; }
				ComboUtil.selectSlot(mc, mace);
				if (cfg.autoStunSlamRotate) ComboUtil.rotateTo(mc, target.getBoundingBox().getCenter());
				mc.gameMode.attack(mc.player, target);
				mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
				reset();
			}
			default -> reset();
		}
	}

	/** Nächstes gültiges Ziel in Reichweite (standardmäßig nur Spieler). */
	private static LivingEntity acquire(Minecraft mc) {
		ModConfig cfg = ModConfig.get();
		double rangeSq = cfg.autoStunSlamRange * cfg.autoStunSlamRange;
		Vec3 eye = mc.player.getEyePosition(1.0f);
		LivingEntity best = null;
		double bestSq = Double.MAX_VALUE;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity le)) continue;
			if (le == mc.player || !le.isAlive() || !le.isAttackable()) continue;
			if (cfg.autoStunSlamPlayersOnly && !(le instanceof Player)) continue;
			if (mc.player.isAlliedTo(le)) continue;
			double d = eye.distanceToSqr(le.getBoundingBox().getCenter());
			if (d <= rangeSq && d < bestSq) {
				bestSq = d;
				best = le;
			}
		}
		return best;
	}

	private static void reset() {
		state = State.IDLE;
		target = null;
		wait = 0;
	}
}
