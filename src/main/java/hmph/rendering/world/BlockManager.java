package hmph.rendering.world;

import hmph.rendering.BlockRegistry;
import java.util.*;

public class BlockManager {
    private final Map<String, BlockObject> blocks = new HashMap<>();
    private final Map<Integer, BlockObject> blocksById = new HashMap<>();
    private final BlockRegistry registry;
    private int nextId = 1;

    public BlockManager(BlockRegistry registry) {
        this.registry = registry;
        initializeBlocks();
    }

    private void initializeBlocks() {
        registerBlock(new BlockObject.Builder("stone", nextId++)
                .allTextures("stone_generic")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("granite", nextId++)
                .allTextures("granite")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("granite_bricks", nextId++)
                .allTextures("granite_bricks")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("diorite", nextId++)
                .allTextures("diorite")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("diorite_dirty", nextId++)
                .allTextures("diorite_dirty")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("gabbro", nextId++)
                .allTextures("gabbro")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("basalt", nextId++)
                .allTextures("basalt")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.25f))
                .build());

        registerBlock(new BlockObject.Builder("basalt_flow", nextId++)
                .allTextures("basalt_flow")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.25f))
                .build());

        registerBlock(new BlockObject.Builder("rhyolite", nextId++)
                .allTextures("rhyolite")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("rhyolite_tiles", nextId++)
                .allTextures("rhyolite_tiles")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("schist", nextId++)
                .allTextures("schist")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.2f))
                .build());

        registerBlock(new BlockObject.Builder("slate", nextId++)
                .allTextures("slate")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("slate_tiles", nextId++)
                .allTextures("slate_tiles")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        // Sedimentary blocks
        registerBlock(new BlockObject.Builder("limestone", nextId++)
                .allTextures("limestone")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.8f))
                .build());

        registerBlock(new BlockObject.Builder("limestone_bricks", nextId++)
                .allTextures("limestone_bricks")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("sandstone", nextId++)
                .allTextures("sandstone")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.8f))
                .build());

        registerBlock(new BlockObject.Builder("sandstone_bricks", nextId++)
                .allTextures("sandstone_bricks")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("sandstone_carved", nextId++)
                .allTextures("sandstone_carved")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.8f))
                .build());

        registerBlock(new BlockObject.Builder("sandstone_tiles", nextId++)
                .allTextures("sandstone_tiles")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        // Marble
        registerBlock(new BlockObject.Builder("marble", nextId++)
                .allTextures("marble")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("marble_bricks", nextId++)
                .allTextures("marble_bricks")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("marble_bricks2", nextId++)
                .allTextures("marble_bricks2")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("marble_bricks3", nextId++)
                .allTextures("marble_bricks3")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        // Serpentine
        registerBlock(new BlockObject.Builder("serpentine", nextId++)
                .allTextures("serpentine")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("serpentine_bricks", nextId++)
                .allTextures("serpentine_bricks")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("serpentine_carved", nextId++)
                .allTextures("serpentine_carved")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        // Soil blocks
        registerBlock(new BlockObject.Builder("dirt", nextId++)
                .allTextures("dirt")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.5f))
                .build());

        registerBlock(new BlockObject.Builder("grass_block", nextId++)
                .topBottom("grass_top", "dirt", "grass_side")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.6f))
                .build());

        registerBlock(new BlockObject.Builder("farmland", nextId++)
                .allTextures("farmland")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.6f))
                .build());

        registerBlock(new BlockObject.Builder("mud", nextId++)
                .allTextures("mud")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.5f))
                .build());

        registerBlock(new BlockObject.Builder("mud_bricks", nextId++)
                .allTextures("mud_bricks")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("mud_cracked", nextId++)
                .allTextures("mud_cracked")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.4f))
                .build());

        // Wood logs
        registerBlock(new BlockObject.Builder("oak_log", nextId++)
                .topBottom("oak_log_top", "oak_log_top", "oak_log_side")
                .type(BlockObject.BlockType.LOG)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        registerBlock(new BlockObject.Builder("beech_log", nextId++)
                .topBottom("beech_log_top", "beech_log_top", "beech_log_side")
                .type(BlockObject.BlockType.LOG)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        registerBlock(new BlockObject.Builder("eucalyptus_log", nextId++)
                .topBottom("eucalyptus_log_top", "eucalyptus_log_top", "eucalyptus_log_side")
                .type(BlockObject.BlockType.LOG)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        registerBlock(new BlockObject.Builder("maple_log", nextId++)
                .topBottom("maple_log_top", "maple_log_top", "maple_log_side")
                .type(BlockObject.BlockType.LOG)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        registerBlock(new BlockObject.Builder("pine_log", nextId++)
                .topBottom("pine_log_top", "pine_log_top", "pine_log_side")
                .type(BlockObject.BlockType.LOG)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        // Wood planks
        registerBlock(new BlockObject.Builder("oak_planks", nextId++)
                .allTextures("oak_planks")
                .type(BlockObject.BlockType.SOLID)
                .properties(new BlockObject.BlockProperties(2.0f, true, false, false, 0))
                .build());

        registerBlock(new BlockObject.Builder("beech_planks", nextId++)
                .allTextures("beech_planks")
                .type(BlockObject.BlockType.SOLID)
                .properties(new BlockObject.BlockProperties(2.0f, true, false, false, 0))
                .build());

        registerBlock(new BlockObject.Builder("eucalyptus_planks", nextId++)
                .allTextures("eucalyptus_planks")
                .type(BlockObject.BlockType.SOLID)
                .properties(new BlockObject.BlockProperties(2.0f, true, false, false, 0))
                .build());

        registerBlock(new BlockObject.Builder("maple_planks", nextId++)
                .allTextures("maple_planks")
                .type(BlockObject.BlockType.SOLID)
                .properties(new BlockObject.BlockProperties(2.0f, true, false, false, 0))
                .build());

        registerBlock(new BlockObject.Builder("pine_planks", nextId++)
                .allTextures("pine_planks")
                .type(BlockObject.BlockType.SOLID)
                .properties(new BlockObject.BlockProperties(2.0f, true, false, false, 0))
                .build());

        // Leaves
        registerBlock(new BlockObject.Builder("oak_leaves", nextId++)
                .allTextures("oak_leaves")
                .type(BlockObject.BlockType.LEAVES)
                .properties(new BlockObject.BlockProperties(0.2f, true, true, false, 0))
                .build());

        registerBlock(new BlockObject.Builder("beech_leaves", nextId++)
                .allTextures("beech_leaves")
                .type(BlockObject.BlockType.LEAVES)
                .properties(new BlockObject.BlockProperties(0.2f, true, true, false, 0))
                .build());

        registerBlock(new BlockObject.Builder("eucalyptus_leaves", nextId++)
                .allTextures("eucalyptus_leaves")
                .type(BlockObject.BlockType.LEAVES)
                .properties(new BlockObject.BlockProperties(0.2f, true, true, false, 0))
                .build());

        registerBlock(new BlockObject.Builder("maple_leaves", nextId++)
                .allTextures("maple_leaves")
                .type(BlockObject.BlockType.LEAVES)
                .properties(new BlockObject.BlockProperties(0.2f, true, true, false, 0))
                .build());

        registerBlock(new BlockObject.Builder("pine_leaves", nextId++)
                .allTextures("pine_leaves")
                .type(BlockObject.BlockType.LEAVES)
                .properties(new BlockObject.BlockProperties(0.2f, true, true, false, 0))
                .build());

        // Ores and special blocks
        registerBlock(new BlockObject.Builder("amethyst", nextId++)
                .allTextures("amethyst")
                .type(BlockObject.BlockType.ORE)
                .properties(BlockObject.BlockProperties.ore(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("obsidian", nextId++)
                .allTextures("obsidian")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(50.0f))
                .build());

        // Coral blocks
        registerBlock(new BlockObject.Builder("coral_block_brain", nextId++)
                .allTextures("coral_block_brain")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("coral_block_brain_bleached", nextId++)
                .allTextures("coral_block_brain_bleached")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("coral_block_cauliflower", nextId++)
                .allTextures("coral_block_cauliflower")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("coral_block_cauliflower_bleached", nextId++)
                .allTextures("coral_block_cauliflower_bleached")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("coral_block_pore", nextId++)
                .allTextures("coral_block_pore")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("coral_block_pore_bleached", nextId++)
                .allTextures("coral_block_pore_bleached")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("coral_block_star", nextId++)
                .allTextures("coral_block_star")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        registerBlock(new BlockObject.Builder("coral_block_star_bleached", nextId++)
                .allTextures("coral_block_star_bleached")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(1.5f))
                .build());

        // Cobblestone variants
        registerBlock(new BlockObject.Builder("cobblestone", nextId++)
                .allTextures("cobblestone")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        registerBlock(new BlockObject.Builder("cobblestone_bricks", nextId++)
                .allTextures("cobblestone_bricks")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        registerBlock(new BlockObject.Builder("cobblestone_bricks_mossy", nextId++)
                .allTextures("cobblestone_bricks_mossy")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        registerBlock(new BlockObject.Builder("cobblestone_mossy", nextId++)
                .allTextures("cobblestone_mossy")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(2.0f))
                .build());

        // Ice blocks
        registerBlock(new BlockObject.Builder("ice_glacier", nextId++)
                .allTextures("ice_glacier")
                .type(BlockObject.BlockType.TRANSPARENT)
                .properties(BlockObject.BlockProperties.transparent(0.5f))
                .build());

        registerBlock(new BlockObject.Builder("ice_icicles", nextId++)
                .allTextures("ice_icicles")
                .type(BlockObject.BlockType.TRANSPARENT)
                .properties(BlockObject.BlockProperties.transparent(0.5f))
                .build());

        // Glass
        registerBlock(new BlockObject.Builder("glass", nextId++)
                .allTextures("glass")
                .type(BlockObject.BlockType.TRANSPARENT)
                .properties(BlockObject.BlockProperties.transparent(0.3f))
                .build());

        // Sand variants
        registerBlock(new BlockObject.Builder("sand_ugly", nextId++)
                .allTextures("sand_ugly")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.5f))
                .build());

        registerBlock(new BlockObject.Builder("sand_ugly_2", nextId++)
                .allTextures("sand_ugly_2")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.5f))
                .build());

        registerBlock(new BlockObject.Builder("sand_ugly_3", nextId++)
                .allTextures("sand_ugly_3")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.5f))
                .build());

        // Gravel and snow
        registerBlock(new BlockObject.Builder("gravel", nextId++)
                .allTextures("gravel")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.6f))
                .build());

        registerBlock(new BlockObject.Builder("snow", nextId++)
                .allTextures("snow")
                .type(BlockObject.BlockType.SOLID)
                .properties(BlockObject.BlockProperties.solid(0.1f))
                .build());

        // Hay block
        registerBlock(new BlockObject.Builder("hay_block", nextId++)
                .topBottom("hay_top", "hay_top", "hay_side")
                .type(BlockObject.BlockType.SOLID)
                .properties(new BlockObject.BlockProperties(0.5f, true, false, false, 0))
                .build());

        // Ore variants
        registerBlock(new BlockObject.Builder("stone_generic_ore_crystalline", nextId++)
                .allTextures("stone_generic_ore_crystalline")
                .type(BlockObject.BlockType.ORE)
                .properties(BlockObject.BlockProperties.ore(3.0f))
                .build());

        registerBlock(new BlockObject.Builder("stone_generic_ore_nuggets", nextId++)
                .allTextures("stone_generic_ore_nuggets")
                .type(BlockObject.BlockType.ORE)
                .properties(BlockObject.BlockProperties.ore(3.0f))
                .build());

        System.out.println("Initialized " + blocks.size() + " block types with " + (nextId - 1) + " IDs");
    }

    public void registerBlock(BlockObject block) {
        blocks.put(block.getName(), block);
        blocksById.put(block.getId(), block);

        Map<Direction, String> textures = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            textures.put(dir, block.getTexture(dir));
        }
        registry.registerBlock(block.getName(), block.getType().name().toLowerCase(), textures, convertProperties(block.getProperties()));
    }

    private BlockRegistry.BlockProperties convertProperties(BlockObject.BlockProperties objProps) {
        return new BlockRegistry.BlockProperties(
                objProps.hardness,
                objProps.isFlammable,
                objProps.isTransparent,
                objProps.emitsLight,
                objProps.lightLevel
        );
    }

    public BlockObject getBlock(String name) {
        return blocks.get(name);
    }

    public BlockObject getBlock(int id) {
        return blocksById.get(id);
    }

    public Collection<BlockObject> getAllBlocks() {
        return blocks.values();
    }

    public int getNextId() {
        return nextId;
    }
}