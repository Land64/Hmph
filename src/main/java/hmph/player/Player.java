package hmph.player;

import hmph.math.Vector3f;
import hmph.rendering.world.chunk.ChunkManager;
import hmph.rendering.BlockRegistry;
import hmph.rendering.camera.Camera;

public class Player {
    public static final float PLAYER_HEIGHT = 2.3f;
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float GRAVITY = -9.8f;
    public static final float JUMP_STRENGTH = 5.0f;
    public static final float MOVE_SPEED = 4.317f;
    public static final float SPRINT_SPEED = 5.612f * 25f;

    private Vector3f position;
    private Vector3f velocity;
    private boolean onGround;
    private boolean sprinting;
    private ChunkManager chunkManager;
    private Camera camera;

    // Block interaction components
    private BlockRegistry blockRegistry;
    private SimpleInventory inventory;
    private BlockInteraction.RaycastResult currentLookingAt;
    private float blockBreakCooldown = 0.0f;
    private float blockPlaceCooldown = 0.0f;
    private static final float BREAK_COOLDOWN_TIME = 0.2f; // 200ms between breaks
    private static final float PLACE_COOLDOWN_TIME = 0.15f; // 150ms between placements

    private static final float EYE_HEIGHT = 1.62f;

    public Player(Vector3f startPosition, ChunkManager chunkManager, Camera camera) {
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f(0, 0, 0);
        this.onGround = false;
        this.sprinting = false;
        this.chunkManager = chunkManager;
        this.camera = camera;
        this.inventory = new SimpleInventory();
        this.currentLookingAt = new BlockInteraction.RaycastResult();
    }

    public void setBlockRegistry(BlockRegistry registry) {
        this.blockRegistry = registry;
    }

    public void update(float deltaTime) {
        if (blockBreakCooldown > 0) {
            blockBreakCooldown -= deltaTime;
        }
        if (blockPlaceCooldown > 0) {
            blockPlaceCooldown -= deltaTime;
        }

        if (!onGround) {
            velocity.y += GRAVITY * deltaTime;
        }

        Vector3f newPosition = new Vector3f(
                position.x + velocity.x * deltaTime,
                position.y + velocity.y * deltaTime,
                position.z + velocity.z * deltaTime
        );

        handleCollisions(newPosition, deltaTime);

        velocity.x *= 0.85f;
        velocity.z *= 0.85f;

        updateLookingAt();
    }

    /**
     * Update which block the player is currently looking at
     */
    private void updateLookingAt() {
        if (camera != null) {
            Vector3f cameraPos = getCameraPosition();
            Vector3f cameraDirection = camera.getFront();
            currentLookingAt = BlockInteraction.raycastToBlock(cameraPos, cameraDirection, chunkManager);
        }
    }

    /**
     * Try to break the block the player is looking at
     */
    public boolean tryBreakBlock() {
        if (blockBreakCooldown > 0 || !currentLookingAt.hit || blockRegistry == null) {
            return false;
        }

        int blockX = (int) currentLookingAt.blockPos.x;
        int blockY = (int) currentLookingAt.blockPos.y;
        int blockZ = (int) currentLookingAt.blockPos.z;

        int blockId = chunkManager.getBlockAt(blockX, blockY, blockZ);
        if (blockId == 0) return false;

        String blockName = blockRegistry.getNameFromID(blockId);
        if (blockName != null) {
            inventory.addItem(blockName, 1);
        }

        chunkManager.setBlockAt(blockX, blockY, blockZ, 0);
        blockBreakCooldown = BREAK_COOLDOWN_TIME;

        return true;
    }

    /**
     * Try to place a block adjacent to the one the player is looking at
     */
    public boolean tryPlaceBlock() {
        if (blockPlaceCooldown > 0 || !currentLookingAt.hit || blockRegistry == null) {
            return false;
        }

        String selectedBlock = inventory.getSelectedBlock();
        if (!inventory.hasItem(selectedBlock, 1)) {
            return false;
        }

        Vector3f placePos = BlockInteraction.getPlacementPosition(
                currentLookingAt.blockPos,
                currentLookingAt.normal
        );

        int placeX = (int) placePos.x;
        int placeY = (int) placePos.y;
        int placeZ = (int) placePos.z;

        if (isPositionInsidePlayer(placeX, placeY, placeZ)) {
            return false;
        }

        if (chunkManager.getBlockAt(placeX, placeY, placeZ) != 0) {
            return false;
        }

        int blockId = blockRegistry.getIDFromName(selectedBlock);
        if (blockId == 0) return false;

        chunkManager.setBlockAt(placeX, placeY, placeZ, blockId);
        inventory.removeItem(selectedBlock, 1);
        blockPlaceCooldown = PLACE_COOLDOWN_TIME;

        return true;
    }

    /**
     * Check if a block position would intersect with the player
     */
    private boolean isPositionInsidePlayer(int blockX, int blockY, int blockZ) {
        float playerMinX = position.x - PLAYER_WIDTH / 2;
        float playerMaxX = position.x + PLAYER_WIDTH / 2;
        float playerMinY = position.y;
        float playerMaxY = position.y + PLAYER_HEIGHT;
        float playerMinZ = position.z - PLAYER_WIDTH / 2;
        float playerMaxZ = position.z + PLAYER_WIDTH / 2;

        return blockX >= playerMinX && blockX < playerMaxX &&  blockY >= playerMinY && blockY < playerMaxY && blockZ >= playerMinZ && blockZ < playerMaxZ;
    }

    /**
     * Switch to next block in hotbar
     */
    public void nextBlock() {
        inventory.nextHotbarSlot();
    }

    /**
     * Switch to previous block in hotbar
     */
    public void prevBlock() {
        inventory.prevHotbarSlot();
    }

    /**
     * Set specific hotbar slot
     */
    public void setHotbarSlot(int slot) {
        inventory.setHotbarSlot(slot);
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
        int blockZ = velocity.z > 0 ?  (int) Math.floor(pos.z + PLAYER_WIDTH / 2) : (int) Math.floor(pos.z - PLAYER_WIDTH / 2);

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

    public void setMovementInput(Vector3f inputDirection, float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        velocity.x = inputDirection.x * speed;
        velocity.z = inputDirection.z * speed;
    }

    public void moveForward(float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        Vector3f front = camera.getFront();
        velocity.x = front.x * speed;
        velocity.z = front.z * speed;
    }

    public void moveBackward(float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        Vector3f front = camera.getFront();
        velocity.x = -front.x * speed;
        velocity.z = -front.z * speed;
    }

    public void moveLeft(float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        Vector3f right = camera.getRight();
        velocity.x = -right.x * speed;
        velocity.z = -right.z * speed;
    }

    public void moveRight(float deltaTime) {
        if (camera == null) return;
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        Vector3f right = camera.getRight();
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

    // Getters
    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getCameraPosition() {
        return new Vector3f(position.x, position.y + EYE_HEIGHT, position.z);
    }
    public Vector3f getVelocity() { return new Vector3f(velocity); }
    public boolean isOnGround() { return onGround; }
    public boolean isSprinting() { return sprinting; }
    public SimpleInventory getInventory() { return inventory; }
    public BlockInteraction.RaycastResult getCurrentLookingAt() { return currentLookingAt; }

    public void setPosition(Vector3f position) {
        this.position = new Vector3f(position);
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }
}