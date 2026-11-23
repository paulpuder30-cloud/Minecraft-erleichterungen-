/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.utils.misc;

public interface ICopyable<T extends ICopyable<T>> {
    T set(T value);

    T copy();
}
