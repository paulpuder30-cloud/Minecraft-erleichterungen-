/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.renderer.text;

import de.erl.client.renderer.Fonts;
import de.erl.client.systems.config.Config;
import de.erl.client.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface TextRenderer {
    static TextRenderer get() {
        return Config.get().customFont.get() ? Fonts.RENDERER : VanillaTextRenderer.INSTANCE;
    }

    void setAlpha(double a);

    void begin(GuiGraphicsExtractor graphics, double scale, boolean scaleOnly, boolean big);
    default void begin(GuiGraphicsExtractor graphics, double scale) { begin(graphics, scale, false, false); }
    default void begin(GuiGraphicsExtractor graphics) { begin(graphics, 1, false, false); }

    default void beginBig(GuiGraphicsExtractor graphics) { begin(graphics, 1, false, true); }

    double getWidth(String text, int length, boolean shadow);
    default double getWidth(String text, boolean shadow) { return getWidth(text, text.length(), shadow); }
    default double getWidth(String text) { return getWidth(text, text.length(), false); }

    double getHeight(boolean shadow);
    default double getHeight() { return getHeight(false); }

    double render(String text, double x, double y, Color color, boolean shadow);
    default double render(String text, double x, double y, Color color) { return render(text, x, y, color, false); }

    boolean isBuilding();

    void end();
}
