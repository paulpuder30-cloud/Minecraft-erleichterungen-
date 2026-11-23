/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.mojang.blaze3d.platform.Window;
import de.erl.client.ErlClient;
import de.erl.client.events.erl.MouseClickEvent;
import de.erl.client.events.erl.MouseScrollEvent;
import de.erl.client.utils.misc.input.Input;
import de.erl.client.utils.misc.input.KeyAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.blaze3d.platform.InputConstants.RELEASE;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    public abstract double getScaledXPos(Window window);

    @Shadow
    public abstract double getScaledYPos(Window window);

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        Input.setButtonState(rawButtonInfo.button(), action != RELEASE);

        MouseButtonEvent click = new MouseButtonEvent(getScaledXPos(minecraft.getWindow()), getScaledYPos(minecraft.getWindow()), rawButtonInfo);
        if (ErlClient.EVENT_BUS.post(MouseClickEvent.get(click, KeyAction.get(action))).isCancelled()) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (ErlClient.EVENT_BUS.post(MouseScrollEvent.get(yoffset)).isCancelled()) ci.cancel();
    }
}
