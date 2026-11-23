/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.player;

import de.erl.client.events.entity.player.SendMovementPacketsEvent;
import de.erl.client.events.packets.PacketEvent;
import de.erl.client.mixin.ServerboundMovePlayerPacketAccessor;
import de.erl.client.settings.BoolSetting;
import de.erl.client.settings.Setting;
import de.erl.client.settings.SettingGroup;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Module;
import de.erl.client.lib.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public class AntiHunger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Spoofs sprinting packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onGround = sgGeneral.add(new BoolSetting.Builder()
        .name("on-ground")
        .description("Spoofs the onGround flag.")
        .defaultValue(true)
        .build()
    );

    private boolean lastOnGround, ignorePacket;

    public AntiHunger() {
        super(Categories.Player, "anti-hunger", "Reduces (does NOT remove) hunger consumption.");
    }

    @Override
    public void onActivate() {
        lastOnGround = mc.player.onGround();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (ignorePacket && event.packet instanceof ServerboundMovePlayerPacket) {
            ignorePacket = false;
            return;
        }

        if (mc.player.isPassenger() || mc.player.isInWater() || mc.player.isUnderWater()) return;

        if (event.packet instanceof ServerboundPlayerCommandPacket packet && sprint.get()) {
            if (packet.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) event.cancel();
        }

        if (event.packet instanceof ServerboundMovePlayerPacket packet && onGround.get() && mc.player.onGround() && mc.player.fallDistance <= 0.0 && !mc.gameMode.isDestroying()) {
            ((ServerboundMovePlayerPacketAccessor) packet).erl$setOnGround(false);
        }
    }

    @EventHandler
    private void onTick(SendMovementPacketsEvent.Pre event) {
        if (mc.player.onGround() && !lastOnGround && onGround.get()) {
            ignorePacket = true; // prevents you from not taking fall damage
        }

        lastOnGround = mc.player.onGround();
    }
}
