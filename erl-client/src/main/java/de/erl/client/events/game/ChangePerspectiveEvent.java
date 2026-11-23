/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.events.game;

import de.erl.client.events.Cancellable;
import net.minecraft.client.CameraType;

public class ChangePerspectiveEvent extends Cancellable {
    private static final ChangePerspectiveEvent INSTANCE = new ChangePerspectiveEvent();

    public CameraType perspective;

    public static ChangePerspectiveEvent get(CameraType perspective) {
        INSTANCE.setCancelled(false);
        INSTANCE.perspective = perspective;
        return INSTANCE;
    }
}
