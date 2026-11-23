/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erl.client.ErlClient;
import de.erl.client.commands.Commands;
import de.erl.client.systems.config.Config;
import de.erl.client.systems.modules.Modules;
import de.erl.client.systems.modules.movement.GUIMove;
import de.erl.client.systems.modules.render.NoRender;
import de.erl.client.utils.Utils;
import de.erl.client.utils.misc.text.ErlClickEvent;
import de.erl.client.utils.misc.text.RunnableClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mojang.blaze3d.platform.InputConstants.*;

@Mixin(value = Screen.class, priority = 500) // needs to be before baritone
public abstract class ScreenMixin {

    @Unique
    private static boolean erl$isArray(int key) {
        return key == KEY_RIGHT || key == KEY_LEFT || key == KEY_DOWN || key == KEY_UP;
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void onExtractTransparentBackground(CallbackInfo ci) {
        if (Utils.canUpdate() && Modules.get().get(NoRender.class).noGuiBackground())
            ci.cancel();
    }

    @Inject(method = "defaultHandleClickEvent", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", remap = false), cancellable = true)
    private static void onDefaultHandleClickEvent(ClickEvent event, Minecraft minecraft, Screen activeScreen, CallbackInfo ci) {
        if (event instanceof RunnableClickEvent runnableClickEvent) {
            runnableClickEvent.runnable.run();
            ci.cancel();
        } else if (event instanceof ErlClickEvent erlClickEvent && erlClickEvent.value.startsWith(Config.get().prefix.get())) {
            try {
                Commands.dispatch(erlClickEvent.value.substring(Config.get().prefix.get().length()));
            } catch (CommandSyntaxException e) {
                ErlClient.LOG.error("Failed to run command", e);
            } finally {
                ci.cancel();
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) (this) instanceof ChatScreen) return;
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        if ((guiMove.disableArrows() && erl$isArray(event.key())) || (guiMove.disableSpace() && event.key() == KEY_SPACE)) {
            cir.setReturnValue(true);
        }
    }
}
