package hmph.GUI;

import hmph.rendering.shaders.ShaderProgram;
import hmph.rendering.shaders.ShaderManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextRenderer {
    private static final int BITMAP_W = 512;
    private static final int BITMAP_H = 512;
    private int vao;
    private int vbo;
    private int textureID;
    private STBTTBakedChar.Buffer charData;
    private ShaderProgram shader;
    private float screenWidth;
    private float screenHeight;

    public TextRenderer(String fontResourcePath, float fontHeight, ShaderProgram shader, float screenWidth, float screenHeight) throws IOException {
        this.shader = shader;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fontResourcePath)) {
            if (is == null) {
                throw new IOException("Font file not found: " + fontResourcePath);
            }
            ByteBuffer fontBuffer = ioResourceToByteBuffer(is);
            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
            charData = STBTTBakedChar.malloc(96);
            if (STBTruetype.stbtt_BakeFontBitmap(fontBuffer, fontHeight, bitmap, BITMAP_W, BITMAP_H, 32, charData) <= 0) {
                throw new IOException("Failed to bake font bitmap");
            }
            textureID = glGenTextures();
            if (textureID == 0) {
                throw new RuntimeException("Failed to generate texture ID");
            }
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureID);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, BITMAP_W, BITMAP_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, 1024 * 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        } catch (Exception e) {
            if (textureID != 0) glDeleteTextures(textureID);
            if (vao != 0) glDeleteVertexArrays(vao);
            if (vbo != 0) glDeleteBuffers(vbo);
            if (charData != null) charData.free();
            throw new IOException("Failed to initialize TextRenderer", e);
        }
    }

    public void renderText(String text, float x, float y) {
        if (text == null || text.isEmpty()) {
            return;
        }
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFace = glIsEnabled(GL_CULL_FACE);
        boolean blend = glIsEnabled(GL_BLEND);
        int[] currentProgram = new int[1];
        glGetIntegerv(GL_CURRENT_PROGRAM, currentProgram);

        try {
            if (depthTest) glDisable(GL_DEPTH_TEST);
            if (cullFace) glDisable(GL_CULL_FACE);
            if (!blend) {
                glEnable(GL_BLEND);
            }
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            while (glGetError() != GL_NO_ERROR);
            shader.bind();
            FloatBuffer projection = BufferUtils.createFloatBuffer(16);
            projection.put(new float[]{
                    2.0f / screenWidth, 0, 0, 0,
                    0, -2.0f / screenHeight, 0, 0,
                    0, 0, -1.0f, 0,
                    -1.0f, 1.0f, 0, 1.0f
            });
            projection.flip();
            int projLoc = glGetUniformLocation(shader.getProgramId(), "projection");
            if (projLoc != -1) {
                glUniformMatrix4fv(projLoc, false, projection);
            }
            int texLoc = glGetUniformLocation(shader.getProgramId(), "textTexture");
            if (texLoc != -1) {
                glUniform1i(texLoc, 0);
            }
            int colorLoc = glGetUniformLocation(shader.getProgramId(), "textColor");
            if (colorLoc != -1) {
                glUniform3f(colorLoc, 1.0f, 1.0f, 1.0f);
            }
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureID);
            glBindVertexArray(vao);
            FloatBuffer vertices = BufferUtils.createFloatBuffer(text.length() * 6 * 4);
            FloatBuffer xb = BufferUtils.createFloatBuffer(1).put(0, x);
            FloatBuffer yb = BufferUtils.createFloatBuffer(1).put(0, y);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc();
            for (char c : text.toCharArray()) {
                if (c < 32 || c >= 128) continue;
                STBTruetype.stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - 32, xb, yb, q, true);
                float x0 = q.x0();
                float y0 = q.y0();
                float x1 = q.x1();
                float y1 = q.y1();
                float s0 = q.s0();
                float t0 = q.t0();
                float s1 = q.s1();
                float t1 = q.t1();
                vertices.put(new float[]{
                        x0, y0, s0, t0,
                        x0, y1, s0, t1,
                        x1, y1, s1, t1,
                        x0, y0, s0, t0,
                        x1, y1, s1, t1,
                        x1, y0, s1, t0
                });
            }
            q.free();
            vertices.flip();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            glDrawArrays(GL_TRIANGLES, 0, vertices.limit() / 4);
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                System.err.println("OpenGL error during text rendering: " + error);
            }
        } catch (Exception e) {
            System.err.println("Error rendering text: " + e.getMessage());
            e.printStackTrace();
        } finally {
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            glBindTexture(GL_TEXTURE_2D, 0);
            if (shader != null) {
                shader.unbind();
            }
            if (currentProgram[0] != 0) {
                glUseProgram(currentProgram[0]);
            }
            if (depthTest) glEnable(GL_DEPTH_TEST);
            if (cullFace) glEnable(GL_CULL_FACE);
            if (!blend) glDisable(GL_BLEND);
        }
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteTextures(textureID);
        charData.free();
    }

    public ShaderProgram getShader() {
        return shader;
    }

    private ByteBuffer ioResourceToByteBuffer(InputStream source) throws IOException {
        byte[] bytes = source.readAllBytes();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }
}