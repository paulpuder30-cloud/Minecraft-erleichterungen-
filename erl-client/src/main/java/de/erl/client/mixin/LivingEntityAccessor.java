/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("jumpInLiquid")
    void erl$swimUpwards(TagKey<Fluid> fluid);

    @Accessor("jumping")
    boolean erl$isJumping();

    @Accessor("noJumpDelay")
    int erl$getJumpCooldown();

    @Accessor("noJumpDelay")
    void erl$setJumpCooldown(int cooldown);
}
