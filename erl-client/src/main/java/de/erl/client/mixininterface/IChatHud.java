/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixininterface;

import net.minecraft.network.chat.Component;

public interface IChatHud {
    void erl$add(Component message, int id);
}
