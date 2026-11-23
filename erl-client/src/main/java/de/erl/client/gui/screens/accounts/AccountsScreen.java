/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.screens.accounts;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.WindowScreen;
import de.erl.client.gui.widgets.WAccount;
import de.erl.client.gui.widgets.containers.WContainer;
import de.erl.client.gui.widgets.containers.WHorizontalList;
import de.erl.client.gui.widgets.pressable.WButton;
import de.erl.client.systems.accounts.Account;
import de.erl.client.systems.accounts.AccountType;
import de.erl.client.systems.accounts.Accounts;
import de.erl.client.utils.misc.NbtUtils;
import de.erl.client.utils.network.ErlExecutor;
import org.jspecify.annotations.Nullable;

import static de.erl.client.ErlClient.mc;

public class AccountsScreen extends WindowScreen {
    public AccountsScreen(GuiTheme theme) {
        super(theme, "Accounts");
    }

    @Override
    public void initWidgets() {
        // Accounts
        for (Account<?> account : Accounts.get()) {
            WAccount wAccount = add(theme.account(this, account)).expandX().widget();
            wAccount.refreshScreenAction = this::reload;
        }

        // Add account
        WHorizontalList l = add(theme.horizontalList()).expandX().widget();

        addButton(l, "Cracked", () -> mc.gui.setScreen(new AddCrackedAccountScreen(theme, this)));
        addButton(l, "Altening", () -> mc.gui.setScreen(new AddAlteningAccountScreen(theme, this)));
        addButton(l, "Session", () -> mc.gui.setScreen(new AddSessionAccountScreen(theme, this)));
        addButton(l, "Microsoft", () -> mc.gui.setScreen(new AddMicrosoftAccountScreen(theme, this)));
    }

    private void addButton(WContainer c, String text, Runnable action) {
        WButton button = c.add(theme.button(text)).expandX().widget();
        button.action = action;
    }

    public static void addAccount(@Nullable AddAccountScreen screen, AccountsScreen parent, Account<?> account) {
        if (screen != null) screen.locked = true;

        ErlExecutor.execute(() -> {
            if (!account.fetchInfo()) {
                mc.execute(() -> {
                    if (screen != null) screen.locked = false;
                });
                return;
            }

            Accounts.get().add(account);

            if (account.login()) {
                if (account.getType() != AccountType.Cracked) account.getCache().loadHead(parent::reload);
                Accounts.get().save();
            }

            mc.execute(() -> {
                if (screen != null) {
                    screen.locked = false;
                    screen.onClose();
                }

                parent.reload();
            });
        });
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Accounts.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Accounts.get());
    }
}
