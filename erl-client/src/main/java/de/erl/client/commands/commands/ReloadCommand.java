/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erl.client.commands.Command;
import de.erl.client.renderer.Fonts;
import de.erl.client.systems.Systems;
import de.erl.client.systems.friends.Friend;
import de.erl.client.systems.friends.Friends;
import de.erl.client.utils.network.Capes;
import de.erl.client.utils.network.ErlExecutor;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super("reload", "Reloads many systems.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            warning("Reloading systems, this may take a while.");

            Systems.load();
            Capes.init();
            Fonts.refresh();
            ErlExecutor.execute(() -> Friends.get().forEach(Friend::updateInfo));

            return SINGLE_SUCCESS;
        });
    }
}
