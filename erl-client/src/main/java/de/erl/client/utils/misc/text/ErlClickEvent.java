/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.utils.misc.text;

import de.erl.client.mixin.ScreenMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class does nothing except ensure that {@link ClickEvent}'s containing ERL-Client commands can only be executed if they come from the client.
 *
 * @see ScreenMixin#onDefaultHandleClickEvent(ClickEvent, Minecraft, Screen, CallbackInfo)
 */
public class ErlClickEvent implements ClickEvent {
    public final String value;

    public ErlClickEvent(String value) {
        this.value = value;
    }

    @Override
    public @NonNull Action action() {
        return Action.RUN_COMMAND;
    }
}
