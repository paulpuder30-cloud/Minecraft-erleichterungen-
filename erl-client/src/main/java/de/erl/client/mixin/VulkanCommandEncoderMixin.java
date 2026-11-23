/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import de.erl.client.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderMixin {
    @Shadow
    @Final
    private VulkanDevice device;

    @SuppressWarnings("deprecation")
    @Inject(method = "createRenderPass", at = @At("RETURN"))
    private void createRenderPass$iGpuDevice(CallbackInfoReturnable<RenderPassBackend> cir) {
        ((IGpuDevice) device).erl$onCreateRenderPass(cir.getReturnValue());
    }
}
