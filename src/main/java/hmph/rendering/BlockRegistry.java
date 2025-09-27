package hmph.rendering;

import hmph.rendering.world.Direction;
import hmph.util.debug.LoggerHelper;
import java.util.*;

public class BlockRegistry {
    private final Map<String, BlockData> blocks = new HashMap<>();
    private final Map<Integer, String> idToName = new HashMap<>();
    private int nextID = 1;

    public static class BlockData {
        public final String type;
        public final Map<Direction, String> faceTextures;
        public final BlockProperties properties;

        public BlockData(String type, Map<Direction, String> faceTextures, BlockProperties properties) {
            this.type = type;
            this.faceTextures = faceTextures;
            this.properties = properties;
        }
    }

    public static class BlockProperties {
        public final float hardness;
        public final boolean isFlammable;
        public final boolean isTransparent;
        public final boolean emitsLight;
        public final int lightLevel;

        public BlockProperties(float hardness, boolean isFlammable, boolean isTransparent, boolean emitsLight, int lightLevel) {
            this.hardness = hardness;
            this.isFlammable = isFlammable;
            this.isTransparent = isTransparent;
            this.emitsLight = emitsLight;
            this.lightLevel = lightLevel;
        }

        public static BlockProperties solid(float hardness) {
            return new BlockProperties(hardness, false, false, false, 0);
        }

        public static BlockProperties transparent(float hardness) {
            return new BlockProperties(hardness, false, true, false, 0);
        }

        public static BlockProperties plant(float hardness) {
            return new BlockProperties(hardness, true, true, false, 0);
        }

        public static BlockProperties ore(float hardness) {
            return new BlockProperties(hardness, false, false, false, 0);
        }
    }

    public BlockRegistry() {
        initializeBlocks();
    }

