package hmph.rendering.shapes;

import hmph.math.Vector3f;
import hmph.rendering.world.Direction;
import hmph.rendering.BlockRegistry;
import java.util.List;

public class BlockMesh {
    private static final float BLOCK_SCALE = 1.0f;
// simple block mesh, no biggie
    public static int addBlockMesh(int x, int y, int z, String[][][] blocks, List<Float> vertices, List<Integer> indices, BlockRegistry registry) {
        int facesAdded = 0;
        int startVertexIndex = vertices.size() / 8;
        String blockName = blocks[x][y][z];
        if (blockName == null) return 0;
        BlockRegistry.BlockData data = registry.get(blockName);
        if (data == null) return 0;

        for (Direction dir : Direction.values()) {
            int nx = x + (int) dir.x();
            int ny = y + (int) dir.y();
            int nz = z + (int) dir.z();
            boolean shouldRender = isAirOrOutOfBounds(nx, ny, nz, blocks);
            if (!shouldRender) continue;

            Vector3f[] faceVertices = dir.getVertices();
            Vector3f normal = dir.getNormal();
            float[][] uvs = getUVsForFace(blockName, dir);

            for (int i = 0; i < 4; i++) {
                Vector3f v = faceVertices[i];
                vertices.add((x + v.x) * BLOCK_SCALE);
                vertices.add((y + v.y) * BLOCK_SCALE);
                vertices.add((z + v.z) * BLOCK_SCALE);
                vertices.add(uvs[i][0]);
                vertices.add(uvs[i][1]);
                vertices.add(normal.x);
                vertices.add(normal.y);
                vertices.add(normal.z);
            }

            int baseIndex = startVertexIndex + facesAdded * 4;
            indices.add(baseIndex);
            indices.add(baseIndex + 1);
            indices.add(baseIndex + 2);
            indices.add(baseIndex);
            indices.add(baseIndex + 2);
            indices.add(baseIndex + 3);
            facesAdded++;
        }

        return facesAdded;
    }

    private static float[][] getUVsForFace(String blockName, Direction dir) {
        if ("grass".equals(blockName)) {
            if (dir == Direction.UP) {
                return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}};
            } else if (dir == Direction.DOWN) {
                return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}};
            } else {
                return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}};
            }
        }
        return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}};
    }

    private static boolean isAirOrOutOfBounds(int x, int y, int z, String[][][] blocks) {
        if (x < 0 || y < 0 || z < 0 || x >= blocks.length || y >= blocks[0].length || z >= blocks[0][0].length) {
            return true;
        }
        return blocks[x][y][z] == null;
    }
}