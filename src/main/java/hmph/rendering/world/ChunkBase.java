package hmph.rendering.world;

import hmph.math.Vector3f;
import hmph.math.PerlinNoise;
import hmph.rendering.shapes.BlockMesh;
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

    private int vao, vbo, ebo;
    private int indexCount;

    private final Vector3f position;
    private final PerlinNoise perlin;

    public ChunkBase(int chunkX, int chunkZ) {
        this.position = new Vector3f(chunkX * SIZE_X, 0, chunkZ * SIZE_Z);
        this.perlin = new PerlinNoise();

        generateTerrain();
        buildMesh();
    }

    //Hoesntly, again this took goddamn forever to do, but its nice.
    private void generateTerrain() {
        double scale = 0.05;
        int maxHeight = 20;

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                double noiseValue = perlin.noise((position.x + x) * scale, 0, (position.z + z) * scale);
                int height = (int) (noiseValue * maxHeight);
                height = Math.min(height, SIZE_Y - 1);

                for (int y = 0; y <= height; y++) {
                    blocks[x][y][z] = 1;
                }
            }
        }
    }


    private boolean isAir(int x, int y, int z) { return x < 0 || y < 0 || z < 0 || x >= SIZE_X || y >= SIZE_Y || z >= SIZE_Z || blocks[x][y][z] == 0; }

    //OH MY GOD I HATED MAKING THIS, BUT ITS SOOOO USEFUL FOR PERFORMACE
    private void buildMesh() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int x = 0; x < SIZE_X; x++)
            for (int y = 0; y < SIZE_Y; y++)
                for (int z = 0; z < SIZE_Z; z++)
                    if (blocks[x][y][z] != 0)
                        BlockMesh.addBlockMesh(x, y, z, blocks, vertices, indices);

        indexCount = indices.size();
        if (indexCount == 0) return;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.size());
        for (float f : vertices) vertexBuffer.put(f);
        vertexBuffer.flip();

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.size());
        for (int i : indices) indexBuffer.put(i);
        indexBuffer.flip();

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    //Render the chunk
    public void render(hmph.rendering.shaders.ShaderProgram shader) {
        shader.bind();
        shader.setUniform("model", new hmph.math.Matrix4f().translate(position));
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        shader.unbind();
    }

    //Cleanup like any old thang.
    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }

    public Vector3f getPosition() { return position; }

    public void setBlock(int x, int y, int z, int id) { blocks[x][y][z] = id; }
}
