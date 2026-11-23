/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.screens.settings;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.screens.settings.base.CollectionListSettingScreen;
import de.erl.client.gui.widgets.WWidget;
import de.erl.client.settings.Setting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

import java.util.List;

public class ScreenHandlerSettingScreen extends CollectionListSettingScreen<MenuType<?>> {
    public ScreenHandlerSettingScreen(GuiTheme theme, Setting<List<MenuType<?>>> setting) {
        super(theme, "Select Screen Handlers", setting, setting.get(), BuiltInRegistries.MENU);
    }

    @Override
    protected WWidget getValueWidget(MenuType<?> value) {
        return theme.label(getName(value));
    }

    @Override
    protected String[] getValueNames(MenuType<?> type) {
        return new String[]{
            getName(type)
        };
    }

    private static String getName(MenuType<?> type) {
        return BuiltInRegistries.MENU.getKey(type).toString();
    }
}
