package de.erleichterungen.werkzeuge;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Hyperion mit der Fähigkeit <b>Wither Impact</b> (Rechtsklick, beliebig oft hintereinander):
 * <ol>
 *   <li><b>Teleport</b> 10 Blöcke in Blickrichtung (stoppt vor Wänden), kein Fallschaden.</li>
 *   <li><b>Implosion</b>: magischer Schaden an allen Gegnern im Umkreis von 10 Blöcken um den Landepunkt.</li>
 *   <li><b>Wither-Schild</b>: 5 Sekunden Absorption und 10 % weniger Schaden; danach wird die Hälfte der übrigen
 *   Absorption in Heilung umgewandelt. Der Schild hat 5 Sekunden Abklingzeit.</li>
 * </ol>
 */
public class HyperionItem extends Item {
	public static final double TELEPORT_DISTANCE = 10.0;
	public static final double IMPLOSION_RADIUS = 10.0;
	public static final float IMPLOSION_DAMAGE = 50.0f;
	/** Absorption-Effekt Stufe 3 = 6 Herzen. */
	private static final int SHIELD_AMPLIFIER = 2;
	private static final int SHIELD_TICKS = 100;
	public static final float SHIELD_DAMAGE_FACTOR = 0.9f;

	/** Spieler → Server-Tick, an dem der Schild endet. */
	private static final Map<UUID, Integer> SHIELDS = new HashMap<>();
	private static int serverTick;

	public HyperionItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
			witherImpact(serverLevel, sp);
		}
		return InteractionResult.SUCCESS;
	}

	private static void witherImpact(ServerLevel level, ServerPlayer player) {
		// 1) Teleport: so weit wie möglich (max. 10 Blöcke), ohne in Blöcken zu landen
		Vec3 start = player.position();
		Vec3 look = player.getLookAngle();
		Vec3 dest = start;
		for (double d = 0.25; d <= TELEPORT_DISTANCE + 1e-6; d += 0.25) {
			Vec3 p = start.add(look.scale(d));
			AABB box = player.getBoundingBox().move(p.subtract(start));
			if (!level.noCollision(player, box)) break;
			dest = p;
		}
		level.sendParticles(ParticleTypes.PORTAL, start.x, start.y + 1, start.z, 30, 0.3, 0.6, 0.3, 0.2);
		player.teleportTo(level, dest.x, dest.y, dest.z, Relative.ROTATION, 0.0f, 0.0f, false);
		player.resetFallDistance();

		// 2) Implosion
		int hits = 0;
		float total = 0;
		AABB area = player.getBoundingBox().inflate(IMPLOSION_RADIUS);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
				e -> e instanceof Enemy && e.isAlive() && e.distanceTo(player) <= IMPLOSION_RADIUS)) {
			float before = e.getHealth() + e.getAbsorptionAmount();
			if (e.hurtServer(level, player.damageSources().indirectMagic(player, player), IMPLOSION_DAMAGE)) {
				hits++;
				total += Math.min(before, IMPLOSION_DAMAGE);
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, dest.x, dest.y + 1, dest.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.EXPLOSION, dest.x, dest.y + 1, dest.z, 12, 2.5, 1.5, 2.5, 0);
		level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
		if (hits > 0) {
			player.sendOverlayMessage(Component.translatable("item.werkzeuge.hyperion.hit", hits, String.format("%.1f", total))
					.withStyle(ChatFormatting.GRAY));
		}

		// 3) Wither-Schild (mit Abklingzeit)
		if (!SHIELDS.containsKey(player.getUUID())) {
			player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SHIELD_TICKS, SHIELD_AMPLIFIER, false, false, true));
			SHIELDS.put(player.getUUID(), serverTick + SHIELD_TICKS - 1);
		}
	}

	/** Ist der Wither-Schild des Spielers aktiv? (10 % weniger Schaden, siehe LivingEntityMixin) */
	public static boolean shieldActive(Player player) {
		return SHIELDS.containsKey(player.getUUID());
	}

	public static void serverTick(MinecraftServer server) {
		serverTick++;
		Iterator<Map.Entry<UUID, Integer>> it = SHIELDS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> e = it.next();
			if (serverTick < e.getValue()) continue;
			it.remove();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			// Hälfte der übrigen Absorption wird zu Heilung
			if (p != null && p.isAlive()) {
				float rest = p.getAbsorptionAmount();
				if (rest > 0) p.heal(rest * 0.5f);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("item.werkzeuge.hyperion.ability").withStyle(ChatFormatting.GOLD));
		for (int i = 1; i <= 4; i++) {
			out.accept(Component.translatable("item.werkzeuge.hyperion.desc" + i).withStyle(ChatFormatting.GRAY));
		}
	}
}
