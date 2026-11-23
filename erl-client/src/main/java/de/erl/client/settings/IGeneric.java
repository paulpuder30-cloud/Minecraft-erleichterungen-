/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.settings;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.WidgetScreen;
import de.erl.client.utils.misc.ICopyable;
import de.erl.client.utils.misc.ISerializable;

public interface IGeneric<T extends IGeneric<T>> extends ICopyable<T>, ISerializable<T> {
    WidgetScreen createScreen(GuiTheme theme, GenericSetting<T> setting);
}
