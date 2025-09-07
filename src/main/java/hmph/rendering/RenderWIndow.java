package hmph.rendering;
import hmph.GUI.ImageRenderer;
import hmph.GUI.TextRenderer;
import hmph.math.Vector3f;
import hmph.rendering.shaders.ShaderProgram;
import hmph.rendering.shapes.CubeRenderer;
import hmph.rendering.world.ChunkBase;
import hmph.rendering.world.Direction;
import hmph.util.TextureManager;
import hmph.util.debug.LoggerHelper;
import hmph.rendering.shaders.ShaderManager;
import java.nio.*;
import java.util.Map;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import hmph.math.Matrix4f;
import hmph.rendering.BlockRegistry;

public class RenderWIndow {
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

    public RenderWIndow(String title, int width, int height, boolean vSync) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
        this.resizeable = true;
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.lastMouseX = width / 2.0;
        this.lastMouseY = height / 2.0;
    }

    public RenderWIndow(String title, int width, int height) { this(title, width, height, true); }

    public void init() {
        initGLFW();
        createWindow();
        initOpenGL();
        loadRenderers();
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
        if (windowBoi == NULL) throw new RuntimeException("Failed to create window");
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

    private void loadRenderers() {
        try {
            shaderManager = new ShaderManager();
            shaderManager.loadDefaultShaders();
            ShaderProgram textShader = shaderManager.getShader("text");
            if (textShader == null) throw new RuntimeException("Failed to load text shader");
            textRenderer = new TextRenderer("assets/font.otf", 24f, textShader, (float)width, (float)height);
            ShaderProgram texturedShader = shaderManager.getShader("textured");
            if (texturedShader == null) throw new RuntimeException("Failed to load textured shader");
            imageRenderer = new ImageRenderer("assets/images/gui/button.png", texturedShader, (float)width, (float)height);
            cubeRenderer = new CubeRenderer(shaderManager);

            
            textureManager = new TextureManager();
            textureManager.loadTexture("dirt", "assets/blocks/dirt_block.png");
            textureManager.loadTexture("grass", "assets/blocks/grass_block.png");
            textureManager.loadTexture("missing", "assets/blocks/missing_texture.png");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize rendering system: " + e.getMessage());
        }
    }

    private void initCameraAndChunk(BlockRegistry registry) {
        camera = new Camera();
        camera.setPosition(8, 30, 8);
        camera.lookAt(new Vector3f(8, 0, 8));
        System.out.println("Checking block registry:");
        String testBlock = registry.getNameFromID(1);
        if (testBlock == null) {
            System.err.println("WARNING: No block found with ID 1. Registry might be empty!");
            System.err.println("Registering a default 'stone' block for testing...");
            Map<Direction, String> defaultTextures = new java.util.EnumMap<>(hmph.rendering.world.Direction.class);
            for (hmph.rendering.world.Direction dir : hmph.rendering.world.Direction.values()) {
                defaultTextures.put(dir, "stone");
            }
            registry.registerBlock("stone", "solid", defaultTextures);
            testBlock = registry.getNameFromID(1);
            System.out.println("Registered default block: " + testBlock);
        } else {
            System.out.println("Found block with ID 1: " + testBlock);
        }
        int chunkX = (int)Math.floor(camera.getPosition().x / ChunkBase.SIZE_X);
        int chunkZ = (int)Math.floor(camera.getPosition().z / ChunkBase.SIZE_Z);
        System.out.println("Creating chunk at chunk coordinates: (" + chunkX + ", " + chunkZ + ")");
        chunk = new ChunkBase(chunkX, chunkZ, registry);
        if (chunk.getVao() == 0) {
            System.err.println("ERROR: Chunk VAO is 0 after creation!");
        } else {
            System.out.println("Chunk created successfully with VAO: " + chunk.getVao() + " and " + chunk.getIndexCount() + " indices");
        }
    }

    private void centerWindow() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(windowBoi, pWidth, pHeight);
            GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
            int xpos = (vid.width() - pWidth.get(0)) / 2;
            int ypos = (vid.height() - pHeight.get(0)) / 2;
            glfwSetWindowPos(windowBoi, xpos, ypos);
        }
    }

    private void setupInputCallbacks() {
        keyCB = glfwSetKeyCallback(windowBoi, (window, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) keys[key] = (action == GLFW_PRESS);
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) { mouseCaptured = !mouseCaptured; glfwSetInputMode(windowBoi, GLFW_CURSOR, mouseCaptured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL); }
        });
        curosrPosCB = glfwSetCursorPosCallback(windowBoi, (window, xpos, ypos) -> {
            if (firstMouse) { lastMouseX = xpos; lastMouseY = ypos; firstMouse = false; }
            double xOffset = xpos - lastMouseX;
            double yOffset = lastMouseY - ypos;
            lastMouseX = xpos; lastMouseY = ypos;
            camera.processMouseMovement((float)xOffset, (float)yOffset, true);
        });
        glfwSetScrollCallback(windowBoi, (window, xoffset, yoffset) -> camera.processMouseScroll((float)yoffset));
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
        while (!glfwWindowShouldClose(windowBoi)) {
            double currentTime = glfwGetTime();
            float deltaTime = (float)(currentTime - lastTime);
            lastTime = currentTime;
            processInput(deltaTime);
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
        if (chunkShader == null) {
            System.err.println("3D shader not found!");
        }
        if (imageShader == null) {
            System.err.println("Image shader not found!");
        }
        if (textShader == null) {
            System.err.println("Text shader not found!");
        }
        checkGLError("after shader setup");
    }

    private void renderScene() {
        
        glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_NONE);
        while (glGetError() != GL_NO_ERROR);
        try {
            renderChunk();
            renderGUI();
            int error = glGetError();
            if (error != GL_NO_ERROR) {
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
        if (chunk == null) return;
        if (!chunk.isMeshBuilt()) return;
        int vao = chunk.getVao();
        if (vao == 0) return;
        int indexCount = chunk.getIndexCount();
        if (indexCount <= 0) return;
        int currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFace = glIsEnabled(GL_CULL_FACE);
        try {
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            chunkShader.bind();
            Matrix4f viewMatrix = camera.getViewMatrix();
            Matrix4f projectionMatrix = camera.getProjectionMatrix((float) width / height, 0.1f, 100.0f);
            chunkShader.setUniform("view", viewMatrix);
            chunkShader.setUniform("projection", projectionMatrix);
            Matrix4f modelMatrix = new Matrix4f().identity().translate(chunk.getPosition());
            chunkShader.setUniform("model", modelMatrix);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureManager.getTexture("missing"));
            chunkShader.setUniform("texture1", 0);
            chunkShader.setUniform("color", new Vector3f(1.0f, 1.0f, 1.0f));
            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
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
            while (glGetError() != GL_NO_ERROR);
            String info = String.format("Pos: (%.2f, %.2f, %.2f) Look: (%.2f, %.2f, %.2f) Dir: %s",
                    camera.getPosition().x, camera.getPosition().y, camera.getPosition().z,
                    camera.getFront().x, camera.getFront().y, camera.getFront().z,
                    camera.getFacingDirection());
            textRenderer.renderText(info, 10, 40);
            float scaledWidth = 128 * 3, scaledHeight = 32 * 3;
            float centerX = (width / 2.0f) - (scaledWidth / 2.0f);
            float centerY = (height / 2.0f) - (scaledHeight / 2.0f);
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                LoggerHelper.betterPrint("OpenGL error during GUI rendering: " + error, LoggerHelper.LogType.ERROR);
            }
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error in renderGUI: " + e.getMessage(), LoggerHelper.LogType.ERROR);
            e.printStackTrace();
        } finally {
            glBindTexture(GL_TEXTURE_2D, 0);
            if (currentProgram != 0) {
                glUseProgram(currentProgram);
            }
            if (depthTest) glEnable(GL_DEPTH_TEST);
            if (!blend) glDisable(GL_BLEND);
            if (cullFace) glEnable(GL_CULL_FACE);
            while (glGetError() != GL_NO_ERROR);
        }
    }

    private void checkGLError(String operation) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            String errorMsg = "OpenGL error during " + operation + ": " + error + " - ";
            switch (error) {
                case GL_INVALID_ENUM: errorMsg += "Invalid enum"; break;
                case GL_INVALID_VALUE: errorMsg += "Invalid value"; break;
                case GL_INVALID_OPERATION: errorMsg += "Invalid operation"; break;
                case GL_OUT_OF_MEMORY: errorMsg += "Out of memory"; break;
                default: errorMsg += "Unknown error";
            }
            LoggerHelper.betterPrint(errorMsg, LoggerHelper.LogType.ERROR);
        }
    }

    private void processInput(float deltaTime) {
        if (keys[GLFW_KEY_W]) camera.moveForward(deltaTime);
        if (keys[GLFW_KEY_S]) camera.moveBackward(deltaTime);
        if (keys[GLFW_KEY_A]) camera.moveLeft(deltaTime);
        if (keys[GLFW_KEY_D]) camera.moveRight(deltaTime);
        if (keys[GLFW_KEY_SPACE]) camera.moveUp(deltaTime);
        if (keys[GLFW_KEY_LEFT_SHIFT]) camera.moveDown(deltaTime);
        camera.setMovementSpeed(keys[GLFW_KEY_LEFT_CONTROL] ? 5.0f : 2.5f);
    }

    private void cleanup() {
        if (cubeRenderer != null) cubeRenderer.cleanup();
        if (shaderManager != null) shaderManager.cleanup();
        if (keyCB != null) keyCB.free();
        if (windowSizeCB != null) windowSizeCB.free();
        if (mouseButtonCB != null) mouseButtonCB.free();
        if (curosrPosCB != null) curosrPosCB.free();
        if (textRenderer != null) textRenderer.cleanup();
        if (chunk != null) chunk.cleanup();
        if (textureManager != null) textureManager.cleanup();
        glfwDestroyWindow(windowBoi);
        glfwTerminate();
        GLFWErrorCallback err = glfwSetErrorCallback(null);
        if (err != null) err.free();
        LoggerHelper.betterPrint("Cleanup was amazing", LoggerHelper.LogType.INFO);
    }

    public CubeRenderer getCubeRenderer() { return cubeRenderer; }
    public Camera getCamera() { return camera; }
    public ShaderManager getShaderManager() { return shaderManager; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean shouldClose() { return glfwWindowShouldClose(windowBoi); }
    public long getWindowHandle() { return windowBoi; }
}