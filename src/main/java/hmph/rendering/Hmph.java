package hmph.rendering;
import hmph.GUI.ImageRenderer;
import hmph.GUI.text.*;
import hmph.GUI.GUIManager;
import hmph.GUI.MainMenuManager;
import hmph.math.Vector3f;
import hmph.rendering.camera.Camera;
import hmph.rendering.shaders.ShaderProgram;
import hmph.rendering.shapes.CubeRenderer;
import hmph.rendering.world.chunk.ChunkBase;
import hmph.rendering.world.chunk.ChunkManagerExtension;
import hmph.rendering.world.chunk.optimized.OptimizedChunkManager;
import hmph.util.TextureManager;
import hmph.util.debug.LoggerHelper;
import hmph.rendering.shaders.ShaderManager;

import java.nio.*;
import java.util.*;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import hmph.math.Matrix4f;
import hmph.player.Player;

public class Hmph {
    private long windowBoi;
    private String title;
    private int width, height;
    private boolean vSync;
    private boolean resizeable;
    private ShaderManager shaderManager;
    private CubeRenderer cubeRenderer;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private GLFWWindowSizeCallback windowSizeCB;
    private GLFWKeyCallback keyCB;
    private GLFWMouseButtonCallback mouseButtonCB;
    private GLFWCursorPosCallback curosrPosCB;
    private GLFWScrollCallback scrollCB;
    private GLFWWindowCloseCallback windowCloseCB;
    private boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private boolean firstMouse = true;
    private double lastMouseX;
    private double lastMouseY;
    private Camera camera;
    private ChunkBase chunk;
    private TextRenderer textRenderer;
    private ImageRenderer imageRenderer;
    private boolean mouseCaptured = false;
    private TextureManager textureManager;
    private Map<Integer, KeyAction> keyActions = new HashMap<>();
    private ChunkManagerExtension chunkManager;
    private int renderDistance = 8;
    private SkyboxRenderer skyboxRenderer;
    private float gameTime = 300.0f;
    private Player player;
    private GUIManager guiManager;
    private float timeOfDay = 0.5f;
    private boolean autoTime = true;
    private MainMenuManager mainMenuManager;
    private boolean gameStarted = false;
    private boolean spawnPlaced = false;
    private boolean cleanedUp = false;
    private List<TextObject> overlayTexts = new ArrayList<>();

