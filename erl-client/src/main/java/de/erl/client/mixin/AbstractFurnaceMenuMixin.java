/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import de.erl.client.mixininterface.IAbstractFurnaceMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin implements IAbstractFurnaceMenu {
    @Shadow
    protected abstract boolean canSmelt(ItemStack itemStack);

    @Override
    public boolean erl$canSmelt(ItemStack itemStack) {
        return canSmelt(itemStack);
    }
}
