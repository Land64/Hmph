package hmph.GUI;

import hmph.rendering.Hmph;
import hmph.rendering.shaders.ShaderProgram;
import hmph.util.debug.LoggerHelper;

import java.util.ArrayList;
import java.util.List;

import hmph.GUI.text.*;

public class GUIManager {
    private List<ButtonRenderer> buttons;
    private Hmph renderWindow;
    private boolean guiEnabled = false;
    private double mouseX, mouseY;
    private boolean mousePressed = false;

    private TextRenderer textRenderer;

    public GUIManager(Hmph renderWindow) {
        this.renderWindow = renderWindow;
        this.buttons = new ArrayList<>();
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