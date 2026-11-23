/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import de.erl.client.utils.misc.FakeClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static de.erl.client.ErlClient.mc;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    // Player model rendering in main menu

    @Inject(method = "getPlayerInfo", at = @At("HEAD"), cancellable = true)
    private void onGetPlayerListEntry(CallbackInfoReturnable<PlayerInfo> cir) {
        if (mc.getConnection() == null) cir.setReturnValue(FakeClientPlayer.getPlayerListEntry());
    }
}
