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
    private boolean menuVisible = true;
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

    /**
     * Create menu manager
     */
    public MainMenuManager(Hmph renderWindow) {
        this.renderWindow = renderWindow;
        this.buttons = new ArrayList<>();
    }

    /**
     * Initialize menu system
     */
    public void initialize() throws Exception {
        ShaderProgram textShader = renderWindow.getShaderManager().getShader("text");
        textRenderer = new TextRenderer("assets/font.otf", 36, textShader, REFERENCE_WIDTH, REFERENCE_HEIGHT);
        titleRenderer = new TextRenderer("assets/font.otf", 72, textShader, REFERENCE_WIDTH, REFERENCE_HEIGHT);

        backgroundMusic = new AudioManager();
        if (backgroundMusic.initialize()) {
            backgroundMusic.loadSound("MM-1", "assets/sounds/music/MenuTheme.wav");
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

    /**
     * Create main menu buttons
     */
    private void createMainMenuButtons(ShaderProgram shader, long windowHandle) throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            org.lwjgl.glfw.GLFW.glfwGetWindowSize(windowHandle, w, h);
            currentScreenWidth = w.get(0);
            currentScreenHeight = h.get(0);
        }

        float centerX = REFERENCE_WIDTH/2f-400f;
        float buttonWidth = 800f;
        float buttonHeight = 120f;
        float buttonSpacing = 150f;
        float startY = REFERENCE_HEIGHT/2f-200f;

        playButton = new ButtonRenderer(
                "assets/images/gui/button_rectangle_depth_gradient.png",
                "assets/images/gui/button_rectangle_depth_gloss.png",
                "assets/images/gui/button_rectangle_depth_border.png",
                shader, windowHandle,
                REFERENCE_WIDTH, REFERENCE_HEIGHT,
                centerX, startY, buttonWidth, buttonHeight
        );
        playButton.setTextRenderer(textRenderer);
        TextObject playText = new TextObject("PLAY GAME", centerX+buttonWidth/2f-60f, startY+buttonHeight/2f+10f);
        playButton.setText(playText);
        playButton.setTextColor(0f,0f,0f,1f, playText);
        playButton.setClickListener(this::startGame);
        buttons.add(playButton);

        settingsButton = new ButtonRenderer(
                "assets/images/gui/button_rectangle_depth_gradient.png",
                "assets/images/gui/button_rectangle_depth_gloss.png",
                "assets/images/gui/button_rectangle_depth_border.png",
                shader, windowHandle,
                REFERENCE_WIDTH, REFERENCE_HEIGHT,
                centerX, startY+buttonSpacing, buttonWidth, buttonHeight
        );
        settingsButton.setTextRenderer(textRenderer);
        TextObject settingsText = new TextObject("SETTINGS", centerX+buttonWidth/2f-45f, startY+buttonSpacing+buttonHeight/2f+10f);
        settingsButton.setText(settingsText);
        settingsButton.setTextColor(0f,0f,0f,1f, settingsText);
        settingsButton.setClickListener(this::showSettings);
        buttons.add(settingsButton);

        creditsButton = new ButtonRenderer(
                "assets/images/gui/button_rectangle_depth_gradient.png",
                "assets/images/gui/button_rectangle_depth_gloss.png",
                "assets/images/gui/button_rectangle_depth_border.png",
                shader, windowHandle,
                REFERENCE_WIDTH, REFERENCE_HEIGHT,
                centerX, startY+buttonSpacing*2, buttonWidth, buttonHeight
        );
        creditsButton.setTextRenderer(textRenderer);
        TextObject creditsText = new TextObject("CREDITS", centerX+buttonWidth/2f-40f, startY+buttonSpacing*2+buttonHeight/2f+10f);
        creditsButton.setText(creditsText);
        creditsButton.setTextColor(0f,0f,0f,1f, creditsText);
        creditsButton.setClickListener(this::showCredits);
        buttons.add(creditsButton);

        exitButton = new ButtonRenderer(
                "assets/images/gui/button_rectangle_depth_gradient.png",
                "assets/images/gui/button_rectangle_depth_gloss.png",
                "assets/images/gui/button_rectangle_depth_border.png",
                shader, windowHandle,
                REFERENCE_WIDTH, REFERENCE_HEIGHT,
                centerX, startY+buttonSpacing*3, buttonWidth, buttonHeight
        );
        exitButton.setTextRenderer(textRenderer);
        TextObject exitText = new TextObject("EXIT GAME", centerX+buttonWidth/2f-55f, startY+buttonSpacing*3+buttonHeight/2f+10f);
        exitButton.setText(exitText);
        exitButton.setTextColor(0f,0f,0f,1f, exitText);
        exitButton.setClickListener(this::exitGame);
        buttons.add(exitButton);
    }

    /**
     * Update menu state
     */
    public void update(double mouseX, double mouseY, boolean mousePressed) {
        if (!menuVisible) return;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mousePressed = mousePressed;
        for (ButtonRenderer button : buttons) {
            button.update(mouseX, mouseY, mousePressed);
        }
    }

    /**
     * Render menu
     */
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

    /**
     * Render background
     */
    private void renderBackground() {
    }

    /**
     * Render title
     */
    private void renderTitle() {
        if (titleRenderer!=null && currentState==MenuState.MAIN) {
            String title = "HMPH GAME";
            float titleWidth = titleRenderer.getTextWidth(title);
            float titleX = (currentScreenWidth-titleWidth)/2f;
            float titleY = 120f*(currentScreenHeight/REFERENCE_HEIGHT);
            titleRenderer.renderText(title, titleX, titleY);
        }
    }

    /**
     * Render main menu
     */
    private void renderMainMenu() {
        for (ButtonRenderer button : buttons) {
            button.render();
        }
        if (textRenderer!=null) {
            String versionText = "Version 1.0.0";
            float versionY = currentScreenHeight-80f;
            textRenderer.renderText(versionText, 50f, versionY);
        }
    }

    /**
     * Render settings menu
     */
    private void renderSettingsMenu() {
        if (textRenderer!=null) {
            textRenderer.renderText("SETTINGS MENU", REFERENCE_WIDTH/2f-150f, 300f);
            textRenderer.renderText("(Settings implementation goes here)", REFERENCE_WIDTH/2f-200f, 400f);
            textRenderer.renderText("Press ESC to return", REFERENCE_WIDTH/2f-120f, 500f);
        }
    }

    /**
     * Render credits menu
     */
    private void renderCreditsMenu() {
        if (textRenderer!=null) {
            textRenderer.renderText("CREDITS", REFERENCE_WIDTH/2f-80f, 300f);
            textRenderer.renderText("Game Engine: HMPH", REFERENCE_WIDTH/2f-100f, 400f);
            textRenderer.renderText("Created with Java & OpenGL", REFERENCE_WIDTH/2f-150f, 450f);
            textRenderer.renderText("Press ESC to return", REFERENCE_WIDTH/2f-120f, 600f);
        }
    }

    /**
     * Start game
     */
    private void startGame() {
        renderWindow.startGame();
    }

    /**
     * Show settings
     */
    private void showSettings() {
        previousState = currentState;
        currentState = MenuState.SETTINGS;
    }

    /**
     * Show credits
     */
    private void showCredits() {
        previousState = currentState;
        currentState = MenuState.CREDITS;
    }

    /**
     * Exit game
     */
    private void exitGame() {
        org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose(renderWindow.getWindowHandle(), true);
    }

    /**
     * Handle key input
     */
    public void handleKeyInput(int key, int action) {
        if (key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && action==org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            if (currentState!=MenuState.MAIN) {
                currentState = MenuState.MAIN;
            } else {
                toggleMenu();
            }
        }
    }

    /**
     * Toggle menu visibility
     */
    public void toggleMenu() {
        menuVisible = !menuVisible;
        LoggerHelper.betterPrint("Main Menu "+(menuVisible ? "shown" : "hidden"), LoggerHelper.LogType.INFO);
    }

    /**
     * Set menu visibility
     */
    public void setMenuVisible(boolean visible) {
        this.menuVisible = visible;

        if (menuVisible) {
            backgroundMusic.playSound("MM-1", 0.1f, true);
        } else {
            backgroundMusic.stopSound("MM-1");
        }
    }

    /**
     * Check if menu is visible
     */
    public boolean isMenuVisible() {
        return menuVisible;
    }

    /**
     * Check if mouse is over menu
     */
    public boolean isMouseOverMenu() {
        if (!menuVisible) return false;
        for (ButtonRenderer button : buttons) {
            if (button.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle window resize
     */
    public void onWindowResize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            org.lwjgl.glfw.GLFW.glfwGetWindowSize(renderWindow.getWindowHandle(), w, h);
            onWindowResize(w.get(0), h.get(0));
        }
    }

    /**
     * Handle window resize with dimensions
     */
    public void onWindowResize(float newWidth, float newHeight) {
        this.currentScreenWidth = newWidth;
        this.currentScreenHeight = newHeight;

        for (ButtonRenderer button : buttons) {
            button.updateScreenSize(newWidth, newHeight);
        }

        if (textRenderer!=null) {
            textRenderer.setScreenSize(newWidth, newHeight);
        }
        if (titleRenderer!=null) {
            titleRenderer.setScreenSize(newWidth, newHeight);
        }
    }

    /**
     * Cleanup menu resources
     */
    public void cleanup() {
        if (textRenderer!=null) {
            textRenderer.cleanup();
        }
        if (titleRenderer!=null) {
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