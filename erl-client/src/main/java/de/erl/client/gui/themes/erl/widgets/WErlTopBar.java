/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.themes.erl.widgets;

import de.erl.client.gui.themes.erl.ErlWidget;
import de.erl.client.gui.widgets.WTopBar;
import de.erl.client.utils.render.color.Color;

public class WErlTopBar extends WTopBar implements ErlWidget {
    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        return theme().backgroundColor.get(pressed, hovered);
    }

    @Override
    protected Color getNameColor() {
        return theme().textColor.get();
    }

    @Override
    protected Color getTitleColor() {
        return theme().accentColor.get();
    }
}
