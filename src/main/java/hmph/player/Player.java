package hmph.player;

import hmph.math.Vector3f;
import hmph.rendering.world.ChunkManager;
import hmph.rendering.Camera;

public class Player {
    public static final float PLAYER_HEIGHT = 2.3f;
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float GRAVITY = -9.8f;
    public static final float JUMP_STRENGTH = 5.0f;
    public static final float MOVE_SPEED = 4.317f; 
    public static final float SPRINT_SPEED = 5.612f; 

    private Vector3f position;
    private Vector3f velocity;
    private boolean onGround;
    private boolean sprinting;
    private ChunkManager chunkManager;
    private Camera camera;

    
    private static final float EYE_HEIGHT = 1.62f;

    public Player(Vector3f startPosition, ChunkManager chunkManager, Camera camera) {
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f(0, 0, 0);
        this.onGround = false;
        this.sprinting = false;
        this.chunkManager = chunkManager;
        this.camera = camera;
    }

    public void update(float deltaTime) {
        
        if (!onGround) {
            velocity.y += GRAVITY * deltaTime;
        }

        
        Vector3f newPosition = new Vector3f(
                position.x + velocity.x * deltaTime,
                position.y + velocity.y * deltaTime,
                position.z + velocity.z * deltaTime
        );

        
        handleCollisions(newPosition, deltaTime);

        
        velocity.x *= 0.91f;
        velocity.z *= 0.91f;
    }

    private void handleCollisions(Vector3f newPosition, float deltaTime) {
        
        if (checkCollisionY(newPosition)) {
            if (velocity.y < 0) {
                
                onGround = true;
                velocity.y = 0;
                
                int blockY = (int) Math.floor(newPosition.y);
                newPosition.y = blockY + 1.0f;
            } else {
                
                velocity.y = 0;
            }
        } else {
            onGround = false;
        }

        
        if (!checkCollisionX(newPosition)) {
            position.x = newPosition.x;
        } else {
            velocity.x = 0;
        }

        
        if (!checkCollisionZ(newPosition)) {
            position.z = newPosition.z;
        } else {
            velocity.z = 0;
        }

        
        position.y = newPosition.y;
    }

    private boolean checkCollisionY(Vector3f pos) {
        
        int minX = (int) Math.floor(pos.x - PLAYER_WIDTH / 2);
        int maxX = (int) Math.floor(pos.x + PLAYER_WIDTH / 2);
        int minZ = (int) Math.floor(pos.z - PLAYER_WIDTH / 2);
        int maxZ = (int) Math.floor(pos.z + PLAYER_WIDTH / 2);

        if (velocity.y < 0) {
            
            int blockY = (int) Math.floor(pos.y);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBlockSolid(x, blockY, z)) {
                        return true;
                    }
                }
            }
        } else {
            
            int blockY = (int) Math.floor(pos.y + PLAYER_HEIGHT);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBlockSolid(x, blockY, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkCollisionX(Vector3f pos) {
        int blockX = velocity.x > 0 ?
                (int) Math.floor(pos.x + PLAYER_WIDTH / 2) :
                (int) Math.floor(pos.x - PLAYER_WIDTH / 2);

        int minY = (int) Math.floor(pos.y);
        int maxY = (int) Math.floor(pos.y + PLAYER_HEIGHT);
        int minZ = (int) Math.floor(pos.z - PLAYER_WIDTH / 2);
        int maxZ = (int) Math.floor(pos.z + PLAYER_WIDTH / 2);

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (isBlockSolid(blockX, y, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkCollisionZ(Vector3f pos) {
        int blockZ = velocity.z > 0 ?
                (int) Math.floor(pos.z + PLAYER_WIDTH / 2) :
                (int) Math.floor(pos.z - PLAYER_WIDTH / 2);

        int minY = (int) Math.floor(pos.y);
        int maxY = (int) Math.floor(pos.y + PLAYER_HEIGHT);
        int minX = (int) Math.floor(pos.x - PLAYER_WIDTH / 2);
        int maxX = (int) Math.floor(pos.x + PLAYER_WIDTH / 2);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isBlockSolid(x, y, blockZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBlockSolid(int x, int y, int z) {
        if (chunkManager == null) return false;
        
        return chunkManager.getBlockAt(x, y, z) != 0;
    }

    public void moveForward(float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        Vector3f front = camera.getFront();
        // Only use horizontal movement (ignore Y component)
        velocity.x = front.x * speed;
        velocity.z = front.z * speed;
    }

    public void moveBackward(float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        Vector3f front = camera.getFront();
        // Only use horizontal movement (ignore Y component)
        velocity.x = -front.x * speed;
        velocity.z = -front.z * speed;
    }

    public void moveLeft(float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        Vector3f right = camera.getRight();
        // Only use horizontal movement (ignore Y component)
        velocity.x = -right.x * speed;
        velocity.z = -right.z * speed;
    }

    public void moveRight(float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        Vector3f right = camera.getRight();
        // Only use horizontal movement (ignore Y component)
        velocity.x = right.x * speed;
        velocity.z = right.z * speed;
    }

    public void jump() {
        if (onGround) {
            velocity.y = JUMP_STRENGTH;
            onGround = false;
        }
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    
    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getCameraPosition() {
        return new Vector3f(position.x, position.y + EYE_HEIGHT, position.z);
    }
    public Vector3f getVelocity() { return new Vector3f(velocity); }
    public boolean isOnGround() { return onGround; }
    public boolean isSprinting() { return sprinting; }

    
    public void setPosition(Vector3f position) {
        this.position = new Vector3f(position);
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }
}
