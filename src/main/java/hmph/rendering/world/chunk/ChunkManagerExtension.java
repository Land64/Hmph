package hmph.rendering.world.chunk;

import hmph.math.Vector3f;
import hmph.rendering.BlockRegistry;
import hmph.rendering.camera.Camera;
import hmph.rendering.world.chunk.optimized.OptimizedChunk;
import hmph.rendering.world.chunk.optimized.OptimizedChunkManager;
import hmph.rendering.world.dimensions.DimensionCreator;
import hmph.math.PerlinNoise;
import hmph.util.debug.LoggerHelper;

import java.util.*;
import java.util.concurrent.*;

public class ChunkManagerExtension extends ChunkManager {
    private DimensionCreator dimensionCreator;
    private String currentDimension = "overworld";
    private Map<Long, ChunkBase> loadedChunks = new ConcurrentHashMap<>();
    private BlockRegistry registry;
    private int renderDistance;
    private Vector3f lastPlayerChunkPos = new Vector3f();
    private PerlinNoise sharedBruh = new PerlinNoise();

    // Thread pool for async chunk generation
    private ExecutorService chunkExecutor = Executors.newFixedThreadPool(4);

    // Chunks that have been prepared but need GL buffers created
    private List<ChunkBase> chunksNeedingGLBuffers = new ArrayList<>();

    // LOD and culling optimization additions
    private final Map<Long, ChunkLOD> chunkLODs = new ConcurrentHashMap<>();
    private final Set<Long> chunksBeingGenerated = ConcurrentHashMap.newKeySet();

    // LOD Settings
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
        LODLevel(int factor) { this.simplificationFactor = factor; }
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

    public ChunkManagerExtension(BlockRegistry registry, int renderDistance) {
        super(registry, renderDistance);
        this.registry = registry;
        this.renderDistance = renderDistance;
        this.dimensionCreator = new DimensionCreator(registry);
        LoggerHelper.betterPrint("ChunkManagerExtension initialized with optimizations", LoggerHelper.LogType.RENDERING);
    }

    /**
     * Update chunks with camera for frustum culling
     */
    public void updateChunksWithCamera(Vector3f playerPosition, Camera camera) {
        int playerChunkX = getChunkCoord(playerPosition.x);
        int playerChunkZ = getChunkCoord(playerPosition.z);

        boolean playerMoved = playerChunkX != (int)lastPlayerChunkPos.x ||
                playerChunkZ != (int)lastPlayerChunkPos.z;

        if (playerMoved) {
            lastPlayerChunkPos.set(playerChunkX, 0, playerChunkZ);
        }

        // Update frustum if camera available
        if (camera != null) {
            camera.updateFrustum(16f/9f, 0.1f, renderDistance * ChunkBase.SIZE_X * 1.5f);
        }

        // Update chunk LODs and visibility
        updateChunkLODs(playerPosition, camera, playerMoved);

        // Process chunks that need GL buffer creation
        processChunksNeedingGLBuffers();

        // Clean up distant chunks periodically
        if (playerMoved) {
            cleanupDistantChunks(playerChunkX, playerChunkZ);
        }
    }

    /**
     * Original updateChunks method - now calls the camera version without camera
     */
    @Override
    public void updateChunks(Vector3f playerPosition) {
        updateChunksWithCamera(playerPosition, null);
    }

    /**
     * Update LOD levels and generate/unload chunks as needed
     */
    private void updateChunkLODs(Vector3f playerPos, Camera camera, boolean forceUpdate) {
        int playerChunkX = getChunkCoord(playerPos.x);
        int playerChunkZ = getChunkCoord(playerPos.z);

        int totalChunks = 0;
        int culledChunks = 0;
        int visibleChunks = 0;

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                long chunkKey = getChunkKey(chunkX, chunkZ);
                totalChunks++;

                ChunkLOD lod = chunkLODs.computeIfAbsent(chunkKey, k -> new ChunkLOD(chunkX, chunkZ));

                float distance = calculateChunkDistance(playerPos, lod.position);
                LODLevel requiredLOD = calculateRequiredLOD(distance);

                if (camera != null) {
                    lod.inFrustum = camera.isChunkVisible(lod.position, ChunkBase.SIZE_X);
                    if (!lod.inFrustum) {
                        culledChunks++;
                    } else {
                        visibleChunks++;
                    }
                } else {
                    lod.inFrustum = true;
                    visibleChunks++;
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

        if (System.currentTimeMillis() % 3000 < 50) {
            System.out.println("FRUSTUM DEBUG: Total=" + totalChunks + " Visible=" + visibleChunks + " Culled=" + culledChunks);
        }
    }

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

        // Generate new chunk asynchronously
        chunksBeingGenerated.add(chunkKey);
        CompletableFuture.supplyAsync(() -> {
            try {
                return new ChunkBase(lod.chunkX, lod.chunkZ, registry, sharedBruh,
                        dimensionCreator, currentDimension);
            } catch (Exception e) {
                LoggerHelper.betterPrint("Error generating chunk (" + lod.chunkX + "," + lod.chunkZ + "): " +
                        e.getMessage(), LoggerHelper.LogType.ERROR);
                return null;
            } finally {
                chunksBeingGenerated.remove(chunkKey);
            }
        }, chunkExecutor).thenAccept(chunk -> {
            if (chunk != null && chunk.isMeshDataPrepared()) {
                synchronized (chunksNeedingGLBuffers) {
                    chunksNeedingGLBuffers.add(chunk);
                }
                lod.currentLOD = newLOD;
            }
        });
    }

