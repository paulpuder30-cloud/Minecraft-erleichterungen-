package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * AutoTotem: legt automatisch ein Totem der Unsterblichkeit in die Nebenhand, sobald dort
 * keins liegt. Nutzt den regulären Container-Swap ({@code ContainerInput.SWAP}, Taste 40 =
 * Nebenhand-Slot), ist also serverkonform und braucht keine gefälschten Pakete.
 */
public final class AutoTotem {
	private static int cooldown;

	private AutoTotem() {
	}

	public static void tick(Minecraft mc) {
		if (cooldown > 0) cooldown--;
		if (!ModConfig.get().autoTotemEnabled) return;

		LocalPlayer player = mc.player;
		if (player == null || player.isSpectator()) return;
		// Nur im eigenen Inventar handeln – kein offener Container (z.B. Truhe)
		if (player.containerMenu != player.inventoryMenu) return;

		ItemStack offhand = player.getOffhandItem();
		if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;
		if (ModConfig.get().autoTotemOnlyWhenEmpty && !offhand.isEmpty()) return;
		if (cooldown > 0) return;

		Inventory inv = player.getInventory();
		int slot = inv.findSlotMatchingItem(new ItemStack(Items.TOTEM_OF_UNDYING));
		if (slot < 0) return;

		// Inventar-Index -> InventoryMenu-Slot (Hotbar 0..8 -> 36..44, Hauptinventar 9..35 -> gleich)
		int menuSlot = slot < 9 ? 36 + slot : slot;
		mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, menuSlot, 40,
				ContainerInput.SWAP, player);
		cooldown = 5; // kurze Pause, bis der Server den Tausch bestätigt
	}
}
