package hmph.math.Raycasting;

import hmph.math.Vector3f;

/**
 * Calculate the inital times (t-value) at where the ray will intersect the next
 * voxel boundary along each 3D axis (x, y, z)
 */

public class VoxelTraversal {

    // Current Voxel position
    // & Step Direction
    public int x,y,z;
    public int sx, sy, sz;

    public float tMaxX, tMaxY, tMaxZ;
    public float tDeltaX, tDeltaY, tDeltaZ;
    public Vector3f normal;

    public VoxelTraversal(Ray ray) {
        x = (int)Math.floor(ray.origin.x);
        y = (int)Math.floor(ray.origin.y);
        z = (int)Math.floor(ray.origin.z);

        if (ray.direction.x < 0) {
            sx = -1;
            tMaxX = (x - ray.origin.x) / ray.direction.x;
        } else {
            sx = 1;
            tMaxX = (x + 1.0f - ray.origin.x) / ray.direction.x;
        }

        if (ray.direction.y < 0) {
            sy = -1;
            tMaxY = (y - ray.origin.y) / ray.direction.y;
        } else {
            sy = 1;
            tMaxY = (y + 1.0f - ray.origin.y) / ray.direction.y;
        }

        if (ray.direction.z < 0) {
            sz = -1;
            tMaxZ = (z - ray.origin.z) / ray.direction.z;
        } else {
            sz = 1;
            tMaxZ = (z + 1.0f - ray.origin.z) / ray.direction.z;
        }

        tDeltaX = Math.abs(1.0f / ray.direction.x);
        tDeltaY = Math.abs(1.0f / ray.direction.y);
        tDeltaZ = Math.abs(1.0f / ray.direction.z);

        normal = new Vector3f();
    }

    /**
     * Step to the next voxel in the traversal
     * Returns the distance traveld along the ray
     */
    public float step() {
        float distance;

        if (tMaxX < tMaxY) {
            if (tMaxX < tMaxZ) {
                distance = tMaxX;
                tMaxX += tDeltaX;
                x += sx;
                normal.set(-sx, 0, 0);
            } else {
                distance = tMaxZ;
                tMaxZ += tDeltaZ;
                z += sz;
                normal.set(0, 0, -sz);
            }
        } else {
            if (tMaxY < tMaxZ) {
                distance = tMaxY;
                tMaxY += tDeltaY;
                y += sy;
                normal.set(0, -sy, 0);
            } else {
                distance = tMaxZ;
                tMaxZ += tDeltaZ;
                z += sz;
                normal.set(0, 0, -sz);
            }
        }

        return distance;
    }

    /**
     * Get current voxel position
     */
    public Vector3f getCurrentVoxel() {
        return new Vector3f(x, y, z);
    }

    /**
     * Get the normal of the face that was crossed in the last step
     */
    public Vector3f getFaceNormal() {
        return new Vector3f(normal);
    }
}
