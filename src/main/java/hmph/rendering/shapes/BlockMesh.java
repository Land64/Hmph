package hmph.rendering.shapes;

import hmph.math.Vector3f;
import hmph.rendering.world.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockMesh {
    private static final Random random = new Random();
    private static final float BLOCK_SCALE = 1.0f / 3.0f; // 3× smaller cubes


    //Heres where it gets fun.
    public static int addBlockMesh(int x, int y, int z, int[][][] blocks, List<Float> vertices, List<Integer> indices) {
        int facesAdded = 0;
        int startVertexIndex = vertices.size() / 6;

        Vector3f blockColor = getRandomBlockColor(x, y, z);

        for (Direction dir : Direction.values()) {
            int nx = x + (int) dir.x();
            int ny = y + (int) dir.y();
            int nz = z + (int) dir.z();

            boolean shouldRender = nx < 0 || ny < 0 || nz < 0 ||  nx >= blocks.length || ny >= blocks[0].length || nz >= blocks[0][0].length || blocks[nx][ny][nz] == 0;

            if (shouldRender) {
                Vector3f[] faceVertices = dir.getVertices();
                Vector3f faceColor = applyFaceShading(blockColor, dir);

                //Adding the vertices to the list
                for (Vector3f v : faceVertices) {
                    vertices.add((x + v.x) * BLOCK_SCALE);
                    vertices.add((y + v.y) * BLOCK_SCALE);
                    vertices.add((z + v.z) * BLOCK_SCALE);

                    vertices.add(faceColor.x);
                    vertices.add(faceColor.y);
                    vertices.add(faceColor.z);
                }


                int baseIndex = startVertexIndex + (facesAdded * 4);

                if (dir == Direction.NORTH || dir == Direction.SOUTH || dir == Direction.EAST || dir == Direction.WEST) {
                    indices.add(baseIndex);
                    indices.add(baseIndex + 2);
                    indices.add(baseIndex + 1);
                    indices.add(baseIndex);
                    indices.add(baseIndex + 3);
                    indices.add(baseIndex + 2);
                } else {
                    indices.add(baseIndex);
                    indices.add(baseIndex + 1);
                    indices.add(baseIndex + 2);
                    indices.add(baseIndex + 2);
                    indices.add(baseIndex + 3);
                    indices.add(baseIndex);
                }


                facesAdded++;
            }
        }
        return facesAdded;
    }

    //Generates a random color for the block
    private static Vector3f getRandomBlockColor(int x, int y, int z) {
        Random blockRandom = new Random((long)x * 73856093 + (long)y * 19349663 + (long)z * 83492791);
        float r = 0.3f + blockRandom.nextFloat() * 0.7f;
        float g = 0.3f + blockRandom.nextFloat() * 0.7f;
        float b = 0.3f + blockRandom.nextFloat() * 0.7f;
        return new Vector3f(r, g, b);
    }


    //Applies shading to the block
    private static Vector3f applyFaceShading(Vector3f baseColor, Direction direction) {
        float shadingFactor;
        switch (direction) {
            case UP:    shadingFactor = 1.0f; break;
            case DOWN:  shadingFactor = 0.5f; break;
            case NORTH:
            case SOUTH: shadingFactor = 0.8f; break;
            case EAST:
            case WEST:  shadingFactor = 0.7f; break;
            default:    shadingFactor = 1.0f;
        }
        return new Vector3f(baseColor.x * shadingFactor, baseColor.y * shadingFactor, baseColor.z * shadingFactor);
    }
}
