/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.screens.settings;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.WindowScreen;
import de.erl.client.gui.widgets.containers.WTable;
import de.erl.client.gui.widgets.pressable.WButton;
import de.erl.client.settings.PotionSetting;
import de.erl.client.utils.misc.MyPotion;
import net.minecraft.client.resources.language.I18n;

public class PotionSettingScreen extends WindowScreen {
    private final PotionSetting setting;

    public PotionSettingScreen(GuiTheme theme, PotionSetting setting) {
        super(theme, "Select Potion");

        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().widget();

        for (MyPotion potion : MyPotion.values()) {
            var stack = potion.potion.get();
            table.add(theme.itemWithLabel(stack, I18n.get(stack.getItem().getDescriptionId())));

            WButton select = table.add(theme.button("Select")).widget();
            select.action = () -> {
                setting.set(potion);
                onClose();
            };

            table.row();
        }
    }
}
