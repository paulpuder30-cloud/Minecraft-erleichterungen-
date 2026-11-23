/*
 * This file is part of ERL-Client, a fork of Meteor Client (https://github.com/MeteorDevelopment/meteor-client).
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client;

import de.erl.client.modules.LungeSwap;
import de.erl.client.modules.ShieldBreaker;
import de.erl.client.modules.StunSlam;
import de.erl.client.systems.modules.Category;
import de.erl.client.systems.modules.Modules;
import de.erl.client.utils.render.DisplayItemUtils;
import net.minecraft.world.item.Items;

/**
 * Registrierung der ERL-eigenen Kategorie und Module.
 * Wird aus {@code ErlClient}, {@code Categories} und {@code Modules} aufgerufen.
 */
public final class ErlExtras {
    public static final String PACKAGE = "de.erl.client";

    /** Eigene Modul-Kategorie für die ERL-Module. */
    public static final Category CATEGORY = new Category("ERL", () -> DisplayItemUtils.toStack(Items.MACE));

    private ErlExtras() {
    }

    public static void registerCategories() {
        Modules.registerCategory(CATEGORY);
    }

    public static void registerModules(Modules modules) {
        modules.add(new ShieldBreaker());
        modules.add(new StunSlam());
        modules.add(new LungeSwap());
    }
}
