/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixininterface;

public interface IServerboundMovePlayerPacket {
    int erl$getTag();

    void erl$setTag(int tag);
}
