/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface ILevelRenderer {
    void erl$pushEntityOutlineFramebuffer(RenderTarget framebuffer);

    void erl$popEntityOutlineFramebuffer();
}
