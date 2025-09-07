package hmph.rendering.world;

import hmph.math.Vector3f;
import hmph.math.PerlinNoise;
import hmph.rendering.shapes.BlockMesh;
import hmph.rendering.BlockRegistry;
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
    private final int[][][] blocks = new int[SIZE_X][SIZE_Y][SIZE_Z];
    private int vao = 0, vbo = 0, ebo = 0, indexCount = 0;
    private final Vector3f position;
    private final PerlinNoise perlin;
    private final BlockRegistry registry;
    private boolean meshBuilt = false;

    public ChunkBase(int chunkX, int chunkZ, BlockRegistry registry) {
        this.position = new Vector3f(chunkX * SIZE_X, 0, chunkZ * SIZE_Z);
        this.perlin = new PerlinNoise();
        this.registry = registry;
        System.out.println("Creating chunk at (" + chunkX + ", " + chunkZ + ") world pos: " + position);
        generateTerrain();
        buildMesh();
    }

    private void generateTerrain() {
        double scale = 0.05;
        int maxHeight = 20;
        int blocksGenerated = 0;
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                double noiseValue = perlin.noise((position.x + x) * scale, 0, (position.z + z) * scale);
                int height = (int) (noiseValue * maxHeight);
                height = Math.max(1, Math.min(height, SIZE_Y - 1));
                for (int y = 0; y <= height; y++) {
                    blocks[x][y][z] = 1;
                    blocksGenerated++;
                }
            }
        }
        System.out.println("Generated " + blocksGenerated + " blocks in chunk");
    }

    private void buildMesh() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int facesAdded = 0;
        System.out.println("Building mesh for chunk at " + position);
        String[][][] blockNames = convertIDsToNames(blocks);
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    if (blocks[x][y][z] != 0) {
                        String blockName = registry.getNameFromID(blocks[x][y][z]);
                        if (blockName != null) {
                            int faces = BlockMesh.addBlockMesh(x, y, z, blockNames, vertices, indices, registry);
                            facesAdded += faces;
                        }
                    }
                }
            }
        }
        System.out.println("Mesh built: " + vertices.size() + " vertices, " + indices.size() + " indices, " + facesAdded + " faces");

        indexCount = indices.size();
        if (indexCount == 0) {
            System.out.println("Warning: No geometry generated for chunk at " + position);
            return;
        }
        try {
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
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);
            glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            meshBuilt = true;
            System.out.println("Successfully created VAO: " + vao + " with " + indexCount + " indices");
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                System.err.println("OpenGL error during mesh creation: " + error);
            }
        } catch (Exception e) {
            System.err.println("Error creating mesh: " + e.getMessage());
            e.printStackTrace();
            cleanup();
        }
    }

    private String[][][] convertIDsToNames(int[][][] intBlocks) {
        String[][][] names = new String[SIZE_X][SIZE_Y][SIZE_Z];
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    if (intBlocks[x][y][z] != 0) {
                        names[x][y][z] = registry.getNameFromID(intBlocks[x][y][z]);
                    } else {
                        names[x][y][z] = null;
                    }
                }
            }
        }
        return names;
    }

    public void render(hmph.rendering.shaders.ShaderProgram shader) {
        if (!meshBuilt || vao == 0 || indexCount == 0) {
            return;
        }

        try {
            shader.bind();
            hmph.math.Matrix4f modelMatrix = new hmph.math.Matrix4f().translate((float)position.x, (float)position.y, (float)position.z);
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

    public Vector3f getPosition() { return position; }
    public int getVao() { return vao; }
    public int getIndexCount() { return indexCount; }
    public boolean isMeshBuilt() { return meshBuilt; }

    public void setBlock(int x, int y, int z, int id) {
        if (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z) {
            blocks[x][y][z] = id;
        }
    }

    public int getBlock(int x, int y, int z) {
        if (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z) {
            return blocks[x][y][z];
        }
        return 0;
    }
}