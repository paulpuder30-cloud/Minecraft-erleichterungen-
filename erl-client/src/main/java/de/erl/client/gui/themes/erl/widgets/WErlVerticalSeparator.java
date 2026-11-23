/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.themes.erl.widgets;

import de.erl.client.gui.renderer.GuiRenderer;
import de.erl.client.gui.themes.erl.ErlGuiTheme;
import de.erl.client.gui.themes.erl.ErlWidget;
import de.erl.client.gui.widgets.WVerticalSeparator;
import de.erl.client.utils.render.color.Color;

public class WErlVerticalSeparator extends WVerticalSeparator implements ErlWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        ErlGuiTheme theme = theme();
        Color colorEdges = theme.separatorEdges.get();
        Color colorCenter = theme.separatorCenter.get();

        double s = theme.scale(1);
        double offsetX = Math.round(width / 2.0);

        renderer.quad(x + offsetX, y, s, height / 2, colorEdges, colorEdges, colorCenter, colorCenter);
        renderer.quad(x + offsetX, y + height / 2, s, height / 2, colorCenter, colorCenter, colorEdges, colorEdges);
    }
}