    public Hmph(String title, int width, int height, boolean vSync) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
        this.resizeable = true;
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.lastMouseX = width/2.0;
        this.lastMouseY = height/2.0;
    }

    public Hmph(String title, int width, int height) {
        this(title, width, height, true);
    }

    public void init() {
        try {
            initGLFW();
            createWindow();
            initOpenGL();
            loadRenderers();
            populateInputs();
            registerBlocks();
            initCameraAndChunk();
            centerWindow();
            setupWindowClose();
            runLoop();
        } finally {
            cleanup();
        }
    }

    /**
     * Initialize GLFW
     */
    private void initGLFW() {
        LoggerHelper.betterPrint("Game Started", LoggerHelper.LogType.INFO);
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, resizeable ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);
    }

    /**
     * Create window
     */
    private void createWindow() {
        windowBoi = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowBoi==NULL) throw new RuntimeException("Failed to create window");
        glfwMakeContextCurrent(windowBoi);
    }

    /**
     * Initialize OpenGL
     */
    private void initOpenGL() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_SAMPLE_ALPHA_TO_COVERAGE);
        glfwSwapInterval(vSync ? 1 : 0);
        glfwShowWindow(windowBoi);
    }

    /**
     * Load input handling
     */
    private void loadInputs(float deltaTime) {
        if (mainMenuManager!=null && mainMenuManager.isMenuVisible()) {
            return;
        }

        if (guiManager!=null && guiManager.isEnabled()) {
            return;
        }

        boolean isMoving = false;
        Vector3f inputDirection = new Vector3f(0, 0, 0);
        if (keys[GLFW_KEY_W]) {
            Vector3f front = camera.getFront();
            inputDirection.x+=front.x;
            inputDirection.z+=front.z;
            isMoving = true;
        }
        if (keys[GLFW_KEY_S]) {
            Vector3f front = camera.getFront();
            inputDirection.x-=front.x;
            inputDirection.z-=front.z;
            isMoving = true;
        }
        if (keys[GLFW_KEY_A]) {
            Vector3f right = camera.getRight();
            inputDirection.x-=right.x;
            inputDirection.z-=right.z;
            isMoving = true;
        }
        if (keys[GLFW_KEY_D]) {
            Vector3f right = camera.getRight();
            inputDirection.x+=right.x;
            inputDirection.z+=right.z;
            isMoving = true;
        }

        if (isMoving) {
            float length = (float) Math.sqrt(inputDirection.x*inputDirection.x+inputDirection.z*inputDirection.z);
            if (length>0) {
                inputDirection.x/=length;
                inputDirection.z/=length;
            }
            player.setMovementInput(inputDirection, deltaTime);
        } else {
            player.setMovementInput(new Vector3f(0, 0, 0), deltaTime);
        }

        player.setSprinting(keys[GLFW_KEY_LEFT_SHIFT]||keys[GLFW_KEY_LEFT_CONTROL]);

        if (keys[GLFW_KEY_SPACE]) {
            player.jump();
        }

        camera.setPosition(player.getCameraPosition());
    }

    /**
     * Populate input actions
     */
    private void populateInputs() {
        keyActions.put(GLFW_KEY_B, (dt) -> showCurrentBiome());
    }

    /**
     * Switch dimension
     */
    private void switchDimension(String dimensionName) {
        if (chunkManager instanceof ChunkManagerExtension) {
            ChunkManagerExtension extendedManager = (ChunkManagerExtension) chunkManager;
            String currentDim = extendedManager.getCurrentDimension();
            if (!currentDim.equals(dimensionName)) {
                extendedManager.switchDimension(dimensionName);
                player.setPosition(player.getPosition().x, 150, player.getPosition().z);
            }
        }
    }

    /**
     * Show current biome
     */
    private void showCurrentBiome() {
        if (chunkManager instanceof ChunkManagerExtension) {
            ChunkManagerExtension extManager = (ChunkManagerExtension) chunkManager;
            Vector3f playerPos = player.getPosition();
            String biome = extManager.getDimensionCreator().getCurrentBiome((int)playerPos.x, (int)playerPos.z);
            LoggerHelper.betterPrint("Current Biome: " + biome, LoggerHelper.LogType.INFO);
        }
    }

    /**
     * Load renderers
     */
    private void loadRenderers() {
        try {
            shaderManager = new ShaderManager();
            shaderManager.loadDefaultShaders();
            ShaderProgram textShader = shaderManager.getShader("text");
            if (textShader==null) throw new RuntimeException("Failed to load text shader");
            textRenderer = new TextRenderer("assets/font.otf", 24f, textShader, (float) width, (float) height);
            ShaderProgram texturedShader = shaderManager.getShader("textured");
            if (texturedShader==null) throw new RuntimeException("Failed to load textured shader");
            imageRenderer = new ImageRenderer("assets/images/gui/button_rectangle_flat.png", texturedShader, (float) width, (float) height);
            cubeRenderer = new CubeRenderer(shaderManager);

            textureManager = new TextureManager();
            guiManager = new GUIManager(this);

            mainMenuManager = new MainMenuManager(this);
            mainMenuManager.initialize();
            mainMenuManager.setMenuVisible(true);

            skyboxRenderer = new SkyboxRenderer(shaderManager);

            overlayTexts.add(new TextObject("Welcome to Hmph!", 16, 36));
            overlayTexts.add(new TextObject("Press M for Main Menu", 16, 68));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize rendering system: " + e.getMessage());
        }
    }

    /**
     * Registers all block types.
     */
    private void registerBlocks() {
        String[] textures = {
                "amethyst", "basalt", "basalt_flow", "beech_leaves", "beech_log_side", "beech_log_top", "beech_planks",
                "cobblestone", "cobblestone_bricks", "cobblestone_bricks_mossy", "cobblestone_mossy",
                "coral_block_brain", "coral_block_brain_bleached", "coral_block_cauliflower", "coral_block_cauliflower_bleached",
                "coral_block_pore", "coral_block_pore_bleached", "coral_block_star", "coral_block_star_bleached",
                "diorite", "diorite_dirty", "dirt", "eucalyptus_leaves", "eucalyptus_log_side", "eucalyptus_log_top", "eucalyptus_planks",
                "farmland", "gabbro", "glass", "granite", "granite_bricks", "grass_side", "grass_snowy_side", "grass_top",
                "gravel", "hay_side", "hay_top", "ice_glacier", "ice_icicles", "limestone", "limestone_bricks",
                "maple_leaves", "maple_log_side", "maple_log_top", "maple_planks", "marble", "marble_bricks", "marble_bricks2", "marble_bricks3",
                "mud", "mud_bricks", "mud_cracked", "oak_leaves", "oak_log_side", "oak_log_top", "oak_planks",
                "obsidian", "pine_leaves", "pine_log_side", "pine_log_top", "pine_planks", "rhyolite", "rhyolite_tiles",
                "sandstone", "sandstone_bricks", "sandstone_carved", "sandstone_tiles", "sand_ugly", "sand_ugly_2", "sand_ugly_3",
                "schist", "serpentine", "serpentine_bricks", "serpentine_carved", "slate", "slate_tiles", "snow",
                "stone_generic", "stone_generic_ore_crystalline", "stone_generic_ore_nuggets"
        };

        if (textureManager != null) {
            Map<String, String> textureResources = new LinkedHashMap<>();
            for (String tex : textures) {
                textureResources.put(tex, "assets/blocks/" + tex + ".png");
            }
            textureManager.loadTextureArray(textureResources);
        }
    }


    /**
     * Initialize camera and chunk system
     */
    private void initCameraAndChunk() {
        camera = new Camera();
        camera.setPosition(0, 30, 0);
        camera.lookAt(new Vector3f(8, 0, 8));

        BlockRegistry registry = new BlockRegistry();
        registry.setTextureLayers(textureManager.getTextureLayers());
        String testBlock = registry.getNameFromID(1);
        if (testBlock == null) {
            System.err.println("ERROR: Block registry failed to initialize!");
            throw new RuntimeException("Block registry initialization failed");
        }

        LoggerHelper.betterPrint("Block registry initialized with " + registry.getBlockCount() + " blocks", LoggerHelper.LogType.RENDERING);
        LoggerHelper.betterPrint("First block (ID 1): " + testBlock, LoggerHelper.LogType.RENDERING);

        chunkManager = new ChunkManagerExtension(registry, renderDistance);

        player = new Player(new Vector3f(0, 70, 0), chunkManager, camera);
        player.setBlockRegistry(registry);

        if (chunkManager instanceof ChunkManagerExtension) {
            ((ChunkManagerExtension) chunkManager).updateChunksWithCamera(
                    player.getPosition(), camera, (float) width / Math.max(1, height));
        } else {
            chunkManager.updateChunks(player.getPosition());
        }

        LoggerHelper.betterPrint("ChunkManagerExtension initialized with render distance: " + renderDistance, LoggerHelper.LogType.RENDERING);
    }

    /**
     * Center window on screen
     */
    private void centerWindow() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(windowBoi, pWidth, pHeight);
            GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vid == null) return;
            int xpos = (vid.width()-pWidth.get(0))/2;
            int ypos = (vid.height()-pHeight.get(0))/2;
            glfwSetWindowPos(windowBoi, xpos, ypos);
        }
    }

    /**
     * Setup input callbacks
     */
    private void setupInputCallbacks() {
        keyCB = glfwSetKeyCallback(windowBoi, (window, key, scancode, action, mods) -> {
            if (key>=0&&key<keys.length) {
                keys[key] = (action==GLFW_PRESS||action==GLFW_REPEAT);
            }

            boolean returningFromSubmenu = key == GLFW_KEY_ESCAPE && action == GLFW_PRESS &&
                    mainMenuManager != null && mainMenuManager.isMenuVisible() &&
                    mainMenuManager.getCurrentState() != MainMenuManager.MenuState.MAIN;
            if (mainMenuManager!=null && mainMenuManager.isMenuVisible()) {
                mainMenuManager.handleKeyInput(key, action);
            }

            if (key==GLFW_KEY_ESCAPE&&action==GLFW_PRESS) {
                if (!returningFromSubmenu && gameStarted && mainMenuManager != null &&
                        mainMenuManager.getCurrentState() == MainMenuManager.MenuState.MAIN) {
                    mainMenuManager.toggleMenu();
                    mouseCaptured = !mainMenuManager.isMenuVisible();
                    glfwSetInputMode(windowBoi, GLFW_CURSOR,
                            mouseCaptured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
                    firstMouse = true;
                }
            }

            if (gameStarted && key==GLFW_KEY_M&&action==GLFW_PRESS) {
                if (mainMenuManager!=null) {
                    mainMenuManager.toggleMenu();
                    if (mainMenuManager.isMenuVisible()) {
                        mouseCaptured = false;
                        glfwSetInputMode(windowBoi, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    } else {
                        mouseCaptured = true;
                        glfwSetInputMode(windowBoi, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                    }
                }
            }
        });

        mouseButtonCB = glfwSetMouseButtonCallback(windowBoi, (window, button, action, mods) -> {
            if (mainMenuManager!=null && mainMenuManager.isMenuVisible()) {
                return;
            }

            if (!mouseCaptured) {
                return;
            }

            if (action==GLFW_PRESS) {
                if (button==GLFW_MOUSE_BUTTON_LEFT) {
                    player.tryBreakBlock();
                } else if (button==GLFW_MOUSE_BUTTON_RIGHT) {
                    player.tryPlaceBlock();
                }
            }
        });

        curosrPosCB = glfwSetCursorPosCallback(windowBoi, (window, xpos, ypos) -> {
            if ((guiManager==null||!guiManager.isEnabled()) && (mainMenuManager==null||!mainMenuManager.isMenuVisible()) && mouseCaptured) {
                if (firstMouse) {
                    lastMouseX = xpos;
                    lastMouseY = ypos;
                    firstMouse = false;
                }

                double xOffset = xpos-lastMouseX;
                double yOffset = lastMouseY-ypos;
                camera.processMouseMovement((float)xOffset, (float)yOffset, true);
            }

            lastMouseX = xpos;
            lastMouseY = ypos;
        });

        scrollCB = glfwSetScrollCallback(windowBoi, (window, xoffset, yoffset) -> {
            if ((guiManager==null||!guiManager.isEnabled()) && (mainMenuManager==null||!mainMenuManager.isMenuVisible())) {
                camera.processMouseScroll((float)yoffset);
            }
        });

        setupWindowSizeCallback();
    }

    /**
     * Setup window size callback
     */
    private void setupWindowSizeCallback() {
        windowSizeCB = glfwSetWindowSizeCallback(windowBoi, (window, newWidth, newHeight) -> {
            if (newWidth <= 0 || newHeight <= 0) return;
            this.width = newWidth;
            this.height = newHeight;

            glViewport(0, 0, newWidth, newHeight);

            if (guiManager!=null) {
                guiManager.onWindowResize();
            }

            if (mainMenuManager!=null) {
                mainMenuManager.onWindowResize();
            }

            if (textRenderer!=null) {
                textRenderer.setScreenSize(newWidth, newHeight);
            }
            if (imageRenderer != null) {
                imageRenderer.setScreenSize(newWidth, newHeight);
            }

            LoggerHelper.betterPrint("Window resized to: " + newWidth + "x" + newHeight, LoggerHelper.LogType.INFO);
        });
    }

    /**
     * Setup window close callback
     */
    private void setupWindowClose() {
        windowCloseCB = glfwSetWindowCloseCallback(windowBoi, window -> {
            LoggerHelper.betterPrint("Running Cleanup", LoggerHelper.LogType.INFO);
        });
    }

    /**
     * Main game loop
     */
    private void runLoop() {
        glfwMakeContextCurrent(windowBoi);
        setupInputCallbacks();
        glfwSetInputMode(windowBoi, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        setupShaders();
        checkGLError("after initial setup");

        double lastTime = glfwGetTime();
        float dayLengthInSeconds = 600.0f;

        while (!glfwWindowShouldClose(windowBoi)) {
            double currentTime = glfwGetTime();
            float deltaTime = Math.min((float)(currentTime-lastTime), 0.1f);
            lastTime = currentTime;

            updateUiInput();

            if (autoTime && gameStarted) {
                gameTime += deltaTime;
                timeOfDay = (gameTime%dayLengthInSeconds)/dayLengthInSeconds;
            }

            boolean paused = mainMenuManager != null && mainMenuManager.isMenuVisible();
            if (gameStarted && !paused && player!=null) {
                ensurePlayerSpawn();
                player.update(deltaTime);
                loadInputs(deltaTime);
            }
            renderScene();
            checkGLError("after render");
            glfwSwapBuffers(windowBoi);
            glfwPollEvents();
        }

        LoggerHelper.betterPrint("Game loop ended", LoggerHelper.LogType.RENDERING);
    }

    private void updateUiInput() {
        boolean pressed = glfwGetMouseButton(windowBoi, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        if (mainMenuManager != null) mainMenuManager.update(lastMouseX, lastMouseY, pressed);
        if (guiManager != null) guiManager.update(lastMouseX, lastMouseY, pressed);
    }

    private void ensurePlayerSpawn() {
        if (spawnPlaced || player == null || chunkManager == null) return;
        Vector3f position = player.getPosition();
        int surface = chunkManager.getSurfaceHeight((int) Math.floor(position.x),
                (int) Math.floor(position.z));
        if (surface < 0) return;

        player.setPosition(position.x, surface + 1.01f, position.z);
        camera.setPosition(player.getCameraPosition());
        camera.lookAt(new Vector3f(position.x + 8f, surface - 3f, position.z + 8f));
        spawnPlaced = true;
        LoggerHelper.betterPrint("Player spawned safely at height " + (surface + 1),
                LoggerHelper.LogType.INFO);
    }

    /**
     * Setup shaders
     */
    private void setupShaders() {
        checkGLError("before shader setup");
        ShaderProgram chunkShader = shaderManager.getShader("3d");
        ShaderProgram imageShader = imageRenderer!=null ? imageRenderer.getShader() : null;
        ShaderProgram textShader = textRenderer!=null ? textRenderer.getShader() : null;
        if (chunkShader==null) {
            System.err.println("3D shader not found!");
        }
        if (imageShader==null) {
            System.err.println("Image shader not found!");
        }
        if (textShader==null) {
            System.err.println("Text shader not found!");
        }
        checkGLError("after shader setup");
    }

    /**
     * Render scene
     */
    private void renderScene() {
        glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);

        if (skyboxRenderer!=null) {
            ShaderProgram skyboxShader = shaderManager.getShader("skybox");
            if (skyboxShader!=null) {
                skyboxShader.bind();
                glDepthMask(false);
                skyboxRenderer.renderWithTime(camera, width, height, timeOfDay);
                glDepthMask(true);
                skyboxShader.unbind();
            }
        }

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        try {
            boolean menuVisible = mainMenuManager != null && mainMenuManager.isMenuVisible();
            if (gameStarted && !menuVisible) renderChunk();
            renderGUI();
            int error = glGetError();
            if (error!=GL_NO_ERROR) {
                LoggerHelper.betterPrint("OpenGL error after rendering: " + error, LoggerHelper.LogType.ERROR);
            }
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error during rendering: " + e.getMessage(), LoggerHelper.LogType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Render chunks
     */
    private void renderChunk() {
        if (chunkManager == null) return;

        ShaderProgram chunkShader = shaderManager.getShader("3d");
        if (chunkShader == null) return;

        chunkManager.updateChunksWithCamera(player.getPosition(), camera,
                (float) width / Math.max(1, height));
        Map<Long, ChunkBase> chunks = chunkManager.getVisibleChunks();

        if (chunks.isEmpty()) return;

        LightingSystem.LightData lighting = LightingSystem.doLighting(timeOfDay);

        int currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFace = glIsEnabled(GL_CULL_FACE);
        boolean blend = glIsEnabled(GL_BLEND);

        try {
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            chunkShader.bind();

            Matrix4f viewMatrix = camera.getViewMatrix();
            Matrix4f projectionMatrix = camera.getProjectionMatrix((float) width/height, 0.1f, renderDistance * 16f * 1.5f);

            chunkShader.setUniform("view", viewMatrix);
            chunkShader.setUniform("projection", projectionMatrix);
            chunkShader.setUniform("lightDirection", lighting.direction);
            chunkShader.setUniform("lightColor", lighting.color);
            chunkShader.setUniform("ambientStrength", lighting.ambientStrength);
            chunkShader.setUniform("ambientColor", lighting.ambientColor);
            chunkShader.setUniform("color", new Vector3f(1.0f, 1.0f, 1.0f));
            chunkShader.setUniform("textureArray", 0);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D_ARRAY, textureManager.getTextureArray());

            List<Map.Entry<Long, ChunkBase>> sortedChunks = new ArrayList<>(chunks.entrySet());
            Vector3f cameraPos = camera.getPosition();

            sortedChunks.sort((a, b) -> {
                float distA = cameraPos.distance(a.getValue().getPosition());
                float distB = cameraPos.distance(b.getValue().getPosition());
                return Float.compare(distA, distB);
            });

            for (Map.Entry<Long, ChunkBase> entry : sortedChunks) {
                ChunkBase chunk = entry.getValue();
                if (!chunk.isMeshBuilt()) continue;
                int vao = chunk.getVao();
                int indexCount = chunk.getIndexCount();
                if (vao == 0 || indexCount <= 0) continue;

                if (camera.isChunkVisible(chunk.getPosition(), ChunkBase.SIZE_X)) {
                    Matrix4f modelMatrix = new Matrix4f().identity().translate(chunk.getPosition());
                    chunkShader.setUniform("model", modelMatrix);
                    glBindVertexArray(vao);
                    glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
                }
            }

            glBindVertexArray(0);
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error in renderChunk: " + e.getMessage(), LoggerHelper.LogType.ERROR);
            e.printStackTrace();
        } finally {
            glBindVertexArray(0);
            glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
            if (!depthTest) glDisable(GL_DEPTH_TEST);
            if (!cullFace) glDisable(GL_CULL_FACE);
            if (!blend) glDisable(GL_BLEND);
            if (currentProgram != 0) glUseProgram(currentProgram);
            else chunkShader.unbind();
        }
    }

    /**
     * Render GUI
     */
    private void renderGUI() {
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean blend = glIsEnabled(GL_BLEND);
        boolean cullFace = glIsEnabled(GL_CULL_FACE);
        int currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        try {
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            if (!blend) {
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
            if (gameStarted && (mainMenuManager == null || !mainMenuManager.isMenuVisible())) {
                for (TextObject textObj : overlayTexts) {
                    textRenderer.renderText(textObj);
                }
            }

            if (mainMenuManager!=null) {
                mainMenuManager.render();
            }

        } catch (Exception e) {
            LoggerHelper.betterPrint("Error in renderGUI: " + e.getMessage(), LoggerHelper.LogType.ERROR);
            e.printStackTrace();
        } finally {
            glBindTexture(GL_TEXTURE_2D, 0);
            if (currentProgram!=0) glUseProgram(currentProgram);
            if (depthTest) glEnable(GL_DEPTH_TEST);
            if (!blend) glDisable(GL_BLEND);
            if (cullFace) glEnable(GL_CULL_FACE);
        }
    }

    /**
     * Check for OpenGL errors
     */
    private void checkGLError(String operation) {
        int error = glGetError();
        if (error!=GL_NO_ERROR) {
            String errorMsg = "OpenGL error during " + operation + ": " + error + " - ";
            switch (error) {
                case GL_INVALID_ENUM: errorMsg+="Invalid enum"; break;
                case GL_INVALID_VALUE: errorMsg+="Invalid value"; break;
                case GL_INVALID_OPERATION: errorMsg+="Invalid operation"; break;
                case GL_OUT_OF_MEMORY: errorMsg+="Out of memory"; break;
                default: errorMsg+="Unknown error";
            }
            LoggerHelper.betterPrint(errorMsg, LoggerHelper.LogType.ERROR);
        }
    }

    /**
     * Start game
     */
    public void startGame() {
        gameStarted = true;
        spawnPlaced = false;
        if (mainMenuManager!=null) {
            mainMenuManager.setMenuVisible(false);
        }
        mouseCaptured = true;
        glfwSetInputMode(windowBoi, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        firstMouse = true;
        LoggerHelper.betterPrint("Game started!", LoggerHelper.LogType.INFO);
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        if (cleanedUp) return;
        cleanedUp = true;

        if (mainMenuManager!=null) mainMenuManager.cleanup();
        if (guiManager!=null) guiManager.cleanup();
        if (chunkManager!=null) chunkManager.cleanup();
        if (chunk!=null) chunk.cleanup();
        if (cubeRenderer!=null) cubeRenderer.cleanup();
        if (skyboxRenderer!=null) skyboxRenderer.cleanup();
        if (imageRenderer!=null) imageRenderer.cleanup();
        if (textRenderer!=null) textRenderer.cleanup();
        if (textureManager!=null) textureManager.cleanup();
        if (shaderManager!=null) shaderManager.cleanup();

        if (keyCB!=null) keyCB.free();
        if (windowSizeCB!=null) windowSizeCB.free();
        if (mouseButtonCB!=null) mouseButtonCB.free();
        if (curosrPosCB!=null) curosrPosCB.free();
        if (scrollCB!=null) scrollCB.free();
        if (windowCloseCB!=null) windowCloseCB.free();
        if (windowBoi != NULL) {
            glfwDestroyWindow(windowBoi);
            windowBoi = NULL;
        }
        glfwTerminate();
        GLFWErrorCallback err = glfwSetErrorCallback(null);
        if (err!=null) err.free();
        LoggerHelper.betterPrint("Cleanup was amazing", LoggerHelper.LogType.INFO);
    }

    @FunctionalInterface
    interface KeyAction {
        void run(float dt);
    }

    public CubeRenderer getCubeRenderer() { return cubeRenderer; }
    public Camera getCamera() { return camera; }
    public ShaderManager getShaderManager() { return shaderManager; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean shouldClose() { return glfwWindowShouldClose(windowBoi); }
    public long getWindowHandle() { return windowBoi; }
}
