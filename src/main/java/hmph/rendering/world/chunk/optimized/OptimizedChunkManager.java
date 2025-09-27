package hmph.rendering.world.chunk.optimized;
import hmph.math.Vector3f;
import hmph.rendering.BlockRegistry;
import hmph.rendering.camera.Camera;
import hmph.rendering.world.chunk.ChunkBase;
import hmph.rendering.world.chunk.ChunkManager;
import hmph.rendering.world.dimensions.DimensionCreator;
import hmph.math.PerlinNoise;
import hmph.util.debug.LoggerHelper;

import java.util.*;
import java.util.concurrent.*;

public class OptimizedChunkManager extends ChunkManager {
    private final Map<Long, ChunkLOD> chunkLODs = new ConcurrentHashMap<>();
    private final Map<Long, ChunkBase> loadedChunks = new ConcurrentHashMap<>();
    private final BlockRegistry registry;
    private final int renderDistance;
    private Vector3f lastPlayerChunkPos = new Vector3f(-999, 0, -999);
    private final PerlinNoise sharedBruh = new PerlinNoise();
    private final DimensionCreator dimensionCreator;
    private String currentDimension = "overworld";

    public final ExecutorService chunkExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    public final Queue<ChunkBase> chunksNeedingGLBuffers = new ConcurrentLinkedQueue<>();
    public final Set<Long> chunksBeingGenerated = ConcurrentHashMap.newKeySet();

    private static final float LOD_DISTANCE_HIGH = 64f;
    private static final float LOD_DISTANCE_MED = 128f;
    private static final float LOD_DISTANCE_LOW = 192f;
    private static final int MAX_CHUNKS_PER_FRAME = 3;

    public static enum LODLevel {
        HIGH(1),
        MEDIUM(2),
        LOW(4),
        UNLOADED(0);

        public final int simplificationFactor;

        LODLevel(int factor) {
            this.simplificationFactor = factor;
        }
    }

    public static class ChunkLOD {
        public final Vector3f position;
        public final int chunkX, chunkZ;
        public LODLevel currentLOD = LODLevel.UNLOADED;
        public float lastDistance;
        public long lastUpdateTime;
        public boolean inFrustum = false;

