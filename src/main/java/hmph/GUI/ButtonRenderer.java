package hmph.GUI;

import hmph.rendering.shaders.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.w3c.dom.Text;
import hmph.GUI.text.*;

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
    private TextObject buttonTextObject;
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
        this.hoverRenderer = hoverImagePath!=null ? new ImageRenderer(hoverImagePath, shader, currentScreenWidth, currentScreenHeight) : this.normalRenderer;
        this.pressedRenderer = pressedImagePath!=null ? new ImageRenderer(pressedImagePath, shader, currentScreenWidth, currentScreenHeight) : this.normalRenderer;
    }

    public ButtonRenderer(ShaderProgram shader, long windowHandle, float referenceWidth, float referenceHeight, float x, float y, float width, float height) throws Exception {
        this("assets/images/gui/button_rectangle_flat.png", shader, windowHandle, referenceWidth, referenceHeight, x, y, width, height);
    }

    /**
     * Set text object for button
     */
    public void setText(TextObject textObject) {
        this.buttonTextObject = textObject;
        if (textRenderer!=null && textObject!=null) {
            textRenderer.addTextObject(textObject);
        }
    }

    /**
     * Set text renderer for button
     */
    public void setTextRenderer(TextRenderer renderer) {
        this.textRenderer = renderer;
        if (buttonTextObject!=null && renderer!=null) {
            renderer.addTextObject(buttonTextObject);
        }
    }

    /**
     * Set text color
     */
    public void setTextColor(float r, float g, float b, float a, TextObject renderer) {
        renderer.setColor(r, g, b, a);
    }

    /**
     * Set text offset
     */
    public void setTextOffset(float offsetX, float offsetY) {
        if (buttonTextObject!=null) {
            buttonTextObject.setPosition(buttonTextObject.getX()+offsetX, buttonTextObject.getY()+offsetY);
        }
    }

    /**
     * Update current screen size from window
     */
    private void updateCurrentScreenSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(windowHandle, width, height);
            this.currentScreenWidth = width.get(0);
            this.currentScreenHeight = height.get(0);
        }
    }

    /**
     * Update scaled dimensions based on current screen size
     */
    private void updateScaledDimensions() {
        float scaleX = currentScreenWidth/referenceWidth;
        float scaleY = currentScreenHeight/referenceHeight;
        float scale = Math.min(scaleX, scaleY);

        this.width = originalWidth*scale;
        this.height = originalHeight*scale;
        this.x = originalX*scale;
        this.y = originalY*scale;

        if (scaleX > scaleY) {
            float xOffset = (currentScreenWidth - referenceWidth*scale)/2;
            this.x += xOffset;
        }
        else if (scaleY > scaleX) {
            float yOffset = (currentScreenHeight - referenceHeight*scale)/2;
            this.y += yOffset;
        }
    }

    /**
     * Update screen size from window
     */
    public void updateScreenSize() {
        updateCurrentScreenSize();
        updateScaledDimensions();
    }

    /**
     * Update screen size with given dimensions
     */
    public void updateScreenSize(float newScreenWidth, float newScreenHeight) {
        this.currentScreenWidth = newScreenWidth;
        this.currentScreenHeight = newScreenHeight;
        updateScaledDimensions();
    }

    /**
     * Render button with proper text positioning
     */
    public void render() {
        if (!enabled) return;

        ImageRenderer currentRenderer = isPressed ? pressedRenderer : isHovered ? hoverRenderer : normalRenderer;
        currentRenderer.renderImage(x, y, width, height);

        if (textRenderer!=null && buttonTextObject!=null) {
            String text = buttonTextObject.getText();
            float textWidth = textRenderer.getTextWidth(text);
            float textX = x+(width-textWidth)/2f;
            float textY = y+(height+textRenderer.getFontHeight())/2f-textRenderer.getFontHeight()*0.3f;
            buttonTextObject.setPosition(textX, textY);
            textRenderer.renderText(buttonTextObject);
        }
    }

    /**
     * Set click listener
     */
    public void setClickListener(ButtonClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Set button position
     */
    public void setPosition(float x, float y) {
        this.originalX = x;
        this.originalY = y;
        updateScaledDimensions();
    }

    /**
     * Set button size
     */
    public void setSize(float width, float height) {
        this.originalWidth = width;
        this.originalHeight = height;
        updateScaledDimensions();
    }

    /**
     * Set button enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            isHovered = false;
            isPressed = false;
        }
    }

    public boolean isEnabled() { return enabled; }
    public boolean isHovered() { return isHovered; }
    public boolean isPressed() { return isPressed; }

    /**
     * Update button state and handle mouse interactions
     */
    public void update(double mouseX, double mouseY, boolean mousePressed) {
        if (!enabled) {
            isHovered = false;
            isPressed = false;
            wasPressed = mousePressed;
            return;
        }

        boolean mouseOver = mouseX>=x && mouseX<=x+width && mouseY>=y && mouseY<=y+height;
        isHovered = mouseOver;

        if (mouseOver && mousePressed && !wasPressed) {
            isPressed = true;
        } else if (isPressed && !mousePressed && mouseOver) {
            if (clickListener!=null) clickListener.onClick();
            isPressed = false;
        } else if (!mousePressed) {
            isPressed = false;
        }

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

    /**
     * Get current scale factor
     */
    public float getScaleFactor() {
        float scaleX = currentScreenWidth/referenceWidth;
        float scaleY = currentScreenHeight/referenceHeight;
        return Math.min(scaleX, scaleY);
    }

    /**
     * Check if point is within button bounds
     */
    public boolean contains(double pointX, double pointY) {
        return pointX>=x && pointX<=x+width && pointY>=y && pointY<=y+height;
    }

    /**
     * Cleanup button resources
     */
    public void cleanup() {
        if (normalRenderer!=null) normalRenderer.cleanup();
        if (hoverRenderer!=null && hoverRenderer!=normalRenderer) {
            hoverRenderer.cleanup();
        }
        if (pressedRenderer!=null && pressedRenderer!=normalRenderer) {
            pressedRenderer.cleanup();
        }
    }
}