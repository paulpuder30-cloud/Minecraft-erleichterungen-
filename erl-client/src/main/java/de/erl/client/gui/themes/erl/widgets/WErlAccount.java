/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.themes.erl.widgets;

import de.erl.client.gui.WidgetScreen;
import de.erl.client.gui.themes.erl.ErlWidget;
import de.erl.client.gui.widgets.WAccount;
import de.erl.client.systems.accounts.Account;
import de.erl.client.utils.render.color.Color;

public class WErlAccount extends WAccount implements ErlWidget {
    public WErlAccount(WidgetScreen screen, Account<?> account) {
        super(screen, account);
    }

    @Override
    protected Color loggedInColor() {
        return theme().loggedInColor.get();
    }

    @Override
    protected Color accountTypeColor() {
        return theme().textSecondaryColor.get();
    }
}
