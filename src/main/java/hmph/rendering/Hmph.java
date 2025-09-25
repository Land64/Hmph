package hmph.rendering;
import hmph.GUI.ImageRenderer;
import hmph.GUI.TextRenderer;
import hmph.GUI.GUIManager;
import hmph.math.Vector3f;
import hmph.rendering.shaders.ShaderProgram;
import hmph.rendering.shapes.CubeRenderer;
import hmph.rendering.world.ChunkBase;
import hmph.rendering.world.ChunkManagerExtension;
import hmph.rendering.world.Direction;
import hmph.util.TextureManager;
import hmph.util.debug.LoggerHelper;
import hmph.rendering.shaders.ShaderManager;
import java.nio.*;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
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
    private GLFWWindowCloseCallback windowCloseCB;
    private boolean[] keys = new boolean[GLFW_KEY_LAST];
    private boolean firstMouse = true;
    private double lastMouseX;
    private double lastMouseY;
    private Camera camera;
    private ChunkBase chunk;
    private TextRenderer textRenderer;
    private ImageRenderer imageRenderer;
    private boolean mouseCaptured = true;
    private TextureManager textureManager;
    private Map<Integer, KeyAction> keyActions = new HashMap<>();
    private ChunkManagerExtension chunkManager;
    private int renderDistance = 5;
    private SkyboxRenderer skyboxRenderer;
    private float gameTime = 0.0f;
    private Player player;
    private GUIManager guiManager;
    private float timeOfDay = 0.5f;
    private boolean autoTime = true;


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

    public Hmph(String title, int width, int height) { this(title, width, height, true); }

    public void init() {
        initGLFW();
        createWindow();
        initOpenGL();
        loadRenderers();
        populateInputs();

        BlockRegistry registry = new BlockRegistry();
        registry.loadBlocks("assets/blocks/models");
        initCameraAndChunk(registry);
        centerWindow();
        setupWindowClose();
        runLoop();
        cleanup();
    }

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
    }

    private void createWindow() {
        windowBoi = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowBoi==NULL) throw new RuntimeException("Failed to create window");
        glfwMakeContextCurrent(windowBoi);
    }

    private void initOpenGL() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glfwSwapInterval(vSync ? 1 : 0);
        glfwShowWindow(windowBoi);
    }

    private void loadInputs(float deltaTime) {
        if (guiManager.isEnabled()) {
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

    private void populateInputs() {
        keyActions.put(GLFW_KEY_1, (dt) -> switchDimension("overworld"));
        keyActions.put(GLFW_KEY_2, (dt) -> switchDimension("greenland"));
        keyActions.put(GLFW_KEY_3, (dt) -> switchDimension("mountains"));
        keyActions.put(GLFW_KEY_4, (dt) -> switchDimension("flatlands"));
    }

    private void switchDimension(String dimensionName) {
        if (chunkManager instanceof ChunkManagerExtension) {
            ChunkManagerExtension extendedManager = (ChunkManagerExtension) chunkManager;
            String currentDim = extendedManager.getCurrentDimension();
            if (!currentDim.equals(dimensionName)) {
                extendedManager.switchDimension(dimensionName);
                player.setPosition(player.getPosition().x, 70, player.getPosition().z);
            }
        }
    }

    private void loadRenderers() {
        try {
            shaderManager = new ShaderManager();
            shaderManager.loadDefaultShaders();
            ShaderProgram textShader = shaderManager.getShader("text");
            if (textShader==null) throw new RuntimeException("Failed to load text shader");
            textRenderer = new TextRenderer("assets/font.otf", 24f, textShader, (float)width, (float)height);
            ShaderProgram texturedShader = shaderManager.getShader("textured");
            if (texturedShader==null) throw new RuntimeException("Failed to load textured shader");
            imageRenderer = new ImageRenderer("assets/images/gui/button.png", texturedShader, (float)width, (float)height);
            cubeRenderer = new CubeRenderer(shaderManager);

            textureManager = new TextureManager();
            textureManager.loadTexture("dirt", "assets/blocks/dirt_block.png");
            textureManager.loadTexture("grass", "assets/blocks/grass_block.png");
            textureManager.loadTexture("missing", "assets/blocks/missing_texture.png");
            textureManager.loadTexture("stone", "assets/blocks/stone_block.png");

            guiManager = new GUIManager(this);
            guiManager.initialize();

            skyboxRenderer = new SkyboxRenderer(shaderManager);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize rendering system: " + e.getMessage());
        }
    }

    private void initCameraAndChunk(BlockRegistry registry) {
        camera = new Camera();
        camera.setPosition(0, 30, 0);
        camera.lookAt(new Vector3f(8, 0, 8));

        String testBlock = registry.getNameFromID(1);
        if (testBlock==null) {
            System.err.println("WARNING: No block found with ID 1. Registry might be empty!");
            System.err.println("Registering a default 'stone' block for testing...");
            Map<Direction, String> defaultTextures = new java.util.EnumMap<>(hmph.rendering.world.Direction.class);
            for (hmph.rendering.world.Direction dir : hmph.rendering.world.Direction.values()) {
                defaultTextures.put(dir, "stone");
            }
            registry.registerBlock("stone", "solid", defaultTextures);
            testBlock = registry.getNameFromID(1);
        }
        chunkManager = new ChunkManagerExtension(registry, renderDistance);
        chunkManager.updateChunks(new Vector3f(-999, 0, -999));
        player = new Player(new Vector3f(0, 70, 0), chunkManager, camera);
        player.setBlockRegistry(registry);
        chunkManager.updateChunks(player.getPosition());

        LoggerHelper.betterPrint("ChunkManager initialized with render distance: " + renderDistance, LoggerHelper.LogType.RENDERING);

    }

    private void centerWindow() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(windowBoi, pWidth, pHeight);
            GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
            int xpos = (vid.width()-pWidth.get(0))/2;
            int ypos = (vid.height()-pHeight.get(0))/2;
            glfwSetWindowPos(windowBoi, xpos, ypos);
        }
    }

    private void setupInputCallbacks() {
        keyCB = glfwSetKeyCallback(windowBoi, (window, key, scancode, action, mods) -> {
            if (key>=0&&key<keys.length) {
                keys[key] = (action==GLFW_PRESS||action==GLFW_REPEAT);
            }
            if (key==GLFW_KEY_ESCAPE&&action==GLFW_PRESS) {
                if (guiManager.isEnabled()) {
                    guiManager.setEnabled(false);
                    mouseCaptured = true;
                    glfwSetInputMode(windowBoi, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                } else {
                    guiManager.setEnabled(true);
                    mouseCaptured = false;
                    glfwSetInputMode(windowBoi, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
            }
        });

        mouseButtonCB = glfwSetMouseButtonCallback(windowBoi, (window, button, action, mods) -> {
            if (!mouseCaptured) {
                return;
            }

            if (action == GLFW_PRESS) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) {
                    player.tryBreakBlock();
                } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                    player.tryPlaceBlock();
                }
            }
        });

        curosrPosCB = glfwSetCursorPosCallback(windowBoi, (window, xpos, ypos) -> {
            guiManager.update(xpos, ypos, glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT)==GLFW_PRESS);

            if (!guiManager.isEnabled()&&mouseCaptured) {
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

        glfwSetScrollCallback(windowBoi, (window, xoffset, yoffset) -> {
            if (!guiManager.isEnabled()) {
                camera.processMouseScroll((float)yoffset);
            }
        });

        setupWindowSizeCallback();
    }

    private void setupWindowSizeCallback() {
        windowSizeCB = glfwSetWindowSizeCallback(windowBoi, (window, newWidth, newHeight) -> {
            this.width = newWidth;
            this.height = newHeight;

            glViewport(0, 0, newWidth, newHeight);

            if (guiManager!=null) {
                guiManager.onWindowResize();
            }

            if (textRenderer!=null) {
                try {
                    textRenderer.getClass().getMethod("updateDimensions", float.class, float.class).invoke(textRenderer, (float)newWidth, (float)newHeight);
                } catch (Exception e) {
                }
            }
            if (imageRenderer!=null) {
                try {
                    imageRenderer.getClass().getMethod("updateDimensions", float.class, float.class).invoke(imageRenderer, (float)newWidth, (float)newHeight);
                } catch (Exception e) {
                }
            }

            LoggerHelper.betterPrint("Window resized to: " + newWidth + "x" + newHeight, LoggerHelper.LogType.INFO);
        });
    }

    private void setupWindowClose() {
        windowCloseCB = glfwSetWindowCloseCallback(windowBoi, window -> { LoggerHelper.betterPrint("Running Cleanup", LoggerHelper.LogType.INFO); cleanup(); });
    }

    private void runLoop() {
        glfwMakeContextCurrent(windowBoi);
        setupInputCallbacks();
        glfwSetInputMode(windowBoi, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        setupShaders();
        checkGLError("after initial setup");

        double lastTime = glfwGetTime();
        float dayLengthInSeconds = 60.0f;
        float daySpeed = 24.0f / dayLengthInSeconds;

        while (!glfwWindowShouldClose(windowBoi)) {
            double currentTime = glfwGetTime();
            float deltaTime = (float)(currentTime - lastTime);
            lastTime = currentTime;

            if (autoTime) {
                gameTime += deltaTime;
                timeOfDay = (gameTime % dayLengthInSeconds) / dayLengthInSeconds;
            }


            player.update(deltaTime);
            loadInputs(deltaTime);
            renderScene();
            checkGLError("after render");
            glfwSwapBuffers(windowBoi);
            glfwPollEvents();
        }

        LoggerHelper.betterPrint("Overlay & Chunk System Created nicely", LoggerHelper.LogType.RENDERING);
    }


    private void setupShaders() {
        checkGLError("before shader setup");
        ShaderProgram chunkShader = shaderManager.getShader("3d");
        ShaderProgram imageShader = imageRenderer.getShader();
        ShaderProgram textShader = textRenderer.getShader();
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

    private void renderScene() {

        glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);

        if (skyboxRenderer != null) {
            ShaderProgram skyboxShader = shaderManager.getShader("skybox");
            if (skyboxShader != null) {
                skyboxShader.bind();
                glDepthMask(false);
                skyboxRenderer.renderWithTime(camera, width, height, timeOfDay);
                glDepthMask(true);
                skyboxShader.unbind();
            }
        }


        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_NONE);
        while (glGetError()!=GL_NO_ERROR);
        try {
            renderChunk();
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


    private void renderChunk() {
        ShaderProgram chunkShader = shaderManager.getShader("3d");
        if (chunkShader == null) return;

        chunkManager.updateChunks(player.getPosition());
        Map<Long, ChunkBase> chunks = chunkManager.getLoadedChunks();

        if (chunks.isEmpty()) return;

        LightingSystem.LightData lighting = LightingSystem.doLighting(timeOfDay);

        int currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFace = glIsEnabled(GL_CULL_FACE);

        try {
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_FRONT);

            chunkShader.bind();

            Matrix4f viewMatrix = camera.getViewMatrix();
            Matrix4f projectionMatrix = camera.getProjectionMatrix((float) width/height, 0.1f, 100.0f);

            chunkShader.setUniform("view", viewMatrix);
            chunkShader.setUniform("projection", projectionMatrix);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureManager.getTexture("grass"));
            chunkShader.setUniform("texture1", 0);
            chunkShader.setUniform("color", new Vector3f(1.0f, 1.0f, 1.0f));

            // woah hey lighting for the day stuff
            chunkShader.setUniform("lightDirection", lighting.direction);
            chunkShader.setUniform("lightColor", lighting.color);
            chunkShader.setUniform("ambientStrength", lighting.ambientStrength);
            chunkShader.setUniform("ambientColor", lighting.ambientColor);

            for (ChunkBase chunk : chunks.values()) {
                if (!chunk.isMeshBuilt()) continue;
                int vao = chunk.getVao();
                if (vao == 0) continue;
                int indexCount = chunk.getIndexCount();
                if (indexCount <= 0) continue;

                Matrix4f modelMatrix = new Matrix4f().identity().translate(chunk.getPosition());
                chunkShader.setUniform("model", modelMatrix);

                glBindVertexArray(vao);
                glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
            }

            glBindVertexArray(0);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            glBindVertexArray(0);
            if (!depthTest) glDisable(GL_DEPTH_TEST);
            if (!cullFace) glDisable(GL_CULL_FACE);
            if (currentProgram != 0) glUseProgram(currentProgram);
            else chunkShader.unbind();
        }
    }

    private String getOpenGLErrorString(int error) {
        switch (error) {
            case GL_INVALID_ENUM: return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE: return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION: return "GL_INVALID_OPERATION";
            case GL_OUT_OF_MEMORY: return "GL_OUT_OF_MEMORY";
            case GL_INVALID_FRAMEBUFFER_OPERATION: return "GL_INVALID_FRAMEBUFFER_OPERATION";
            default: return "Unknown error: " + error;
        }
    }

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
            while (glGetError()!=GL_NO_ERROR);

            String currentDim = (chunkManager instanceof ChunkManagerExtension) ? ((ChunkManagerExtension) chunkManager).getCurrentDimension() : "overworld";

            String info = String.format("Pos: (%.2f, %.2f, %.2f) Dim: %s Chunks: %d",
                    player.getPosition().x, player.getPosition().y, player.getPosition().z,
                    currentDim, chunkManager.getLoadedChunkCount());
            textRenderer.renderText(info, 10, 40);

            Map<Long, ChunkBase> chunks = chunkManager.getLoadedChunks();
            if (!chunks.isEmpty()) {
                ChunkBase firstChunk = chunks.values().iterator().next();
                String chunkInfo = "Chunks " + chunks.size() + " Chunk at " + firstChunk.getPosition();
                textRenderer.renderText(chunkInfo, 10, 80);
            }

            if (guiManager!=null) {
                guiManager.render();
            }

            float scaledWidth = 128*3, scaledHeight = 32*3;
            float centerX = (width/2.0f)-(scaledWidth/2.0f);
            float centerY = (height/2.0f)-(scaledHeight/2.0f);
            int error = glGetError();
            if (error!=GL_NO_ERROR) {
                LoggerHelper.betterPrint("OpenGL error during GUI rendering: " + error, LoggerHelper.LogType.ERROR);
            }
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error in renderGUI: " + e.getMessage(), LoggerHelper.LogType.ERROR);
            e.printStackTrace();
        } finally {
            glBindTexture(GL_TEXTURE_2D, 0);
            if (currentProgram!=0) {
                glUseProgram(currentProgram);
            }
            if (depthTest) glEnable(GL_DEPTH_TEST);
            if (!blend) glDisable(GL_BLEND);
            if (cullFace) glEnable(GL_CULL_FACE);
            while (glGetError()!=GL_NO_ERROR);
        }
    }

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

    private void cleanup() {
        if (guiManager!=null) guiManager.cleanup();
        if (cubeRenderer!=null) cubeRenderer.cleanup();
        if (shaderManager!=null) shaderManager.cleanup();
        if (keyCB!=null) keyCB.free();
        if (windowSizeCB!=null) windowSizeCB.free();
        if (mouseButtonCB!=null) mouseButtonCB.free();
        if (chunkManager!=null) chunkManager.cleanup();
        if (curosrPosCB!=null) curosrPosCB.free();
        if (textRenderer!=null) textRenderer.cleanup();
        if (chunk!=null) chunk.cleanup();
        if (textureManager!=null) textureManager.cleanup();
        glfwDestroyWindow(windowBoi);
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