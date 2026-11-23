/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixininterface;

import com.mojang.authlib.GameProfile;

public interface IGuiMessage {
    String erl$getText();

    int erl$getId();

    void erl$setId(int id);

    GameProfile erl$getSender();

    void erl$setSender(GameProfile profile);
}
