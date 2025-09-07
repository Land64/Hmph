package hmph.rendering;
import hmph.math.Matrix4f;
import hmph.math.Vector3f;
public class Camera {

    private Vector3f position;

    private Vector3f target;

    private Vector3f up;

    private Vector3f front;

    private Vector3f right;

    private Vector3f worldUp;

    private float yaw;

    private float pitch;

    private float movementSpeed;

    private float mouseSensitivity;

    private float zoom;

    private Matrix4f viewMatrix;

    private Matrix4f projectionMatrix;

    private static final float DEFAULT_YAW = -90.0f;

    private static final float DEFAULT_PITCH = 0.0f;

    private static final float DEFAULT_SPEED = 2.5f;

    private static final float DEFAULT_SENSITIVITY = 0.1f;

    private static final float DEFAULT_ZOOM = 45.0f;

    public Camera() {
        this(new Vector3f(0.0f, 0.0f, 3.0f));
    }

    public Camera(Vector3f position) {
        this(position, new Vector3f(0.0f, 1.0f, 0.0f), DEFAULT_YAW, DEFAULT_PITCH);
    }

    public Camera(Vector3f position, Vector3f up, float yaw, float pitch) {
        this.position = new Vector3f(position);
        this.worldUp = new Vector3f(up);
        this.yaw = yaw;
        this.pitch = pitch;
        this.front = new Vector3f();
        this.up = new Vector3f();
        this.right = new Vector3f();
        this.target = new Vector3f();
        this.movementSpeed = DEFAULT_SPEED;
        this.mouseSensitivity = DEFAULT_SENSITIVITY;
        this.zoom = DEFAULT_ZOOM;
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        updateCameraVectors();
    }

    public Matrix4f getViewMatrix() {
        target.set(position).add(front);
        viewMatrix.lookAt(position, target, up);
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix(float aspectRatio, float nearPlane, float farPlane) {
        projectionMatrix.perspective((float) Math.toRadians(zoom), aspectRatio, nearPlane, farPlane);
        return projectionMatrix;
    }

    //Positions

    public void moveForward(float deltaTime) {
        Vector3f velocity = new Vector3f(front).mul(movementSpeed * deltaTime);
        position.add(velocity);
    }

    public void moveBackward(float deltaTime) {
        Vector3f velocity = new Vector3f(front).mul(movementSpeed * deltaTime);
        position.add(velocity.mul(-1));
    }

    public void moveLeft(float deltaTime) {
        Vector3f velocity = new Vector3f(right).mul(movementSpeed * deltaTime);
        position.add(velocity.mul(-1));
    }

    public void moveRight(float deltaTime) {
        Vector3f velocity = new Vector3f(right).mul(movementSpeed * deltaTime);
        position.add(velocity);
    }

    public void moveUp(float deltaTime) {
        Vector3f velocity = new Vector3f(up).mul(movementSpeed * deltaTime);
        position.add(velocity);
    }

    public void moveDown(float deltaTime) {
        Vector3f velocity = new Vector3f(up).mul(movementSpeed * deltaTime);
        position.add(velocity.mul(-1));
    }

    //Mouse
    public void processMouseMovement(float xOffset, float yOffset, boolean constrainPitch) {
        xOffset *= mouseSensitivity;
        yOffset *= mouseSensitivity;
        yaw += xOffset;
        pitch += yOffset;
        if (constrainPitch) {
            if (pitch > 89.0f) pitch = 89.0f;
            if (pitch < -89.0f) pitch = -89.0f;
        }
        updateCameraVectors();
    }

    //Zoom
    public void processMouseScroll(float yOffset) {
        zoom -= yOffset;
        if (zoom < 1.0f) zoom = 1.0f;
        if (zoom > 45.0f) zoom = 45.0f;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    public void setMovementSpeed(float speed) {
        this.movementSpeed = speed;
    }

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = sensitivity;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    //Like a quaterion rotation.
    public void lookAt(Vector3f target) {
        Vector3f direction = new Vector3f(target).add(position.x * -1, position.y * -1, position.z * -1).normalize();
        this.yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
        this.pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        updateCameraVectors();
    }

    public Vector3f getPosition() { return new Vector3f(position); }

    public Vector3f getFront() { return new Vector3f(front); }

    public Vector3f getUp() { return new Vector3f(up); }

    public Vector3f getRight() { return new Vector3f(right); }

    public float getZoom() { return zoom; }

    public float getYaw() { return yaw; }

    public float getPitch() { return pitch; }

    public float getMovementSpeed() { return movementSpeed; }

    public float getMouseSensitivity() { return mouseSensitivity; }

    private void updateCameraVectors() {
        front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalize();
        right = front.cross(worldUp).normalize();
        up = right.cross(front).normalize();
    }

    public String getFacingDirection() {
        float normalizedYaw = (yaw % 360 + 360) % 360;

        if (normalizedYaw >= 45 && normalizedYaw < 135)  return "South";
        if (normalizedYaw >= 135 && normalizedYaw < 225) return "West";
        if (normalizedYaw >= 225 && normalizedYaw < 315) return "North";
        return "East";
    }


    //Love @Override, it's very nice.
    @Override
    public String toString() {
        return String.format("Camera[pos=%s, yaw=%.1f, pitch=%.1f, zoom=%.1f]", position, yaw, pitch, zoom);
    }
}