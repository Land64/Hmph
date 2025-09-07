package hmph.rendering.world;

import hmph.math.Vector3f;
import hmph.rendering.BlockRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {
    private Map<Long, ChunkBase> loadedChunks = new ConcurrentHashMap<>();
    private BlockRegistry registry;
    private int renderDistance;
    private Vector3f lastPlayerChunkPos = new Vector3f();

    public ChunkManager(BlockRegistry registry, int renderDistance) {
        this.registry = registry;
        this.renderDistance = renderDistance;
    }

    /**
     * Convert chunk coordinates to a unique key
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Get chunk coordinates from world position
     */
    private int getChunkCoord(float worldCoord) {
        return (int) Math.floor(worldCoord / ChunkBase.SIZE_X);
    }

    /**
     * Update chunks around the player position
     */
    public void updateChunks(Vector3f playerPosition) {
        int playerChunkX = getChunkCoord(playerPosition.x);
        int playerChunkZ = getChunkCoord(playerPosition.z);

        if (playerChunkX != (int)lastPlayerChunkPos.x || playerChunkZ != (int)lastPlayerChunkPos.z) {
            lastPlayerChunkPos.set(playerChunkX, 0, playerChunkZ);

            for (int x = playerChunkX - renderDistance; x <= playerChunkX + renderDistance; x++) {
                for (int z = playerChunkZ - renderDistance; z <= playerChunkZ + renderDistance; z++) {
                    long key = getChunkKey(x, z);
                    if (!loadedChunks.containsKey(key)) {
                        ChunkBase chunk = new ChunkBase(x, z, registry);
                        chunk.setChunkManager(this);
                        loadedChunks.put(key, chunk);
                    }
                }
            }

            loadedChunks.entrySet().removeIf(entry -> {
                long key = entry.getKey();
                int chunkX = (int)(key >> 32);
                int chunkZ = (int)(key & 0xFFFFFFFFL);

                if (Math.abs(chunkX - playerChunkX) > renderDistance ||
                        Math.abs(chunkZ - playerChunkZ) > renderDistance) {
                    entry.getValue().cleanup();
                    return true;
                }
                return false;
            });
        }
    }

    public Map<Long, ChunkBase> getLoadedChunks() {
        return loadedChunks;
    }

    public ChunkBase getChunkAt(float worldX, float worldZ) {
        int chunkX = getChunkCoord(worldX);
        int chunkZ = getChunkCoord(worldZ);
        return loadedChunks.get(getChunkKey(chunkX, chunkZ));
    }
    public void cleanup() {
        for (ChunkBase chunk : loadedChunks.values()) {
            chunk.cleanup();
        }
        loadedChunks.clear();
    }

    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }
}