/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import de.erl.client.ErlClient;
import de.erl.client.events.render.Render2DEvent;
import de.erl.client.gui.WidgetScreen;
import de.erl.client.systems.hud.screens.HudEditorScreen;
import de.erl.client.utils.Utils;
import de.erl.client.utils.render.ErlMcGuiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Unique
    private GuiRenderState renderState;

    @Unique
    private ErlMcGuiRenderer guiRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$erl(GuiRenderState renderState, FeatureRenderDispatcher featureRenderDispatcher, List<PictureInPictureRenderer<?>> pictureInPictureRenderers, CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof ErlMcGuiRenderer) return;

        this.renderState = new GuiRenderState();

        guiRenderer = new ErlMcGuiRenderer(
            this.renderState,
            featureRenderDispatcher,
            pictureInPictureRenderers
        );
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render$preGui(CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof ErlMcGuiRenderer) return;
        var mc = Minecraft.getInstance();

        if (mc.gui.screen() == null || mc.gui.screen() instanceof WidgetScreen) return;
        erl$render2D(mc);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render$postGui(CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof ErlMcGuiRenderer) return;
        var mc = Minecraft.getInstance();

        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.gameRenderer.mainRenderTarget().getDepthTexture(), 1.0);

        if (mc.gui.screen() == null || mc.gui.screen() instanceof WidgetScreen) {
            erl$render2D(mc);
        }

        guiRenderer.endFrame();
    }

    @Unique
    private void erl$render2D(Minecraft mc) {
        var mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        var mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (Utils.canUpdate() || HudEditorScreen.isOpen()) {
            Profiler.get().push(ErlClient.MOD_ID + "_render_2d");
            Utils.unscaledProjection();

            var graphics = new GuiGraphicsExtractor(mc, renderState, mouseX, mouseY);
            var tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

            ErlClient.EVENT_BUS.post(Render2DEvent.get(graphics, graphics.guiWidth(), graphics.guiHeight(), tickDelta));
            guiRenderer.render();

            Utils.scaledProjection();
            Profiler.get().pop();
        }

        if (mc.gui.screen() instanceof WidgetScreen widgetScreen) {
            var graphics = new GuiGraphicsExtractor(mc, renderState, mouseX, mouseY);
            var guiDelta = mc.getDeltaTracker().getGameTimeDeltaTicks();

            widgetScreen.renderCustom(graphics, mouseX, mouseY, guiDelta);
            guiRenderer.render();
        }
    }
}
