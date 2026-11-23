/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.modules.misc;

import de.erl.client.events.entity.EntityAddedEvent;
import de.erl.client.settings.BoolSetting;
import de.erl.client.settings.Setting;
import de.erl.client.settings.SettingGroup;
import de.erl.client.settings.StringSetting;
import de.erl.client.systems.friends.Friends;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Module;
import de.erl.client.utils.player.ChatUtils;
import de.erl.client.lib.orbit.EventHandler;
import net.minecraft.world.entity.player.Player;

public class MessageAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("The specified message sent to the player.")
        .defaultValue("ERL-Client on top!")
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Will not send any messages to people friended.")
        .defaultValue(false)
        .build()
    );

    public MessageAura() {
        super(Categories.Misc, "message-aura", "Sends a specified message to any player that enters render distance.");
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof Player) || event.entity.getUUID().equals(mc.player.getUUID())) return;

        if (!ignoreFriends.get() || (ignoreFriends.get() && !Friends.get().isFriend((Player) event.entity))) {
            ChatUtils.sendPlayerMsg("/msg " + event.entity.getName().getString() + " " + message.get());
        }
    }
}
