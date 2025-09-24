package hmph.rendering.world;

import hmph.math.Vector3f;
import hmph.rendering.BlockRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import hmph.math.PerlinNoise;
import hmph.rendering.world.dimensions.DimensionCreator;

public class ChunkManager {
    private Map<Long, ChunkBase> loadedChunks = new ConcurrentHashMap<>();
    private BlockRegistry registry;
    private int renderDistance;
    private Vector3f lastPlayerChunkPos = new Vector3f();
    private PerlinNoise sharedBruh = new PerlinNoise();
    private DimensionCreator dimensionCreator;
    private String currentDimension = "overworld";

    public ChunkManager(BlockRegistry registry, int renderDistance) {
        this.registry = registry;
        this.renderDistance = renderDistance;
        this.dimensionCreator = new DimensionCreator(registry);
    }

    /**
     * Generate chunk key for HashMap lookup
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
    }


    private int getChunkCoord(float worldCoord) {
        return Math.floorDiv((int)worldCoord, ChunkBase.SIZE_X);
    }

    public void updateChunks(Vector3f playerPosition) {
        int playerChunkX = getChunkCoord(playerPosition.x);
        int playerChunkZ = getChunkCoord(playerPosition.z);

        if (playerChunkX != (int)lastPlayerChunkPos.x || playerChunkZ != (int)lastPlayerChunkPos.z) {
            lastPlayerChunkPos.set(playerChunkX, 0, playerChunkZ);

            for (int x = playerChunkX - renderDistance; x <= playerChunkX + renderDistance; x++) {
                for (int z = playerChunkZ - renderDistance; z <= playerChunkZ + renderDistance; z++) {
                    long key = getChunkKey(x, z);
                    if (!loadedChunks.containsKey(key)) {
                        ChunkBase chunk = new ChunkBase(x, z, registry, sharedBruh, dimensionCreator, currentDimension);
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

    /**
     * Get a chunk at world coordinates (for block lookup)
     */
    public ChunkBase getChunkAt(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, ChunkBase.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, ChunkBase.SIZE_Z);
        long chunkKey = getChunkKey(chunkX, chunkZ);
        return loadedChunks.get(chunkKey);
    }

    public void cleanup() {
        for (ChunkBase chunk : loadedChunks.values()) {
            chunk.cleanup();
        }
        loadedChunks.clear();
    }

    public int getBlockAt(int worldX, int worldY, int worldZ) {
        int chunkX = getChunkCoord(worldX);
        int chunkZ = getChunkCoord(worldZ);
        long key = getChunkKey(chunkX, chunkZ);

        ChunkBase chunk = loadedChunks.get(key);
        if (chunk != null) {
            return chunk.getBlockWorld(worldX, worldY, worldZ);
        }
        return 0;
    }

    /**
     * Set a block at world coordinates and rebuild affected chunk meshes
     */
    public void setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        int chunkX = Math.floorDiv(worldX, ChunkBase.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, ChunkBase.SIZE_Z);

        long chunkKey = getChunkKey(chunkX, chunkZ);
        ChunkBase chunk = loadedChunks.get(chunkKey);

        if (chunk != null) {
            int localX = worldX - (chunkX * ChunkBase.SIZE_X);
            int localZ = worldZ - (chunkZ * ChunkBase.SIZE_Z);

            if (localX >= 0 && localX < ChunkBase.SIZE_X && localZ >= 0 && localZ < ChunkBase.SIZE_Z && worldY >= 0 && worldY < ChunkBase.SIZE_Y) {

                chunk.setBlock(localX, worldY, localZ, blockId);
                chunk.rebuildMesh();

                rebuildNeighborChunksIfNeeded(worldX, worldY, worldZ, chunkX, chunkZ);
            }
        }
    }

    /**
     * Rebuild neighboring chunk meshes if the modified block is on a chunk boundary
     */
    private void rebuildNeighborChunksIfNeeded(int worldX, int worldY, int worldZ, int chunkX, int chunkZ) {
        int localX = worldX - (chunkX * ChunkBase.SIZE_X);
        int localZ = worldZ - (chunkZ * ChunkBase.SIZE_Z);

        if (localX == 0) {
            ChunkBase westChunk = loadedChunks.get(getChunkKey(chunkX - 1, chunkZ));
            if (westChunk != null) westChunk.rebuildMesh();
        }
        if (localX == ChunkBase.SIZE_X - 1) {
            ChunkBase eastChunk = loadedChunks.get(getChunkKey(chunkX + 1, chunkZ));
            if (eastChunk != null) eastChunk.rebuildMesh();
        }
        if (localZ == 0) {
            ChunkBase northChunk = loadedChunks.get(getChunkKey(chunkX, chunkZ - 1));
            if (northChunk != null) northChunk.rebuildMesh();
        }
        if (localZ == ChunkBase.SIZE_Z - 1) {
            ChunkBase southChunk = loadedChunks.get(getChunkKey(chunkX, chunkZ + 1));
            if (southChunk != null) southChunk.rebuildMesh();
        }
    }

    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }
}