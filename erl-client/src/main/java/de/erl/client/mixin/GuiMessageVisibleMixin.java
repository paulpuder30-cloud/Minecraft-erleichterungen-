/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.mojang.authlib.GameProfile;
import de.erl.client.mixininterface.IGuiMessageVisible;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.Line.class)
public abstract class GuiMessageVisibleMixin implements IGuiMessageVisible {
    @Shadow
    @Final
    private FormattedCharSequence content;
    @Unique
    private int id;
    @Unique
    private GameProfile sender;
    @Unique
    private boolean startOfEntry;

    @Override
    public String erl$getText() {
        StringBuilder sb = new StringBuilder();

        content.accept((_, _, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });

        return sb.toString();
    }

    @Override
    public int erl$getId() {
        return id;
    }

    @Override
    public void erl$setId(int id) {
        this.id = id;
    }

    @Override
    public GameProfile erl$getSender() {
        return sender;
    }

    @Override
    public void erl$setSender(GameProfile profile) {
        sender = profile;
    }

    @Override
    public boolean erl$isStartOfEntry() {
        return startOfEntry;
    }

    @Override
    public void erl$setStartOfEntry(boolean start) {
        startOfEntry = start;
    }
}
