package hmph.math;

import java.nio.FloatBuffer;


// You can kill me before explaining this.
public class Matrix4f {
    public float[] m = new float[16];

    public Matrix4f() {
        identity();
    }

    public Matrix4f(Matrix4f other) {
        set(other);
    }

    public Matrix4f identity() {
        for (int i = 0; i < 16; i++) {
            m[i] = 0.0f;
        }
        m[0] = m[5] = m[10] = m[15] = 1.0f;
        return this;
    }

    public Matrix4f set(Matrix4f other) {
        System.arraycopy(other.m, 0, this.m, 0, 16);
        return this;
    }

    public Matrix4f translate(Vector3f translation) {
        return translate(translation.x, translation.y, translation.z);
    }

    public Matrix4f translate(float x, float y, float z) {
        Matrix4f temp = new Matrix4f();
        temp.m[12] = x;
        temp.m[13] = y;
        temp.m[14] = z;
        return multiply(temp);
    }

    public Matrix4f rotateX(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        Matrix4f temp = new Matrix4f();
        temp.m[5] = cos;
        temp.m[6] = sin;
        temp.m[9] = -sin;
        temp.m[10] = cos;
        return multiply(temp);
    }

    public Matrix4f rotateY(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        Matrix4f temp = new Matrix4f();
        temp.m[0] = cos;
        temp.m[2] = -sin;
        temp.m[8] = sin;
        temp.m[10] = cos;
        return multiply(temp);
    }

    public Matrix4f rotateZ(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        Matrix4f temp = new Matrix4f();
        temp.m[0] = cos;
        temp.m[1] = sin;
        temp.m[4] = -sin;
        temp.m[5] = cos;
        return multiply(temp);
    }

    public Matrix4f scale(Vector3f scale) {
        return scale(scale.x, scale.y, scale.z);
    }

    public Matrix4f scale(float x, float y, float z) {
        Matrix4f temp = new Matrix4f();
        temp.m[0] = x;
        temp.m[5] = y;
        temp.m[10] = z;
        return multiply(temp);
    }

    public Matrix4f perspective(float fovy, float aspect, float zNear, float zFar) {
        identity();
        float f = 1.0f / (float) Math.tan(fovy * 0.5f);
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (zFar + zNear) / (zNear - zFar);
        m[11] = -1.0f;
        m[14] = (2.0f * zFar * zNear) / (zNear - zFar);
        m[15] = 0.0f;
        return this;
    }

    public Matrix4f lookAt(Vector3f eye, Vector3f center, Vector3f up) {
        Vector3f f = new Vector3f(center.x - eye.x, center.y - eye.y, center.z - eye.z).normalize();
        Vector3f s = f.cross(up).normalize();
        Vector3f u = s.cross(f);

        identity();
        m[0] = s.x;
        m[4] = s.y;
        m[8] = s.z;
        m[1] = u.x;
        m[5] = u.y;
        m[9] = u.z;
        m[2] = -f.x;
        m[6] = -f.y;
        m[10] = -f.z;
        m[12] = -s.dot(eye);
        m[13] = -u.dot(eye);
        m[14] = f.dot(eye);

        return this;
    }

    public Matrix4f multiply(Matrix4f other) {
        float[] result = new float[16];

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                result[row * 4 + col] =
                        m[row * 4 + 0] * other.m[0 * 4 + col] +
                                m[row * 4 + 1] * other.m[1 * 4 + col] +
                                m[row * 4 + 2] * other.m[2 * 4 + col] +
                                m[row * 4 + 3] * other.m[3 * 4 + col];
            }
        }

        System.arraycopy(result, 0, this.m, 0, 16);
        return this;
    }

    public void get(FloatBuffer buffer) {
        buffer.put(m);
        buffer.flip();
    }
}
