package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.util.Chat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ausheben: gräbt einen ausgewählten Quader automatisch aus.
 * <ul>
 *   <li><b>Normale Spitzhacke</b>: Block für Block, Schicht für Schicht von oben nach unten.</li>
 *   <li><b>3x3-Spitzhacke</b> (baut eine senkrechte 3x3-Fläche quer zur Blickrichtung ab): Der Bereich wird in
 *   3 Blöcke hohe Bänder und 3 Blöcke breite Bahnen geteilt. Der Spieler schaut in Grabrichtung und schlägt
 *   jeweils den mittleren Block einer 3x3-Wand an – so, dass nichts außerhalb des Bereichs mit abgebaut wird.</li>
 * </ul>
 * Die Grabrichtung ist die Blickrichtung beim Start. Mit "Automatisch laufen" läuft der Spieler selbst
 * zur nächsten Stelle, sonst muss man selbst hinlaufen.
 */
public final class Excavator {
	public static final int TOOL_NORMAL = 0;
	public static final int TOOL_3X3 = 1;
	public static final String[] TOOL_LABELS = {"Normal", "3x3"};

	/** Größter erlaubter Bereich (Performance). */
	private static final long MAX_VOLUME = 2_000_000;
	/** Ticks ohne abgebauten Block, nach denen abgebrochen wird. */
	private static final int STUCK_LIMIT = 20 * 15;
	/** Wartezeit nach einem 3x3-Schlag, bis der Server die Nachbarblöcke entfernt hat. */
	private static final int AFTER_3X3_WAIT = 5;

	private static BlockPos pos1;
	private static BlockPos pos2;
	private static boolean active;

	/** Grabrichtung (horizontal), beim Start aus der Blickrichtung bestimmt. */
	private static Direction digDir = Direction.NORTH;
	/** Aktuelles Ziel (für die Anzeige) und Zustand. */
	private static BlockPos target;
	private static BlockPos walkTarget;
	private static boolean walking;
	private static boolean jump;
	private static int waitTicks;
	private static int idleTicks;
	private static int mined;
	private static boolean lavaWarned;

	private Excavator() {
	}

	// ------------------------------------------------------------------ Auswahl

	public static boolean isActive() {
		return active;
	}

	public static BlockPos pos1() {
		return pos1;
	}

	public static BlockPos pos2() {
		return pos2;
	}

	public static int minedCount() {
		return mined;
	}

