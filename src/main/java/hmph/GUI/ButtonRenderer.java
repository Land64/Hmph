package hmph.GUI;

import hmph.rendering.shaders.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ButtonRenderer {
    private ImageRenderer normalRenderer;
    private ImageRenderer hoverRenderer;
    private ImageRenderer pressedRenderer;

    private float originalX, originalY, originalWidth, originalHeight;

    private float x, y, width, height;

    private float currentScreenWidth, currentScreenHeight;
    private float referenceWidth, referenceHeight;
    private long windowHandle;

    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean wasPressed = false;
    private boolean enabled = true;

    private TextRenderer textRenderer;
    private String buttonText = "";
    private float textOffX = 0;
    private float textOffY = 0;

    private ButtonClickListener clickListener;

    public interface ButtonClickListener {
        void onClick();
    }

    public ButtonRenderer(String imagePath, ShaderProgram shader, long windowHandle, float referenceWidth, float referenceHeight, float x, float y, float width, float height) throws Exception {
        this.windowHandle = windowHandle;
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;

        updateCurrentScreenSize();

        this.originalX = x;
        this.originalY = y;
        this.originalWidth = width;
        this.originalHeight = height;

        updateScaledDimensions();

        this.normalRenderer = new ImageRenderer(imagePath, shader, currentScreenWidth, currentScreenHeight);
        this.hoverRenderer = this.normalRenderer;
        this.pressedRenderer = this.normalRenderer;
    }

    public ButtonRenderer(String normalImagePath, String hoverImagePath, String pressedImagePath, ShaderProgram shader, long windowHandle, float referenceWidth, float referenceHeight, float x, float y, float width, float height) throws Exception {
        this.windowHandle = windowHandle;
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;

        updateCurrentScreenSize();

        this.originalX = x;
        this.originalY = y;
        this.originalWidth = width;
        this.originalHeight = height;

        updateScaledDimensions();

        this.normalRenderer = new ImageRenderer(normalImagePath, shader, currentScreenWidth, currentScreenHeight);
        this.hoverRenderer = hoverImagePath != null ?  new ImageRenderer(hoverImagePath, shader, currentScreenWidth, currentScreenHeight) : this.normalRenderer;
        this.pressedRenderer = pressedImagePath != null ? new ImageRenderer(pressedImagePath, shader, currentScreenWidth, currentScreenHeight) : this.normalRenderer;
    }

    public ButtonRenderer(ShaderProgram shader, long windowHandle,
                          float referenceWidth, float referenceHeight,
                          float x, float y, float width, float height) throws Exception {
        this("assets/images/gui/buttan.png", shader, windowHandle,
                referenceWidth, referenceHeight, x, y, width, height);
    }

    public void setText(String text, TextRenderer renderer) {
        this.buttonText = text != null ? text : "";
        this.textRenderer = renderer;
    }

    public void setTextOffset(float offsetX, float offsetY) {
        this.textOffX = offsetX;
        this.textOffY = offsetY;
    }

    private void updateCurrentScreenSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(windowHandle, width, height);
            this.currentScreenWidth = width.get(0);
            this.currentScreenHeight = height.get(0);
        }
    }

    private void updateScaledDimensions() {
        float scaleX = currentScreenWidth / referenceWidth;
        float scaleY = currentScreenHeight / referenceHeight;

        float scale = Math.min(scaleX, scaleY);

        this.x = originalX * scale;
        this.y = originalY * scale;
        this.width = originalWidth * scale;
        this.height = originalHeight * scale;

        if (scale == scaleY) {
            float xOffset = (currentScreenWidth - referenceWidth * scale) / 2;
            this.x += xOffset;
        } else {
            float yOffset = (currentScreenHeight - referenceHeight * scale) / 2;
            this.y += yOffset;
        }
    }

    public void updateScreenSize() {
        updateCurrentScreenSize();
        updateScaledDimensions();
    }

    public void updateScreenSize(float newScreenWidth, float newScreenHeight) {
        this.currentScreenWidth = newScreenWidth;
        this.currentScreenHeight = newScreenHeight;
        updateScaledDimensions();
    }

    public void render() {
        if (!enabled) return;
        ImageRenderer currentRenderer = isPressed ? pressedRenderer : isHovered ? hoverRenderer : normalRenderer;
        currentRenderer.renderImage(x, y, width, height);
        if (textRenderer != null && buttonText != null && !buttonText.isEmpty()) {
            FloatBuffer tmpX = BufferUtils.createFloatBuffer(1).put(0, 0);
            FloatBuffer tmpY = BufferUtils.createFloatBuffer(1).put(0, 0);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc();
            float totalWidth = 0;
            for (char c : buttonText.toCharArray()) {
                if (c < 32 || c >= 128) continue;
                FloatBuffer xbCopy = BufferUtils.createFloatBuffer(1).put(0, 0);
                FloatBuffer ybCopy = BufferUtils.createFloatBuffer(1).put(0, 0);
                STBTruetype.stbtt_GetBakedQuad(textRenderer.charData, TextRenderer.BITMAP_W, TextRenderer.BITMAP_H, c - 32, xbCopy, ybCopy, q, true);
                totalWidth += q.x1() - q.x0();
            }
            q.free();
            float textX = x + (width - totalWidth) / 2 + textOffX;
            float textY = y + (height / 2) - (textRenderer.getFontHeight() / 2) + textOffY;
            textRenderer.renderText(buttonText, textX, textY);
        }
    }

    public void setClickListener(ButtonClickListener listener) {
        this.clickListener = listener;
    }

    public void setPosition(float x, float y) {
        this.originalX = x;
        this.originalY = y;
        updateScaledDimensions();
    }

    public void setSize(float width, float height) {
        this.originalWidth = width;
        this.originalHeight = height;
        updateScaledDimensions();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            isHovered = false;
            isPressed = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHovered() {
        return isHovered;
    }

    public boolean isPressed() {
        return isPressed;
    }

    public void update(double mouseX, double mouseY, boolean mousePressed) {
        if (!enabled) {
            isHovered = false;
            isPressed = false;
            return;
        }
        boolean mouseOver = mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
        isHovered = mouseOver;
        if (mouseOver && mousePressed && !wasPressed) isPressed = true;
        else if (isPressed && !mousePressed) {
            if (mouseOver && clickListener != null) clickListener.onClick();
            isPressed = false;
        } else if (!mousePressed) isPressed = false;
        wasPressed = mousePressed;
    }


    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public float getOriginalX() { return originalX; }
    public float getOriginalY() { return originalY; }
    public float getOriginalWidth() { return originalWidth; }
    public float getOriginalHeight() { return originalHeight; }

    public float getScaleFactor() {
        float scaleX = currentScreenWidth / referenceWidth;
        float scaleY = currentScreenHeight / referenceHeight;
        return Math.min(scaleX, scaleY);
    }

    public boolean contains(double pointX, double pointY) {
        return pointX >= x && pointX <= x + width &&
                pointY >= y && pointY <= y + height;
    }

    public void cleanup() {
        if (normalRenderer != null) normalRenderer.cleanup();
        if (hoverRenderer != null && hoverRenderer != normalRenderer) {
            hoverRenderer.cleanup();
        }
        if (pressedRenderer != null && pressedRenderer != normalRenderer) {
            pressedRenderer.cleanup();
        }
    }
}