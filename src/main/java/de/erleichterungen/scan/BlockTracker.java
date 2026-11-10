package de.erleichterungen.scan;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.function.Predicate;

/**
 * Merkt sich alle Positionen in geladenen Chunks, deren Blockzustand einem Filter entspricht.
 * Wird vom {@link ScanManager} bei Chunk-Load/Unload und bei Block-Updates vom Server gefüttert.
 */
public class BlockTracker {
	/** Chunk-Key -> (BlockPos-Long -> BlockState). */
	private final Long2ObjectMap<Long2ObjectOpenHashMap<BlockState>> byChunk = new Long2ObjectOpenHashMap<>();
	private Predicate<BlockState> filter;
	private int size;

	public BlockTracker(Predicate<BlockState> filter) {
		this.filter = filter;
	}

	public void setFilter(Predicate<BlockState> filter) {
		this.filter = filter;
	}

	public boolean matches(BlockState state) {
		return state != null && filter.test(state);
	}

	public int size() {
		return size;
	}

	/** Wird aufgerufen, wenn sich ein überwachter Eintrag ändert. old/neu können null sein. */
	protected void onChanged(long pos, BlockState oldState, BlockState newState, boolean fromScan) {
	}

	public void scanChunk(LevelChunk chunk) {
		long key = ScanManager.chunkKey(chunk.getPos().x(), chunk.getPos().z());
		Long2ObjectOpenHashMap<BlockState> old = byChunk.remove(key);
		if (old != null) size -= old.size();

		Long2ObjectOpenHashMap<BlockState> found = null;
		LevelChunkSection[] sections = chunk.getSections();
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		int minY = chunk.getMinY();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (section == null || section.hasOnlyAir() || !section.maybeHas(filter)) continue;
			int baseY = minY + (i << 4);
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState state = section.getBlockState(x, y, z);
						if (filter.test(state)) {
							if (found == null) found = new Long2ObjectOpenHashMap<>();
							found.put(BlockPos.asLong(baseX + x, baseY + y, baseZ + z), state);
						}
					}
				}
			}
		}

		// Änderungen gegenüber dem alten Stand melden
		if (old != null) {
			for (Long2ObjectMap.Entry<BlockState> e : old.long2ObjectEntrySet()) {
				BlockState now = found == null ? null : found.get(e.getLongKey());
				if (now == null) onChanged(e.getLongKey(), e.getValue(), null, true);
			}
		}
		if (found != null) {
			for (Long2ObjectMap.Entry<BlockState> e : found.long2ObjectEntrySet()) {
				BlockState before = old == null ? null : old.get(e.getLongKey());
				if (before != e.getValue()) onChanged(e.getLongKey(), before, e.getValue(), true);
			}
			byChunk.put(key, found);
			size += found.size();
		}
	}

	public void unloadChunk(long chunkKey) {
		Long2ObjectOpenHashMap<BlockState> old = byChunk.remove(chunkKey);
		if (old != null) {
			size -= old.size();
			for (Long2ObjectMap.Entry<BlockState> e : old.long2ObjectEntrySet()) {
				onUnloaded(e.getLongKey(), e.getValue());
			}
		}
	}

	/** Eintrag fällt durch Chunk-Unload weg (kein echter Blockwechsel). */
	protected void onUnloaded(long pos, BlockState state) {
	}

	public void onBlockChanged(long pos, BlockState newState) {
		long key = ScanManager.chunkKey(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4);
		Long2ObjectOpenHashMap<BlockState> map = byChunk.get(key);
		BlockState old = map == null ? null : map.get(pos);
		boolean match = matches(newState);
		if (match) {
			if (map == null) {
				map = new Long2ObjectOpenHashMap<>();
				byChunk.put(key, map);
			}
			if (old == null) size++;
			map.put(pos, newState);
			if (old != newState) onChanged(pos, old, newState, false);
		} else if (old != null) {
			map.remove(pos);
			size--;
			if (map.isEmpty()) byChunk.remove(key);
			onChanged(pos, old, null, false);
		}
	}

	public void clear() {
		byChunk.clear();
		size = 0;
	}

	public interface EntryConsumer {
		void accept(long pos, BlockState state);
	}

	/** Iteriert über alle Einträge in Chunks innerhalb von {@code chunkRadius} um (cx, cz). */
	public void forEachNear(int cx, int cz, int chunkRadius, EntryConsumer consumer) {
		for (Long2ObjectMap.Entry<Long2ObjectOpenHashMap<BlockState>> chunk : byChunk.long2ObjectEntrySet()) {
			long key = chunk.getLongKey();
			int x = ScanManager.chunkX(key);
			int z = ScanManager.chunkZ(key);
			if (Math.abs(x - cx) > chunkRadius || Math.abs(z - cz) > chunkRadius) continue;
			for (Long2ObjectMap.Entry<BlockState> e : chunk.getValue().long2ObjectEntrySet()) {
				consumer.accept(e.getLongKey(), e.getValue());
			}
		}
	}

	public void forEach(EntryConsumer consumer) {
		for (Long2ObjectOpenHashMap<BlockState> chunk : byChunk.values()) {
			for (Long2ObjectMap.Entry<BlockState> e : chunk.long2ObjectEntrySet()) {
				consumer.accept(e.getLongKey(), e.getValue());
			}
		}
	}

	/** Entfernt einen Eintrag, falls er nicht mehr mit der Welt übereinstimmt. */
	public void invalidate(long pos, BlockState actual) {
		onBlockChanged(pos, actual);
	}
}
