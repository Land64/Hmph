package hmph.math;

//Same with vector3!
public class Vector3f {
    public float x, y, z;

    // --- CONSTRUCTORS ---
    public Vector3f() {
        this(0.0f, 0.0f, 0.0f);
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f(Vector3f other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    // --- SETTERS (fluent interface) ---
    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3f set(Vector3f other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    public Vector3f zero() {
        this.x = 0.0f;
        this.y = 0.0f;
        this.z = 0.0f;
        return this;
    }

    // --- BASIC ARITHMETIC (modifies this vector) ---
    public Vector3f add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3f add(Vector3f other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }

    public Vector3f sub(float x, float y, float z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vector3f sub(Vector3f other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
        return this;
    }

    public Vector3f mul(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public Vector3f mul(Vector3f other) { // Component-wise multiplication
        this.x *= other.x;
        this.y *= other.y;
        this.z *= other.z;
        return this;
    }

    public Vector3f div(float scalar) {
        if (scalar == 0) {
            throw new IllegalArgumentException("Division by zero");
        }
        this.x /= scalar;
        this.y /= scalar;
        this.z /= scalar;
        return this;
    }

    public Vector3f div(Vector3f other) { // Component-wise division
        if (other.x == 0 || other.y == 0 || other.z == 0) {
            throw new IllegalArgumentException("Component-wise division by zero");
        }
        this.x /= other.x;
        this.y /= other.y;
        this.z /= other.z;
        return this;
    }

    // --- OPERATIONS RETURNING NEW VECTORS (does not modify this vector) ---
    public Vector3f added(Vector3f other) {
        return new Vector3f(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vector3f subtracted(Vector3f other) {
        return new Vector3f(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector3f multiplied(float scalar) {
        return new Vector3f(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vector3f multiplied(Vector3f other) { // Component-wise
        return new Vector3f(this.x * other.x, this.y * other.y, this.z * other.z);
    }

    public Vector3f divided(float scalar) {
        if (scalar == 0) {
            throw new IllegalArgumentException("Division by zero");
        }
        return new Vector3f(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    public Vector3f divided(Vector3f other) { // Component-wise
        if (other.x == 0 || other.y == 0 || other.z == 0) {
            throw new IllegalArgumentException("Component-wise division by zero");
        }
        return new Vector3f(this.x / other.x, this.y / other.y, this.z / other.z);
    }


    // --- MAGNITUDE AND NORMALIZATION ---
    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public Vector3f normalize() {
        float len = length();
        if (len != 0) {
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    public Vector3f normalized() {
        float len = length();
        if (len != 0) {
            return new Vector3f(x / len, y / len, z / len);
        }
        return new Vector3f(); // Or throw exception, or return this
    }

    // --- DOT AND CROSS PRODUCT ---
    public float dot(Vector3f other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3f cross(Vector3f other) {
        // Modifies this vector and returns it
        float newX = y * other.z - z * other.y;
        float newY = z * other.x - x * other.z;
        float newZ = x * other.y - y * other.x;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    public Vector3f crossed(Vector3f other) {
        // Returns a new vector without modifying this one
        return new Vector3f(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    // --- DISTANCE ---
    public float distance(Vector3f other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float dz = this.z - other.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float distanceSquared(Vector3f other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    // --- ANGLE ---
    /**
     * Calculates the angle in radians between this vector and another vector.
     * @param other The other vector.
     * @return The angle in radians. Returns NaN if either vector is a zero vector.
     */
    public float angle(Vector3f other) {
        float lenProd = this.length() * other.length();
        if (lenProd == 0.0f) {
            return Float.NaN; // Or 0.0f, or throw exception, depending on desired behavior
        }
        float dot = this.dot(other);
        float cosTheta = dot / lenProd;
        // Clamp to avoid floating point inaccuracies leading to Math.acos domain errors
        cosTheta = Math.max(-1.0f, Math.min(1.0f, cosTheta));
        return (float) Math.acos(cosTheta);
    }

    // --- INTERPOLATION ---
    /**
     * Linearly interpolates between this vector and the target vector by alpha.
     * Modifies this vector: this = this + alpha * (target - this)
     * @param target The target vector.
     * @param alpha The interpolation factor (typically between 0 and 1).
     * @return This vector after interpolation.
     */
    public Vector3f lerp(Vector3f target, float alpha) {
        this.x += alpha * (target.x - this.x);
        this.y += alpha * (target.y - this.y);
        this.z += alpha * (target.z - this.z);
        return this;
    }

    /**
     * Returns a new vector that is the linear interpolation between this vector and the target vector.
     * result = this + alpha * (target - this)
     * @param target The target vector.
     * @param alpha The interpolation factor (typically between 0 and 1).
     * @return The new interpolated vector.
     */
    public Vector3f lerped(Vector3f target, float alpha) {
        return new Vector3f(
                this.x + alpha * (target.x - this.x),
                this.y + alpha * (target.y - this.y),
                this.z + alpha * (target.z - this.z)
        );
    }

    // --- PROJECTION ---
    /**
     * Projects this vector onto another vector. Modifies this vector.
     * @param onto The vector to project onto.
     * @return This vector, after being projected.
     */
    public Vector3f project(Vector3f onto) {
        float ontoLenSq = onto.lengthSquared();
        if (ontoLenSq == 0) {
            return this.zero(); // Cannot project onto a zero vector
        }
        float scale = this.dot(onto) / ontoLenSq;
        return this.set(onto.x * scale, onto.y * scale, onto.z * scale);
    }

    /**
     * Returns a new vector that is this vector projected onto another vector.
     * @param onto The vector to project onto.
     * @return The new projected vector.
     */
    public Vector3f projected(Vector3f onto) {
        float ontoLenSq = onto.lengthSquared();
        if (ontoLenSq == 0) {
            return new Vector3f(); // Cannot project onto a zero vector
        }
        float scale = this.dot(onto) / ontoLenSq;
        return new Vector3f(onto.x * scale, onto.y * scale, onto.z * scale);
    }

    // --- REFLECTION ---
    /**
     * Reflects this vector about a normal. Modifies this vector.
     * Assumes the normal is normalized. If not, normalize it first.
     * R = V - 2 * (V dot N) * N
     * @param normal The normal vector to reflect about (should be normalized).
     * @return This vector after reflection.
     */
    public Vector3f reflect(Vector3f normal) {
        // Ensure normal is normalized if it isn't guaranteed
        // Vector3f n = normal.normalized(); // If normal might not be unit length
        float dotProduct = this.dot(normal);
        this.x -= 2 * dotProduct * normal.x;
        this.y -= 2 * dotProduct * normal.y;
        this.z -= 2 * dotProduct * normal.z;
        return this;
    }

    /**
     * Returns a new vector that is this vector reflected about a normal.
     * Assumes the normal is normalized.
     * R = V - 2 * (V dot N) * N
     * @param normal The normal vector to reflect about (should be normalized).
     * @return The new reflected vector.
     */
    public Vector3f reflected(Vector3f normal) {
        // Vector3f n = normal.normalized(); // If normal might not be unit length
        float dotProduct = this.dot(normal);
        return new Vector3f(
                this.x - 2 * dotProduct * normal.x,
                this.y - 2 * dotProduct * normal.y,
                this.z - 2 * dotProduct * normal.z
        );
    }

    // --- NEGATION ---
    public Vector3f negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    public Vector3f negated() {
        return new Vector3f(-this.x, -this.y, -this.z);
    }

    // --- COMPARISON ---
    /**
     * Checks if this vector is approximately equal to another vector within a tolerance.
     * @param other The other vector.
     * @param epsilon The tolerance for each component.
     * @return True if approximately equal, false otherwise.
     */
    public boolean equals(Vector3f other, float epsilon) {
        if (other == null) return false;
        return (Math.abs(this.x - other.x) < epsilon) &&
                (Math.abs(this.y - other.y) < epsilon) &&
                (Math.abs(this.z - other.z) < epsilon);
    }

    /**
     * Checks if this vector is a zero vector.
     * @return True if all components are zero, false otherwise.
     */
    public boolean isZero() {
        return x == 0.0f && y == 0.0f && z == 0.0f;
    }

    /**
     * Checks if this vector is a zero vector within a tolerance.
     * @param epsilon The tolerance.
     * @return True if all components are close to zero, false otherwise.
     */
    public boolean isZero(float epsilon) {
        return (Math.abs(x) < epsilon) &&
                (Math.abs(y) < epsilon) &&
                (Math.abs(z) < epsilon);
    }


    // --- UTILITY ---
    /**
     * Creates a copy of this vector.
     * @return A new Vector3f instance with the same x, y, z values.
     */
    public Vector3f copy() {
        return new Vector3f(this.x, this.y, this.z);
    }


    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector3f other = (Vector3f) obj;
        return Float.compare(other.x, x) == 0 &&
                Float.compare(other.y, y) == 0 &&
                Float.compare(other.z, z) == 0;
    }

    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        result = 31 * result + (z != +0.0f ? Float.floatToIntBits(z) : 0);
        return result;
    }

    // --- Static Utility Methods (Optional - can also be external helper class) ---

    /**
     * Static method to create a new vector by adding two vectors.
     */
    public static Vector3f add(Vector3f v1, Vector3f v2) {
        return new Vector3f(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }

    /**
     * Static method to create a new vector by subtracting the second from the first.
     */
    public static Vector3f sub(Vector3f v1, Vector3f v2) {
        return new Vector3f(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
    }

    /**
     * Static method for scalar multiplication.
     */
    public static Vector3f mul(Vector3f v, float scalar) {
        return new Vector3f(v.x * scalar, v.y * scalar, v.z * scalar);
    }

    /**
     * Static method for dot product.
     */
    public static float dot(Vector3f v1, Vector3f v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    /**
     * Static method for cross product.
     */
    public static Vector3f cross(Vector3f v1, Vector3f v2) {
        return new Vector3f(
                v1.y * v2.z - v1.z * v2.y,
                v1.z * v2.x - v1.x * v2.z,
                v1.x * v2.y - v1.y * v2.x
        );
    }

    /**
     * Static method for linear interpolation.
     */
    public static Vector3f lerp(Vector3f start, Vector3f end, float alpha) {
        return new Vector3f(
                start.x + alpha * (end.x - start.x),
                start.y + alpha * (end.y - start.y),
                start.z + alpha * (end.z - start.z)
        );
    }
}