	/** Block, auf den der Spieler schaut (sonst der Block unter den Füßen). */
	private static BlockPos lookedAt(Minecraft mc) {
		if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) return bhr.getBlockPos();
		return mc.player.blockPosition().below();
	}

	public static void setPos1(BlockPos pos) {
		pos1 = pos.immutable();
		announcePos("Position 1", pos1);
	}

	public static void setPos2(BlockPos pos) {
		pos2 = pos.immutable();
		announcePos("Position 2", pos2);
	}

	public static void setPos1FromLook() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) setPos1(lookedAt(mc));
	}

	public static void setPos2FromLook() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) setPos2(lookedAt(mc));
	}

	public static void clearSelection() {
		stop(false);
		pos1 = null;
		pos2 = null;
		Chat.send("Ausheben", "Auswahl gelöscht.");
	}

	private static void announcePos(String what, BlockPos p) {
		String size = "";
		if (pos1 != null && pos2 != null) {
			size = "  (" + sizeX() + " x " + sizeY() + " x " + sizeZ() + " = " + volume() + " Blöcke)";
		}
		Chat.send("Ausheben", Component.literal(what + ": ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(p.getX() + " " + p.getY() + " " + p.getZ()).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(size).withStyle(ChatFormatting.DARK_GRAY)));
	}

	private static int minX() { return Math.min(pos1.getX(), pos2.getX()); }
	private static int minY() { return Math.min(pos1.getY(), pos2.getY()); }
	private static int minZ() { return Math.min(pos1.getZ(), pos2.getZ()); }
	private static int maxX() { return Math.max(pos1.getX(), pos2.getX()); }
	private static int maxY() { return Math.max(pos1.getY(), pos2.getY()); }
	private static int maxZ() { return Math.max(pos1.getZ(), pos2.getZ()); }
	private static int sizeX() { return maxX() - minX() + 1; }
	private static int sizeY() { return maxY() - minY() + 1; }
	private static int sizeZ() { return maxZ() - minZ() + 1; }

	private static long volume() {
		return (long) sizeX() * sizeY() * sizeZ();
	}

	private static boolean inArea(int x, int y, int z) {
		return x >= minX() && x <= maxX() && y >= minY() && y <= maxY() && z >= minZ() && z <= maxZ();
	}

	// ------------------------------------------------------------------ Start / Stopp

	public static void toggle() {
		if (active) stop(true);
		else start();
	}

	public static void start() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gameMode == null) return;
		if (pos1 == null || pos2 == null) {
			Chat.send("Ausheben", Component.literal("Erst Position 1 und 2 setzen (Tasten oder /erl ausheben pos1|pos2).")
					.withStyle(ChatFormatting.RED));
			return;
		}
		if (volume() > MAX_VOLUME) {
			Chat.send("Ausheben", Component.literal("Bereich zu groß (" + volume() + " Blöcke, max. " + MAX_VOLUME + ").")
					.withStyle(ChatFormatting.RED));
			return;
		}
		digDir = mc.player.getDirection();
		if (digDir.getAxis() == Direction.Axis.Y) digDir = Direction.NORTH;
		active = true;
		mined = 0;
		idleTicks = 0;
		waitTicks = 0;
		lavaWarned = false;
		target = null;
		walkTarget = null;

		boolean x3 = ModConfig.get().excavateTool == TOOL_3X3;
		Chat.send("Ausheben", Component.literal("Gestartet ").withStyle(ChatFormatting.GREEN)
				.append(Component.literal("(" + (x3 ? "3x3-Spitzhacke" : "normale Spitzhacke") + ", Richtung " + directionName(digDir) + ")")
						.withStyle(ChatFormatting.GRAY)));
		if (x3 && (sizeY() % 3 == 1 || crossSize() % 3 == 1)) {
			Chat.send("Ausheben", Component.literal("Hinweis: Höhe " + sizeY() + " / Breite " + crossSize()
					+ " – bei 3n+1 (1, 4, 7, ...) bricht die 3x3-Spitzhacke am Rand eine Reihe mehr ab. Ideal: Vielfache von 3.")
					.withStyle(ChatFormatting.YELLOW));
		}
	}

	public static void stop(boolean announce) {
		boolean was = active;
		active = false;
		walking = false;
		jump = false;
		target = null;
		walkTarget = null;
		Minecraft mc = Minecraft.getInstance();
		if (was && mc.gameMode != null && mc.player != null) mc.gameMode.stopDestroyBlock();
		if (was && announce) {
			Chat.send("Ausheben", Component.literal("Gestoppt").withStyle(ChatFormatting.RED)
					.append(Component.literal(" – " + countText() + ".").withStyle(ChatFormatting.GRAY)));
		}
	}

	private static String directionName(Direction d) {
		return switch (d) {
			case NORTH -> "Norden";
			case SOUTH -> "Süden";
			case EAST -> "Osten";
			case WEST -> "Westen";
			default -> d.getName();
		};
	}

	// ------------------------------------------------------------------ Eingaben (für KeyboardInputMixin)

	public static boolean isWalking() {
		return active && walking;
	}

	public static boolean wantsJump() {
		return active && walking && jump;
	}

	// ------------------------------------------------------------------ Logik

	/** Abbaubarer Block im Bereich? (keine Luft, keine Flüssigkeit, nicht unzerstörbar) */
	private static boolean mineable(ClientLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.getBlock() instanceof LiquidBlock) return false;
		if (state.getDestroySpeed(level, pos) < 0) return false;
		return !ModConfig.get().excavateAvoidLava || !nextToLava(level, pos);
	}

	private static boolean nextToLava(ClientLevel level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (d == Direction.DOWN) continue; // Lava unterhalb fließt nicht hoch
			if (level.getFluidState(pos.relative(d)).is(FluidTags.LAVA)) {
				if (!lavaWarned) {
					lavaWarned = true;
					Chat.send("Ausheben", Component.literal("Lava in der Nähe – betroffene Blöcke werden übersprungen.")
							.withStyle(ChatFormatting.GOLD));
				}
				return true;
			}
		}
		return false;
	}

	private static int crossSize() {
		return digDir.getAxis() == Direction.Axis.X ? sizeZ() : sizeX();
	}

	public static void tick(Minecraft mc) {
		walking = false;
		jump = false;
		if (!active) return;
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null || mc.gameMode == null || !player.isAlive()) {
			stop(false);
			return;
		}
		if (mc.gui.screen() != null || Freecam.isActive()) return; // Pause in Menüs / Freecam

		if (waitTicks > 0) {
			waitTicks--;
			return;
		}

		// Oberste Schicht mit abbaubaren Blöcken suchen
		int topY = Integer.MIN_VALUE;
		for (int y = maxY(); y >= minY() && topY == Integer.MIN_VALUE; y--) {
			if (layerHasBlocks(level, y)) topY = y;
		}
		if (topY == Integer.MIN_VALUE) {
			Chat.send("Ausheben", Component.literal("Fertig! ").withStyle(ChatFormatting.GREEN)
					.append(Component.literal(countText() + ".").withStyle(ChatFormatting.GRAY)));
			stop(false);
			return;
		}

		boolean done = ModConfig.get().excavateTool == TOOL_3X3 && sizeY() >= 3 && crossSize() >= 3
				? tick3x3(mc, player, level, topY)
				: tickNormal(mc, player, level, topY, topY);

		if (done) {
			idleTicks = 0;
		} else if (++idleTicks > STUCK_LIMIT && ModConfig.get().excavateAutoWalk) {
			Chat.send("Ausheben", Component.literal("Kein Fortschritt – nächster Block nicht erreichbar. Abgebrochen.")
					.withStyle(ChatFormatting.RED));
			stop(false);
		}
	}

	private static boolean layerHasBlocks(ClientLevel level, int y) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = minX(); x <= maxX(); x++) {
			for (int z = minZ(); z <= maxZ(); z++) {
				if (mineable(level, p.set(x, y, z))) return true;
			}
		}
		return false;
	}

	/**
	 * Normaler Modus: nächster erreichbarer Block der Schichten {@code fromY..toY}; sonst hinlaufen.
	 * @return true, wenn gerade abgebaut wird (Fortschritt)
	 */
	private static boolean tickNormal(Minecraft mc, LocalPlayer player, ClientLevel level, int fromY, int toY) {
		Vec3 eye = player.getEyePosition();
		double reach = player.blockInteractionRange() - 0.3;
		BlockPos feet = player.blockPosition().below();
		BlockPos best = null, nearest = null;
		double bestScore = Double.MAX_VALUE, nearestDist = Double.MAX_VALUE;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int y = fromY; y >= toY; y--) {
			for (int x = minX(); x <= maxX(); x++) {
				for (int z = minZ(); z <= maxZ(); z++) {
					p.set(x, y, z);
					if (!mineable(level, p)) continue;
					double d = eye.distanceToSqr(x + 0.5, y + 0.5, z + 0.5);
					if (d < nearestDist) {
						nearestDist = d;
						nearest = p.immutable();
					}
					if (Math.sqrt(d) > reach + 0.5) continue;
					// Block unter den Füßen zuletzt (sonst fällt man ständig)
					double score = d + (p.equals(feet) ? 100 : 0);
					if (score < bestScore) {
						bestScore = score;
						best = p.immutable();
					}
				}
			}
		}
		if (best != null) {
			mine(mc, player, best, faceToward(best, eye), false, null);
			return true;
		}
		if (nearest != null) walkTo(player, Vec3.atBottomCenterOf(nearest));
		return false;
	}

	/**
	 * 3x3-Modus (Spitzhacke baut eine senkrechte 3x3-Fläche quer zur Blickrichtung ab).
	 * Der Bereich wird in Bänder (3 hoch) geteilt, von oben nach unten, abwechselnd hin und zurück.
	 * Im Band wird entlang der Grabrichtung Scheibe für Scheibe (1 dick) abgetragen. Jeder Schlag trifft einen Block
	 * der vordersten Scheibe mit Blick in Grabrichtung; gewählt wird der Block, dessen 3x3-Fläche möglichst viele
	 * Blöcke im Bereich und keine außerhalb erwischt (in der Regel die Bahnmitten).
	 */
	private static boolean tick3x3(Minecraft mc, LocalPlayer player, ClientLevel level, int topY) {
		// Band so legen, dass es an der Oberkante des Bereichs ausgerichtet ist
		int band = (maxY() - topY) / 3;
		int bandTop = maxY() - band * 3;
		int cy = Math.max(minY() + 1, bandTop - 1); // Mittelhöhe
		// Abwechselnd vor und zurück graben, damit man nicht zurücklaufen muss
		Direction dir = band % 2 == 0 ? digDir : digDir.getOpposite();
		int sign = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
		boolean alongX = dir.getAxis() == Direction.Axis.X;
		int dMin = alongX ? minX() : minZ(), dMax = alongX ? maxX() : maxZ();
		int cMin = alongX ? minZ() : minX(), cMax = alongX ? maxZ() : maxX();

		// Front = erste Scheibe (in Grabrichtung) mit Blöcken im Band
		int front = Integer.MIN_VALUE;
		for (int i = 0; i <= dMax - dMin && front == Integer.MIN_VALUE; i++) {
			int d = sign > 0 ? dMin + i : dMax - i;
			if (sliceHasBlocks(level, alongX, d, cMin, cMax, cy)) front = d;
		}
		if (front == Integer.MIN_VALUE) return tickNormal(mc, player, level, topY, topY);
		boolean entry = front == (sign > 0 ? dMin : dMax);

		Vec3 eye = player.getEyePosition();
		double reach = player.blockInteractionRange() - 0.3;
		// Angeschlagen wird die Seite der Scheibe, die zum Spieler zeigt. Die 3x3-Fläche hängt nur von der
		// Achse der Blickrichtung ab – man kann also von vorne, von hinten oder von oben schlagen.
		double eyeD = alongX ? eye.x : eye.z;
		Direction face = (eyeD - (front + 0.5)) * sign < 0 ? dir.getOpposite() : dir;

		// Bahnmitten: cMin+1, cMin+4, ..., letzte an den Rand geklemmt
		boolean[] lane = new boolean[cMax - cMin + 1];
		for (int c = cMin + 1; ; c += 3) {
			int cc = Math.min(c, cMax - 1);
			if (cc >= cMin) lane[cc - cMin] = true;
			if (cc >= cMax - 1) break;
		}

		BlockPos bestClean = null, bestGrid = null, bestAny = null, walkGoal = null;
		int bestCleanRank = -1, bestAnyOut = Integer.MAX_VALUE, bestAnyIn = -1;
		double bestCleanDist = 0, bestAnyDist = 0, walkScore = Double.MAX_VALUE;
		boolean anyClean = false, anyGrid = false;
		double bestGridDist = Double.MAX_VALUE;
		for (int y = Math.min(maxY(), cy + 1); y >= Math.max(minY(), cy - 1); y--) {
			for (int c = cMin; c <= cMax; c++) {
				BlockPos b = alongX ? new BlockPos(front, y, c) : new BlockPos(c, y, front);
				if (!mineable(level, b)) continue;
				// Was würde die 3x3-Fläche um b erwischen?
				int in = 0, out = 0;
				for (int dy = -1; dy <= 1; dy++) {
					for (int dc = -1; dc <= 1; dc++) {
						int x = alongX ? front : c + dc, z = alongX ? c + dc : front;
						if (!mineable(level, new BlockPos(x, y + dy, z))) continue;
						if (inArea(x, y + dy, z)) in++;
						else out++;
					}
				}
				Vec3 facePoint = Vec3.atCenterOf(b).add(face.getUnitVec3().scale(0.5));
				double dist = eye.distanceTo(facePoint);
				Direction facing = aimFacing(eye, facePoint);
				boolean hittable = dist <= reach && (facing == null || facing.getAxis() == dir.getAxis())
						&& !undermines(player, alongX, front, y, c);
				// Bahnmitten zuerst – sonst bleiben am Rand Einzelreihen übrig, die nur mit Überstand gehen
				boolean grid = y == cy && lane[c - cMin];
				if (out == 0) {
					anyClean = true;
					if (grid) {
						anyGrid = true;
						if (hittable && dist < bestGridDist) {
							bestGridDist = dist;
							bestGrid = b;
						}
					}
					int rank = (grid ? 100 : 0) + in;
					if (hittable && (rank > bestCleanRank || rank == bestCleanRank && dist < bestCleanDist)) {
						bestClean = b;
						bestCleanRank = rank;
						bestCleanDist = dist;
					}
					double score = (grid ? 0 : 1000) + dist;
					if (score < walkScore) {
						walkScore = score;
						walkGoal = b;
					}
				}
				if (hittable && (out < bestAnyOut || out == bestAnyOut && (in > bestAnyIn || in == bestAnyIn && dist < bestAnyDist))) {
					bestAny = b;
					bestAnyOut = out;
					bestAnyIn = in;
					bestAnyDist = dist;
				}
			}
		}

		// Solange noch Bahnmitten stehen, nur diese anschlagen (sonst Laufen); danach sonstige saubere Schläge
		BlockPos hit = anyGrid ? bestGrid : bestClean;
		if (hit != null) {
			hit3x3(mc, player, hit, face, dir);
			return true;
		}
		// Ohne Überstand geht es nicht (z.B. Höhe/Breite 3n+1) oder der Standplatz ist nicht erreichbar
		// → bester Schlag mit möglichst wenig Überstand
		if (bestAny != null && (!anyClean || idleTicks > 100 && ModConfig.get().excavateAutoWalk)) {
			hit3x3(mc, player, bestAny, face, dir);
			return true;
		}
		BlockPos goal = walkGoal != null ? walkGoal : bestAny;
		if (goal == null) {
			goal = alongX ? new BlockPos(front, cy, (cMin + cMax) / 2) : new BlockPos((cMin + cMax) / 2, cy, front);
		}
		// Standplatz: in der schon gegrabenen Scheibe davor. Beim Einstieg ins Band gibt es die nicht –
		// dann oben auf die nächste Scheibe stellen und schräg nach unten/hinten schlagen.
		BlockPos stand;
		if (!entry) stand = goal.relative(dir.getOpposite());
		else if (dMax > dMin) stand = goal.relative(dir);
		else stand = goal;
		walkTo(player, new Vec3(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5));
		return false;
	}

	/**
	 * Würde die 3x3-Fläche um (front, y, c) einen Block wegnehmen, auf dem der Spieler steht?
	 * Dann fiele er in den schmalen Schacht und käme nicht mehr an die Nachbarbahnen.
	 */
	private static boolean undermines(LocalPlayer player, boolean alongX, int front, int y, int c) {
		AABB bb = player.getBoundingBox();
		double footY = bb.minY - 0.2;
		for (double px : new double[]{bb.minX + 0.01, bb.maxX - 0.01}) {
			for (double pz : new double[]{bb.minZ + 0.01, bb.maxZ - 0.01}) {
				BlockPos sp = BlockPos.containing(px, footY, pz);
				int sd = alongX ? sp.getX() : sp.getZ(), sc = alongX ? sp.getZ() : sp.getX();
				if (sd == front && Math.abs(sc - c) <= 1 && Math.abs(sp.getY() - y) <= 1) return true;
			}
		}
		return false;
	}

	/** In welche Himmelsrichtung schaut der Spieler, wenn er auf diesen Punkt zielt? (null = senkrecht drunter/drüber) */
	private static Direction aimFacing(Vec3 eye, Vec3 point) {
		double dx = point.x - eye.x, dz = point.z - eye.z;
		if (dx * dx + dz * dz < 0.8 * 0.8) return null;
		return Direction.fromYRot((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f);
	}

	/** Schlag mit der 3x3-Spitzhacke: Blick in Grabrichtung, angeschlagen wird die Seite zum Spieler. */
	private static void hit3x3(Minecraft mc, LocalPlayer player, BlockPos pos, Direction face, Direction dir) {
		mine(mc, player, pos, face, true, dir);
	}

	private static boolean sliceHasBlocks(ClientLevel level, boolean alongX, int d, int cMin, int cMax, int cy) {
		for (int c = cMin; c <= cMax; c++) {
			if (laneColumnHasBlocks(level, alongX, d, c, cy)) return true;
		}
		return false;
	}

	private static boolean laneColumnHasBlocks(ClientLevel level, boolean alongX, int d, int c, int cy) {
		for (int y = cy - 1; y <= cy + 1; y++) {
			int x = alongX ? d : c, z = alongX ? c : d;
			if (inArea(x, y, z) && mineable(level, new BlockPos(x, y, z))) return true;
		}
		return false;
	}

	/** Seite des Blocks, die zum Auge zeigt. */
	private static Direction faceToward(BlockPos pos, Vec3 eye) {
		Vec3 diff = eye.subtract(Vec3.atCenterOf(pos));
		return Direction.getApproximateNearest(diff.x, diff.y, diff.z);
	}

	/**
	 * @param facing gewünschte Blickrichtung (Himmelsrichtung) oder null; wird genutzt, wenn der Block
	 *               (fast) senkrecht unter/über dem Spieler liegt
	 */
	private static void mine(Minecraft mc, LocalPlayer player, BlockPos pos, Direction face, boolean hit3x3, Direction facing) {
		boolean newTarget = !pos.equals(target) || !mc.gameMode.isDestroying();
		target = pos;
		walkTarget = null;
		Vec3 point = Vec3.atCenterOf(pos).add(face.getUnitVec3().scale(0.5));
		lookAt(player, point);
		if (facing != null && aimFacing(player.getEyePosition(), point) == null) {
			player.setYRot(facing.toYRot());
			player.setYHeadRot(facing.toYRot());
		}
		if (newTarget) {
			// Blickrichtung sofort an den Server schicken – 3x3-Spitzhacken richten die Fläche danach aus
			player.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(),
					player.onGround(), player.horizontalCollision));
		}
		if (mc.gameMode.continueDestroyBlock(pos, face)) {
			player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
			player.connection.send(ServerboundPunchPacket.INSTANCE);
		}
		if (!mineable(mc.level, pos)) {
			mined++;
			waitTicks = Math.max(ModConfig.get().excavateDelay, hit3x3 ? AFTER_3X3_WAIT : 0);
		}
	}

	private static void lookAt(LocalPlayer player, Vec3 point) {
		Vec3 eye = player.getEyePosition();
		double dx = point.x - eye.x, dy = point.y - eye.y, dz = point.z - eye.z;
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
		float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
		player.setYRot(yaw);
		player.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
		player.setYHeadRot(yaw);
	}

	private static void walkTo(LocalPlayer player, Vec3 dest) {
		if (!ModConfig.get().excavateAutoWalk) return;
		Vec3 pos = player.position();
		double dx = dest.x - pos.x, dz = dest.z - pos.z;
		if (dx * dx + dz * dz < 0.3 * 0.3) return;
		walkTarget = BlockPos.containing(dest);
		target = null;
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot(20.0f);
		walking = true;
		jump = player.horizontalCollision && player.onGround();
	}

	// ------------------------------------------------------------------ Anzeige

	public static void render(Minecraft mc) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.excavateShowArea) return;
		int color = ModConfig.parseColor(cfg.excavateColor, 0xFFFFAA00) | 0xFF000000;
		if (pos1 != null && pos2 != null) {
			AABB box = new AABB(minX(), minY(), minZ(), maxX() + 1, maxY() + 1, maxZ() + 1);
			Gizmos.cuboid(box, GizmoStyle.strokeAndFill(color, 2.0f, (color & 0x00FFFFFF) | 0x18000000)).setAlwaysOnTop();
		}
		if (pos1 != null) Gizmos.cuboid(pos1, GizmoStyle.stroke(0xFF55FF55, 2.5f)).setAlwaysOnTop();
		if (pos2 != null) Gizmos.cuboid(pos2, GizmoStyle.stroke(0xFF5599FF, 2.5f)).setAlwaysOnTop();
		if (active && target != null) Gizmos.cuboid(target, GizmoStyle.stroke(0xFFFF3B3B, 3.0f)).setAlwaysOnTop();
		if (active && walkTarget != null) Gizmos.cuboid(walkTarget, GizmoStyle.stroke(0xFF7FFFD4, 2.0f)).setAlwaysOnTop();
	}

	/** "x Blöcke abgebaut" bzw. im 3x3-Modus "x Schläge". */
	private static String countText() {
		return mined + (ModConfig.get().excavateTool == TOOL_3X3 ? " Schläge (3x3)" : " Blöcke abgebaut");
	}

	/** Kurzinfo für das HUD. */
	public static String hudLine() {
		if (!active) return null;
		return "Ausheben: " + countText() + (walking ? " – läuft" : "");
	}
}
