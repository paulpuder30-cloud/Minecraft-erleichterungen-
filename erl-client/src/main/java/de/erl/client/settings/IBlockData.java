/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.settings;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.WidgetScreen;
import de.erl.client.utils.misc.IChangeable;
import de.erl.client.utils.misc.ICopyable;
import de.erl.client.utils.misc.ISerializable;
import net.minecraft.world.level.block.Block;

public interface IBlockData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> {
    WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<T> setting);
}
