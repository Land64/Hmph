package hmph.rendering.world.chunk;

import hmph.math.Vector3f;
import hmph.math.PerlinNoise;
import hmph.rendering.shapes.BlockMesh;
import hmph.rendering.BlockRegistry;
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

public class ChunkBase {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 256;
    public static final int SIZE_Z = 16;
    private static final int MAX_FACES_PER_CHUNK = 65536;

    private final int[][][] blocks = new int[SIZE_X][SIZE_Y][SIZE_Z];
    private int vao = 0, vbo = 0, ebo = 0, indexCount = 0;
    private final Vector3f position;
    private final int chunkX, chunkZ;
    private final PerlinNoise perlin;
    public final BlockRegistry registry;
    private boolean meshBuilt = false;
    private ChunkManager chunkManager;

    // Prepared mesh data that can be generated on any thread
    public List<Float> preparedVertices = null;
    public List<Integer> preparedIndices = null;
    public boolean meshDataPrepared = false;

    private static final int AIR = 0;
    private static final int STONE = 1;
    private static final int DIRT = 2;
    private static final int GRASS = 3;

    public ChunkBase(int chunkX, int chunkZ, BlockRegistry registry, PerlinNoise perlin) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.position = new Vector3f(chunkX * SIZE_X, 0, chunkZ * SIZE_Z);
        this.perlin = perlin;
        this.registry = registry;

        long startTime = System.nanoTime();

        DimensionCreator defaultDim = new DimensionCreator(registry);
        generateTerrainWithDimension(defaultDim, "overworld");

