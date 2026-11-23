/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

public class OnlinePlayers {
    private static long lastPingTime;

    private OnlinePlayers() {
    }

    public static void update() {
        long time = System.currentTimeMillis();

        // ERL-Client: Meteors Online-Zähler (meteorclient.com/api/online) wird bewusst nicht angepingt,
        // da dieser Fork nicht als Meteor-Client-Nutzer gezählt werden soll.
        lastPingTime = time;
    }

    public static void leave() {
        // ERL-Client: siehe update()
    }
}
