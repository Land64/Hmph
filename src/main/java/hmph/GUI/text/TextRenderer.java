package hmph.GUI.text;

import hmph.rendering.shaders.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextRenderer {
    public static final int BITMAP_W = 1024;
    public static final int BITMAP_H = 1024;
    private static final int FIRST_CHARACTER = 32;
    private static final int CHARACTER_COUNT = 96;
    private static final int MAX_CHARACTERS_PER_DRAW = 4096;

    private int vao;
    private int vbo;
    private int textureID;
    private final Glyph[] glyphs = new Glyph[CHARACTER_COUNT];
    private final ShaderProgram shader;
    public float screenWidth;
    public float screenHeight;
    private final float fontHeight;
    private float textColorR = 1f;
    private float textColorG = 1f;
    private float textColorB = 1f;
    private float textColorA = 1f;
    private final List<TextObject> textObjects = new ArrayList<>();

    public TextRenderer(String fontResourcePath, float fontHeight, ShaderProgram shader,
                        float screenWidth, float screenHeight) throws IOException {
        this.shader = shader;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.fontHeight = fontHeight;

        STBTTBakedChar.Buffer baked = null;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fontResourcePath)) {
            if (input == null) throw new IOException("Font file not found: " + fontResourcePath);
            ByteBuffer fontBuffer = toDirectBuffer(input);
            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
            baked = STBTTBakedChar.malloc(CHARACTER_COUNT);
            int result = STBTruetype.stbtt_BakeFontBitmap(fontBuffer, fontHeight, bitmap,
                    BITMAP_W, BITMAP_H, FIRST_CHARACTER, baked);
            if (result <= 0) throw new IOException("Failed to bake font bitmap");

            for (int i = 0; i < CHARACTER_COUNT; i++) {
                STBTTBakedChar character = baked.get(i);
                glyphs[i] = new Glyph(character.x0(), character.y0(), character.x1(), character.y1(),
                        character.xoff(), character.yoff(), character.xadvance());
            }

            textureID = glGenTextures();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureID);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, BITMAP_W, BITMAP_H,
                    0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER,
                    (long) MAX_CHARACTERS_PER_DRAW * 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            glBindTexture(GL_TEXTURE_2D, 0);
        } catch (Exception e) {
            cleanup();
            throw new IOException("Failed to initialize TextRenderer", e);
        } finally {
            if (baked != null) baked.free();
        }
    }

    public float getFontHeight() {
        return fontHeight;
    }

    public void renderText(String text, float x, float y) {
        if (text == null || text.isEmpty() || screenWidth <= 0 || screenHeight <= 0) return;

        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFace = glIsEnabled(GL_CULL_FACE);
        boolean blend = glIsEnabled(GL_BLEND);
        int currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        try {
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            shader.bind();
            FloatBuffer projection = BufferUtils.createFloatBuffer(16);
            projection.put(new float[]{
                    2.0f / screenWidth, 0, 0, 0,
                    0, -2.0f / screenHeight, 0, 0,
                    0, 0, -1.0f, 0,
                    -1.0f, 1.0f, 0, 1.0f
            }).flip();
            int projectionLocation = glGetUniformLocation(shader.getProgramId(), "projection");
            if (projectionLocation >= 0) glUniformMatrix4fv(projectionLocation, false, projection);
            int textureLocation = glGetUniformLocation(shader.getProgramId(), "textTexture");
            if (textureLocation >= 0) glUniform1i(textureLocation, 0);
            int colorLocation = glGetUniformLocation(shader.getProgramId(), "textColor");
            if (colorLocation >= 0) glUniform4f(colorLocation,
                    textColorR, textColorG, textColorB, textColorA);

            int drawLength = Math.min(text.length(), MAX_CHARACTERS_PER_DRAW);
            FloatBuffer vertices = BufferUtils.createFloatBuffer(drawLength * 6 * 4);
            float cursorX = x;
            for (int i = 0; i < drawLength; i++) {
                char c = text.charAt(i);
                int glyphIndex = c - FIRST_CHARACTER;
                if (glyphIndex < 0 || glyphIndex >= glyphs.length) continue;
                Glyph glyph = glyphs[glyphIndex];

                float x0 = (float) Math.floor(cursorX + glyph.xOffset + 0.5f);
                float y0 = (float) Math.floor(y + glyph.yOffset + 0.5f);
                float x1 = x0 + glyph.pixelX1 - glyph.pixelX0;
                float y1 = y0 + glyph.pixelY1 - glyph.pixelY0;
                float s0 = glyph.pixelX0 / (float) BITMAP_W;
                float t0 = glyph.pixelY0 / (float) BITMAP_H;
                float s1 = glyph.pixelX1 / (float) BITMAP_W;
                float t1 = glyph.pixelY1 / (float) BITMAP_H;

                vertices.put(x0).put(y0).put(s0).put(t0);
                vertices.put(x0).put(y1).put(s0).put(t1);
                vertices.put(x1).put(y1).put(s1).put(t1);
                vertices.put(x0).put(y0).put(s0).put(t0);
                vertices.put(x1).put(y1).put(s1).put(t1);
                vertices.put(x1).put(y0).put(s1).put(t0);
                cursorX += glyph.advance;
            }
            vertices.flip();

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureID);
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            glDrawArrays(GL_TRIANGLES, 0, vertices.remaining() / 4);
        } finally {
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            glBindTexture(GL_TEXTURE_2D, 0);
            if (currentProgram != 0) glUseProgram(currentProgram);
            else glUseProgram(0);
            if (depthTest) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
            if (cullFace) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
            if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        }
    }

    public void renderText(TextObject object) {
        setColor(object.getR(), object.getG(), object.getB(), object.getA());
        renderText(object.getText(), object.getX(), object.getY());
    }

    public void renderAllTexts() {
        for (TextObject object : textObjects) renderText(object);
    }

    public void addTextObject(TextObject object) {
        if (!textObjects.contains(object)) textObjects.add(object);
    }

    public void removeTextObject(TextObject object) {
        textObjects.remove(object);
    }

    public void clearTextObjects() {
        textObjects.clear();
    }

    public void cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (textureID != 0) {
            glDeleteTextures(textureID);
            textureID = 0;
        }
    }

    public float getTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0f;
        float width = 0f;
        for (char c : text.toCharArray()) {
            int glyphIndex = c - FIRST_CHARACTER;
            if (glyphIndex >= 0 && glyphIndex < glyphs.length) width += glyphs[glyphIndex].advance;
        }
        return width;
    }

    public void setColor(float r, float g, float b, float a) {
        textColorR = clamp01(r);
        textColorG = clamp01(g);
        textColorB = clamp01(b);
        textColorA = clamp01(a);
    }

    public void setScreenSize(float screenWidth, float screenHeight) {
        this.screenWidth = Math.max(1f, screenWidth);
        this.screenHeight = Math.max(1f, screenHeight);
    }

    public ShaderProgram getShader() {
        return shader;
    }

    private static ByteBuffer toDirectBuffer(InputStream source) throws IOException {
        byte[] bytes = source.readAllBytes();
        return BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private record Glyph(int pixelX0, int pixelY0, int pixelX1, int pixelY1,
                         float xOffset, float yOffset, float advance) {}
}
