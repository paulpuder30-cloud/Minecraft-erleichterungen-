package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.scan.BlockTracker;
import de.erleichterungen.scan.ScanManager;
import de.erleichterungen.util.Chat;
import de.erleichterungen.util.Tracers;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

/**
 * Geoden-Wachstums-Tracker.
 * <p>
 * Findet Knospender Amethyst (Budding Amethyst) und alle Amethyst-Knospen in geladenen Chunks,
 * färbt sie nach Wachstumsstufe ein, erkennt Wachstum in Echtzeit (über Block-Updates vom Server)
 * und meldet, wenn Cluster ausgewachsen und erntebereit sind.
 */
public final class GeodeTracker {
	public static final int STAGE_BUDDING = 0;
	public static final int STAGE_SMALL = 1;
	public static final int STAGE_MEDIUM = 2;
	public static final int STAGE_LARGE = 3;
	public static final int STAGE_CLUSTER = 4;

	private static final int[] STAGE_COLORS = {
			0xFFB25CFF, // Budding: lila
			0xFFFF4040, // klein: rot
			0xFFFF9A2E, // mittel: orange
			0xFFFFF03A, // groß: gelb
			0xFF3BFF5A  // Cluster: grün (erntebereit)
	};

	private static final ArrayDeque<Long> GROWTH_EVENTS = new ArrayDeque<>();
	private static final int[] GROWTH_BY_STAGE = new int[5];
	private static long lastGrowth;
	private static int newlyGrownThisTick;
	private static long lastNotifyPos;

	/** Gezählte Werte im Umkreis (bei jedem Render aktualisiert). */
	private static final int[] counts = new int[5];
	private static int freeFaces;

	public static final BlockTracker TRACKER = new BlockTracker(GeodeTracker::isGeodeBlock) {
		@Override
		protected void onChanged(long pos, BlockState oldState, BlockState newState, boolean fromScan) {
			// Beim ersten Scan eines Chunks ist alles "neu" – das ist kein Wachstum
			if (!fromScan) handleChange(pos, oldState, newState);
		}
	};

	private GeodeTracker() {
	}

	public static void init() {
		ScanManager.register(TRACKER);
	}

	public static boolean isGeodeBlock(BlockState state) {
		return stageOf(state) >= 0;
	}

	public static int stageOf(BlockState state) {
		if (state == null) return -1;
		Block b = state.getBlock();
		if (b == Blocks.BUDDING_AMETHYST) return STAGE_BUDDING;
		if (b == Blocks.SMALL_AMETHYST_BUD) return STAGE_SMALL;
		if (b == Blocks.MEDIUM_AMETHYST_BUD) return STAGE_MEDIUM;
		if (b == Blocks.LARGE_AMETHYST_BUD) return STAGE_LARGE;
		if (b == Blocks.AMETHYST_CLUSTER) return STAGE_CLUSTER;
		return -1;
	}

	public static String stageName(int stage) {
		return switch (stage) {
			case STAGE_BUDDING -> "Knospender Amethyst";
			case STAGE_SMALL -> "Kleine Knospe";
			case STAGE_MEDIUM -> "Mittlere Knospe";
			case STAGE_LARGE -> "Große Knospe";
			case STAGE_CLUSTER -> "Cluster";
			default -> "?";
		};
	}

	private static void handleChange(long pos, BlockState oldState, BlockState newState) {
		int oldStage = stageOf(oldState);
		int newStage = stageOf(newState);
		if (newStage < STAGE_SMALL) return;
		// Wachstum: neue Knospe (aus Luft/Wasser) oder höhere Stufe
		boolean grew = (oldStage < 0 && newStage == STAGE_SMALL) || (oldStage >= STAGE_SMALL && newStage > oldStage);
		if (!grew) return;
		// Nur zählen, wenn die Knospe auf knospendem Amethyst sitzt (keine von Spielern platzierten)
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && !isOnBudding(mc.level, BlockPos.of(pos), newState)) return;

