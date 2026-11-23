/*
 * This file is part of ERL-Client, a fork of Meteor Client (https://github.com/MeteorDevelopment/meteor-client).
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.utils;

import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.SwingMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.ToIntFunction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Gemeinsame Kampf-Hilfsfunktionen der ERL-Module.
 *
 * <p>Die Logik bildet die Vanilla-Mechanik von Minecraft 26.3 nach:
 * <ul>
 *     <li>{@code LivingEntity#getItemBlockingWith}: geblockt wird erst nach {@code BlocksAttacks#blockDelayTicks}.</li>
 *     <li>{@code LivingEntity#applyItemBlocking}: ein Schild blockt nur Treffer innerhalb seines
 *     {@code horizontal_blocking_angle} (Schild: 90°) um die Kopfrichtung des Verteidigers.</li>
 *     <li>{@code Player#blockUsingItem}: nur <b>Spieler</b> verlieren ihren Schild, und zwar wenn die Waffe des
 *     Angreifers eine {@code minecraft:weapon}-Komponente mit {@code disable_blocking_for_seconds > 0} hat (Äxte: 5 s).</li>
 * </ul>
 */
public final class ErlCombatUtils {
    /** Reihenfolge Netherite > Diamant > Eisen > Kupfer > Stein > Gold > Holz. */
    private static final List<Item> AXE_RANKING = List.of(
        Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.COPPER_AXE,
        Items.STONE_AXE, Items.GOLDEN_AXE, Items.WOODEN_AXE
    );

    private ErlCombatUtils() {
    }

    // Schild

    /** {@code true}, wenn dieses Item beim Treffen den Schild eines Spielers deaktiviert (z. B. jede Axt). */
    public static boolean disablesShields(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Weapon weapon = stack.get(DataComponents.WEAPON);
        return weapon != null && weapon.disableBlockingForSeconds() > 0;
    }

    /**
     * {@code true}, wenn {@code target} gerade aktiv blockt <b>und</b> der Schild zur Position des lokalen Spielers zeigt,
     * ein Treffer also tatsächlich geblockt würde.
     */
    public static boolean isBlockingTowardsUs(LivingEntity target) {
        ItemStack blockingWith = target.getItemBlockingWith();
        if (blockingWith == null) return false;

        BlocksAttacks blocksAttacks = blockingWith.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null) return false;

        double maxAngleDeg = 0;
        for (BlocksAttacks.DamageReduction reduction : blocksAttacks.damageReductions()) {
            maxAngleDeg = Math.max(maxAngleDeg, reduction.horizontalBlockingAngle());
        }
        if (maxAngleDeg <= 0) return false;

        // Gleiche Rechnung wie LivingEntity#applyItemBlocking
        double yawRad = Math.toRadians(target.getYHeadRot());
        Vec3 view = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 toAttacker = mc.player.position().subtract(target.position());
        toAttacker = new Vec3(toAttacker.x, 0, toAttacker.z);
        if (toAttacker.lengthSqr() < 1.0E-7) return true;

        double angle = Math.acos(Math.max(-1, Math.min(1, toAttacker.normalize().dot(view))));
        return angle <= Math.toRadians(maxAngleDeg);
    }

    /**
     * Sucht in der Hotbar ein Item, das Schilde deaktiviert.
     *
     * @return Hotbar-Slot (0-8) oder -1
     */
    public static int findShieldDisabler(AxePriority priority, boolean antiBreak) {
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!disablesShields(stack)) continue;
            if (antiBreak && isAboutToBreak(stack)) continue;

            int score = switch (priority) {
                case Best -> axeRank(stack);
                case Worst -> -axeRank(stack);
                case FirstSlot -> -i;
            };

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    /** Höherer Wert = besseres Material. Unbekannte Items (z. B. aus Mods) landen ganz unten. */
    private static int axeRank(ItemStack stack) {
        int idx = AXE_RANKING.indexOf(stack.getItem());
        return idx == -1 ? 0 : AXE_RANKING.size() - idx;
    }

    public static boolean isAboutToBreak(ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 10;
    }

    /**
     * Sucht in der Hotbar den Slot mit dem höchsten bzw. niedrigsten Wert laut {@code score}.
     * Einträge mit Wert {@code < 0} werden ignoriert.
     */
    public static int findHotbarSlot(ToIntFunction<ItemStack> score, boolean preferHighest) {
        int bestSlot = -1;
        int bestScore = 0;

        for (int i = 0; i < 9; i++) {
            int s = score.applyAsInt(mc.player.getInventory().getItem(i));
            if (s < 0) continue;

            if (bestSlot == -1 || (preferHighest ? s > bestScore : s < bestScore)) {
                bestSlot = i;
                bestScore = s;
            }
        }

        return bestSlot;
    }

    public static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return Utils.getEnchantmentLevel(stack, enchantment);
    }

    // Ziele

    /** Abstand der Augen zum nächsten Punkt der Hitbox – so prüft auch der Server die Angriffsreichweite. */
    public static boolean isInRange(Entity entity, double range) {
        Vec3 eyes = mc.player.getEyePosition();
        AABB box = entity.getBoundingBox();
        double x = Math.clamp(eyes.x, box.minX, box.maxX);
        double y = Math.clamp(eyes.y, box.minY, box.maxY);
        double z = Math.clamp(eyes.z, box.minZ, box.maxZ);
        return eyes.distanceToSqr(x, y, z) <= range * range;
    }

    /** Grundlegende Ziel-Prüfung: lebend, angreifbar, kein Freund, nicht man selbst. */
    public static boolean isValidTarget(Entity entity, boolean onlyPlayers) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (entity == mc.player || entity == mc.getCameraEntity()) return false;
        if (!living.isAlive() || living.isDeadOrDying() || !entity.isAttackable()) return false;

        if (entity instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) return false;
            return Friends.get().shouldAttack(player);
        }

        return !onlyPlayers;
    }

    // Angriff

    /** Normaler Nahkampftreffer wie bei einem Linksklick (Paket + Schwung). */
    public static void attack(Entity target) {
        mc.gameMode.attack(mc.player, target);
        SwingMode.Both.attackSwing(InteractionHand.MAIN_HAND);
    }

    public enum AxePriority {
        /** Netherite > Diamant > Eisen > Kupfer > Stein > Gold > Holz */
        Best,
        /** Holz > Gold > Stein > … – schont die wertvollen Äxte */
        Worst,
        /** Erste Axt in der Hotbar */
        FirstSlot
    }
}
