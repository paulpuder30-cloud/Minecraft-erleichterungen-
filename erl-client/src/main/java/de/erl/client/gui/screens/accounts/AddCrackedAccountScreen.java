/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.screens.accounts;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.widgets.containers.WTable;
import de.erl.client.gui.widgets.input.WTextBox;
import de.erl.client.systems.accounts.Accounts;
import de.erl.client.systems.accounts.types.CrackedAccount;

public class AddCrackedAccountScreen extends AddAccountScreen {
    public AddCrackedAccountScreen(GuiTheme theme, AccountsScreen parent) {
        super(theme, "Add Cracked Account", parent);
    }

    @Override
    public void initWidgets() {
        WTable t = add(theme.table()).widget();

        // Name
        t.add(theme.label("Name: "));
        WTextBox name = t.add(theme.textBox("", "seasnail8169", (_, c) ->
            /// @see net.minecraft.util.StringUtil#isValidPlayerName
            c > 32 && c < 127
        )).minWidth(400).expandX().widget();
        name.setFocused(true);
        t.row();

        // Add
        add = t.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            String username = name.get().trim();
            if (username.length() > 16) return;

            CrackedAccount account = new CrackedAccount(username);
            if (!(Accounts.get().exists(account))) {
                AccountsScreen.addAccount(this, parent, account);
            }
        };

        enterAction = add.action;
    }
}
