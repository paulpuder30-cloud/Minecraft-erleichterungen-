/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.screens.settings;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.screens.settings.base.CollectionListSettingScreen;
import de.erl.client.gui.widgets.WWidget;
import de.erl.client.settings.PacketListSetting;
import de.erl.client.settings.Setting;
import de.erl.client.utils.network.PacketUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.function.Predicate;

public class PacketBoolSettingScreen extends CollectionListSettingScreen<PacketType<? extends @NonNull Packet<?>>> {
    public PacketBoolSettingScreen(GuiTheme theme, Setting<Set<PacketType<? extends @NonNull Packet<?>>>> setting) {
        super(theme, "Select Packets", setting, setting.get(), PacketUtils.getPackets());
    }

    @Override
    protected boolean includeValue(PacketType<? extends @NonNull Packet<?>> value) {
        Predicate<PacketType<? extends @NonNull Packet<?>>> filter = ((PacketListSetting) setting).filter;

        if (filter == null) return true;
        return filter.test(value);
    }

    @Override
    protected WWidget getValueWidget(PacketType<? extends @NonNull Packet<?>> value) {
        return theme.label(value.toString());
    }

    @Override
    protected String[] getValueNames(PacketType<? extends @NonNull Packet<?>> value) {
        return new String[]{
            value.toString()
        };
    }
}
