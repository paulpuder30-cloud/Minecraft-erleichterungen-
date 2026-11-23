/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.themes.erl.widgets;

import de.erl.client.gui.renderer.GuiRenderer;
import de.erl.client.gui.themes.erl.ErlWidget;
import de.erl.client.gui.widgets.containers.WView;

public class WErlView extends WView implements ErlWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (canScroll && hasScrollBar) {
            renderer.quad(handleX(), handleY(), handleWidth(), handleHeight(), theme().scrollbarColor.get(focused, handleMouseOver));
        }
    }
}
