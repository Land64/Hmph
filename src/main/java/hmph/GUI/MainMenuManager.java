package hmph.GUI;

import hmph.GUI.ButtonRenderer;
import hmph.GUI.text.*;
import hmph.rendering.Hmph;
import hmph.rendering.shaders.ShaderProgram;
import hmph.util.audio.AudioManager;
import hmph.util.debug.LoggerHelper;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainMenuManager {
    private List<ButtonRenderer> buttons;
    private Hmph renderWindow;
    private boolean menuVisible = false;
    private double mouseX, mouseY;
    private boolean mousePressed = false;
    private TextObject titleText;
    private List<TextObject> overlayTexts = new ArrayList<>();
    private float currentScreenWidth, currentScreenHeight;
    private AudioManager backgroundMusic;

    private static final float REFERENCE_WIDTH = 1920f;
    private static final float REFERENCE_HEIGHT = 1080f;

    private ButtonRenderer playButton;
    private ButtonRenderer settingsButton;
    private ButtonRenderer creditsButton;
    private ButtonRenderer exitButton;

    private MenuState currentState = MenuState.MAIN;
    private MenuState previousState = MenuState.MAIN;

    private TextRenderer textRenderer;
    private TextRenderer titleRenderer;

    public enum MenuState {
        MAIN,
        SETTINGS,
        CREDITS
    }

    /** Create menu manager */
    public MainMenuManager(Hmph renderWindow) {
        this.renderWindow = renderWindow;
        this.buttons = new ArrayList<>();
    }

    /** Initialize menu system */
    public void initialize() throws Exception {
        ShaderProgram textShader = renderWindow.getShaderManager().getShader("text");
        textRenderer = new TextRenderer("assets/font.otf", 36, textShader, REFERENCE_WIDTH, REFERENCE_HEIGHT);
        titleRenderer = new TextRenderer("assets/font.otf", 72, textShader, REFERENCE_WIDTH, REFERENCE_HEIGHT);

        backgroundMusic = new AudioManager();
        if (backgroundMusic.initialize()) {
            backgroundMusic.setMasterVolume(0.10f);
            boolean theme1Loaded = backgroundMusic.loadSound("MM-THEME", "assets/sounds/music/MenuTheme.wav");

            if (theme1Loaded) {
                List<String> playlistSongs = new ArrayList<>();
                playlistSongs.add("MM-THEME");

                backgroundMusic.createPlaylist("MENU-PLAYLIST", playlistSongs, true);
                LoggerHelper.betterPrint("Background music playlist created with " + playlistSongs.size() + " songs", LoggerHelper.LogType.INFO);
            } else {
                LoggerHelper.betterPrint("Failed to load any background music", LoggerHelper.LogType.ERROR);
            }
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            org.lwjgl.glfw.GLFW.glfwGetWindowSize(renderWindow.getWindowHandle(), w, h);
            currentScreenWidth = w.get(0);
            currentScreenHeight = h.get(0);
            textRenderer.setScreenSize(currentScreenWidth, currentScreenHeight);
            titleRenderer.setScreenSize(currentScreenWidth, currentScreenHeight);
        }

        titleText = new TextObject("HMPH GAME", REFERENCE_WIDTH/2f-200f, 120f);
        titleRenderer.addTextObject(titleText);

        TextObject versionText = new TextObject("Version 1.0.0", 50f, textRenderer.screenHeight-80f);
        textRenderer.addTextObject(versionText);
        overlayTexts.add(versionText);

        createMainMenuButtons(renderWindow.getShaderManager().getShader("textured"), renderWindow.getWindowHandle());
    }

    private void createMainMenuButtons(ShaderProgram shader, long windowHandle) throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            org.lwjgl.glfw.GLFW.glfwGetWindowSize(windowHandle, w, h);
            currentScreenWidth = w.get(0);
            currentScreenHeight = h.get(0);
        }

        float scaleX = currentScreenWidth / REFERENCE_WIDTH;
        float scaleY = currentScreenHeight / REFERENCE_HEIGHT;
        float scale = Math.min(scaleX, scaleY);

        float centerX = (REFERENCE_WIDTH/2f - 400f) * scale;
        float buttonWidth = 800f * scale;
        float buttonHeight = 120f * scale;
        float buttonSpacing = 150f * scale;
        float startY = (REFERENCE_HEIGHT/2f - 200f) * scale;

        playButton = new ButtonRenderer(
                "assets/images/gui/button_rectangle_depth_gradient.png",
                "assets/images/gui/button_rectangle_depth_gloss.png",
                "assets/images/gui/button_rectangle_depth_border.png",
                shader, windowHandle,
                currentScreenWidth, currentScreenHeight,
                centerX, startY, buttonWidth, buttonHeight
        );
        playButton.setTextRenderer(textRenderer);
        TextObject playText = new TextObject("PLAY GAME", centerX+buttonWidth/2f-60f*scale, startY+buttonHeight/2f+10f*scale);
        playButton.setText(playText);
        playButton.setTextColor(0f,0f,0f,1f, playText);
        playButton.setClickListener(this::startGame);
        buttons.add(playButton);

        settingsButton = new ButtonRenderer(
                "assets/images/gui/button_rectangle_depth_gradient.png",
                "assets/images/gui/button_rectangle_depth_gloss.png",
                "assets/images/gui/button_rectangle_depth_border.png",
                shader, windowHandle,
                currentScreenWidth, currentScreenHeight,
                centerX, startY+buttonSpacing, buttonWidth, buttonHeight
        );
        settingsButton.setTextRenderer(textRenderer);
        TextObject settingsText = new TextObject("SETTINGS", centerX+buttonWidth/2f-45f*scale, startY+buttonSpacing+buttonHeight/2f+10f*scale);
        settingsButton.setText(settingsText);
        settingsButton.setTextColor(0f,0f,0f,1f, settingsText);
        settingsButton.setClickListener(this::showSettings);
        buttons.add(settingsButton);

        creditsButton = new ButtonRenderer(
                "assets/images/gui/button_rectangle_depth_gradient.png",
                "assets/images/gui/button_rectangle_depth_gloss.png",
                "assets/images/gui/button_rectangle_depth_border.png",
                shader, windowHandle,
                currentScreenWidth, currentScreenHeight,
                centerX, startY+buttonSpacing*2, buttonWidth, buttonHeight
        );
        creditsButton.setTextRenderer(textRenderer);
        TextObject creditsText = new TextObject("CREDITS", centerX+buttonWidth/2f-40f*scale, startY+buttonSpacing*2+buttonHeight/2f+10f*scale);
        creditsButton.setText(creditsText);
        creditsButton.setTextColor(0f,0f,0f,1f, creditsText);
        creditsButton.setClickListener(this::showCredits);
        buttons.add(creditsButton);

        exitButton = new ButtonRenderer(
                "assets/images/gui/button_rectangle_depth_gradient.png",
                "assets/images/gui/button_rectangle_depth_gloss.png",
                "assets/images/gui/button_rectangle_depth_border.png",
                shader, windowHandle,
                currentScreenWidth, currentScreenHeight,
                centerX, startY+buttonSpacing*3, buttonWidth, buttonHeight
        );
        exitButton.setTextRenderer(textRenderer);
        TextObject exitText = new TextObject("EXIT GAME", centerX+buttonWidth/2f-55f*scale, startY+buttonSpacing*3+buttonHeight/2f+10f*scale);
        exitButton.setText(exitText);
        exitButton.setTextColor(0f,0f,0f,1f, exitText);
        exitButton.setClickListener(this::exitGame);
        buttons.add(exitButton);
    }

    /** Update menu state */
    public void update(double mouseX, double mouseY, boolean mousePressed) {
        if (!menuVisible) return;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mousePressed = mousePressed;
        if (currentState == MenuState.MAIN) {
            for (ButtonRenderer button : buttons) button.update(mouseX, mouseY, mousePressed);
        }
    }

    /** Render menu */
    public void render() {
        if (!menuVisible) return;
        renderBackground();
        renderTitle();
        switch (currentState) {
            case MAIN:
                renderMainMenu();
                break;
            case SETTINGS:
                renderSettingsMenu();
                break;
            case CREDITS:
                renderCreditsMenu();
                break;
        }
    }

    private void renderBackground() {
    }

    private void renderTitle() {
        if (titleRenderer != null && currentState == MenuState.MAIN) {
            String title = "HMPH GAME";
            float titleWidth = titleRenderer.getTextWidth(title);
            float titleX = (currentScreenWidth-titleWidth)/2f;
            float titleY = 120f*(currentScreenHeight/REFERENCE_HEIGHT);
            titleRenderer.renderText(title, titleX, titleY);
        }
    }

    private void renderMainMenu() {
        for (ButtonRenderer button : buttons) {
            button.render();
        }
        if (textRenderer != null) {
            String versionText = "Version 1.0.0";
            float versionY = currentScreenHeight-80f;
            textRenderer.renderText(versionText, 50f, versionY);
        }
    }

    private void renderSettingsMenu() {
        if (textRenderer != null) {
            drawCentered("SETTINGS", currentScreenHeight * 0.34f);
            drawCentered("Music volume: 10%", currentScreenHeight * 0.48f);
            drawCentered("Press ESC to return", currentScreenHeight * 0.62f);
        }
    }

    private void renderCreditsMenu() {
        if (textRenderer != null) {
            drawCentered("CREDITS", currentScreenHeight * 0.30f);
            drawCentered("Game Engine: HMPH", currentScreenHeight * 0.43f);
            drawCentered("Created with Java & OpenGL", currentScreenHeight * 0.51f);
            drawCentered("Press ESC to return", currentScreenHeight * 0.67f);
        }
    }

    private void drawCentered(String text, float y) {
        textRenderer.renderText(text, (currentScreenWidth - textRenderer.getTextWidth(text)) / 2f, y);
    }

    private void startGame() {
        renderWindow.startGame();
    }

    private void showSettings() {
        previousState = currentState;
        currentState = MenuState.SETTINGS;
    }

    private void showCredits() {
        previousState = currentState;
        currentState = MenuState.CREDITS;
    }

    private void exitGame() {
        org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose(renderWindow.getWindowHandle(), true);
    }

    /** Handle key input */
    public void handleKeyInput(int key, int action) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            if (currentState != MenuState.MAIN) {
                currentState = MenuState.MAIN;
            }
        }
    }

    /** Toggle menu visibility */
    public void toggleMenu() {
        setMenuVisible(!menuVisible);
    }

    /** Set menu visibility */
    public void setMenuVisible(boolean visible) {
        if (this.menuVisible == visible) return;
        this.menuVisible = visible;

        if (backgroundMusic != null) {
            if (visible) {
                LoggerHelper.betterPrint("Starting background music playlist", LoggerHelper.LogType.DEBUG);
                backgroundMusic.playPlaylist("MENU-PLAYLIST");
            } else {
                LoggerHelper.betterPrint("Stopping background music playlist", LoggerHelper.LogType.DEBUG);
                backgroundMusic.stopPlaylist();
            }
        }
        LoggerHelper.betterPrint("Main Menu " + (menuVisible ? "shown" : "hidden"), LoggerHelper.LogType.INFO);
    }

    /** Check if menu is visible */
    public boolean isMenuVisible() {
        return menuVisible;
    }

    /** Check if mouse is over menu */
    public boolean isMouseOverMenu() {
        if (!menuVisible) return false;
        for (ButtonRenderer button : buttons) {
            if (button.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /** Handle window resize */
    public void onWindowResize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            org.lwjgl.glfw.GLFW.glfwGetWindowSize(renderWindow.getWindowHandle(), w, h);
            onWindowResize(w.get(0), h.get(0));
        }
    }

    /** Handle window resize with dimensions */
    public void onWindowResize(float newWidth, float newHeight) {
        this.currentScreenWidth = newWidth;
        this.currentScreenHeight = newHeight;

        float scaleX = currentScreenWidth / REFERENCE_WIDTH;
        float scaleY = currentScreenHeight / REFERENCE_HEIGHT;
        float scale = Math.min(scaleX, scaleY);

        float centerX = (REFERENCE_WIDTH/2f - 400f) * scale;
        float buttonWidth = 800f * scale;
        float buttonHeight = 120f * scale;
        float buttonSpacing = 150f * scale;
        float startY = (REFERENCE_HEIGHT/2f - 200f) * scale;

        if (playButton != null) {
            playButton.updateScreenSize(newWidth, newHeight);
            playButton.updatePosition(centerX, startY, buttonWidth, buttonHeight);
            TextObject playText = new TextObject("PLAY GAME", centerX+buttonWidth/2f-60f*scale, startY+buttonHeight/2f+10f*scale);
            playButton.setText(playText);
            playButton.setTextColor(0f, 0f, 0f, 1f, playText);
        }

        if (settingsButton != null) {
            settingsButton.updateScreenSize(newWidth, newHeight);
            settingsButton.updatePosition(centerX, startY+buttonSpacing, buttonWidth, buttonHeight);
            TextObject settingsText = new TextObject("SETTINGS", centerX+buttonWidth/2f-45f*scale, startY+buttonSpacing+buttonHeight/2f+10f*scale);
            settingsButton.setText(settingsText);
            settingsButton.setTextColor(0f, 0f, 0f, 1f, settingsText);
        }

        if (creditsButton != null) {
            creditsButton.updateScreenSize(newWidth, newHeight);
            creditsButton.updatePosition(centerX, startY+buttonSpacing*2, buttonWidth, buttonHeight);
            TextObject creditsText = new TextObject("CREDITS", centerX+buttonWidth/2f-40f*scale, startY+buttonSpacing*2+buttonHeight/2f+10f*scale);
            creditsButton.setText(creditsText);
            creditsButton.setTextColor(0f, 0f, 0f, 1f, creditsText);
        }

        if (exitButton != null) {
            exitButton.updateScreenSize(newWidth, newHeight);
            exitButton.updatePosition(centerX, startY+buttonSpacing*3, buttonWidth, buttonHeight);
            TextObject exitText = new TextObject("EXIT GAME", centerX+buttonWidth/2f-55f*scale, startY+buttonSpacing*3+buttonHeight/2f+10f*scale);
            exitButton.setText(exitText);
            exitButton.setTextColor(0f, 0f, 0f, 1f, exitText);
        }

        if (textRenderer != null) {
            textRenderer.setScreenSize(newWidth, newHeight);
        }
        if (titleRenderer != null) {
            titleRenderer.setScreenSize(newWidth, newHeight);
        }
    }

    /** Cleanup menu resources */
    public void cleanup() {
        if (backgroundMusic != null) {
            backgroundMusic.cleanup();
        }
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
        if (titleRenderer != null) {
            titleRenderer.cleanup();
        }
        for (ButtonRenderer button : buttons) {
            button.cleanup();
        }
        buttons.clear();
        LoggerHelper.betterPrint("MainMenu Manager cleaned up", LoggerHelper.LogType.INFO);
    }

    public MenuState getCurrentState() { return currentState; }
    public void setCurrentState(MenuState state) { this.currentState = state; }
}
