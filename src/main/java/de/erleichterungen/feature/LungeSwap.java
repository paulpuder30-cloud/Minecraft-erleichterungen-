package de.erleichterungen.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;

import java.util.Set;

/**
 * Auto-LungeSwap: schlägt auf Tastendruck mit der Wind Charge zu (geringer Cooldown, schneller
 * Hit auf das anvisierte Ziel) und wechselt im selben Tick auf den Speer (mit Lunge-Verzauberung).
 * Die Items werden automatisch in der Hotbar gesucht.
 */
public final class LungeSwap {
	private static final Set<Item> SPEARS = Set.of(
			Items.WOODEN_SPEAR, Items.STONE_SPEAR, Items.COPPER_SPEAR, Items.IRON_SPEAR,
			Items.GOLDEN_SPEAR, Items.DIAMOND_SPEAR, Items.NETHERITE_SPEAR);

	private LungeSwap() {
	}

	public static void trigger(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.gui.screen() != null || mc.player.isSpectator()) return;

		int wc = ComboUtil.findHotbar(mc, Items.WIND_CHARGE);
		int spear = ComboUtil.findHotbar(mc, SPEARS);
		if (wc < 0 || spear < 0) return; // Wind Charge und/oder Speer nicht in der Hotbar

		ComboUtil.selectSlot(mc, wc);
		// Mit der Wind Charge zuschlagen (nicht werfen): anvisiertes Ziel angreifen, Arm schwingen
		Entity e = mc.crosshairPickEntity;
		if (e instanceof LivingEntity target && target.isAlive()) {
			mc.gameMode.attack(mc.player, target);
		}
		mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		ComboUtil.selectSlot(mc, spear); // gleichzeitig auf den Speer (Lunge) wechseln
	}
}
