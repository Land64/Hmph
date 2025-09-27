package hmph.math;

import hmph.math.Matrix4f;
import hmph.math.Vector3f;

public class Frustum {
    private Plane[] planes = new Plane[6];

    // Plane indices
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    private static final int NEAR = 4;
    private static final int FAR = 5;

    public static class Plane {
        public float a, b, c, d;

        public Plane() {}

        public Plane(float a, float b, float c, float d) {
            set(a, b, c, d);
        }

        public void set(float a, float b, float c, float d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            normalize();
        }

        private void normalize() {
            float length = (float)Math.sqrt(a*a + b*b + c*c);
            if (length > 1e-6f) {
                float invLength = 1.0f / length;
                a *= invLength;
                b *= invLength;
                c *= invLength;
                d *= invLength;
            }
        }

        public float distanceToPoint(Vector3f point) {
            return a * point.x + b * point.y + c * point.z + d;
        }

        public float distanceToPoint(float x, float y, float z) {
            return a * x + b * y + c * z + d;
        }
    }

    public Frustum() {
        for (int i = 0; i < 6; i++) {
            planes[i] = new Plane();
        }
    }

    /**
     * Extract frustum planes from view-projection matrix
     * Using the standard method that actually works
     */
    public void extractFromMatrix(Matrix4f viewProj) {
        float[] m = viewProj.m;

        // Left plane: row4 + row1
        planes[LEFT].set(
                m[3] + m[0],   // m03 + m00
                m[7] + m[4],   // m13 + m10
                m[11] + m[8],  // m23 + m20
                m[15] + m[12]  // m33 + m30
        );

        // Right plane: row4 - row1
        planes[RIGHT].set(
                m[3] - m[0],   // m03 - m00
                m[7] - m[4],   // m13 - m10
                m[11] - m[8],  // m23 - m20
                m[15] - m[12]  // m33 - m30
        );

        // Bottom plane: row4 + row2
        planes[BOTTOM].set(
                m[3] + m[1],   // m03 + m01
                m[7] + m[5],   // m13 + m11
                m[11] + m[9],  // m23 + m21
                m[15] + m[13]  // m33 + m31
        );

        // Top plane: row4 - row2
        planes[TOP].set(
                m[3] - m[1],   // m03 - m01
                m[7] - m[5],   // m13 - m11
                m[11] - m[9],  // m23 - m21
                m[15] - m[13]  // m33 - m31
        );

        // Near plane: row4 + row3
        planes[NEAR].set(
                m[3] + m[2],   // m03 + m02
                m[7] + m[6],   // m13 + m12
                m[11] + m[10], // m23 + m22
                m[15] + m[14]  // m33 + m32
        );

        // Far plane: row4 - row3
        planes[FAR].set(
                m[3] - m[2],   // m03 - m02
                m[7] - m[6],   // m13 - m12
                m[11] - m[10], // m23 - m22
                m[15] - m[14]  // m33 - m32
        );
    }

    /**
     * Test if an AABB is completely outside the frustum
     * Returns true if the box intersects or is inside the frustum
     */
    public boolean intersectsAABB(Vector3f min, Vector3f max) {
        for (int i = 0; i < 6; i++) {
            Plane plane = planes[i];

            float px = plane.a >= 0 ? max.x : min.x;
            float py = plane.b >= 0 ? max.y : min.y;
            float pz = plane.c >= 0 ? max.z : min.z;

            if (plane.distanceToPoint(px, py, pz) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if a sphere intersects the frustum
     */
    public boolean intersectsSphere(Vector3f center, float radius) {
        for (int i = 0; i < 6; i++) {
            if (planes[i].distanceToPoint(center) < -radius) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if a chunk is visible
     */
    public boolean isChunkVisible(Vector3f chunkPos, float chunkSize) {
        // Create bounding box for the chunk
        Vector3f min = new Vector3f(chunkPos.x, chunkPos.y, chunkPos.z);
        Vector3f max = new Vector3f(
                chunkPos.x + chunkSize,
                chunkPos.y + 256f,
                chunkPos.z + chunkSize
        );

        return intersectsAABB(min, max);
    }

    /**
     * Test a specific point against the frustum (for debugging)
     */
    public boolean containsPoint(Vector3f point) {
        for (int i = 0; i < 6; i++) {
            if (planes[i].distanceToPoint(point) < 0) {
                return false;
            }
        }
        return true;
    }
}