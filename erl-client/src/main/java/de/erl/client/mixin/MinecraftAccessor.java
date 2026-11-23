/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("fps")
    static int erl$getFps() {
        return 0;
    }

    @Mutable
    @Accessor("user")
    void erl$setUser(User session);

    @Accessor("reloadStateTracker")
    ResourceLoadStateTracker erl$getReloadStateTracker();

    @Accessor("missTime")
    int erl$getMissTime();

    @Accessor("missTime")
    void erl$setMissTime(int attackCooldown);

    @Invoker("startAttack")
    boolean erl$leftClick();

    @Mutable
    @Accessor("profileKeyPairManager")
    void erl$setProfileKeyPairManager(ProfileKeyPairManager keys);

    @Mutable
    @Accessor("userApiService")
    void erl$setUserApiService(UserApiService apiService);

    @Mutable
    @Accessor("skinManager")
    void erl$setSkinManager(SkinManager skinProvider);

    @Mutable
    @Accessor("playerSocialManager")
    void erl$setPlayerSocialManager(PlayerSocialManager socialInteractionsManager);

    @Mutable
    @Accessor("reportingContext")
    void erl$setReportingContext(ReportingContext abuseReportContext);

    @Mutable
    @Accessor("profileFuture")
    void erl$setProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("services")
    void erl$setServices(Services apiServices);

    @Invoker("handleKeybinds")
    void erl$handleInputEvents();
}
