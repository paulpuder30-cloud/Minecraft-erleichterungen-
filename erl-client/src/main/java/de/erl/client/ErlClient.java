/*
 * This file is part of ERL-Client, a fork of Meteor Client (https://github.com/MeteorDevelopment/meteor-client).
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.world.item.Items;

import java.lang.invoke.MethodHandles;

/**
 * Einstiegspunkt für alle ERL-eigenen Erweiterungen des Meteor-Kerns.
 * Wird aus {@code MeteorClient}, {@code Categories} und {@code Modules} aufgerufen.
 */
public final class ErlClient {
    public static final String PACKAGE = "de.erl.client";

    /** Eigene Modul-Kategorie für die ERL-Module. */
    public static final Category CATEGORY = new Category("ERL", () -> DisplayItemUtils.toStack(Items.MACE));

    private ErlClient() {
    }

    /** Orbit braucht pro Package eine Lambda-Factory, damit {@code @EventHandler} in de.erl.client funktioniert. */
    public static void registerEventPackage() {
        MeteorClient.EVENT_BUS.registerLambdaFactory(PACKAGE, (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
    }

    public static void registerCategories() {
        Modules.registerCategory(CATEGORY);
    }

    public static void registerModules(Modules modules) {
    }
}
