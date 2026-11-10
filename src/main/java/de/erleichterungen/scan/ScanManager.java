package de.erleichterungen.scan;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet das Scannen geladener Chunks für alle {@link BlockTracker}.
 * Chunks werden in eine Warteschlange gelegt und pro Tick nur in begrenzter Zahl gescannt,
 * damit das Spiel beim Betreten einer Welt nicht ruckelt.
 */
public final class ScanManager {
	private static final int CHUNKS_PER_TICK = 6;

	private static final List<BlockTracker> TRACKERS = new ArrayList<>();
	private static final LongOpenHashSet LOADED = new LongOpenHashSet();
	private static final LongLinkedOpenHashSet QUEUE = new LongLinkedOpenHashSet();

	private ScanManager() {
	}

	public static long chunkKey(int x, int z) {
		return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
	}

	public static int chunkX(long key) {
		return (int) key;
	}

	public static int chunkZ(long key) {
		return (int) (key >>> 32);
	}

	public static void register(BlockTracker tracker) {
		TRACKERS.add(tracker);
	}

	public static void init() {
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			long key = chunkKey(chunk.getPos().x(), chunk.getPos().z());
			LOADED.add(key);
			QUEUE.add(key);
		});
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			long key = chunkKey(chunk.getPos().x(), chunk.getPos().z());
			LOADED.remove(key);
			QUEUE.remove(key);
			for (BlockTracker t : TRACKERS) t.unloadChunk(key);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ScanManager::reset));
	}

	public static void reset() {
		LOADED.clear();
		QUEUE.clear();
		for (BlockTracker t : TRACKERS) t.clear();
	}

	/** Legt alle geladenen Chunks erneut in die Scan-Warteschlange (z.B. nach Filteränderung). */
	public static void rescanAll() {
		QUEUE.addAll(LOADED);
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null || QUEUE.isEmpty()) return;
		for (int i = 0; i < CHUNKS_PER_TICK && !QUEUE.isEmpty(); i++) {
			long key = QUEUE.removeFirstLong();
			LevelChunk chunk = level.getChunkSource().getChunk(chunkX(key), chunkZ(key), false);
			if (chunk == null) continue;
			for (BlockTracker t : TRACKERS) t.scanChunk(chunk);
		}
	}

	/** Aufgerufen aus dem Mixin, wenn der Server einen Blockwechsel schickt. */
	public static void onBlockChanged(BlockPos pos, BlockState state) {
		long p = pos.asLong();
		for (BlockTracker t : TRACKERS) t.onBlockChanged(p, state);
	}

	public static int queued() {
		return QUEUE.size();
	}
}
