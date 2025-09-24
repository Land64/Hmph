package hmph.math.Raycasting;

import hmph.math.Vector3f;

import java.util.Vector;

/**
 * Ray class for 3D Raycasting
 * Provides utility for ray tracing and other operations
 */

public class Ray {
    public Vector3f origin;
    public Vector3f direction;

    /**
     * Creates a new ray with the given origin and direction (in Vector3Fs)
     * @param origin
     * @param direction
     */

    public Ray(Vector3f origin, Vector3f direction) {
        this.origin = new Vector3f(origin);
        this.direction = new Vector3f(direction);
    }

    /**
     * Creates a new ray with the given origin and direction (in given XYZ floats)
     * @param oX
     * @param oY
     * @param oZ
     * @param dirX
     * @param dirY
     * @param dirZ
     */

    public Ray(float oX, float oY, float oZ, float dirX, float dirY, float dirZ) {
        this.origin = new Vector3f(oX, oY, oZ);
        this.direction = new Vector3f(dirX, dirY, dirZ);

    }

    /**
     * @param t
     * @return The point along the ray at distance "t (time)"
     */
    public Vector3f getPointAt(float t) {
        return new Vector3f(
                origin.x + direction.x * t,
                origin.y + direction.y * t,
                origin.z + direction.z * t
        );
    }

    /**
     * Test intersection with an axis-alinged bounding box (AABB)
     * Returns the distance to intersection, or -1 if there is none
     */
    public float intersectAABB(Vector3f boxMin, Vector3f boxMax) {
        float tmin = (boxMin.x - origin.x) / direction.x;
        float tmax = (boxMin.x - origin.x) / direction.x;

        if (tmin > tmax) {
            float temp = tmin;
            tmin = tmax;
            tmax = temp;
        }

        float tymin = (boxMin.y - origin.y) / direction.y;
        float tymax = (boxMin.y - origin.y) / direction.y;

        if (tymin > tymax) {
            float temp = tmin;
            tymin = tymax;
            tymax = temp;
        }

        if (tmin > tmax || tymin > tymax) {
            return -1;
        }

        if (tymin > tmin) tmin = tymin;
        if (tymax > tmax) tmax = tymax;

        float tzmin = (boxMin.z - origin.z) / direction.z;
        float tzmax = (boxMin.z - origin.z) / direction.z;

        if (tzmin > tzmax) {
            float temp = tmin;
            tzmin = tzmax;
            tzmax = temp;
        }

        if (tmin > tmax || tzmin > tzmax) {
            return -1;
        }

        if (tzmin > tmin) tmin = tzmin;
        if (tzmax > tmax) tmax = tzmax;

        return tmin > 0 ? tmin : (tmax > 0 ? tmax : -1);
    }

    /**
     * Intersect with a unit cube at a given position
     * Returns the distance to a intersection, or -1 its none.
     */
    public float intersectBlock(int blockX, int blockY, int blockZ) {
        Vector3f boxMin = new Vector3f(blockX, blockY, blockZ);
        Vector3f boxMax = new Vector3f(blockX + 1, blockY + 1, blockZ + 1);
        return intersectAABB(boxMin, boxMax);
    }

    /**
     * Get the face normal of a block that was hit by the ray
     */
    public Vector3f getBlockFaceNormal(int blockX, int blockY, float blockZ, float hitDistance) {
        float baseBlockVal = 0.5f;

        Vector3f hitPoint = getPointAt(hitDistance);
        Vector3f blockCenter = new Vector3f(blockX + baseBlockVal, blockY + + baseBlockVal, blockZ + + baseBlockVal);

        float relX = hitPoint.x - blockCenter.x;
        float relY = hitPoint.y - blockCenter.y;
        float relZ = hitPoint.z - blockCenter.z;

        float absX = Math.abs(relX);
        float absY = Math.abs(relY);
        float absZ = Math.abs(relZ);

        if (absX > absY && absX > absZ) {
            return new Vector3f(relX > 0 ? 1 : -1, 0, 0);
        } else if (absY > absX && absY > absZ) {
            return new Vector3f(0, relY > 0 ? 1 : -1, 0);
        } else {
            return new Vector3f(0, 0, relY > 0 ? 1 : -1);
        }
    }

    public Vector3f getOrigin() { return new Vector3f(origin); }
    public Vector3f getDirection() { return new Vector3f(direction); }

    public void setOrigin(Vector3f origin) { this.origin.set(origin); }
    public void setDirection(Vector3f direction) { this.direction.set(direction).normalize(); };

    @Override
    public String toString() {
        return String.format("Ray[origin=%s, direction=%s]", origin, direction);
    }
}
