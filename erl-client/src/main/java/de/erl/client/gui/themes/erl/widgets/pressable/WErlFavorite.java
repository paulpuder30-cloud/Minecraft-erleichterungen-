/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.themes.erl.widgets.pressable;

import de.erl.client.gui.themes.erl.ErlWidget;
import de.erl.client.gui.widgets.pressable.WFavorite;
import de.erl.client.utils.render.color.Color;

public class WErlFavorite extends WFavorite implements ErlWidget {
    public WErlFavorite(boolean checked) {
        super(checked);
    }

    @Override
    protected Color getColor() {
        return theme().favoriteColor.get();
    }
}
