package hmph.GUI;

import hmph.rendering.Hmph;
import hmph.rendering.shaders.ShaderProgram;
import hmph.util.debug.LoggerHelper;

import java.util.ArrayList;
import java.util.List;

public class GUIManager {
    private List<ButtonRenderer> buttons;
    private Hmph renderWindow;
    private boolean guiEnabled = false;
    private double mouseX, mouseY;
    private boolean mousePressed = false;

    private static final float REFERENCE_WIDTH = 1920f;
    private static final float REFERENCE_HEIGHT = 1080f;

    public ButtonRenderer resumeButton;
    public ButtonRenderer settingsButton;
    public ButtonRenderer exitButton;

    private TextRenderer textRenderer;

    public GUIManager(Hmph renderWindow) {
        this.renderWindow = renderWindow;
        this.buttons = new ArrayList<>();
    }

    public void initialize() throws Exception {
        try {
            ShaderProgram texturedShader = renderWindow.getShaderManager().getShader("textured");
            if (texturedShader == null) {
                LoggerHelper.betterPrint("Warning: textured shader not found for GUI", LoggerHelper.LogType.WARNING);
                return;
            }

            ShaderProgram textShader = renderWindow.getShaderManager().getShader("text");
            if (textShader == null) {
                LoggerHelper.betterPrint("Warning: text shader not found for GUI", LoggerHelper.LogType.WARNING);
                return;
            }

            long windowHandle = renderWindow.getWindowHandle();

            textRenderer = new TextRenderer("assets/font.otf", 36, textShader, REFERENCE_WIDTH, REFERENCE_HEIGHT);

            resumeButton = new ButtonRenderer(
                    texturedShader, windowHandle,
                    REFERENCE_WIDTH, REFERENCE_HEIGHT,
                    REFERENCE_WIDTH/2 - 500, REFERENCE_HEIGHT/2 - 500, 1000, 250
            );
            resumeButton.setClickListener(() -> {
                LoggerHelper.betterPrint("Resume clicked!", LoggerHelper.LogType.INFO);
                toggleGUI();
            });
            resumeButton.setText("RESUME", textRenderer);
            buttons.add(resumeButton);

            settingsButton = new ButtonRenderer(
                    texturedShader, windowHandle,
                    REFERENCE_WIDTH, REFERENCE_HEIGHT,
                    REFERENCE_WIDTH/2 - 500, REFERENCE_HEIGHT/2 - 150, 1000, 250
            );
            settingsButton.setClickListener(() -> {
                LoggerHelper.betterPrint("Settings clicked!", LoggerHelper.LogType.INFO);
            });
            settingsButton.setText("SETTINGS", textRenderer);
            buttons.add(settingsButton);

            exitButton = new ButtonRenderer(
                    texturedShader, windowHandle,
                    REFERENCE_WIDTH, REFERENCE_HEIGHT,
                    REFERENCE_WIDTH/2 - 500, REFERENCE_HEIGHT/2 + 200, 1000, 250
            );
            exitButton.setClickListener(() -> {
                LoggerHelper.betterPrint("Exit clicked!", LoggerHelper.LogType.INFO);
                org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose(windowHandle, true);
            });
            exitButton.setText("EXIT", textRenderer);
            buttons.add(exitButton);

            LoggerHelper.betterPrint("GUI Manager initialized with " + buttons.size() + " buttons", LoggerHelper.LogType.INFO);

        } catch (Exception e) {
            LoggerHelper.betterPrint("Failed to initialize GUI: " + e.getMessage(), LoggerHelper.LogType.ERROR);
            throw e;
        }
    }

    public void addButton(ButtonRenderer button) {
        buttons.add(button);
    }

    public void update(double mouseX, double mouseY, boolean mousePressed) {
        if (!guiEnabled) return;

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mousePressed = mousePressed;

        for (ButtonRenderer button : buttons) {
            button.update(mouseX, mouseY, mousePressed);
        }
    }

    public void render() {
        if (!guiEnabled) return;

        // TODO: Render background later on

        for (ButtonRenderer button : buttons) {
            button.render();
        }
    }

    private void renderBackground() {
        // For now we'll just clear the background slightly
    }

    public void toggleGUI() {
        guiEnabled = !guiEnabled;
        LoggerHelper.betterPrint("GUI " + (guiEnabled ? "enabled" : "disabled"), LoggerHelper.LogType.INFO);
    }

    public void setEnabled(boolean enabled) {
        this.guiEnabled = enabled;
    }

    public boolean isEnabled() {
        return guiEnabled;
    }

    public void onWindowResize() {
        for (ButtonRenderer button : buttons) {
            button.updateScreenSize();
        }
    }

    public void onWindowResize(float newWidth, float newHeight) {
        for (ButtonRenderer button : buttons) {
            button.updateScreenSize(newWidth, newHeight);
        }
    }

    public boolean isMouseOverGUI() {
        if (!guiEnabled) return false;

        for (ButtonRenderer button : buttons) {
            if (button.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public void cleanup() {
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
        for (ButtonRenderer button : buttons) {
            button.cleanup();
        }
        buttons.clear();
        LoggerHelper.betterPrint("GUI Manager cleaned up", LoggerHelper.LogType.INFO);
    }
}