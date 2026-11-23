/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.erl.client.systems.modules.Modules;
import de.erl.client.systems.modules.render.BetterTooltips;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CreativeModeTabs.class)
public abstract class CreativeModeTabsMixin {
    @ModifyReturnValue(method = "tryRebuildTabContents", at = @At("RETURN"))
    private static boolean modifyReturn(boolean original) {
        return original || Modules.get().get(BetterTooltips.class).updateTooltips();
    }
}
