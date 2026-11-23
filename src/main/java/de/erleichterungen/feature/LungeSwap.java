package de.erleichterungen.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * Auto-LungeSwap: wirft auf Tastendruck eine Wind Charge (geringer Cooldown) und wechselt
 * im selben Tick auf den Speer. Die Items werden automatisch in der Hotbar gesucht.
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
		mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); // Wind Charge werfen
		ComboUtil.selectSlot(mc, spear); // sofort auf den Speer wechseln
	}
}