        public ChunkLOD(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.position = new Vector3f(chunkX * ChunkBase.SIZE_X, 0, chunkZ * ChunkBase.SIZE_Z);
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    public OptimizedChunkManager(BlockRegistry registry, int renderDistance) {
        super(registry, renderDistance);
        this.registry = registry;
        this.renderDistance = renderDistance;
        this.dimensionCreator = new DimensionCreator(registry);

        LoggerHelper.betterPrint("Initialized Optimized Chunk Manager with " +
                chunkExecutor.toString() + " threads", LoggerHelper.LogType.RENDERING);
    }

    @Override
    public void updateChunks(Vector3f playerPosition) {
        updateChunksWithCamera(playerPosition, null);
    }

    /**
     * Main update method with camera for frustum culling
     */
    public void updateChunksWithCamera(Vector3f playerPosition, Camera camera) {
        int playerChunkX = getChunkCoord(playerPosition.x);
        int playerChunkZ = getChunkCoord(playerPosition.z);

        boolean playerMoved = playerChunkX != (int)lastPlayerChunkPos.x || playerChunkZ != (int)lastPlayerChunkPos.z;

        if (playerMoved) {
            lastPlayerChunkPos.set(playerChunkX, 0, playerChunkZ);
        }

        if (camera != null) {
            camera.updateFrustum((16f/9f), 0.1f, renderDistance * ChunkBase.SIZE_X * 1.5f);
        }
        updateChunkLODs(playerPosition, camera, playerMoved);

        processGLBufferQueue();

        if (playerMoved) {
            cleanupDistantChunks(playerChunkX, playerChunkZ);
        }
    }

    /**
     * Update LOD levels and generate/unload chunks as needed
     */
    private void updateChunkLODs(Vector3f playerPos, Camera camera, boolean forceUpdate) {
        int playerChunkX = getChunkCoord(playerPos.x);
        int playerChunkZ = getChunkCoord(playerPos.z);

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                long chunkKey = getChunkKey(chunkX, chunkZ);

                ChunkLOD lod = chunkLODs.computeIfAbsent(chunkKey, k -> new ChunkLOD(chunkX, chunkZ));

                float distance = calculateChunkDistance(playerPos, lod.position);
                LODLevel requiredLOD = calculateRequiredLOD(distance);

                if (camera != null) {
                    lod.inFrustum = camera.isChunkVisible(lod.position, ChunkBase.SIZE_X);
                } else {
                    lod.inFrustum = true;
                }

                if (!lod.inFrustum && distance > LOD_DISTANCE_MED) {
                    if (lod.currentLOD != LODLevel.UNLOADED) {
                        unloadChunk(chunkKey);
                        lod.currentLOD = LODLevel.UNLOADED;
                    }
                    continue;
                }

                if (lod.currentLOD != requiredLOD || forceUpdate) {
                    updateChunkLOD(chunkKey, lod, requiredLOD);
                }

                lod.lastDistance = distance;
                lod.lastUpdateTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Calculate required LOD level based on distance
     */
    private LODLevel calculateRequiredLOD(float distance) {
        if (distance <= LOD_DISTANCE_HIGH) {
            return LODLevel.HIGH;
        } else if (distance <= LOD_DISTANCE_MED) {
            return LODLevel.MEDIUM;
        } else if (distance <= LOD_DISTANCE_LOW) {
            return LODLevel.LOW;
        } else {
            return LODLevel.UNLOADED;
        }
    }

    /**
     * Update a chunk's LOD level
     */
    private void updateChunkLOD(long chunkKey, ChunkLOD lod, LODLevel newLOD) {
        if (newLOD == LODLevel.UNLOADED) {
            unloadChunk(chunkKey);
            lod.currentLOD = LODLevel.UNLOADED;
            return;
        }

        if (chunksBeingGenerated.contains(chunkKey)) {
            return;
        }

        ChunkBase existingChunk = loadedChunks.get(chunkKey);
        if (existingChunk != null && lod.currentLOD == newLOD) {
            return;
        }

        chunksBeingGenerated.add(chunkKey);
        CompletableFuture.supplyAsync(() -> {
            try {
                return new OptimizedChunk(lod.chunkX, lod.chunkZ, registry, sharedBruh, dimensionCreator, currentDimension, newLOD);
            } catch (Exception e) {
                LoggerHelper.betterPrint("Error generating chunk (" + lod.chunkX + "," + lod.chunkZ + "): " +
                        e.getMessage(), LoggerHelper.LogType.ERROR);
                return null;
            } finally {
                chunksBeingGenerated.remove(chunkKey);
            }
        }, chunkExecutor).thenAccept(chunk -> {
            if (chunk != null && chunk.isMeshDataPrepared()) {
                chunksNeedingGLBuffers.offer(chunk);
                lod.currentLOD = newLOD;
            }
        });
    }

    /**
     * Process chunks waiting for GL buffer creation (main thread only)
     */
    private void processGLBufferQueue() {
        int processed = 0;
        while (!chunksNeedingGLBuffers.isEmpty() && processed < MAX_CHUNKS_PER_FRAME) {
            ChunkBase chunk = chunksNeedingGLBuffers.poll();
            if (chunk != null) {
                try {
                    chunk.buildGLBuffers();
                    if (chunk.isMeshBuilt()) {
                        long chunkKey = getChunkKey(chunk.getChunkX(), chunk.getChunkZ());
                        loadedChunks.put(chunkKey, chunk);
                        processed++;
                    }
                } catch (Exception e) {
                    LoggerHelper.betterPrint("Error creating GL buffers: " + e.getMessage(), LoggerHelper.LogType.ERROR);
                }
            }
        }

        if (processed > 0) {
            LoggerHelper.betterPrint("Processed " + processed + " chunks for GL buffers", LoggerHelper.LogType.RENDERING);
        }
    }

    /**
     * Clean up chunks that are too far away
     */
    private void cleanupDistantChunks(int playerChunkX, int playerChunkZ) {
        Iterator<Map.Entry<Long, ChunkLOD>> lodIterator = chunkLODs.entrySet().iterator();
        while (lodIterator.hasNext()) {
            Map.Entry<Long, ChunkLOD> entry = lodIterator.next();
            long chunkKey = entry.getKey();
            ChunkLOD lod = entry.getValue();

            int distance = Math.max(Math.abs(lod.chunkX - playerChunkX),
                    Math.abs(lod.chunkZ - playerChunkZ));

            if (distance > renderDistance + 2) {
                unloadChunk(chunkKey);
                lodIterator.remove();
            }
        }
    }

    /**
     * Unload a chunk and free its resources
     */
    private void unloadChunk(long chunkKey) {
        ChunkBase chunk = loadedChunks.remove(chunkKey);
        if (chunk != null) {
            chunk.cleanup();
        }
    }

    /**
     * Calculate distance between player and chunk center
     */
    private float calculateChunkDistance(Vector3f playerPos, Vector3f chunkPos) {
        Vector3f chunkCenter = new Vector3f(
                chunkPos.x + ChunkBase.SIZE_X * 0.5f,
                playerPos.y, // Use player Y for distance calc
                chunkPos.z + ChunkBase.SIZE_Z * 0.5f
        );
        return playerPos.distance(chunkCenter);
    }

    @Override
    public Map<Long, ChunkBase> getLoadedChunks() {
        return loadedChunks;
    }

    /**
     * Get only chunks that are visible for rendering
     */
    public Map<Long, ChunkBase> getVisibleChunks() {
        Map<Long, ChunkBase> visible = new HashMap<>();
        for (Map.Entry<Long, ChunkBase> entry : loadedChunks.entrySet()) {
            long key = entry.getKey();
            ChunkLOD lod = chunkLODs.get(key);
            if (lod != null && lod.inFrustum && lod.currentLOD != LODLevel.UNLOADED) {
                visible.put(key, entry.getValue());
            }
        }
        return visible;
    }

    @Override
    public ChunkBase getChunkAt(int worldX, int worldZ) {
        int chunkX = getChunkCoord(worldX);
        int chunkZ = getChunkCoord(worldZ);
        return loadedChunks.get(getChunkKey(chunkX, chunkZ));
    }

    @Override
    public int getBlockAt(int worldX, int worldY, int worldZ) {
        ChunkBase chunk = getChunkAt(worldX, worldZ);
        return chunk != null ? chunk.getBlockWorld(worldX, worldY, worldZ) : 0;
    }

    @Override
    public void setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        ChunkBase chunk = getChunkAt(worldX, worldZ);
        if (chunk != null) {
            int chunkX = getChunkCoord(worldX);
            int chunkZ = getChunkCoord(worldZ);
            int localX = worldX - (chunkX * ChunkBase.SIZE_X);
            int localZ = worldZ - (chunkZ * ChunkBase.SIZE_Z);

            if (isValidLocalCoord(localX, worldY, localZ)) {
                chunk.setBlock(localX, worldY, localZ, blockId);

                // Trigger mesh rebuild
                long chunkKey = getChunkKey(chunkX, chunkZ);
                ChunkLOD lod = chunkLODs.get(chunkKey);
                if (lod != null) {
                    updateChunkLOD(chunkKey, lod, lod.currentLOD);
                    rebuildNeighborChunksIfNeeded(worldX, worldY, worldZ, chunkX, chunkZ);
                }
            }
        }
    }

    public boolean isValidLocalCoord(int localX, int y, int localZ) {
        return localX >= 0 && localX < ChunkBase.SIZE_X &&
                localZ >= 0 && localZ < ChunkBase.SIZE_Z &&
                y >= 0 && y < ChunkBase.SIZE_Y;
    }

    public static boolean isStaticValidLocalCoord(int localX, int y, int localZ) {
        return localX >= 0 && localX < ChunkBase.SIZE_X &&
                localZ >= 0 && localZ < ChunkBase.SIZE_Z &&
                y >= 0 && y < ChunkBase.SIZE_Y;
    }

    /**
     * Rebuild neighboring chunks if block is on boundary
     */
    private void rebuildNeighborChunksIfNeeded(int worldX, int worldY, int worldZ, int chunkX, int chunkZ) {
        int localX = worldX - (chunkX * ChunkBase.SIZE_X);
        int localZ = worldZ - (chunkZ * ChunkBase.SIZE_Z);

        if (localX == 0) {
            long westKey = getChunkKey(chunkX - 1, chunkZ);
            ChunkLOD westLOD = chunkLODs.get(westKey);
            if (westLOD != null) updateChunkLOD(westKey, westLOD, westLOD.currentLOD);
        }
        if (localX == ChunkBase.SIZE_X - 1) {
            long eastKey = getChunkKey(chunkX + 1, chunkZ);
            ChunkLOD eastLOD = chunkLODs.get(eastKey);
            if (eastLOD != null) updateChunkLOD(eastKey, eastLOD, eastLOD.currentLOD);
        }
        if (localZ == 0) {
            long northKey = getChunkKey(chunkX, chunkZ - 1);
            ChunkLOD northLOD = chunkLODs.get(northKey);
            if (northLOD != null) updateChunkLOD(northKey, northLOD, northLOD.currentLOD);
        }
        if (localZ == ChunkBase.SIZE_Z - 1) {
            long southKey = getChunkKey(chunkX, chunkZ + 1);
            ChunkLOD southLOD = chunkLODs.get(southKey);
            if (southLOD != null) updateChunkLOD(southKey, southLOD, southLOD.currentLOD);
        }
    }

    @Override
    public void cleanup() {
        // Shutdown executor
        chunkExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                chunkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkExecutor.shutdownNow();
        }

        // Clean up all chunks
        for (ChunkBase chunk : loadedChunks.values()) {
            chunk.cleanup();
        }
        loadedChunks.clear();
        chunkLODs.clear();
        chunksNeedingGLBuffers.clear();
        chunksBeingGenerated.clear();

        LoggerHelper.betterPrint("Optimized Chunk Manager cleaned up", LoggerHelper.LogType.INFO);
    }

