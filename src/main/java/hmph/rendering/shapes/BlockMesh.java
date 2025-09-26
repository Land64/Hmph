package hmph.rendering.shapes;

import hmph.math.Vector3f;
import hmph.rendering.world.Direction;
import hmph.rendering.BlockRegistry;
import java.util.List;

public class BlockMesh {
    private static final float BLOCK_SCALE = 1.0f;

    public static class MeshData {
        public List<Float> vertices;
        public List<Integer> indices;
        public List<String> texturesUsed;
        public int facesAdded;

        public MeshData(List<Float> vertices, List<Integer> indices, List<String> texturesUsed, int facesAdded) {
            this.vertices = vertices;
            this.indices = indices;
            this.texturesUsed = texturesUsed;
            this.facesAdded = facesAdded;
        }
    }

    public static int addBlockMesh(int x, int y, int z, String[][][] blocks, List<Float> vertices,
                                   List<Integer> indices, BlockRegistry registry) {
        int facesAdded = 0;
        int startVertexIndex = vertices.size() / 9; // Now using 9 floats per vertex (pos + uv + normal + texture_id)
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

            // Get texture name for this face
            String textureName = registry.getTexture(blockName, dir);
            float textureId = getTextureIndex(textureName); // You'll need to implement this

            for (int i = 0; i < 4; i++) {
                Vector3f v = faceVertices[i];
                // Position
                vertices.add((x + v.x) * BLOCK_SCALE);
                vertices.add((y + v.y) * BLOCK_SCALE);
                vertices.add((z + v.z) * BLOCK_SCALE);
                // UV coordinates
                vertices.add(uvs[i][0]);
                vertices.add(uvs[i][1]);
                // Normal
                vertices.add(normal.x);
                vertices.add(normal.y);
                vertices.add(normal.z);
                // Texture ID (for texture atlas or array)
                vertices.add(textureId);
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
        // Enhanced UV mapping based on block name and face direction
        switch (blockName) {
            case "grass":
                if (dir == Direction.UP) {
                    return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}}; // grass_top texture
                } else if (dir == Direction.DOWN) {
                    return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}}; // dirt texture
                } else {
                    return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}}; // grass_side texture
                }
            case "oak_log":
            case "pine_log":
            case "beech_log":
            case "maple_log":
            case "eucalyptus_log":
                if (dir == Direction.UP || dir == Direction.DOWN) {
                    return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}}; // log_top texture
                } else {
                    return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}}; // log_side texture
                }
            default:
                return new float[][]{{0f,0f},{1f,0f},{1f,1f},{0f,1f}};
        }
    }

    private static float getTextureIndex(String textureName) {
        // This is a placeholder - you'll need to implement a texture indexing system
        // For now, return 0 (you could map texture names to indices)
        if (textureName == null) return 0f;

        // Simple hash-based mapping (not ideal, but works for testing)
        switch (textureName) {
            case "stone_generic": return 0f;
            case "dirt": return 1f;
            case "grass_top": return 2f;
            case "grass_side": return 3f;
            case "granite": return 4f;
            case "marble": return 5f;
            case "sandstone": return 6f;
            case "basalt": return 7f;
            case "oak_leaves": return 8f;
            case "pine_leaves": return 9f;
            default: return 0f;
        }
    }

    private static boolean isAirOrOutOfBounds(int x, int y, int z, String[][][] blocks) {
        if (x < 0 || y < 0 || z < 0 ||
                x >= blocks.length || y >= blocks[0].length || z >= blocks[0][0].length) {
            return true;
        }
        return blocks[x][y][z] == null;
    }
}