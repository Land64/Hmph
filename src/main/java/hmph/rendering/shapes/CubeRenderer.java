package hmph.rendering.shapes;

import hmph.math.Matrix4f;
import hmph.math.Vector3f;
import hmph.rendering.shaders.ShaderManager;
import hmph.rendering.shaders.ShaderProgram;
import hmph.rendering.Camera;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class CubeRenderer {
    private int vao, vbo, ebo;
    private ShaderManager shaderManager;
    private Matrix4f modelMatrix;
    private Vector3f position;
    private Vector3f rotation;
    private Vector3f scale;
    private Vector3f color;

    //Constructor
    public CubeRenderer(ShaderManager shaderManager) {
        this.shaderManager = shaderManager;
        this.modelMatrix = new Matrix4f();
        this.position = new Vector3f(0.0f, 0.0f, 0.0f);
        this.rotation = new Vector3f(0.0f, 0.0f, 0.0f);
        this.scale = new Vector3f(1.0f, 1.0f, 1.0f);
        this.color = new Vector3f(1.0f, 1.0f, 1.0f);
        setupCubeBuffers();
        setupUniforms();
    }

    //Sets up the cube buffers
    private void setupCubeBuffers() {
        float[] vertices = {
                -0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,
                0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 1.0f,
                0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 1.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f,
                -0.5f,  0.5f, -0.5f,  0.5f, 0.5f, 0.5f
        };
        int[] indices = {
                // front
                0, 1, 2, 2, 3, 0,
                // back
                5, 4, 7, 7, 6, 5,
                // left
                4, 0, 3, 3, 7, 4,
                // right
                1, 5, 6, 6, 2, 1,
                // top
                3, 2, 6, 6, 7, 3,
                // bottom
                4, 5, 1, 1, 0, 4
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    //Sets up the uniforms
    private void setupUniforms() {
        try {
            ShaderProgram shader = shaderManager.getShader("3d");
            shader.bind();
            shader.createUniform("model");
            shader.createUniform("view");
            shader.createUniform("projection");
            shader.unbind();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //Extra Helper methods, as a "just in case."
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    public void setRotation(float x, float y, float z) {
        this.rotation.set(x, y, z);
    }

    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
    }

    public void setColor(float r, float g, float b) {
        this.color.set(r, g, b);
    }

    public void translate(float x, float y, float z) {
        this.position.add(x, y, z);
    }

    public void rotate(float x, float y, float z) {
        this.rotation.add(x, y, z);
    }

    public void scaleBy(float factor) {
        this.scale.mul(factor);
    }

    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getRotation() { return new Vector3f(rotation); }
    public Vector3f getScale() { return new Vector3f(scale); }
    public Vector3f getColor() { return new Vector3f(color); }

    public void render(Camera camera, int windowWidth, int windowHeight) {
        ShaderProgram shader = shaderManager.getShader("3d");
        shader.bind();
        modelMatrix.identity().translate(position).rotateX((float) Math.toRadians(rotation.x)).rotateY((float) Math.toRadians(rotation.y)).rotateZ((float) Math.toRadians(rotation.z)).scale(scale);
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = camera.getProjectionMatrix((float) windowWidth / windowHeight, 0.1f, 100.0f);
        shader.setUniform("model", modelMatrix);
        shader.setUniform("view", viewMatrix);
        shader.setUniform("projection", projectionMatrix);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        shader.unbind();
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}