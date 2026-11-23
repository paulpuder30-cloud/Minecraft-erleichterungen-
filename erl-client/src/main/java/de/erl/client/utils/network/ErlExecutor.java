/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.utils.network;

import de.erl.client.utils.PreInit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ErlExecutor {
    public static ExecutorService executor;

    private ErlExecutor() {
    }

    @PreInit
    public static void init() {
        AtomicInteger threadNumber = new AtomicInteger(1);

        executor = Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.setName("ERL-Executor-" + threadNumber.getAndIncrement());
            return thread;
        });
    }

    public static void execute(Runnable task) {
        executor.execute(task);
    }
}
