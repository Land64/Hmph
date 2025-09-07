package hmph.rendering;

import hmph.GUI.ImageRenderer;
import hmph.GUI.TextRenderer;
import hmph.math.Vector3f;
import hmph.rendering.shaders.ShaderProgram;
import hmph.rendering.shapes.CubeRenderer;
import hmph.rendering.world.ChunkBase;
import hmph.util.debug.LoggerHelper;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import hmph.rendering.shaders.ShaderManager;

import java.nio.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import hmph.math.Matrix4f;

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

    public RenderWIndow(String title, int width, int height) {
        this(title, width, height, true);
    }

    //This is where HELL starts. (just joshing)
    public void init() {
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
        windowBoi = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowBoi == NULL) throw new RuntimeException("Failed to create window");
        glfwMakeContextCurrent(windowBoi);
        GL.createCapabilities();

        try {
            shaderManager = new ShaderManager();
            shaderManager.loadDefaultShaders();

            ShaderProgram textShader = shaderManager.getShader("text");
            if (textShader == null) {
                throw new RuntimeException("Failed to load text shader");
            }

            textRenderer = new TextRenderer("assets/font.otf", 24f, textShader, (float)width, (float)height);
            ShaderProgram texturedShader = shaderManager.getShader("textured");

            if (texturedShader == null) {
                throw new RuntimeException("Failed to load textured shader");
            }
            imageRenderer = new ImageRenderer("assets/images/gui/button.png", texturedShader, (float)width, (float)height);

            cubeRenderer = new CubeRenderer(shaderManager);
            camera = new Camera();

            camera.setPosition(8, 30, 8);
            camera.lookAt(new Vector3f(8, 0, 8));

            int chunkX = (int)Math.floor(camera.getPosition().x / ChunkBase.SIZE_X);
            int chunkZ = (int)Math.floor(camera.getPosition().z / ChunkBase.SIZE_Z);
            chunk = new ChunkBase(chunkX, chunkZ);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize rendering system: " + e.getMessage());
        }

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CCW);

        glfwSwapInterval(vSync ? 1 : 0);
        glfwShowWindow(windowBoi);
        centerWindow();
        setupWindowClose();
        runLoop();

        cleanup();
    }

    //Center the window, why not?
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

    //Input callbacks, like for ESC and Movement
    private void setupInputCallbacks() {
        keyCB = glfwSetKeyCallback(windowBoi, (window, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) keys[key] = (action == GLFW_PRESS);

            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                mouseCaptured = !mouseCaptured;
                glfwSetInputMode(windowBoi, GLFW_CURSOR, mouseCaptured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
            }
        });

        curosrPosCB = glfwSetCursorPosCallback(windowBoi, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }
            double xOffset = xpos - lastMouseX;
            double yOffset = lastMouseY - ypos;
            lastMouseX = xpos;
            lastMouseY = ypos;
            camera.processMouseMovement((float)xOffset, (float)yOffset, true);
        });

        glfwSetScrollCallback(windowBoi, (window, xoffset, yoffset) -> camera.processMouseScroll((float)yoffset));
    }

    //Quick Logger when it closes
    private void setupWindowClose() {
        windowCloseCB = glfwSetWindowCloseCallback(windowBoi, window -> {
            LoggerHelper.betterPrint("Running Cleanup", LoggerHelper.LogType.INFO);
            cleanup();
        });
    }

    //Like 'Tick' in minecraft, but for LWJGL (there is no difference)
    private void runLoop() {
        glfwMakeContextCurrent(windowBoi);
        GL.createCapabilities();
        setupInputCallbacks();
        glfwSetInputMode(windowBoi, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        double lastTime = glfwGetTime();
        ShaderProgram chunkShader = shaderManager.getShader("3d");
        ShaderProgram imageShader = imageRenderer.getShader();

        if (chunkShader != null) {
            chunkShader.bind();
            try {
                chunkShader.createUniform("model");
                chunkShader.createUniform("view");
                chunkShader.createUniform("projection");
            } catch (Exception e) {
                //handle shader uniform creation error
            }
            chunkShader.unbind();
        }

        if (imageShader != null) {
            imageShader.bind();
            try {
                imageShader.createUniform("model");
                imageShader.createUniform("projection");
                imageShader.createUniform("color");
                imageShader.createUniform("texture1");
            } catch (Exception e) {
            }
            imageShader.unbind();
        }

        while (!glfwWindowShouldClose(windowBoi)) {
            double currentTime = glfwGetTime();
            float deltaTime = (float)(currentTime - lastTime);
            lastTime = currentTime;
            processInput(deltaTime);

            glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);

            //render 3d world first
            if (chunkShader != null) {
                chunkShader.bind();
                projectionMatrix = camera.getProjectionMatrix((float) width / height, 0.1f, 100.0f);
                viewMatrix = camera.getViewMatrix();
                chunkShader.setUniform("projection", projectionMatrix);
                chunkShader.setUniform("view", viewMatrix);
                chunk.render(chunkShader);
                chunkShader.unbind();
            }

            //render gui elements last so they appear on top
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            //Debug info, pretty helpful if you ask me
            String info = String.format("Pos: (%.2f, %.2f, %.2f) Look: (%.2f, %.2f, %.2f) Dir: %s",
                    camera.getPosition().x, camera.getPosition().y, camera.getPosition().z,
                    camera.getFront().x, camera.getFront().y, camera.getFront().z,
                    camera.getFacingDirection());
            textRenderer.renderText(info, 10, 40);


            float scaledWidth = 128 * 3;  //384
            float scaledHeight = 32 * 3;  //96

            float centerX = (width / 2.0f) - (scaledWidth / 2.0f);
            float centerY = (height / 2.0f) - (scaledHeight / 2.0f);
            imageRenderer.renderImage(centerX, centerY, scaledWidth, scaledHeight);


            glEnable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);

            glfwSwapBuffers(windowBoi);
            glfwPollEvents();
        }

        LoggerHelper.betterPrint("Overlay & Chunk System Created nicely", LoggerHelper.LogType.RENDERING);
    }

    //Input processing
    private void processInput(float deltaTime) {
        if (keys[GLFW_KEY_W]) camera.moveForward(deltaTime);
        if (keys[GLFW_KEY_S]) camera.moveBackward(deltaTime);
        if (keys[GLFW_KEY_A]) camera.moveLeft(deltaTime);
        if (keys[GLFW_KEY_D]) camera.moveRight(deltaTime);
        if (keys[GLFW_KEY_SPACE]) camera.moveUp(deltaTime);
        if (keys[GLFW_KEY_LEFT_SHIFT]) camera.moveDown(deltaTime);
        camera.setMovementSpeed(keys[GLFW_KEY_LEFT_CONTROL] ? 5.0f : 2.5f);
    }

    //Cleanup Crew, Love these guys
    private void cleanup() {
        if (cubeRenderer != null) cubeRenderer.cleanup();
        if (shaderManager != null) shaderManager.cleanup();
        if (keyCB != null) keyCB.free();
        if (windowSizeCB != null) windowSizeCB.free();
        if (mouseButtonCB != null) mouseButtonCB.free();
        if (curosrPosCB != null) curosrPosCB.free();
        if (textRenderer != null) textRenderer.cleanup();
        if (chunk != null) chunk.cleanup();
        glfwDestroyWindow(windowBoi);
        glfwTerminate();
        GLFWErrorCallback err = glfwSetErrorCallback(null);
        if (err != null) err.free();

        LoggerHelper.betterPrint("Cleanup was amazing", LoggerHelper.LogType.INFO);
    }

    //Extra stuff, forgot what they do tbf
    public CubeRenderer getCubeRenderer() { return cubeRenderer; }
    public Camera getCamera() { return camera; }
    public ShaderManager getShaderManager() { return shaderManager; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean shouldClose() { return glfwWindowShouldClose(windowBoi); }
    public long getWindowHandle() { return windowBoi; }
}