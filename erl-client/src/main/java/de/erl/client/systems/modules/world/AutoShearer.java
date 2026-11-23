/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.world;

import de.erl.client.events.world.TickEvent;
import de.erl.client.settings.BoolSetting;
import de.erl.client.settings.DoubleSetting;
import de.erl.client.settings.Setting;
import de.erl.client.settings.SettingGroup;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Module;
import de.erl.client.utils.player.FindItemResult;
import de.erl.client.utils.player.InvUtils;
import de.erl.client.utils.player.PlayerUtils;
import de.erl.client.utils.player.Rotations;
import de.erl.client.lib.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

public class AutoShearer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The maximum distance the sheep have to be to be sheared.")
        .min(0.0)
        .defaultValue(5.0)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Prevents shears from being broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the animal being sheared.")
        .defaultValue(true)
        .build()
    );

    private Entity entity;
    private InteractionHand hand;

    public AutoShearer() {
        super(Categories.World, "auto-shearer", "Automatically shears sheep.");
    }

    @Override
    public void onDeactivate() {
        entity = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        entity = null;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Sheep sheep) || sheep.isSheared() || sheep.isBaby() || !PlayerUtils.isWithin(entity, distance.get()))
                continue;

            FindItemResult findShear = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.SHEARS && (!antiBreak.get() || itemStack.getDamageValue() < itemStack.getMaxDamage() - 1));
            if (!InvUtils.swap(findShear.slot(), true)) return;

            this.hand = findShear.getHand();
            this.entity = entity;

            if (rotate.get())
                Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, this::interact);
            else interact();

            return;
        }
    }

    private void interact() {
        EntityHitResult location = new EntityHitResult(entity, entity.getBoundingBox().getCenter());
        mc.gameMode.interact(mc.player, entity, location, hand);
        InvUtils.swapBack();
    }
}
