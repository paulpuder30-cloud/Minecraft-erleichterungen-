/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.events.world;

import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Crosby
 * @implNote Shouldn't be put in a {@link de.erl.client.utils.misc.Pool} to avoid a race-condition, or in a {@link ThreadLocal} as it is shared between threads.
 */
public record ChunkDataEvent(LevelChunk chunk) {
}
