package hmph.player;

import java.util.HashMap;
import java.util.Map;

public class SimpleInventory {
    private Map<String, Integer> items;
    private String[] hotbarSlots = {
            "stone", "dirt", "grass", "granite", "marble", "basalt",
            "sandstone", "oak_leaves", "pine_leaves"
    };
    private int selectedHotbarSlot = 0;

    public SimpleInventory() {
        items = new HashMap<>();
        // Use the actual block names from BlockManager
        items.put("stone", 64);
        items.put("dirt", 64);
        items.put("grass_block", 64);
        items.put("granite", 32);
        items.put("marble", 32);
        items.put("basalt", 32);
        items.put("sandstone", 32);
        items.put("oak_leaves", 32);
        items.put("pine_leaves", 32);
    }

    /**
     * Add items to inventory
     */
    public void addItem(String blockName, int count) {
        items.put(blockName, items.getOrDefault(blockName, 0) + count);
    }

    /**
     * Remove items from inventory
     * @return actual amount removed
     */
    public int removeItem(String blockName, int count) {
        int currentAmount = items.getOrDefault(blockName, 0);
        int actualRemoved = Math.min(currentAmount, count);

        if (actualRemoved > 0) {
            items.put(blockName, currentAmount - actualRemoved);
            if (items.get(blockName) <= 0) {
                items.remove(blockName);
            }
        }

        return actualRemoved;
    }

    /**
     * Check if player has enough of an item
     */
    public boolean hasItem(String blockName, int count) {
        return items.getOrDefault(blockName, 0) >= count;
    }

    /**
     * Get count of specific item
     */
    public int getItemCount(String blockName) {
        return items.getOrDefault(blockName, 0);
    }

    /**
     * Get currently selected block for placement
     */
    public String getSelectedBlock() {
        return hotbarSlots[selectedHotbarSlot];
    }

    /**
     * Switch to next hotbar slot
     */
    public void nextHotbarSlot() {
        selectedHotbarSlot = (selectedHotbarSlot + 1) % hotbarSlots.length;
    }

    /**
     * Switch to previous hotbar slot
     */
    public void prevHotbarSlot() {
        selectedHotbarSlot = (selectedHotbarSlot - 1 + hotbarSlots.length) % hotbarSlots.length;
    }

    /**
     * Set hotbar slot directly
     */
    public void setHotbarSlot(int slot) {
        if (slot >= 0 && slot < hotbarSlots.length) {
            selectedHotbarSlot = slot;
        }
    }

    /**
     * Get current hotbar slot
     */
    public int getCurrentHotbarSlot() {
        return selectedHotbarSlot;
    }

    /**
     * Get hotbar slots for UI display
     */
    public String[] getHotbarSlots() {
        return hotbarSlots.clone();
    }

    /**
     * Try to place a block (consumes from inventory)
     */
    public boolean tryPlaceBlock(String blockName) {
        if (hasItem(blockName, 1)) {
            removeItem(blockName, 1);
            return true;
        }
        return false;
    }

    /**
     * Get all items for debugging/display
     */
    public Map<String, Integer> getAllItems() {
        return new HashMap<>(items);
    }
}