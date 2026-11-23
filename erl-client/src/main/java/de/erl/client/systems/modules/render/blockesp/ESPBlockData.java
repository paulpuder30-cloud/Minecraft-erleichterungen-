/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.render.blockesp;

import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.WidgetScreen;
import de.erl.client.renderer.ShapeMode;
import de.erl.client.settings.BlockDataSetting;
import de.erl.client.settings.GenericSetting;
import de.erl.client.settings.IBlockData;
import de.erl.client.settings.IGeneric;
import de.erl.client.utils.misc.IChangeable;
import de.erl.client.utils.render.color.SettingColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

public class ESPBlockData implements IGeneric<ESPBlockData>, IChangeable, IBlockData<ESPBlockData> {
    public ShapeMode shapeMode;
    public SettingColor lineColor;
    public SettingColor sideColor;

    public boolean tracer;
    public SettingColor tracerColor;

    private boolean changed;

    public ESPBlockData(ShapeMode shapeMode, SettingColor lineColor, SettingColor sideColor, boolean tracer, SettingColor tracerColor) {
        this.shapeMode = shapeMode;
        this.lineColor = lineColor;
        this.sideColor = sideColor;

        this.tracer = tracer;
        this.tracerColor = tracerColor;
    }

    @Override
    public WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<ESPBlockData> setting) {
        return new ESPBlockDataScreen(theme, this, block, setting);
    }

    @Override
    public WidgetScreen createScreen(GuiTheme theme, GenericSetting<ESPBlockData> setting) {
        return new ESPBlockDataScreen(theme, this, setting);
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    public void changed() {
        changed = true;
    }

    public void tickRainbow() {
        lineColor.update();
        sideColor.update();
        tracerColor.update();
    }

    @Override
    public ESPBlockData set(ESPBlockData value) {
        shapeMode = value.shapeMode;
        lineColor.set(value.lineColor);
        sideColor.set(value.sideColor);

        tracer = value.tracer;
        tracerColor.set(value.tracerColor);

        changed = value.changed;

        return this;
    }

    @Override
    public ESPBlockData copy() {
        return new ESPBlockData(shapeMode, new SettingColor(lineColor), new SettingColor(sideColor), tracer, new SettingColor(tracerColor));
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("shapeMode", shapeMode.name());
        tag.put("lineColor", lineColor.toTag());
        tag.put("sideColor", sideColor.toTag());

        tag.putBoolean("tracer", tracer);
        tag.put("tracerColor", tracerColor.toTag());

        tag.putBoolean("changed", changed);

        return tag;
    }

    @Override
    public ESPBlockData fromTag(CompoundTag tag) {
        shapeMode = ShapeMode.valueOf(tag.getStringOr("shapeMode", ""));
        lineColor.fromTag(tag.getCompoundOrEmpty("lineColor"));
        sideColor.fromTag(tag.getCompoundOrEmpty("sideColor"));

        tracer = tag.getBooleanOr("tracer", false);
        tracerColor.fromTag(tag.getCompoundOrEmpty("tracerColor"));

        changed = tag.getBooleanOr("changed", false);

        return this;
    }
}
