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

    private Vector3f horizonColor = new Vector3f(0.8f, 0.9f, 1.0f);
    private Vector3f zenithColor = new Vector3f(0.3f, 0.5f, 0.9f);
    private Vector3f fogColor = new Vector3f(0.7f, 0.8f, 0.9f);

    private float[] vertices = {
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,
            -1.0f,  1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f
    };

    private int[] indices = {
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
        LoggerHelper.betterPrint("Skybox renderer initialized", LoggerHelper.LogType.RENDERING);
    }

    private void createShader() {
        String vertexSource = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            
            uniform mat4 projection;
            uniform mat4 view;
            
            out vec3 worldPos;
            
            void main() {
                mat4 rotView = mat4(mat3(view));
                vec4 pos = projection * rotView * vec4(aPos, 1.0);
                gl_Position = pos.xyww;
                worldPos = aPos;
            }
            """;

        String fragmentSource = """
            #version 330 core
            in vec3 worldPos;
            
            uniform vec3 horizonColor;
            uniform vec3 zenithColor;
            uniform vec3 fogColor;
            uniform float time;
            
            out vec4 FragColor;
            
            void main() {
                float normalizedY = (worldPos.y + 1.0) * 0.5;
                
                vec3 skyColor;
                if (normalizedY < 0.3) {
                    float t = normalizedY / 0.3;
                    skyColor = mix(fogColor, horizonColor, t);
                } else {
                    float t = (normalizedY - 0.3) / 0.7;
                    t = smoothstep(0.0, 1.0, t);
                    skyColor = mix(horizonColor, zenithColor, t);
                }
                
                float pulse = 0.02 * sin(time * 0.1);
                skyColor += vec3(pulse, pulse * 0.5, pulse);
                
                float atmosphere = 1.0 - abs(worldPos.y) * 0.1;
                skyColor *= atmosphere;
                
                FragColor = vec4(skyColor, 1.0);
            }
            """;

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

    public void render(Camera camera, int width, int height, float time) {
        skyboxShader.bind();

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);

        Matrix4f view = new Matrix4f(camera.getViewMatrix());
        view.m[12] = 0;
        view.m[13] = 0;
        view.m[14] = 0;

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70.0f), (float) width / height, 0.1f, 1000.0f);

        skyboxShader.setUniform("view", view);
        skyboxShader.setUniform("projection", projection);
        skyboxShader.setUniform("horizonColor", horizonColor);
        skyboxShader.setUniform("zenithColor", zenithColor);
        skyboxShader.setUniform("fogColor", fogColor);
        skyboxShader.setUniform("time", time);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);

        skyboxShader.unbind();
    }

    public void setHorizonColor(Vector3f color) { this.horizonColor = color; }
    public void setZenithColor(Vector3f color) { this.zenithColor = color; }
    public void setFogColor(Vector3f color) { this.fogColor = color; }

    public void setDayColors() {
        horizonColor = new Vector3f(0.8f, 0.9f, 1.0f);
        zenithColor = new Vector3f(0.3f, 0.5f, 0.9f);
        fogColor = new Vector3f(0.7f, 0.8f, 0.9f);
    }

    public void setSunsetColors() {
        horizonColor = new Vector3f(1.0f, 0.6f, 0.3f);
        zenithColor = new Vector3f(0.4f, 0.3f, 0.6f);
        fogColor = new Vector3f(1.0f, 0.8f, 0.5f);
    }

    public void setNightColors() {
        horizonColor = new Vector3f(0.1f, 0.1f, 0.2f);
        zenithColor = new Vector3f(0.02f, 0.02f, 0.1f);
        fogColor = new Vector3f(0.05f, 0.05f, 0.15f);
    }

    public void cleanup() {
        if (vao != 0) { glDeleteVertexArrays(vao); vao = 0; }
        if (vbo != 0) { glDeleteBuffers(vbo); vbo = 0; }
        if (ebo != 0) { glDeleteBuffers(ebo); ebo = 0; }
        if (skyboxShader != null) { skyboxShader.cleanup(); skyboxShader = null; }
        LoggerHelper.betterPrint("Skybox renderer cleaned up", LoggerHelper.LogType.RENDERING);
    }
}