		long now = System.currentTimeMillis();
		GROWTH_EVENTS.addLast(now);
		GROWTH_BY_STAGE[newStage]++;
		lastGrowth = now;
		if (newStage == STAGE_CLUSTER) {
			newlyGrownThisTick++;
			lastNotifyPos = pos;
		}
	}

	public static boolean isOnBudding(ClientLevel level, BlockPos pos, BlockState state) {
		if (!state.hasProperty(AmethystClusterBlock.FACING)) return false;
		Direction facing = state.getValue(AmethystClusterBlock.FACING);
		return level.getBlockState(pos.relative(facing.getOpposite())).is(Blocks.BUDDING_AMETHYST);
	}

	public static void reset() {
		GROWTH_EVENTS.clear();
		java.util.Arrays.fill(GROWTH_BY_STAGE, 0);
		lastGrowth = 0;
		newlyGrownThisTick = 0;
	}

	public static void tick(Minecraft mc) {
		long now = System.currentTimeMillis();
		while (!GROWTH_EVENTS.isEmpty() && now - GROWTH_EVENTS.peekFirst() > 10 * 60_000L) {
			GROWTH_EVENTS.pollFirst();
		}
		if (newlyGrownThisTick > 0) {
			ModConfig cfg = ModConfig.get();
			if (cfg.geodeTrackerEnabled && cfg.geodeNotifyFullyGrown) {
				Component msg;
				if (newlyGrownThisTick == 1) {
					BlockPos p = BlockPos.of(lastNotifyPos);
					msg = Component.literal("Amethyst-Cluster ausgewachsen bei ").withStyle(ChatFormatting.GRAY)
							.append(Component.literal(p.getX() + " " + p.getY() + " " + p.getZ()).withStyle(ChatFormatting.GREEN));
				} else {
					msg = Component.literal(newlyGrownThisTick + " Amethyst-Cluster ausgewachsen").withStyle(ChatFormatting.GREEN);
				}
				Chat.send("Geoden", msg);
				if (cfg.geodeSound) Chat.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
			}
			newlyGrownThisTick = 0;
		}
	}

	// ------------------------------------------------------------------ Rendering

	public static void render(Minecraft mc, Vec3 cam) {
		ModConfig cfg = ModConfig.get();
		java.util.Arrays.fill(counts, 0);
		freeFaces = 0;
		if (!cfg.geodeTrackerEnabled || mc.level == null) return;
		ClientLevel level = mc.level;

		int range = Math.max(8, cfg.geodeRange);
		double rangeSq = (double) range * range;
		int camCx = ((int) Math.floor(cam.x)) >> 4;
		int camCz = ((int) Math.floor(cam.z)) >> 4;
		LongArrayList stale = new LongArrayList();
		BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

		TRACKER.forEachNear(camCx, camCz, (range >> 4) + 1, (pos, state) -> {
			int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
			double dx = x + 0.5 - cam.x, dy = y + 0.5 - cam.y, dz = z + 0.5 - cam.z;
			if (dx * dx + dy * dy + dz * dz > rangeSq) return;
			BlockPos bp = new BlockPos(x, y, z);
			if (level.getBlockState(bp) != state) {
				stale.add(pos);
				return;
			}
			int stage = stageOf(state);
			if (stage < 0) return;

			if (stage == STAGE_BUDDING) {
				counts[STAGE_BUDDING]++;
				// Freie Seiten zählen, an denen eine neue Knospe wachsen kann
				for (Direction d : Direction.values()) {
					BlockState n = level.getBlockState(mut.setWithOffset(bp, d));
					if (n.isAir() || (n.is(Blocks.WATER) && n.getFluidState().getType() == Fluids.WATER && n.getFluidState().isSource())) {
						freeFaces++;
					}
				}
				if (cfg.geodeShowBudding && cfg.geodeMode.box()) {
					Gizmos.cuboid(bp, GizmoStyle.stroke(STAGE_COLORS[STAGE_BUDDING], 1.5f)).setAlwaysOnTop();
				}
				return;
			}

			if (!isOnBudding(level, bp, state)) return; // platzierte Deko-Knospen ignorieren
			counts[stage]++;
			if (stage < STAGE_CLUSTER && !cfg.geodeShowGrowing) return;

			int color = STAGE_COLORS[stage];
			// Knospen sind kleiner als ein Block → Box an die echte Form anpassen
			AABB shape = state.getShape(level, bp).isEmpty()
					? new AABB(bp)
					: state.getShape(level, bp).bounds().move(bp);
			if (cfg.geodeMode.box()) {
				GizmoStyle style = stage == STAGE_CLUSTER
						? GizmoStyle.strokeAndFill(color, 2.5f, (color & 0x00FFFFFF) | 0x50000000)
						: GizmoStyle.stroke(color, 2.0f);
				Gizmos.cuboid(shape, style).setAlwaysOnTop();
			}
			if (cfg.geodeMode.tracer() && stage == STAGE_CLUSTER) {
				Tracers.line(shape.getCenter(), ModConfig.parseColor(cfg.geodeTracerColor, color));
			}
		});

		for (int i = 0; i < stale.size(); i++) {
			long p = stale.getLong(i);
			TRACKER.invalidate(p, level.getBlockState(BlockPos.of(p)));
		}
	}

	// ------------------------------------------------------------------ HUD-Daten

	public static int count(int stage) {
		return counts[stage];
	}

	public static int freeFaces() {
		return freeFaces;
	}

	public static int growthLast10Min() {
		return GROWTH_EVENTS.size();
	}

	public static int grownTo(int stage) {
		return GROWTH_BY_STAGE[stage];
	}

	public static long lastGrowthMillis() {
		return lastGrowth;
	}

	public static int stageColor(int stage) {
		return STAGE_COLORS[stage];
	}
}
