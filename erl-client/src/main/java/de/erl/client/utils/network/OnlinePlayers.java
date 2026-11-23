/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.utils.network;

public class OnlinePlayers {
    private static long lastPingTime;

    private OnlinePlayers() {
    }

    public static void update() {
        long time = System.currentTimeMillis();

        // ERL-Client sendet keine Online-Statistik an externe Server.
        lastPingTime = time;
    }

    public static void leave() {
        // ERL-Client: siehe update()
    }
}
