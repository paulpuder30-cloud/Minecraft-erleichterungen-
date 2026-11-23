/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixininterface;


import com.mojang.renderpearl.backend.api.RenderPassBackend;

public interface IGpuDevice {
    /**
     * Currently there can only be a single scissor pushed at once.
     */
    void erl$pushScissor(int x, int y, int width, int height);

    void erl$popScissor();

    /**
     * This is an *INTERNAL* method, it shouldn't be called.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    void erl$onCreateRenderPass(RenderPassBackend backend);
}
