/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.tabs;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.WidgetScreen;
import de.erl.client.gui.utils.Cell;
import de.erl.client.gui.widgets.WWidget;

public abstract class TabScreen extends WidgetScreen {
    public final Tab tab;

    public TabScreen(GuiTheme theme, Tab tab) {
        super(theme, tab.name);

        this.tab = tab;
    }

    public <T extends WWidget> Cell<T> addDirect(T widget) {
        return super.add(widget);
    }
}