    private void initializeBlocks() {
        registerBlock("stone", "solid", allTextures("stone_generic"), BlockProperties.solid(1.5f));
        registerBlock("dirt", "solid", allTextures("dirt"), BlockProperties.solid(0.5f));
        registerBlock("grass", "solid", topBottomSide("grass_top", "dirt", "grass_side"), BlockProperties.solid(0.6f));
        registerBlock("granite", "solid", allTextures("granite"), BlockProperties.solid(1.5f));
        registerBlock("granite_bricks", "solid", allTextures("granite_bricks"), BlockProperties.solid(1.5f));
        registerBlock("diorite", "solid", allTextures("diorite"), BlockProperties.solid(1.5f));
        registerBlock("diorite_dirty", "solid", allTextures("diorite_dirty"), BlockProperties.solid(1.5f));
        registerBlock("gabbro", "solid", allTextures("gabbro"), BlockProperties.solid(1.5f));
        registerBlock("basalt", "solid", allTextures("basalt"), BlockProperties.solid(1.25f));
        registerBlock("basalt_flow", "solid", allTextures("basalt_flow"), BlockProperties.solid(1.25f));
        registerBlock("rhyolite", "solid", allTextures("rhyolite"), BlockProperties.solid(1.5f));
        registerBlock("rhyolite_tiles", "solid", allTextures("rhyolite_tiles"), BlockProperties.solid(1.5f));
        registerBlock("schist", "solid", allTextures("schist"), BlockProperties.solid(1.2f));
        registerBlock("slate", "solid", allTextures("slate"), BlockProperties.solid(1.5f));
        registerBlock("slate_tiles", "solid", allTextures("slate_tiles"), BlockProperties.solid(1.5f));
        registerBlock("limestone", "solid", allTextures("limestone"), BlockProperties.solid(0.8f));
        registerBlock("limestone_bricks", "solid", allTextures("limestone_bricks"), BlockProperties.solid(1.5f));
        registerBlock("sandstone", "solid", allTextures("sandstone"), BlockProperties.solid(0.8f));
        registerBlock("sandstone_bricks", "solid", allTextures("sandstone_bricks"), BlockProperties.solid(1.5f));
        registerBlock("sandstone_carved", "solid", allTextures("sandstone_carved"), BlockProperties.solid(0.8f));
        registerBlock("sandstone_tiles", "solid", allTextures("sandstone_tiles"), BlockProperties.solid(1.5f));
        registerBlock("marble", "solid", allTextures("marble"), BlockProperties.solid(1.5f));
        registerBlock("marble_bricks", "solid", allTextures("marble_bricks"), BlockProperties.solid(1.5f));
        registerBlock("marble_bricks2", "solid", allTextures("marble_bricks2"), BlockProperties.solid(1.5f));
        registerBlock("marble_bricks3", "solid", allTextures("marble_bricks3"), BlockProperties.solid(1.5f));
        registerBlock("serpentine", "solid", allTextures("serpentine"), BlockProperties.solid(1.5f));
        registerBlock("serpentine_bricks", "solid", allTextures("serpentine_bricks"), BlockProperties.solid(1.5f));
        registerBlock("serpentine_carved", "solid", allTextures("serpentine_carved"), BlockProperties.solid(1.5f));
        registerBlock("farmland", "solid", allTextures("farmland"), BlockProperties.solid(0.6f));
        registerBlock("mud", "solid", allTextures("mud"), BlockProperties.solid(0.5f));
        registerBlock("mud_bricks", "solid", allTextures("mud_bricks"), BlockProperties.solid(1.5f));
        registerBlock("mud_cracked", "solid", allTextures("mud_cracked"), BlockProperties.solid(0.4f));
        registerBlock("oak_log", "log", topBottomSide("oak_log_top", "oak_log_top", "oak_log_side"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("beech_log", "log", topBottomSide("beech_log_top", "beech_log_top", "beech_log_side"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("eucalyptus_log", "log", topBottomSide("eucalyptus_log_top", "eucalyptus_log_top", "eucalyptus_log_side"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("maple_log", "log", topBottomSide("maple_log_top", "maple_log_top", "maple_log_side"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("pine_log", "log", topBottomSide("pine_log_top", "pine_log_top", "pine_log_side"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("oak_planks", "solid", allTextures("oak_planks"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("beech_planks", "solid", allTextures("beech_planks"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("eucalyptus_planks", "solid", allTextures("eucalyptus_planks"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("maple_planks", "solid", allTextures("maple_planks"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("pine_planks", "solid", allTextures("pine_planks"), new BlockProperties(2.0f, true, false, false, 0));
        registerBlock("oak_leaves", "leaves", allTextures("oak_leaves"), new BlockProperties(0.2f, true, true, false, 0));
        registerBlock("beech_leaves", "leaves", allTextures("beech_leaves"), new BlockProperties(0.2f, true, true, false, 0));
        registerBlock("eucalyptus_leaves", "leaves", allTextures("eucalyptus_leaves"), new BlockProperties(0.2f, true, true, false, 0));
        registerBlock("maple_leaves", "leaves", allTextures("maple_leaves"), new BlockProperties(0.2f, true, true, false, 0));
        registerBlock("pine_leaves", "leaves", allTextures("pine_leaves"), new BlockProperties(0.2f, true, true, false, 0));
        registerBlock("amethyst", "ore", allTextures("amethyst"), BlockProperties.ore(1.5f));
        registerBlock("obsidian", "solid", allTextures("obsidian"), BlockProperties.solid(50.0f));
        registerBlock("coral_block_brain", "solid", allTextures("coral_block_brain"), BlockProperties.solid(1.5f));
        registerBlock("coral_block_brain_bleached", "solid", allTextures("coral_block_brain_bleached"), BlockProperties.solid(1.5f));
        registerBlock("coral_block_cauliflower", "solid", allTextures("coral_block_cauliflower"), BlockProperties.solid(1.5f));
        registerBlock("coral_block_cauliflower_bleached", "solid", allTextures("coral_block_cauliflower_bleached"), BlockProperties.solid(1.5f));
        registerBlock("coral_block_pore", "solid", allTextures("coral_block_pore"), BlockProperties.solid(1.5f));
        registerBlock("coral_block_pore_bleached", "solid", allTextures("coral_block_pore_bleached"), BlockProperties.solid(1.5f));
        registerBlock("coral_block_star", "solid", allTextures("coral_block_star"), BlockProperties.solid(1.5f));
        registerBlock("coral_block_star_bleached", "solid", allTextures("coral_block_star_bleached"), BlockProperties.solid(1.5f));
        registerBlock("cobblestone", "solid", allTextures("cobblestone"), BlockProperties.solid(2.0f));
        registerBlock("cobblestone_bricks", "solid", allTextures("cobblestone_bricks"), BlockProperties.solid(2.0f));
        registerBlock("cobblestone_bricks_mossy", "solid", allTextures("cobblestone_bricks_mossy"), BlockProperties.solid(2.0f));
        registerBlock("cobblestone_mossy", "solid", allTextures("cobblestone_mossy"), BlockProperties.solid(2.0f));
        registerBlock("ice_glacier", "transparent", allTextures("ice_glacier"), BlockProperties.transparent(0.5f));
        registerBlock("ice_icicles", "transparent", allTextures("ice_icicles"), BlockProperties.transparent(0.5f));
        registerBlock("glass", "transparent", allTextures("glass"), BlockProperties.transparent(0.3f));
        registerBlock("sand_ugly", "solid", allTextures("sand_ugly"), BlockProperties.solid(0.5f));
        registerBlock("sand_ugly_2", "solid", allTextures("sand_ugly_2"), BlockProperties.solid(0.5f));
        registerBlock("sand_ugly_3", "solid", allTextures("sand_ugly_3"), BlockProperties.solid(0.5f));
        registerBlock("gravel", "solid", allTextures("gravel"), BlockProperties.solid(0.6f));
        registerBlock("snow", "solid", allTextures("snow"), BlockProperties.solid(0.1f));
        registerBlock("hay_block", "solid", topBottomSide("hay_top", "hay_top", "hay_side"), new BlockProperties(0.5f, true, false, false, 0));
        registerBlock("stone_generic_ore_crystalline", "ore", allTextures("stone_generic_ore_crystalline"), BlockProperties.ore(3.0f));
        registerBlock("stone_generic_ore_nuggets", "ore", allTextures("stone_generic_ore_nuggets"), BlockProperties.ore(3.0f));

        LoggerHelper.betterPrint("Initialized " + blocks.size() + " block types", LoggerHelper.LogType.RENDERING);
    }

    public void registerBlock(String name, String type, Map<Direction, String> textures, BlockProperties properties) {
        blocks.put(name, new BlockData(type, textures, properties));
        idToName.put(nextID, name);
        nextID++;
    }

    private Map<Direction, String> allTextures(String texture) {
        Map<Direction, String> textures = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            textures.put(dir, texture);
        }
        return textures;
    }

    private Map<Direction, String> topBottomSide(String top, String bottom, String sides) {
        Map<Direction, String> textures = new EnumMap<>(Direction.class);
        textures.put(Direction.UP, top);
        textures.put(Direction.DOWN, bottom);
        textures.put(Direction.NORTH, sides);
        textures.put(Direction.SOUTH, sides);
        textures.put(Direction.EAST, sides);
        textures.put(Direction.WEST, sides);
        return textures;
    }

    public String getTexture(String name, Direction dir) {
        BlockData data = blocks.get(name);
        if (data == null) return null;
        return data.faceTextures.get(dir);
    }

    public String getType(String name) {
        BlockData data = blocks.get(name);
        if (data == null) return null;
        return data.type;
    }

    public BlockProperties getProperties(String name) {
        BlockData data = blocks.get(name);
        if (data == null) return null;
        return data.properties;
    }

    public String getNameFromID(int id) {
        if (id == 0) return null; // Air block
        return idToName.get(id);
    }

    public int getIDFromName(String name) {
        for (Map.Entry<Integer, String> entry : idToName.entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }
        return 0; // Not found, return air
    }

    public BlockData get(String name) {
        return blocks.get(name);
    }

    public Collection<String> getAllBlockNames() {
        return blocks.keySet();
    }

    public int getBlockCount() {
        return blocks.size();
    }

}