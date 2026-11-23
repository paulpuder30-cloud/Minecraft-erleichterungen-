/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixininterface;

public interface IRenderPipeline {
    void erl$setLineSmooth(boolean lineSmooth);

    boolean erl$getLineSmooth();
}
