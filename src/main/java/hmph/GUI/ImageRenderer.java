package hmph.GUI;

import hmph.math.Matrix4f;
import hmph.math.Vector3f;
import hmph.rendering.shaders.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ImageRenderer {
    private int textureID;
    private int vao, vbo;
    private int width, height;
    private ShaderProgram shader;
    private float screenWidth, screenHeight;

    //Nice Image Renderer, Took forever tho.
    public ImageRenderer(String resourcePath, ShaderProgram shader, float screenWidth, float screenHeight) throws Exception {
        this.shader = shader;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        ByteBuffer imageBuffer;
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer comp = BufferUtils.createIntBuffer(1);

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) throw new Exception("Failed to load image: " + resourcePath);
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            STBImage.stbi_set_flip_vertically_on_load(false);
            imageBuffer = STBImage.stbi_load_from_memory(buffer, w, h, comp, 4);
            if (imageBuffer == null) {
                String error = STBImage.stbi_failure_reason();
                throw new Exception("Failed to load image from memory: " + resourcePath + " Error: " + error);
            }
        }

        width = w.get(0);
        height = h.get(0);
        

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.err.println("OpenGL error after texture creation: " + error);
        }

        STBImage.stbi_image_free(imageBuffer);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        error = glGetError();
        if (error != GL_NO_ERROR) {
            System.err.println("OpenGL error after VAO/VBO setup: " + error);
        }

    }


    //Actually Renders the darn thing
    public void renderImage(float x, float y, float w, float h) {
        boolean depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean blendEnabled = glIsEnabled(GL_BLEND);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();

        Matrix4f projection = new Matrix4f().identity();
        projection.m[0] = 2.0f / screenWidth;
        projection.m[5] = -2.0f / screenHeight;
        projection.m[12] = -1.0f;
        projection.m[13] = 1.0f;

        Matrix4f model = new Matrix4f().identity();

        shader.setUniform("projection", projection);
        shader.setUniform("model", model);
        shader.setUniform("color", new Vector3f(1f, 1f, 1f));
        shader.setUniform("texture1", 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glBindVertexArray(vao);

        //Extra vertices for the quad
        FloatBuffer vertices = BufferUtils.createFloatBuffer(6 * 4);
        vertices.put(new float[]{
                //triangle 1
                x,     y + h, 0, 0,  //bottom-left
                x,     y,     0, 1,  //top-left
                x + w, y,     1, 1,  //top-right
                //triangle 2
                x,     y + h, 0, 0,  //bottom-left
                x + w, y,     1, 1,  //top-right
                x + w, y + h, 1, 0   //bottom-right
        }).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glDrawArrays(GL_TRIANGLES, 0, 6);

        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.err.println("OpenGL error during rendering: " + error);
        }

        //cleanup
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        shader.unbind();

        //restore previous state
        if (depthTestEnabled) glEnable(GL_DEPTH_TEST);
        else glDisable(GL_DEPTH_TEST);

        if (blendEnabled) glEnable(GL_BLEND);
        else glDisable(GL_BLEND);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public ShaderProgram getShader() { return shader; }
}