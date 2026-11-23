/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.player;

import de.erl.client.events.world.TickEvent;
import de.erl.client.settings.BoolSetting;
import de.erl.client.settings.ItemListSetting;
import de.erl.client.settings.Setting;
import de.erl.client.settings.SettingGroup;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Module;
import de.erl.client.utils.Utils;
import de.erl.client.utils.player.InvUtils;
import de.erl.client.utils.player.SlotUtils;
import de.erl.client.lib.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class AutoMend extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("Item blacklist.")
        .filter(item -> item.components().get(DataComponents.DAMAGE) != null)
        .bypassFilterWhenSavingAndLoading()                                                        
        .build()
    );

    private final Setting<Boolean> force = sgGeneral.add(new BoolSetting.Builder()
        .name("force")
        .description("Replaces item in offhand even if there is some other non-repairable item.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Automatically disables when there are no more items to repair.")
        .defaultValue(true)
        .build()
    );

    private boolean didMove;

    public AutoMend() {
        super(Categories.Player, "auto-mend", "Automatically replaces items in your offhand with mending when fully repaired.");
    }

    @Override
    public void onActivate() {
        didMove = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (shouldWait()) return;

        int slot = getSlot();

        if (slot == -1) {
            if (autoDisable.get()) {
                info("Repaired all items, disabling");

                if (didMove) {
                    int emptySlot = getEmptySlot();
                    InvUtils.move().fromOffhand().to(emptySlot);
                }

                toggle();
            }
        } else {
            InvUtils.move().from(slot).toOffhand();
            didMove = true;
        }
    }

    private boolean shouldWait() {
        ItemStack itemStack = mc.player.getOffhandItem();

        if (itemStack.isEmpty()) return false;

        if (Utils.hasEnchantments(itemStack, Enchantments.MENDING)) {
            return itemStack.getDamageValue() != 0;
        }

        return !force.get();
    }

    private int getSlot() {
        return InvUtils.find(itemStack -> !blacklist.get().contains(itemStack.getItem()) && Utils.hasEnchantments(itemStack, Enchantments.MENDING) && itemStack.getDamageValue() > 0, SlotUtils.HOTBAR_START, SlotUtils.MAIN_END).slot();
    }

    private int getEmptySlot() {
        return InvUtils.find(ItemStack::isEmpty, SlotUtils.HOTBAR_START, SlotUtils.MAIN_END).slot();
    }
}