        long terrainTime = System.nanoTime();
        prepareMeshData(); // This can be done on any thread
        long meshTime = System.nanoTime();

    }

    public ChunkBase(int chunkX, int chunkZ, BlockRegistry registry, PerlinNoise perlin,
                     DimensionCreator dimensionCreator, String dimensionName) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.position = new Vector3f(chunkX * SIZE_X, 0, chunkZ * SIZE_Z);
        this.perlin = perlin;
        this.registry = registry;

        long startTime = System.nanoTime();

        generateTerrainWithDimension(dimensionCreator, dimensionName);

        long terrainTime = System.nanoTime();
        prepareMeshData();
        long meshTime = System.nanoTime();


    }

    public void setChunkManager(ChunkManager manager) {
        this.chunkManager = manager;
    }

    /**
     * Generate terrain using basic height-based algorithm
     * Creates grass on top, dirt underneath, and stone at the bottom
     */
    private void generateTerrain() {
        double scale = 0.05;
        int maxHeight = 30;
        int seaLevel = 10;
        int blocksGenerated = 0;
        int grassCount = 0, dirtCount = 0, stoneCount = 0;

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                double worldX = (position.x + x) * scale;
                double worldZ = (position.z + z) * scale;

                double noiseValue = perlin.noise(worldX, 0, worldZ);
                int height = (int) ((noiseValue * 0.5 + 0.5) * maxHeight);
                height = Math.max(seaLevel, Math.min(height, SIZE_Y - 1));

                for (int y = 0; y <= height; y++) {
                    if (y == height && height >= seaLevel) {
                        blocks[x][y][z] = GRASS;
                        grassCount++;
                    } else if (y >= height - 3 && y < height && height >= seaLevel) {
                        blocks[x][y][z] = DIRT;
                        dirtCount++;
                    } else {
                        blocks[x][y][z] = STONE;
                        stoneCount++;
                    }
                    blocksGenerated++;
                }
            }
        }

        LoggerHelper.betterPrint("Generated " + blocksGenerated + " blocks in chunk: " +
                grassCount + " grass, " + dirtCount + " dirt, " +
                stoneCount + " stone", LoggerHelper.LogType.RENDERING);
    }

    /**
     * Generate terrain using dimension creator for advanced biome-based generation
     */
    private void generateTerrainWithDimension(DimensionCreator dimensionCreator, String dimensionName) {
        DimensionCreator.TerrainData terrainData = dimensionCreator.generateTerrain(
                dimensionName, chunkX, chunkZ, perlin);

        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    blocks[x][y][z] = terrainData.blocks[x][y][z];
                }
            }
        }
    }

    /**
     * Prepare mesh data on any thread - no OpenGL calls here
     * This separates CPU-intensive mesh preparation from OpenGL buffer creation
     */
    private void prepareMeshData() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int facesAdded = 0;

        String[][][] blockNames = convertIDsToNames(blocks);

        for (int x = 0; x < SIZE_X && facesAdded < MAX_FACES_PER_CHUNK; x++) {
            for (int y = 0; y < SIZE_Y && facesAdded < MAX_FACES_PER_CHUNK; y++) {
                for (int z = 0; z < SIZE_Z && facesAdded < MAX_FACES_PER_CHUNK; z++) {
                    if (blocks[x][y][z] != AIR) {
                        String blockName = registry.getNameFromID(blocks[x][y][z]);
                        if (blockName != null) {
                            int faces = BlockMesh.addBlockMesh(x, y, z, blockNames,
                                    vertices, indices, registry);
                            facesAdded += faces;
                        }
                    }
                }
            }
        }

        if (facesAdded >= MAX_FACES_PER_CHUNK) {
            LoggerHelper.betterPrint("Chunk mesh complexity limited at " +
                    MAX_FACES_PER_CHUNK + " faces", LoggerHelper.LogType.WARNING);
        }

        this.preparedVertices = vertices;
        this.preparedIndices = indices;
        this.meshDataPrepared = true;
    }

    /**
     * Build OpenGL buffers from prepared data - MUST be called on main thread
     * This creates the actual GPU resources using the prepared CPU data
     */
    public void buildGLBuffers() {
        if (!meshDataPrepared || preparedVertices == null || preparedIndices == null) {
            return;
        }

        if (vao != 0 || vbo != 0 || ebo != 0) {
            cleanup();
        }

        indexCount = preparedIndices.size();
        if (indexCount == 0) {
            return;
        }

        try {
            createGLBuffers(preparedVertices, preparedIndices);
            meshBuilt = true;

            preparedVertices = null;
            preparedIndices = null;

            int error = glGetError();
            if (error != GL_NO_ERROR) {
                System.err.println("OpenGL error during mesh creation: " + error);
            }
        } catch (Exception e) {
            System.err.println("Error creating GL buffers: " + e.getMessage());
            e.printStackTrace();
            cleanup();
        }
    }

    /**
     * Create OpenGL buffers for the mesh data - MUST be called on main thread
     */
    private void createGLBuffers(List<Float> vertices, List<Integer> indices) {
        vao = glGenVertexArrays();
        if (vao == 0) {
            throw new RuntimeException("Failed to generate VAO");
        }
        glBindVertexArray(vao);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.size());
        for (float f : vertices) {
            vertexBuffer.put(f);
        }
        vertexBuffer.flip();

        vbo = glGenBuffers();
        if (vbo == 0) {
            throw new RuntimeException("Failed to generate VBO");
        }
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.size());
        for (int i : indices) {
            indexBuffer.put(i);
        }
        indexBuffer.flip();

        ebo = glGenBuffers();
        if (ebo == 0) {
            throw new RuntimeException("Failed to generate EBO");
        }
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // UV attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Normal attribute
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 9 * Float.BYTES, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Texture ID attribute
        glVertexAttribPointer(3, 1, GL_FLOAT, false, 9 * Float.BYTES, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * Convert block IDs to names for mesh generation
     */
    public String[][][] convertIDsToNames(int[][][] intBlocks) {
        String[][][] names = new String[SIZE_X][SIZE_Y][SIZE_Z];
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    if (intBlocks[x][y][z] != AIR) {
                        names[x][y][z] = registry.getNameFromID(intBlocks[x][y][z]);
                    } else {
                        names[x][y][z] = null;
                    }
                }
            }
        }
        return names;
    }

    /**
     * Get block at world coordinates
     * Handles coordinate conversion from world space to chunk-local space
     */
    public int getBlockWorld(int worldX, int worldY, int worldZ) {
        int localX = worldX - (int)position.x;
        int localZ = worldZ - (int)position.z;

        if (localX >= 0 && localX < SIZE_X && localZ >= 0 && localZ < SIZE_Z &&
                worldY >= 0 && worldY < SIZE_Y) {
            return blocks[localX][worldY][localZ];
        }

        if (chunkManager != null) {
            ChunkBase neighborChunk = chunkManager.getChunkAt(worldX, worldZ);
            if (neighborChunk != null && neighborChunk != this) {
                return neighborChunk.getBlockWorld(worldX, worldY, worldZ);
            }
        }

        return AIR;
    }

    /**
     * Render the chunk using the provided shader
     */
    public void render(hmph.rendering.shaders.ShaderProgram shader) {
        if (!meshBuilt || vao == 0 || indexCount == 0) {
            return;
        }

        try {
            shader.bind();
            hmph.math.Matrix4f modelMatrix = new hmph.math.Matrix4f().translate(
                    (float)position.x, (float)position.y, (float)position.z);
            shader.setUniform("model", modelMatrix);
            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        } catch (Exception e) {
            System.err.println("Error in ChunkBase.render(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            shader.unbind();
        }
    }

    /**
     * Clean up OpenGL resources
     */
    public void cleanup() {
        if (ebo != 0) {
            glDeleteBuffers(ebo);
            ebo = 0;
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
        meshBuilt = false;
    }

    /**
     * Rebuild the mesh after block changes
     */
    public void rebuildMesh() {
        meshBuilt = false;
        prepareMeshData();
    }

    // Getters
    public Vector3f getPosition() { return position; }
    public int getVao() { return vao; }
    public int getIndexCount() { return indexCount; }
    public boolean isMeshBuilt() { return meshBuilt; }
    public boolean isMeshDataPrepared() { return meshDataPrepared; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }

    /**
     * Set block at local chunk coordinates
     */
    public void setBlock(int x, int y, int z, int id) {
        if (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z) {
            blocks[x][y][z] = id;
        }
    }

    /**
     * Get block at local chunk coordinates
     */
    public int getBlock(int x, int y, int z) {
        if (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z) {
            return blocks[x][y][z];
        }
        return AIR;
    }
}