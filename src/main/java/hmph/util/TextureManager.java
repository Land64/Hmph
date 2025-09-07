package hmph.util;

import hmph.util.debug.LoggerHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TextureManager {
    private Map<String, Integer> textures = new HashMap<>();
    private int defaultTexture = 0;

    public TextureManager() {
        createDefaultTexture();
    }

    private void createDefaultTexture() {
        byte[] pixels = {
                (byte)255, (byte)255, (byte)255, (byte)255, // White
                (byte)255, (byte)255, (byte)255, (byte)255, // White
                (byte)255, (byte)255, (byte)255, (byte)255, // White
                (byte)255, (byte)255, (byte)255, (byte)255  // White
        };

        ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length);
        buffer.put(pixels);
        buffer.flip();

        defaultTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, defaultTexture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 2, 2, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        LoggerHelper.betterPrint("Created default texture: " + defaultTexture, LoggerHelper.LogType.RENDERING);
    }


    public int loadTexture(String name, String path) {
        if (textures.containsKey(name)) {
            return textures.get(name);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
            if (inputStream == null) {
                System.err.println("Could not find texture: " + path);
                textures.put(name, defaultTexture);
                return defaultTexture;
            }

            byte[] imageBytes = inputStream.readAllBytes();
            ByteBuffer imageBuffer = stack.malloc(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();

            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);
            if (image == null) {
                System.err.println("Failed to load texture: " + path + " - " + STBImage.stbi_failure_reason());
                textures.put(name, defaultTexture);
                return defaultTexture;
            }

            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            STBImage.stbi_image_free(image);
            glBindTexture(GL_TEXTURE_2D, 0);

            textures.put(name, textureId);
            LoggerHelper.betterPrint("Loaded texture: " + name + " (" + width.get(0) + "x" + height.get(0) + ") -> ID " + textureId, LoggerHelper.LogType.RENDERING);
            return textureId;

        } catch (Exception e) {
            System.err.println("Error loading texture " + path + ": " + e.getMessage());
            e.printStackTrace();
            textures.put(name, defaultTexture);
            return defaultTexture;
        }
    }

    public int getTexture(String name) {
        return textures.getOrDefault(name, defaultTexture);
    }

    public void bindTexture(String name, int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, getTexture(name));
    }

    public void cleanup() {
        for (int textureId : textures.values()) {
            glDeleteTextures(textureId);
        }
        textures.clear();
    }
}