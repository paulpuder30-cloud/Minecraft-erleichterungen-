/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.erl.client.ErlClient;
import de.erl.client.events.entity.player.FinishUsingItemEvent;
import de.erl.client.events.entity.player.StoppedUsingItemEvent;
import de.erl.client.events.game.ItemStackTooltipEvent;
import de.erl.client.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static de.erl.client.ErlClient.mc;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"))
    private List<Component> onGetTooltipLines(List<Component> original) {
        if (Utils.canUpdate()) {
            ItemStackTooltipEvent event = ErlClient.EVENT_BUS.post(new ItemStackTooltipEvent((ItemStack) (Object) this, original));
            return event.list();
        }

        return original;
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void onFinishUsingItem(Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
        if (livingEntity == mc.player) {
            ErlClient.EVENT_BUS.post(FinishUsingItemEvent.get((ItemStack) (Object) this));
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void onReleaseUsing(Level level, LivingEntity entity, int remainingTime, CallbackInfo ci) {
        if (entity == mc.player) {
            ErlClient.EVENT_BUS.post(StoppedUsingItemEvent.get((ItemStack) (Object) this));
        }
    }
}
