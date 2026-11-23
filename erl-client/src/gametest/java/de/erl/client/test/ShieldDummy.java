/*
 * This file is part of ERL-Client, a fork of Meteor Client (https://github.com/MeteorDevelopment/meteor-client).
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.test;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

/**
 * Serverseitiger Test-Gegner: ein echter (verwundbarer, tickender) Spieler, der mit einem Schild blockt.
 * Fabrics {@link FakePlayer} ist unverwundbar und tickt nicht – beides wird hier zurückgedreht, damit die
 * Vanilla-Schildmechanik (Blockverzögerung, Deaktivierung durch Äxte) wie bei einem echten Gegner abläuft.
 */
public class ShieldDummy extends FakePlayer {
    public static final ItemStack SHIELD = new ItemStack(Items.SHIELD);

    private ShieldDummy(ServerLevel level, GameProfile profile) {
        super(level, profile);
    }

    /** Erzeugt den Dummy bei (x, y, z), mit Blick Richtung {@code yaw}, und macht ihn für Clients sichtbar. */
    public static ShieldDummy spawn(ServerLevel level, String name, double x, double y, double z, float yaw) {
        ShieldDummy dummy = new ShieldDummy(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        dummy.snapTo(x, y, z, yaw, 0);
        dummy.setYHeadRot(yaw);
        dummy.setYBodyRot(yaw);

        // Spieler-Entities werden auf dem Client nur angelegt, wenn vorher ein Tab-Listen-Eintrag existiert
        level.getServer().getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(dummy)));
        level.addNewPlayer(dummy);

        // Mehr Leben, damit der Mace-Smash im Test messbar bleibt und den Dummy nicht tötet
        dummy.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 20 * 60 * 5, 4));
        dummy.setHealth(dummy.getMaxHealth());

        dummy.setItemInHand(InteractionHand.OFF_HAND, SHIELD.copy());
        dummy.raiseShield();
        return dummy;
    }

    public void raiseShield() {
        startUsingItem(InteractionHand.OFF_HAND);
    }

    public boolean isShieldDisabled() {
        return getCooldowns().isOnCooldown(SHIELD);
    }

    public void despawn() {
        level().getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(getUUID())));
        remove(Entity.RemovalReason.DISCARDED);
    }

    /** Echtes Spieler-Ticken (u. a. Schild-Benutzungsdauer), das {@link FakePlayer} sonst unterdrückt. */
    @Override
    public void tick() {
        doTick();
        // Kopf immer in Blickrichtung halten
        setYHeadRot(getYRot());
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return isRemoved() || isSpectator();
    }
}
