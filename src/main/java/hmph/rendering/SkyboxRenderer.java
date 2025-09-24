package hmph.rendering;

import hmph.math.Matrix4f;
import hmph.math.Vector3f;
import hmph.rendering.shaders.ShaderProgram;
import hmph.rendering.shaders.ShaderManager;
import hmph.util.debug.LoggerHelper;
import hmph.rendering.Camera;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class SkyboxRenderer {
    private int vao, vbo, ebo;
    private ShaderProgram skyboxShader;
    private ShaderManager shaderManager;

    private float timeOfDay = 0.5f;
    private boolean autoTime = true;
    private float gameTime = 0f;
    private float dayLength = 60f;

    private final float[] vertices = {
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,
            -1.0f,  1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f
    };

    private final int[] indices = {
            0, 1, 2,  2, 3, 0,
            4, 6, 5,  6, 4, 7,
            0, 4, 5,  5, 1, 0,
            2, 6, 7,  7, 3, 2,
            0, 3, 7,  7, 4, 0,
            1, 5, 6,  6, 2, 1
    };

    public SkyboxRenderer(ShaderManager shaderManager) {
        this.shaderManager = shaderManager;
        init();
    }

    private void init() {
        createShader();
        setupMesh();
    }

    private void createShader() {
        String vertexSource =
                "#version 330 core\n" +
                        "layout (location = 0) in vec3 aPos;\n" +
                        "\n" +
                        "uniform mat4 projection;\n" +
                        "uniform mat4 view;\n" +
                        "\n" +
                        "out vec3 worldPos;\n" +
                        "\n" +
                        "void main() {\n" +
                        "    mat4 rotView = mat4(mat3(view));\n" +
                        "    vec4 pos = projection * rotView * vec4(aPos, 1.0);\n" +
                        "    gl_Position = pos.xyww;\n" +
                        "    worldPos = aPos;\n" +
                        "}\n";

        String fragmentSource =
                "#version 330 core\n" +
                        "in vec3 worldPos;\n" +
                        "\n" +
                        "uniform float timeOfDay;\n" +
                        "uniform vec3 horizonColor;\n" +
                        "uniform vec3 zenithColor;\n" +
                        "uniform vec3 fogColor;\n" +
                        "\n" +
                        "\n" +
                        "out vec4 FragColor;\n" +
                        "\n" +
                        "void main() {\n" +
                        "    float normalizedY = (worldPos.y + 1.0) * 0.5;\n" +
                        "\n" +
                        "    vec3 dayHorizon = vec3(0.8, 0.9, 1.0);\n" +
                        "    vec3 dayZenith  = vec3(0.3, 0.5, 0.9);\n" +
                        "\n" +
                        "    vec3 sunsetHorizon = vec3(1.0, 0.6, 0.3);\n" +
                        "    vec3 sunsetZenith  = vec3(0.4, 0.3, 0.6);\n" +
                        "\n" +
                        "    vec3 nightHorizon = vec3(0.1, 0.1, 0.2);\n" +
                        "    vec3 nightZenith  = vec3(0.02, 0.02, 0.1);\n" +
                        "\n" +
                        "    vec3 horizon;\n" +
                        "    vec3 zenith;\n" +
                        "\n" +
                        "    if (timeOfDay < 0.25) {\n" +
                        "        float t = timeOfDay / 0.25;\n" +
                        "        horizon = mix(nightHorizon, sunsetHorizon, t);\n" +
                        "        zenith  = mix(nightZenith, sunsetZenith, t);\n" +
                        "    } else if (timeOfDay < 0.5) {\n" +
                        "        float t = (timeOfDay - 0.25) / 0.25;\n" +
                        "        horizon = mix(sunsetHorizon, dayHorizon, t);\n" +
                        "        zenith  = mix(sunsetZenith, dayZenith, t);\n" +
                        "    } else if (timeOfDay < 0.75) {\n" +
                        "        float t = (timeOfDay - 0.5) / 0.25;\n" +
                        "        horizon = mix(dayHorizon, sunsetHorizon, t);\n" +
                        "        zenith  = mix(dayZenith, sunsetZenith, t);\n" +
                        "    } else {\n" +
                        "        float t = (timeOfDay - 0.75) / 0.25;\n" +
                        "        horizon = mix(sunsetHorizon, nightHorizon, t);\n" +
                        "        zenith  = mix(sunsetZenith, nightZenith, t);\n" +
                        "    }\n" +
                        "\n" +
                        "    vec3 skyColor;\n" +
                        "    if (normalizedY < 0.3) {\n" +
                        "        float t = normalizedY / 0.3;\n" +
                        "        skyColor = mix(nightHorizon, horizon, t);\n" +
                        "    } else {\n" +
                        "        float t = (normalizedY - 0.3) / 0.7;\n" +
                        "        skyColor = mix(horizon, zenith, t);\n" +
                        "    }\n" +
                        "\n" +
                        "    FragColor = vec4(skyColor, 1.0);\n" +
                        "}\n";

        try {
            shaderManager.loadShader("skybox", vertexSource, fragmentSource);
            skyboxShader = shaderManager.getShader("skybox");
        } catch (Exception e) {
            LoggerHelper.betterPrint("Failed to create skybox shader: " + e.getMessage(), LoggerHelper.LogType.ERROR);
            throw new RuntimeException("Skybox shader creation failed", e);
        }
    }

    private void setupMesh() {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        memFree(vertexBuffer);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = memAllocInt(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        memFree(indexBuffer);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
    }

    public void setTimeOfDay(float t) {
        timeOfDay = Math.max(0.0f, Math.min(1.0f, t));
        autoTime = false;
    }

    public void resumeDayCycle() {
        autoTime = true;
    }

    public void render(Camera camera, int width, int height, float deltaTime) {
        renderWithTime(camera, width, height, 0.5f);
    }

    public void renderWithTime(Camera camera, int width, int height, float timeOfDay) {
        skyboxShader.bind();

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);

        Matrix4f view = new Matrix4f(camera.getViewMatrix());
        view.m[12] = 0;
        view.m[13] = 0;
        view.m[14] = 0;

        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(70.0f),
                (float) width / height,
                0.1f,
                1000.0f
        );

        skyboxShader.setUniform("view", view);
        skyboxShader.setUniform("projection", projection);
        skyboxShader.setUniform("timeOfDay", timeOfDay);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);

        skyboxShader.unbind();
    }

    public void cleanup() {
        if (vao != 0) { glDeleteVertexArrays(vao); vao = 0; }
        if (vbo != 0) { glDeleteBuffers(vbo); vbo = 0; }
        if (ebo != 0) { glDeleteBuffers(ebo); ebo = 0; }
        if (skyboxShader != null) { skyboxShader.cleanup(); skyboxShader = null; }
    }
}