/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.tabs;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.utils.Cell;
import de.erl.client.gui.widgets.WWidget;
import de.erl.client.gui.widgets.containers.WWindow;

public abstract class WindowTabScreen extends TabScreen {
    protected final WWindow window;

    public WindowTabScreen(GuiTheme theme, Tab tab) {
        super(theme, tab);

        window = super.add(theme.window(tab.name)).center().widget();
    }

    @Override
    public <W extends WWidget> Cell<W> add(W widget) {
        return window.add(widget);
    }

    @Override
    public void clear() {
        window.clear();
    }
}