    @Override
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }

    public int getChunksInQueue() {
        return chunksNeedingGLBuffers.size();
    }

    public int getChunksBeingGenerated() {
        return chunksBeingGenerated.size();
    }

    /**
     * Get performance statistics
     */
    public String getPerformanceStats() {
        int totalChunks = chunkLODs.size();
        int loadedChunks = this.loadedChunks.size();
        int inFrustum = 0;
        int high = 0, medium = 0, low = 0, unloaded = 0;

        for (ChunkLOD lod : chunkLODs.values()) {
            if (lod.inFrustum) inFrustum++;
            switch (lod.currentLOD) {
                case HIGH: high++; break;
                case MEDIUM: medium++; break;
                case LOW: low++; break;
                case UNLOADED: unloaded++; break;
            }
        }

        return String.format("Chunks: %d total, %d loaded, %d visible | LOD: %d high, %d med, %d low, %d unloaded | Queue: %d, Generating: %d",
                totalChunks, loadedChunks, inFrustum, high, medium, low, unloaded,
                chunksNeedingGLBuffers.size(), chunksBeingGenerated.size());
    }

    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private int getChunkCoord(float worldCoord) {
        return Math.floorDiv((int)worldCoord, ChunkBase.SIZE_X);
    }

    private int getChunkCoord(int worldCoord) {
        return Math.floorDiv(worldCoord, ChunkBase.SIZE_X);
    }
}
