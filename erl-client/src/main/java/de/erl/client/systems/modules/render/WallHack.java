/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.render;

import de.erl.client.MixinPlugin;
import de.erl.client.events.world.ChunkOcclusionEvent;
import de.erl.client.gui.GuiTheme;
import de.erl.client.gui.widgets.WWidget;
import de.erl.client.settings.*;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Module;
import de.erl.client.lib.orbit.EventHandler;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class WallHack extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> opacity = sgGeneral.add(new IntSetting.Builder()
        .name("opacity")
        .description("The opacity for rendered blocks.")
        .defaultValue(0)
        .range(0, 255)
        .sliderMax(255)
        .onChanged(_ -> {
            if (this.isActive()) {
                mc.levelExtractor.allChanged();
            }
        })
        .build()
    );

    public final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("What blocks should be targeted for Wall Hack.")
        .defaultValue()
        .onChanged(_ -> {
            if (isActive()) {
                mc.levelExtractor.allChanged();
            }
        })
        .build()
    );

    public final Setting<Boolean> occludeChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("occlude-chunks")
        .description("Whether caves should occlude underground (may look wonky when on).")
        .defaultValue(false)
        .build()
    );

    public WallHack() {
        super(Categories.Render, "wall-hack", "Makes blocks translucent.");
    }

    @Override
    public void onActivate() {
        mc.levelExtractor.allChanged();
    }

    @Override
    public void onDeactivate() {
        mc.levelExtractor.allChanged();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse())
            return theme.label("Warning: Due to shaders in use, opacity is overridden to 0.");

        return null;
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        if (!occludeChunks.get()) event.cancel();
    }
}
