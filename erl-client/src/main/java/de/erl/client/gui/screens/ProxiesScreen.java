/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.gui.screens;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.WindowScreen;
import de.erl.client.gui.renderer.GuiRenderer;
import de.erl.client.gui.widgets.WLabel;
import de.erl.client.gui.widgets.containers.WContainer;
import de.erl.client.gui.widgets.containers.WHorizontalList;
import de.erl.client.gui.widgets.containers.WTable;
import de.erl.client.gui.widgets.pressable.WButton;
import de.erl.client.gui.widgets.pressable.WCheckbox;
import de.erl.client.gui.widgets.pressable.WConfirmedButton;
import de.erl.client.gui.widgets.pressable.WMinus;
import de.erl.client.settings.Settings;
import de.erl.client.systems.proxies.Proxies;
import de.erl.client.systems.proxies.Proxy;
import de.erl.client.utils.misc.FileDialogs;
import de.erl.client.utils.misc.NbtUtils;
import de.erl.client.utils.network.ErlExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.erl.client.ErlClient.mc;

public class ProxiesScreen extends WindowScreen {
    private final List<WCheckbox> checkboxes = new ArrayList<>();
    private final WButton refreshButton = theme.button("Refresh");
    private final WConfirmedButton cleanButton = theme.confirmedButton("Cleanup", "Confirm");
    private Map<Proxy, WLabel> statuses = new HashMap<>();
    private int timer = 0;

    public ProxiesScreen(GuiTheme theme) {
        super(theme, "Proxies");
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().minWidth(400).widget();
        initTable(table);

        add(theme.horizontalSeparator()).expandX();

        WHorizontalList l = add(theme.horizontalList()).expandX().widget();

        // New
        WButton newBtn = l.add(theme.button("New")).expandX().widget();
        newBtn.action = () -> mc.gui.setScreen(new EditProxyScreen(theme, null, this::reload));

        // Import
        WButton importBtn = l.add(theme.button("Import")).expandX().widget();
        importBtn.action = () -> FileDialogs.openFile("txt", file -> mc.gui.setScreen(new ProxiesImportScreen(theme, file)));

        l.add(refreshButton).expandX();
        refreshButton.action = () -> Proxies.get().checkProxies(true);

        l.add(cleanButton).expandX();
        cleanButton.action = () -> {
            if (Proxies.get().refreshing) return;
            Proxies.get().clean();
            initTable(table);
        };

        WButton configButton = l.add(theme.button(GuiRenderer.EDIT)).widget();
        configButton.action = () -> mc.gui.setScreen(new ConfigScreen(theme));
        configButton.tooltip = "Proxies Config";
    }

    private void initTable(WTable table) {
        table.clear();
        if (Proxies.get().isEmpty()) return;

        statuses = new HashMap<>(Proxies.get().size(), 1);
        for (Proxy proxy : Proxies.get()) {
            WCheckbox enabled = table.add(theme.checkbox(proxy.enabled.get())).widget();
            checkboxes.add(enabled);
            enabled.action = () -> {
                boolean checked = enabled.checked;
                Proxies.get().setEnabled(proxy, checked);

                for (WCheckbox checkbox : checkboxes) checkbox.checked = false;
                enabled.checked = checked;
            };

            WLabel name = table.add(theme.label(proxy.name.get())).widget();
            name.color = theme.textColor();

            WLabel type = table.add(theme.label("(" + proxy.type.get() + ")")).widget();
            type.color = theme.textSecondaryColor();

            WHorizontalList ipList = table.add(theme.horizontalList()).expandCellX().widget();
            ipList.spacing = 0;

            ipList.add(theme.label(proxy.address.get()));
            ipList.add(theme.label(":")).widget().color = theme.textSecondaryColor();
            ipList.add(theme.label(Integer.toString(proxy.port.get())));

            String s = (proxy.status == Proxy.Status.ALIVE ? proxy.latency + "ms" : proxy.status.toString());
            WLabel status = table.add(theme.label(s)).widget();
            status.color = proxy.status.getColor();
            statuses.put(proxy, status);

            WButton refresh = table.add(theme.button(GuiRenderer.RESET)).widget();
            refresh.action = () -> ErlExecutor.execute(proxy::checkStatus);
            refresh.tooltip = "Refresh";

            WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> mc.gui.setScreen(new EditProxyScreen(theme, proxy, this::reload));

            WMinus remove = table.add(theme.minus()).widget();
            remove.action = () -> {
                Proxies.get().remove(proxy);
                reload();
            };

            table.row();
        }
    }

    @Override
    public void tick() {
        if (Proxies.get().refreshing) {
            if (cleanButton.getText().equals("Cleanup")) {
                cleanButton.set("---", "---");
            }
            if (timer > 2) {
                refreshButton.set(getNext(refreshButton));
                timer = 0;
            } else timer++;
        } else {
            if (!refreshButton.getText().equals("Refresh")) {
                refreshButton.set("Refresh");
            }
            if (!cleanButton.getText().equals("Cleanup")) {
                cleanButton.set("Cleanup", "Confirm");
            }
        }

        for (var entry : statuses.entrySet()) {
            Proxy proxy = entry.getKey();
            WLabel label = entry.getValue();

            // only update them when there is a change in status
            if (label.get().equals(proxy.status.toString())) continue;

            label.set(proxy.status == Proxy.Status.ALIVE ? proxy.latency + "ms" : proxy.status.toString());
            label.color = proxy.status.getColor();
        }
    }

    private String getNext(WButton b) {
        return switch (b.getText()) {
            case "Refresh", "oo0" -> "ooo";
            case "ooo" -> "0oo";
            case "0oo" -> "o0o";
            case "o0o" -> "oo0";
            default -> "Refresh";
        };
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Proxies.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Proxies.get());
    }

    protected static class EditProxyScreen extends EditSystemScreen<Proxy> {
        public EditProxyScreen(GuiTheme theme, Proxy value, Runnable reload) {
            super(theme, value, reload);
        }

        @Override
        public Proxy create() {
            return new Proxy.Builder().build();
        }

        @Override
        public boolean save() {
            ErlExecutor.execute(value::checkStatus);
            return value.resolveAddress() && (!isNew || Proxies.get().add(value));
        }

        @Override
        public Settings getSettings() {
            return value.settings;
        }
    }

    private static class ConfigScreen extends WindowScreen {
        private WContainer settingsContainer;

        public ConfigScreen(GuiTheme theme) {
            super(theme, "Proxies Config");
        }

        @Override
        public void initWidgets() {
            settingsContainer = add(theme.verticalList()).expandX().minWidth(400).widget();
            settingsContainer.add(theme.settings(Proxies.get().settings)).expandX();
        }

        @Override
        public void tick() {
            Proxies.get().settings.tick(settingsContainer, theme);
        }
    }
}
