/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.themes.erl.widgets;

import de.erl.client.gui.renderer.GuiRenderer;
import de.erl.client.gui.widgets.WQuad;
import de.erl.client.utils.render.color.Color;

public class WErlQuad extends WQuad {
    public WErlQuad(Color color) {
        super(color);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.quad(x, y, width, height, color);
    }
}
