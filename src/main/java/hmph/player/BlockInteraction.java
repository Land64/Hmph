package hmph.player;

import hmph.math.Raycasting.VoxelTraversal;
import hmph.math.Vector3f;
import hmph.math.Raycasting.Ray;
import hmph.rendering.world.chunk.ChunkManager;

public class BlockInteraction {
    public static final float max_reach = 20f;

    public static class RaycastResult {
        public boolean hit;
        public Vector3f blockPos = new Vector3f();
        public Vector3f normal = new Vector3f();
        public float distance;
    }

    /**
     * Cast a ray from the camera's position using the Voxel-Class
     * Better than going through each ray in a for loop or something like that.
     */
    public static RaycastResult raycastToBlock(Vector3f origin, Vector3f direction, ChunkManager manager) {
        RaycastResult result = new RaycastResult();

        Ray ray = new Ray(origin, direction);
        VoxelTraversal travisscott = new VoxelTraversal(ray);

        float distance = 0.0f;

        int blockId = manager.getBlockAt(travisscott.x, travisscott.y, travisscott.z);
        if (blockId != 0) {
            result.hit = true;
            result.blockPos.set(travisscott.x, travisscott.y, travisscott.z);
            result.normal.set(0, 1, 0);
            result.distance = 0.0f;
            return result;
        }

        while (distance < max_reach) {
            float stepDist = travisscott.step();
            distance += stepDist;

            blockId = manager.getBlockAt(travisscott.x, travisscott.y, travisscott.z);
            if (blockId != 0) {
                result.hit = true;
                result.blockPos.set(travisscott.x, travisscott.y, travisscott.z);
                result.normal.set(travisscott.getFaceNormal());
                result.distance = distance;
                return result;
            }
        }

        return result;
    }

    /**
     * Better but slower way then @raycastToBlock
     */
    public static RaycastResult raycastToBlockPrecise(Vector3f origin, Vector3f direction, ChunkManager chunkManager) {
        RaycastResult result = new RaycastResult();

        Ray ray = new Ray(origin, direction);
        float closestDistance = max_reach + 1;

        // Get bounding box of area to check
        Vector3f start = ray.getOrigin();
        Vector3f end = ray.getPointAt(max_reach);

        int minX = (int) Math.floor(Math.min(start.x, end.x)) - 1;
        int maxX = (int) Math.ceil(Math.max(start.x, end.x)) + 1;
        int minY = (int) Math.floor(Math.min(start.y, end.y)) - 1;
        int maxY = (int) Math.ceil(Math.max(start.y, end.y)) + 1;
        int minZ = (int) Math.floor(Math.min(start.z, end.z)) - 1;
        int maxZ = (int) Math.ceil(Math.max(start.z, end.z)) + 1;

        // Check all blocks in the ray's path
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int blockId = chunkManager.getBlockAt(x, y, z);
                    if (blockId != 0) {
                        float distance = ray.intersectBlock(x, y, z);
                        if (distance >= 0 && distance < closestDistance) {
                            closestDistance = distance;
                            result.hit = true;
                            result.blockPos.set(x, y, z);
                            result.distance = distance;
                            result.normal.set(ray.getBlockFaceNormal(x, y, z, distance));
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Get the adjacent block position for placement based on face normal
     */
    public static Vector3f getPlacementPosition(Vector3f blockPos, Vector3f normal) {
        return new Vector3f(
                blockPos.x + normal.x,
                blockPos.y + normal.y,
                blockPos.z + normal.z
        );
    }

    /**
     * Test if a ray intersects with a sphere (useful for entity targeting)
     */
    public static float rayIntersectSphere(Vector3f rayOrigin, Vector3f rayDirection, Vector3f sphereCenter, float sphereRadius) {
        Vector3f oc = new Vector3f(rayOrigin.x - sphereCenter.x,
                rayOrigin.y - sphereCenter.y,
                rayOrigin.z - sphereCenter.z);

        float a = rayDirection.dot(rayDirection);
        float b = 2.0f * oc.dot(rayDirection);
        float c = oc.dot(oc) - sphereRadius * sphereRadius;

        float discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            return -1;
        }

        float sqrt_discriminant = (float) Math.sqrt(discriminant);
        float t1 = (-b - sqrt_discriminant) / (2 * a);
        float t2 = (-b + sqrt_discriminant) / (2 * a);

        if (t1 >= 0) return t1;
        if (t2 >= 0) return t2;
        return -1;
    }
}
