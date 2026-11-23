/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.frontend.shaders.PipelineBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import de.erl.client.mixininterface.IRenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executor;

@Mixin(PipelineBuilder.class)
public abstract class PipelineBuilderMixin {
    @Inject(method = "lambda$compilePipeline$1", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/Executor;execute(Ljava/lang/Runnable;)V"))
    private static void compilePipeline$lineSmooth(RenderPipeline pipeline, BackendRenderPipeline.Pending backendPendingPipeline, Executor executor, ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos, Object2IntOpenHashMap<String> uniformBindings, CallbackInfoReturnable<CompiledRenderPipeline> cir, @Local(name = "backendPipeline") BackendRenderPipeline backendPipeline) {
        if (backendPipeline instanceof IRenderPipeline iPipeline) {
            iPipeline.erl$setLineSmooth(((IRenderPipeline) pipeline).erl$getLineSmooth());
        }
    }
}