    private void processChunksNeedingGLBuffers() {
        List<ChunkBase> chunksToProcess;

        synchronized (chunksNeedingGLBuffers) {
            if (chunksNeedingGLBuffers.isEmpty()) {
                return;
            }

            int processCount = Math.min(MAX_CHUNKS_PER_FRAME, chunksNeedingGLBuffers.size());
            chunksToProcess = new ArrayList<>(chunksNeedingGLBuffers.subList(0, processCount));
            chunksNeedingGLBuffers.subList(0, processCount).clear();
        }

        for (ChunkBase chunk : chunksToProcess) {
            try {
                chunk.buildGLBuffers();
                if (chunk.isMeshBuilt()) {
                    long chunkKey = getChunkKey(chunk.getChunkX(), chunk.getChunkZ());
                    loadedChunks.put(chunkKey, chunk);
                }
            } catch (Exception e) {
                LoggerHelper.betterPrint("Error creating GL buffers for chunk (" +
                                chunk.getChunkX() + "," + chunk.getChunkZ() + "): " + e.getMessage(),
                        LoggerHelper.LogType.ERROR);
            }
        }
    }

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

    private void unloadChunk(long chunkKey) {
        ChunkBase chunk = loadedChunks.remove(chunkKey);
        if (chunk != null) {
            chunk.cleanup();
        }
    }

    private float calculateChunkDistance(Vector3f playerPos, Vector3f chunkPos) {
        Vector3f chunkCenter = new Vector3f(
                chunkPos.x + ChunkBase.SIZE_X * 0.5f,
                playerPos.y,
                chunkPos.z + ChunkBase.SIZE_Z * 0.5f
        );
        return playerPos.distance(chunkCenter);
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
    public Map<Long, ChunkBase> getLoadedChunks() {
        return loadedChunks;
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

    public void setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        ChunkBase chunk = getChunkAt(worldX, worldZ);
        if (chunk != null) {
            int chunkX = getChunkCoord(worldX);
            int chunkZ = getChunkCoord(worldZ);
            int localX = worldX - (chunkX * ChunkBase.SIZE_X);
            int localZ = worldZ - (chunkZ * ChunkBase.SIZE_Z);

            if (localX >= 0 && localX < ChunkBase.SIZE_X &&
                    localZ >= 0 && localZ < ChunkBase.SIZE_Z &&
                    worldY >= 0 && worldY < ChunkBase.SIZE_Y) {

                chunk.setBlock(localX, worldY, localZ, blockId);

                CompletableFuture.runAsync(() -> {
                    chunk.rebuildMesh();
                    synchronized (chunksNeedingGLBuffers) {
                        if (chunk.isMeshDataPrepared()) {
                            chunksNeedingGLBuffers.add(chunk);
                        }
                    }
                }, chunkExecutor);

                rebuildNeighborChunksIfNeeded(worldX, worldY, worldZ, chunkX, chunkZ);
            }
        }
    }

    private void rebuildNeighborChunksIfNeeded(int worldX, int worldY, int worldZ,
                                               int chunkX, int chunkZ) {
        int localX = worldX - (chunkX * ChunkBase.SIZE_X);
        int localZ = worldZ - (chunkZ * ChunkBase.SIZE_Z);

        if (localX == 0) {
            ChunkBase westChunk = loadedChunks.get(getChunkKey(chunkX - 1, chunkZ));
            if (westChunk != null) rebuildChunkAsync(westChunk);
        }
        if (localX == ChunkBase.SIZE_X - 1) {
            ChunkBase eastChunk = loadedChunks.get(getChunkKey(chunkX + 1, chunkZ));
            if (eastChunk != null) rebuildChunkAsync(eastChunk);
        }
        if (localZ == 0) {
            ChunkBase northChunk = loadedChunks.get(getChunkKey(chunkX, chunkZ - 1));
            if (northChunk != null) rebuildChunkAsync(northChunk);
        }
        if (localZ == ChunkBase.SIZE_Z - 1) {
            ChunkBase southChunk = loadedChunks.get(getChunkKey(chunkX, chunkZ + 1));
            if (southChunk != null) rebuildChunkAsync(southChunk);
        }
    }

    private void rebuildChunkAsync(ChunkBase chunk) {
        CompletableFuture.runAsync(() -> {
            chunk.rebuildMesh();
            synchronized (chunksNeedingGLBuffers) {
                if (chunk.isMeshDataPrepared()) {
                    chunksNeedingGLBuffers.add(chunk);
                }
            }
        }, chunkExecutor);
    }

    @Override
    public void cleanup() {
        chunkExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                chunkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkExecutor.shutdownNow();
        }

        for (ChunkBase chunk : loadedChunks.values()) {
            chunk.cleanup();
        }
        loadedChunks.clear();
        chunkLODs.clear();

        synchronized (chunksNeedingGLBuffers) {
            chunksNeedingGLBuffers.clear();
        }
        chunksBeingGenerated.clear();
    }

    @Override
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }

    public int getChunksAwaitingGLBuffers() {
        synchronized (chunksNeedingGLBuffers) {
            return chunksNeedingGLBuffers.size();
        }
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
                getChunksAwaitingGLBuffers(), getChunksBeingGenerated());
    }

    public void switchDimension(String dimensionName) {
        if (!dimensionCreator.getAvailableDimensions().contains(dimensionName)) return;

        cleanup();
        loadedChunks = new ConcurrentHashMap<>();
        chunkLODs.clear();
        synchronized (chunksNeedingGLBuffers) {
            chunksNeedingGLBuffers.clear();
        }
        currentDimension = dimensionName;
        lastPlayerChunkPos.set(-999, 0, -999);
    }

    public String getCurrentDimension() {
        return currentDimension;
    }

    public DimensionCreator getDimensionCreator() {
        return dimensionCreator;
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