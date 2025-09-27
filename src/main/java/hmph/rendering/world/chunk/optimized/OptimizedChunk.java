package hmph.rendering.world.chunk.optimized;

import hmph.math.Vector3f;
import hmph.math.PerlinNoise;
import hmph.rendering.shapes.BlockMesh;
import hmph.rendering.BlockRegistry;
import hmph.rendering.world.chunk.ChunkBase;
import hmph.rendering.world.dimensions.DimensionCreator;
import hmph.util.debug.LoggerHelper;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class OptimizedChunk extends ChunkBase {
    private OptimizedChunkManager.LODLevel lodLevel;
    private int facesSkipped = 0;
    private long meshGenerationTime = 0;

    public OptimizedChunk(int chunkX, int chunkZ, BlockRegistry registry, PerlinNoise perlin, DimensionCreator dimensionCreator, String dimensionName, OptimizedChunkManager.LODLevel lod) {
        super(chunkX, chunkZ, registry, perlin, dimensionCreator, dimensionName);
        this.lodLevel = lod;

        prepareLODMeshData();
    }

    /**
     * Prepare mesh data with LOD optimizations
     */
    private void prepareLODMeshData() {
        long startTime = System.nanoTime();

        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int facesAdded = 0;
        facesSkipped = 0;

        String[][][] blockNames = convertIDsToNames(getBlocksArray());

        int step = lodLevel.simplificationFactor;
        if (step == 0) return; 

        for (int x = 0; x < SIZE_X; x += step) {
            for (int y = 0; y < SIZE_Y; y += step) {
                for (int z = 0; z < SIZE_Z; z += step) {
                    if (getBlock(x, y, z) != 0) {
                        String blockName = getRegistry().getNameFromID(getBlock(x, y, z));
                        if (blockName != null) {
                            
                            if (shouldRenderBlock(x, y, z, blockNames)) {
                                int faces = BlockMesh.addBlockMesh(x, y, z, blockNames,
                                        vertices, indices, getRegistry());
                                facesAdded += faces;
                            } else {
                                facesSkipped++;
                            }
                        }
                    }
                }
            }
        }

        
        if (lodLevel == OptimizedChunkManager.LODLevel.MEDIUM) {
            optimizeMeshForMediumLOD(vertices, indices);
        } else if (lodLevel == OptimizedChunkManager.LODLevel.LOW) {
            optimizeMeshForLowLOD(vertices, indices);
        }

        setPreparedMeshData(vertices, indices);

        meshGenerationTime = System.nanoTime() - startTime;

        LoggerHelper.betterPrint(String.format("Chunk (%d,%d) LOD %s: %d faces, %d skipped, %.2fms",
                getChunkX(), getChunkZ(), lodLevel.name(), facesAdded, facesSkipped,
                meshGenerationTime / 1_000_000.0), LoggerHelper.LogType.RENDERING);
    }

    /**
     * Determine if a block should be rendered at this LOD level
     */
    private boolean shouldRenderBlock(int x, int y, int z, String[][][] blockNames) {
        
        if (isBlockExposed(x, y, z, blockNames)) {
            return true;
        }

        
        switch (lodLevel) {
            case HIGH:
                return true; 

            case MEDIUM:
                
                if (y < 32 && !isNearSurface(x, y, z, blockNames)) {
                    return Math.random() > 0.3; 
                }
                return true;

            case LOW:
                
                return y > 16 || isNearSurface(x, y, z, blockNames);

            default:
                return false;
        }
    }

    /**
     * Check if a block is exposed (has at least one air neighbor)
     */
    private boolean isBlockExposed(int x, int y, int z, String[][][] blockNames) {
        int[] dx = {-1, 1, 0, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 0, 0};
        int[] dz = {0, 0, 0, 0, -1, 1};

        for (int i = 0; i < 6; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            int nz = z + dz[i];

            if (isAirOrOutOfBounds(nx, ny, nz, blockNames)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a block is near the surface
     */
    private boolean isNearSurface(int x, int y, int z, String[][][] blockNames) {
        
        for (int dy = 1; dy <= 3; dy++) {
            if (y + dy >= SIZE_Y) return true;
            if (isAirOrOutOfBounds(x, y + dy, z, blockNames)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Further optimize mesh for medium LOD
     */
    private void optimizeMeshForMediumLOD(List<Float> vertices, List<Integer> indices) {
        
        

        if (vertices.size() > 10000) {
            
            for (int i = vertices.size() - 36; i >= 0; i -= 144) { 
                if (Math.random() < 0.25) {
                    for (int j = 0; j < 36; j++) {
                        vertices.remove(i);
                    }
                    
                    for (int j = indices.size() - 1; j >= 0; j--) {
                        int idx = indices.get(j);
                        if (idx >= i / 9 && idx < (i + 36) / 9) {
                            indices.remove(j);
                        }
                    }
                }
            }
        }
    }

    /**
     * Further optimize mesh for low LOD
     */
    private void optimizeMeshForLowLOD(List<Float> vertices, List<Integer> indices) {
        
        

        if (vertices.size() > 5000) {
            
            for (int i = vertices.size() - 36; i >= 0; i -= 72) {
                if (Math.random() < 0.5) {
                    for (int j = 0; j < 36; j++) {
                        if (i + j < vertices.size()) {
                            vertices.remove(i);
                        }
                    }
                    
                    for (int j = indices.size() - 1; j >= 0; j--) {
                        int idx = indices.get(j);
                        if (idx >= i / 9 && idx < (i + 36) / 9) {
                            indices.remove(j);
                        }
                    }
                }
            }
        }
    }

    private boolean isAirOrOutOfBounds(int x, int y, int z, String[][][] blocks) {
        if (x < 0 || y < 0 || z < 0 ||
                x >= blocks.length || y >= blocks[0].length || z >= blocks[0][0].length) {
            return true;
        }
        return blocks[x][y][z] == null;
    }

    
    public OptimizedChunkManager.LODLevel getLODLevel() {
        return lodLevel;
    }

    public int getFacesSkipped() {
        return facesSkipped;
    }

    public long getMeshGenerationTime() {
        return meshGenerationTime;
    }

    public float getMeshComplexity() {
        return getIndexCount() / 6.0f; 
    }

    
    private int[][][] getBlocksArray() {
        
        
        int[][][] blocks = new int[SIZE_X][SIZE_Y][SIZE_Z];
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    blocks[x][y][z] = getBlock(x, y, z);
                }
            }
        }
        return blocks;
    }

    private BlockRegistry getRegistry() {
        
        return registry; 
    }

    private void setPreparedMeshData(List<Float> vertices, List<Integer> indices) {
        this.preparedVertices = vertices;
        this.preparedIndices = indices;
        this.meshDataPrepared = true;
    }
}