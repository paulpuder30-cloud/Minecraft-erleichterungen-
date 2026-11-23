/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.events.world;

import de.erl.client.events.Cancellable;
import net.minecraft.core.particles.ParticleOptions;

public class ParticleEvent extends Cancellable {
    private static final ParticleEvent INSTANCE = new ParticleEvent();

    public ParticleOptions particle;

    public static ParticleEvent get(ParticleOptions particle) {
        INSTANCE.setCancelled(false);
        INSTANCE.particle = particle;
        return INSTANCE;
    }
}
