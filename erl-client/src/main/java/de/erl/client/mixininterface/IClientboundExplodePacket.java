/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixininterface;

public interface IClientboundExplodePacket {
    void erl$setVelocityX(float velocity);

    void erl$setVelocityY(float velocity);

    void erl$setVelocityZ(float velocity);
}
