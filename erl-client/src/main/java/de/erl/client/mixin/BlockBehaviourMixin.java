/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import de.erl.client.ErlClient;
import de.erl.client.events.world.AmbientOcclusionEvent;
import de.erl.client.systems.modules.Modules;
import de.erl.client.systems.modules.render.NoRender;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevel(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        AmbientOcclusionEvent event = ErlClient.EVENT_BUS.post(AmbientOcclusionEvent.get());

        if (event.lightLevel != -1) cir.setReturnValue(event.lightLevel);
    }

    @Inject(method = "getSeed", at = @At("HEAD"), cancellable = true)
    private void onRenderingSeed(BlockState state, BlockPos pos, CallbackInfoReturnable<Long> cir) {
        if (Modules.get().get(NoRender.class).noTextureRotations()) cir.setReturnValue(0L);
    }
}
