/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.widgets;

public class WVerticalSeparator extends WWidget {
    @Override
    protected void onCalculateSize() {
        width = theme.scale(3);
        height = 1;
    }
}
