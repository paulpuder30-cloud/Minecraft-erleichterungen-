/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.screens.settings;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.screens.settings.base.CollectionListSettingScreen;
import de.erl.client.gui.widgets.WWidget;
import de.erl.client.settings.ItemListSetting;
import de.erl.client.utils.misc.Names;
import de.erl.client.utils.render.DisplayItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Predicate;

public class ItemListSettingScreen extends CollectionListSettingScreen<Item> {
    public ItemListSettingScreen(GuiTheme theme, ItemListSetting setting) {
        super(theme, "Select Items", setting, setting.get(), BuiltInRegistries.ITEM);
    }

    @Override
    protected boolean includeValue(Item value) {
        Predicate<Item> filter = ((ItemListSetting) setting).filter;
        if (filter != null && !filter.test(value)) return false;

        return value != Items.AIR;
    }

    @Override
    protected WWidget getValueWidget(Item value) {
        return theme.itemWithLabel(DisplayItemUtils.toStack(value));
    }

    @Override
    protected String[] getValueNames(Item value) {
        return new String[]{
            Names.get(value),
            BuiltInRegistries.ITEM.getKey(value).toString()
        };
    }
}
