/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.themes.erl.widgets.pressable;

import de.erl.client.gui.renderer.GuiRenderer;
import de.erl.client.gui.themes.erl.ErlGuiTheme;
import de.erl.client.gui.themes.erl.ErlWidget;
import de.erl.client.gui.widgets.pressable.WPlus;

public class WErlPlus extends WPlus implements ErlWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        ErlGuiTheme theme = theme();
        double pad = pad();
        double s = theme.scale(3);

        renderBackground(renderer, this, pressed, mouseOver);
        renderer.quad(x + pad, y + height / 2 - s / 2, width - pad * 2, s, theme.plusColor.get());
        renderer.quad(x + width / 2 - s / 2, y + pad, s, height - pad * 2, theme.plusColor.get());
    }
}
