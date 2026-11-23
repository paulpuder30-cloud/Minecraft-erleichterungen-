/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.addons;

import de.erl.client.ErlClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.util.ArrayList;
import java.util.List;

public class AddonManager {
    public static final List<ErlAddon> ADDONS = new ArrayList<>();

    public static void init() {
        // ERL-Client pseudo addon
        {
            ErlClient.ADDON = new ErlAddon() {
                @Override
                public void onInitialize() {}

                @Override
                public String getPackage() {
                    return "de.erl.client";
                }

                @Override
                public String getWebsite() {
                    return "https://github.com/paulpuder30-cloud/Minecraft-erleichterungen-";
                }

                @Override
                public GithubRepo getRepo() {
                    return new GithubRepo("paulpuder30-cloud", "Minecraft-erleichterungen-");
                }

                @Override
                public String getCommit() {
                    String commit = ErlClient.MOD_META.getCustomValue(ErlClient.MOD_ID + ":commit").getAsString();
                    return commit.isEmpty() ? null : commit;
                }
            };

            ModMetadata metadata = FabricLoader.getInstance().getModContainer(ErlClient.MOD_ID).get().getMetadata();

            ErlClient.ADDON.name = metadata.getName();
            ErlClient.ADDON.authors = new String[metadata.getAuthors().size()];
            if (metadata.containsCustomValue(ErlClient.MOD_ID + ":color")) {
                ErlClient.ADDON.color.parse(metadata.getCustomValue(ErlClient.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                ErlClient.ADDON.authors[i++] = author.getName();
            }

            ADDONS.add(ErlClient.ADDON);
        }

        // Addons
        for (EntrypointContainer<ErlAddon> entrypoint : FabricLoader.getInstance().getEntrypointContainers("erl", ErlAddon.class)) {
            ModMetadata metadata = entrypoint.getProvider().getMetadata();
            ErlAddon addon;
            try {
                addon = entrypoint.getEntrypoint();
            } catch (Throwable throwable) {
                throw new RuntimeException("Exception during addon init \"%s\".".formatted(metadata.getName()), throwable);
            }

            addon.name = metadata.getName();

            if (metadata.getAuthors().isEmpty()) throw new RuntimeException("Addon \"%s\" requires at least 1 author to be defined in it's fabric.mod.json. See https://fabricmc.net/wiki/documentation:fabric_mod_json_spec".formatted(addon.name));
            addon.authors = new String[metadata.getAuthors().size()];

            if (metadata.containsCustomValue(ErlClient.ADDON_META_NAMESPACE + ":color")) {
                addon.color.parse(metadata.getCustomValue(ErlClient.ADDON_META_NAMESPACE + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                addon.authors[i++] = author.getName();
            }

            ADDONS.add(addon);
        }
    }
}
