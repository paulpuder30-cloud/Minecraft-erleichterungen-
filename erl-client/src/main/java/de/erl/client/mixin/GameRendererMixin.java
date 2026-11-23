/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import de.erl.client.ErlClient;
import de.erl.client.MixinPlugin;
import de.erl.client.events.render.Render3DEvent;
import de.erl.client.events.render.RenderAfterWorldEvent;
import de.erl.client.renderer.ErlRenderPipelines;
import de.erl.client.renderer.Renderer3D;
import de.erl.client.systems.modules.Modules;
import de.erl.client.systems.modules.render.Freecam;
import de.erl.client.systems.modules.render.NoRender;
import de.erl.client.systems.modules.render.Zoom;
import de.erl.client.utils.Utils;
import de.erl.client.utils.render.CustomBannerGuiElementRenderer;
import de.erl.client.utils.render.NametagUtils;
import de.erl.client.utils.render.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.profiling.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Camera mainCamera;

    @Unique
    private Renderer3D renderer;

    @Unique
    private Renderer3D depthRenderer;

    @Unique
    private final PoseStack matrices = new PoseStack();

    @Shadow
    protected abstract void bobView(final CameraRenderState cameraState, final PoseStack poseStack);

    @Shadow
    protected abstract void bobHurt(final CameraRenderState cameraState, final PoseStack poseStack);

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"))
    private List<PictureInPictureRenderer<?>> erl$addSpecialRenderers(List<PictureInPictureRenderer<?>> list) {
        List<PictureInPictureRenderer<?>> result = new ArrayList<>(list.size() + 1);
        result.addAll(list);
        result.add(new CustomBannerGuiElementRenderer(minecraft.getAtlasManager()));
        return result;
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render3dHud(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/OptionsRenderState;Z)V"))
    private void onRenderLevel(CallbackInfo ci, @Local(name = "projectionMatrix") Matrix4f projectionMatrix, @Local(name = "cameraState") CameraRenderState cameraState, @Local(name = "worldPartialTicks") float worldPartialTicks, @Local(name = "bobStack") PoseStack bobStack) {
        if (!Utils.canUpdate()) return;

        Profiler.get().push(ErlClient.MOD_ID + "_render");

        // Create renderer and event

        if (renderer == null)
            renderer = new Renderer3D(ErlRenderPipelines.WORLD_COLORED_LINES, ErlRenderPipelines.WORLD_COLORED);
        if (depthRenderer == null)
            depthRenderer = new Renderer3D(ErlRenderPipelines.WORLD_COLORED_LINES_DEPTH, ErlRenderPipelines.WORLD_COLORED_DEPTH);
        Render3DEvent event = Render3DEvent.get(bobStack, renderer, depthRenderer, worldPartialTicks, mainCamera.position().x, mainCamera.position().y, mainCamera.position().z);

        // Update model view matrix
        Matrix4fc modelViewMatrix = cameraState.viewRotationMatrix;
        RenderSystem.getModelViewStack().pushMatrix().mul(modelViewMatrix);

        matrices.pushPose();
        bobHurt(this.gameRenderState.levelRenderState.cameraRenderState, matrices);
        if (minecraft.options.bobView().get()) {
            bobView(this.gameRenderState.levelRenderState.cameraRenderState, matrices);
        }

        Matrix4f inverseBob = new Matrix4f(matrices.last().pose()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        matrices.popPose();

        // Call utility classes (apply bob correction when Iris shaders are active)

        Matrix4fc correctedPosition = MixinPlugin.isIrisPresent && RenderUtils.isShaderPackInUse() ? new Matrix4f(modelViewMatrix).mul(inverseBob) : modelViewMatrix;
        RenderUtils.updateScreenCenter(projectionMatrix, correctedPosition);
        NametagUtils.onRender(modelViewMatrix);

        // Render

        renderer.begin();
        depthRenderer.begin();
        ErlClient.EVENT_BUS.post(event);
        renderer.render(bobStack);
        depthRenderer.render(bobStack);

        // Revert model view matrix

        RenderSystem.getModelViewStack().popMatrix();

        Profiler.get().pop();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevelTail(CallbackInfo ci) {
        ErlClient.EVENT_BUS.post(RenderAfterWorldEvent.get());
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float applyCameraTransformationsMathHelperLerpProxy(float original) {
        return Modules.get().get(NoRender.class).noNausea() ? 0 : original;
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void renderItemInHand(CameraRenderState cameraState, PlayerRenderState playerState, GpuTextureView depthTextureView, CallbackInfo ci) {
        if (!Modules.get().get(Freecam.class).renderHands() || !Modules.get().get(Zoom.class).renderHands()) {
            ci.cancel();
        }
    }
}
