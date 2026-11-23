package de.erleichterungen.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Hilfsfunktionen für Kampf-Combos: Hotbar-Slots finden und serverkonform den gehaltenen
 * Gegenstand wechseln (inkl. {@link ServerboundSetCarriedItemPacket}, damit der Server den
 * Wechsel sofort registriert).
 */
public final class ComboUtil {
	private ComboUtil() {
	}

	/** Hotbar-Slot (0..8) mit genau diesem Item, sonst -1. */
	public static int findHotbar(Minecraft mc, Item item) {
		Inventory inv = mc.player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getItem(i).getItem() == item) return i;
		}
		return -1;
	}

	/** Hotbar-Slot (0..8) mit einem Item aus der Menge, sonst -1. */
	public static int findHotbar(Minecraft mc, Set<Item> items) {
		Inventory inv = mc.player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (items.contains(inv.getItem(i).getItem())) return i;
		}
		return -1;
	}

	/** Wählt einen Hotbar-Slot (0..8) und meldet den Wechsel sofort an den Server. */
	public static void selectSlot(Minecraft mc, int slot) {
		if (slot < 0 || slot > 8) return;
		Inventory inv = mc.player.getInventory();
		if (inv.getSelectedSlot() == slot) return;
		inv.setSelectedSlot(slot);
		mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
	}

	/** Dreht den Spieler (Yaw/Pitch) so, dass er von den Augen aus auf den Zielpunkt schaut. */
	public static void rotateTo(Minecraft mc, Vec3 to) {
		Vec3 from = mc.player.getEyePosition(1.0f);
		double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
		double distXZ = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));
		mc.player.setYRot(yaw);
		mc.player.setXRot(pitch);
	}
}
