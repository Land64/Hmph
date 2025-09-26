package hmph.rendering.world;

import hmph.math.Vector3f;
import hmph.rendering.BlockRegistry;
import hmph.rendering.world.dimensions.DimensionCreator;
import hmph.math.PerlinNoise;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

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

    public ChunkManagerExtension(BlockRegistry registry, int renderDistance) {
        super(registry, renderDistance);
        this.registry = registry;
        this.renderDistance = renderDistance;
        this.dimensionCreator = new DimensionCreator(registry);
    }

    /**
     * Update chunks with two-phase async loading
     * Phase 1: Generate terrain and prepare mesh data in background threads
     * Phase 2: Create OpenGL buffers on main thread
     */
    @Override
    public void updateChunks(Vector3f playerPosition) {
        int playerChunkX = getChunkCoord(playerPosition.x);
        int playerChunkZ = getChunkCoord(playerPosition.z);

        if (playerChunkX != (int)lastPlayerChunkPos.x || playerChunkZ != (int)lastPlayerChunkPos.z) {
            lastPlayerChunkPos.set(playerChunkX, 0, playerChunkZ);

            // Phase 1: Start async chunk generation for new chunks
            for (int x = playerChunkX - renderDistance; x <= playerChunkX + renderDistance; x++) {
                for (int z = playerChunkZ - renderDistance; z <= playerChunkZ + renderDistance; z++) {
                    long key = getChunkKey(x, z);
                    if (!loadedChunks.containsKey(key)) {
                        generateChunkAsync(x, z, key);
                    }
                }
            }

            // Clean up distant chunks
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

        // Phase 2: Process chunks that need GL buffer creation on main thread
        processChunksNeedingGLBuffers();
    }

    /**
     * Generate chunk data asynchronously - no OpenGL calls here
     */
    private void generateChunkAsync(int chunkX, int chunkZ, long chunkKey) {
        CompletableFuture.supplyAsync(() -> {
            try {
                ChunkBase chunk = new ChunkBase(chunkX, chunkZ, registry, sharedBruh,
                        dimensionCreator, currentDimension);
                chunk.setChunkManager(this);
                return chunk;
            } catch (Exception e) {
                System.err.println("Error generating chunk (" + chunkX + "," + chunkZ + "): " +
                        e.getMessage());
                return null;
            }
        }, chunkExecutor).thenAccept(chunk -> {
            if (chunk != null && chunk.isMeshDataPrepared()) {
                // Add to queue for GL buffer creation on main thread
                synchronized (chunksNeedingGLBuffers) {
                    chunksNeedingGLBuffers.add(chunk);
                }
            }
        });
    }

    /**
     * Process chunks that need GL buffer creation - called on main thread
     * This handles the OpenGL context-dependent operations
     */
    private void processChunksNeedingGLBuffers() {
        List<ChunkBase> chunksToProcess;

        synchronized (chunksNeedingGLBuffers) {
            if (chunksNeedingGLBuffers.isEmpty()) {
                return;
            }

            // Process up to 4 chunks per frame to avoid frame drops
            int processCount = Math.min(4, chunksNeedingGLBuffers.size());
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
                System.err.println("Error creating GL buffers for chunk (" +
                        chunk.getChunkX() + "," + chunk.getChunkZ() + "): " + e.getMessage());
            }
        }
    }

    /**
     * Set a block and handle mesh rebuilding efficiently
     */
    public void setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        int chunkX = getChunkCoord(worldX);
        int chunkZ = getChunkCoord(worldZ);

        long chunkKey = getChunkKey(chunkX, chunkZ);
        ChunkBase chunk = loadedChunks.get(chunkKey);

        if (chunk != null) {
            int localX = worldX - (chunkX * ChunkBase.SIZE_X);
            int localZ = worldZ - (chunkZ * ChunkBase.SIZE_Z);

            if (localX >= 0 && localX < ChunkBase.SIZE_X &&
                    localZ >= 0 && localZ < ChunkBase.SIZE_Z &&
                    worldY >= 0 && worldY < ChunkBase.SIZE_Y) {

                chunk.setBlock(localX, worldY, localZ, blockId);

                // Rebuild mesh asynchronously for data preparation
                CompletableFuture.runAsync(() -> {
                    chunk.rebuildMesh();

                    // Queue for GL buffer creation on main thread
                    synchronized (chunksNeedingGLBuffers) {
                        if (chunk.isMeshDataPrepared()) {
                            chunksNeedingGLBuffers.add(chunk);
                        }
                    }
                }, chunkExecutor);

                // Handle neighbor chunks if on boundary
                rebuildNeighborChunksIfNeeded(worldX, worldY, worldZ, chunkX, chunkZ);
            }
        }
    }

    /**
     * Rebuild neighboring chunks if the block change is on a chunk boundary
     */
    private void rebuildNeighborChunksIfNeeded(int worldX, int worldY, int worldZ,
                                               int chunkX, int chunkZ) {
        int localX = worldX - (chunkX * ChunkBase.SIZE_X);
        int localZ = worldZ - (chunkZ * ChunkBase.SIZE_Z);

        if (localX == 0) {
            ChunkBase westChunk = loadedChunks.get(getChunkKey(chunkX - 1, chunkZ));
            if (westChunk != null) {
                rebuildChunkAsync(westChunk);
            }
        }
        if (localX == ChunkBase.SIZE_X - 1) {
            ChunkBase eastChunk = loadedChunks.get(getChunkKey(chunkX + 1, chunkZ));
            if (eastChunk != null) {
                rebuildChunkAsync(eastChunk);
            }
        }
        if (localZ == 0) {
            ChunkBase northChunk = loadedChunks.get(getChunkKey(chunkX, chunkZ - 1));
            if (northChunk != null) {
                rebuildChunkAsync(northChunk);
            }
        }
        if (localZ == ChunkBase.SIZE_Z - 1) {
            ChunkBase southChunk = loadedChunks.get(getChunkKey(chunkX, chunkZ + 1));
            if (southChunk != null) {
                rebuildChunkAsync(southChunk);
            }
        }
    }

    /**
     * Rebuild a chunk asynchronously and queue it for GL buffer creation
     */
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
    public Map<Long, ChunkBase> getLoadedChunks() {
        return loadedChunks;
    }

    /**
     * Get chunk at world coordinates for block lookups
     */
    public ChunkBase getChunkAt(int worldX, int worldZ) {
        int chunkX = getChunkCoord(worldX);
        int chunkZ = getChunkCoord(worldZ);
        return loadedChunks.get(getChunkKey(chunkX, chunkZ));
    }

    /**
     * Clean up all resources including the thread pool
     */
    @Override
    public void cleanup() {
        // Shutdown the executor service first
        chunkExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
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

        synchronized (chunksNeedingGLBuffers) {
            chunksNeedingGLBuffers.clear();
        }
    }

    @Override
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

    @Override
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }

    /**
     * Get number of chunks waiting for GL buffer creation
     */
    public int getChunksAwaitingGLBuffers() {
        synchronized (chunksNeedingGLBuffers) {
            return chunksNeedingGLBuffers.size();
        }
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

    /**
     * Switch to a different dimension and clear existing chunks
     */
    public void switchDimension(String dimensionName) {
        if (!dimensionCreator.getAvailableDimensions().contains(dimensionName)) return;

        cleanup();
        loadedChunks = new ConcurrentHashMap<>();
        synchronized (chunksNeedingGLBuffers) {
            chunksNeedingGLBuffers.clear();
        }
        currentDimension = dimensionName;
        lastPlayerChunkPos.set(-999, 0, -999);
    }

    public String getCurrentDimension() {
        return currentDimension;
    }

    public String getCurrentBiome() {
        return dimensionCreator.getCurrentBiome((int)lastPlayerChunkPos.x, (int)lastPlayerChunkPos.z);
    }

    public DimensionCreator getDimensionCreator() {
        return dimensionCreator;
    }
}