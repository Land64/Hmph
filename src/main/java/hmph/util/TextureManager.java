package hmph.util;

import hmph.util.debug.LoggerHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL12.glTexSubImage3D;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class TextureManager {
    public static final int BLOCK_TEXTURE_SIZE = 16;

    private final Map<String, Integer> textureLayers = new LinkedHashMap<>();
    private int textureArray;

    public void loadTextureArray(Map<String, String> resources) {
        cleanup();
        textureLayers.clear();

        textureArray = glGenTextures();
        if (textureArray == 0) throw new IllegalStateException("Failed to create block texture array");

        glBindTexture(GL_TEXTURE_2D_ARRAY, textureArray);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        int layerCount = resources.size() + 1;
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8,
                BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE, layerCount,
                0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        uploadFallbackLayer(0);
        int layer = 1;
        int loaded = 0;
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            textureLayers.put(entry.getKey(), layer);
            if (uploadLayer(layer, entry.getValue())) loaded++;
            else uploadFallbackLayer(layer);
            layer++;
        }

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        LoggerHelper.betterPrint("Loaded " + loaded + "/" + resources.size() +
                " block textures into one texture array", LoggerHelper.LogType.RENDERING);
    }

    private boolean uploadLayer(int layer, String resourcePath) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                LoggerHelper.betterPrint("Missing texture: " + resourcePath, LoggerHelper.LogType.ERROR);
                return false;
            }

            byte[] bytes = input.readAllBytes();
            ByteBuffer encoded = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
            IntBuffer width = BufferUtils.createIntBuffer(1);
            IntBuffer height = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);
            STBImage.stbi_set_flip_vertically_on_load(false);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
            if (pixels == null) {
                LoggerHelper.betterPrint("Could not decode " + resourcePath + ": " +
                        STBImage.stbi_failure_reason(), LoggerHelper.LogType.ERROR);
                return false;
            }

            try {
                if (width.get(0) != BLOCK_TEXTURE_SIZE || height.get(0) != BLOCK_TEXTURE_SIZE) {
                    LoggerHelper.betterPrint("Texture must be 16x16: " + resourcePath,
                            LoggerHelper.LogType.ERROR);
                    return false;
                }
                glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer,
                        BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE, 1,
                        GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                return true;
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error loading texture " + resourcePath + ": " + e.getMessage(),
                    LoggerHelper.LogType.ERROR);
            return false;
        }
    }

    private void uploadFallbackLayer(int layer) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(BLOCK_TEXTURE_SIZE * BLOCK_TEXTURE_SIZE * 4);
        for (int y = 0; y < BLOCK_TEXTURE_SIZE; y++) {
            for (int x = 0; x < BLOCK_TEXTURE_SIZE; x++) {
                boolean dark = ((x / 4) + (y / 4)) % 2 == 0;
                pixels.put((byte) (dark ? 35 : 220));
                pixels.put((byte) 0);
                pixels.put((byte) (dark ? 45 : 220));
                pixels.put((byte) 255);
            }
        }
        pixels.flip();
        glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer,
                BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE, 1,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    }

    public int getTextureArray() {
        return textureArray;
    }

    public int getTextureLayer(String name) {
        return textureLayers.getOrDefault(name, 0);
    }

    public Map<String, Integer> getTextureLayers() {
        return Collections.unmodifiableMap(textureLayers);
    }

    public void bindTextureArray(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureArray);
    }

    public void cleanup() {
        if (textureArray != 0) {
            glDeleteTextures(textureArray);
            textureArray = 0;
        }
    }
}
