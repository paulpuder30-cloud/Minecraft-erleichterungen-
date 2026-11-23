/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.widgets;

import de.erl.client.ErlClient;
import de.erl.client.gui.renderer.GuiRenderer;
import de.erl.client.gui.tabs.Tab;
import de.erl.client.gui.tabs.TabScreen;
import de.erl.client.gui.tabs.Tabs;
import de.erl.client.gui.widgets.containers.WHorizontalList;
import de.erl.client.gui.widgets.pressable.WPressable;
import de.erl.client.utils.render.color.Color;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.sdl.SDLMouse;

import static de.erl.client.ErlClient.mc;

public abstract class WTopBar extends WHorizontalList {
    protected abstract Color getButtonColor(boolean pressed, boolean hovered);

    protected abstract Color getNameColor();

    /** Farbe des "ERL-Client"-Titels links in der Leiste. */
    protected Color getTitleColor() {
        return getNameColor();
    }

    public WTopBar() {
        spacing = 0;
    }

    @Override
    public void init() {
        add(new WTopBarTitle());

        for (Tab tab : Tabs.get()) {
            add(new WTopBarButton(tab));
        }
    }

    protected class WTopBarTitle extends WWidget {
        @Override
        protected void onCalculateSize() {
            double pad = pad();

            width = pad + theme.textWidth(ErlClient.NAME) + pad * 2;
            height = pad + theme.textHeight() + pad;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double pad = pad();

            renderer.quad(x, y, width, height, getButtonColor(false, false));
            renderer.text(ErlClient.NAME, x + pad, y + pad, getTitleColor(), true);
        }
    }

    protected class WTopBarButton extends WPressable {
        private final Tab tab;

        public WTopBarButton(Tab tab) {
            this.tab = tab;
        }

        @Override
        protected void onCalculateSize() {
            double pad = pad();

            width = pad + theme.textWidth(tab.name) + pad;
            height = pad + theme.textHeight() + pad;
        }

        @Override
        protected void onPressed(int button) {
            Screen screen = mc.gui.screen();

            if (!(screen instanceof TabScreen tabScreen) || tabScreen.tab != tab) {
                double mouseX = mc.mouseHandler.xpos();
                double mouseY = mc.mouseHandler.ypos();

                tab.openScreen(theme);
                SDLMouse.SDL_WarpMouseInWindow(mc.getWindow().handle(), (float) mouseX, (float) mouseY);
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double pad = pad();
            Color color = getButtonColor(pressed || (mc.gui.screen() instanceof TabScreen tabScreen && tabScreen.tab == tab), mouseOver);

            renderer.quad(x, y, width, height, color);
            renderer.text(tab.name, x + pad, y + pad, getNameColor(), false);
        }
    }
}
