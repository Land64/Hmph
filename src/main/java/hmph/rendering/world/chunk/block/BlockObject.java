package hmph.rendering.world.chunk.block;

import hmph.rendering.world.Direction;

import java.util.Map;
import java.util.EnumMap;

public class BlockObject {
    private final String name;
    private final int id;
    private final BlockType type;
    private final Map<Direction, String> textures;
    private final BlockProperties properties;

    public enum BlockType {
        SOLID,
        TRANSPARENT,
        LIQUID,
        PLANT,
        ORE,
        LOG,
        LEAVES
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

    public BlockObject(String name, int id, BlockType type, Map<Direction, String> textures, BlockProperties properties) {
        this.name = name;
        this.id = id;
        this.type = type;
        this.textures = textures;
        this.properties = properties;
    }

    public static class Builder {
        private String name;
        private int id;
        private BlockType type = BlockType.SOLID;
        private Map<Direction, String> textures = new EnumMap<>(Direction.class);
        private BlockProperties properties = BlockProperties.solid(1.0f);

        public Builder(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public Builder type(BlockType type) {
            this.type = type;
            return this;
        }

        public Builder allTextures(String texture) {
            for (Direction dir : Direction.values()) {
                textures.put(dir, texture);
            }
            return this;
        }

        public Builder topBottom(String top, String bottom, String sides) {
            textures.put(Direction.UP, top);
            textures.put(Direction.DOWN, bottom);
            textures.put(Direction.NORTH, sides);
            textures.put(Direction.SOUTH, sides);
            textures.put(Direction.EAST, sides);
            textures.put(Direction.WEST, sides);
            return this;
        }

        public Builder texture(Direction direction, String texture) {
            textures.put(direction, texture);
            return this;
        }

        public Builder properties(BlockProperties properties) {
            this.properties = properties;
            return this;
        }

        public BlockObject build() {
            return new BlockObject(name, id, type, textures, properties);
        }
    }

    public String getName() { return name; }
    public int getId() { return id; }
    public BlockType getType() { return type; }
    public String getTexture(Direction direction) { return textures.get(direction); }
    public BlockProperties getProperties() { return properties; }
    public boolean isTransparent() { return properties.isTransparent; }
    public boolean isFlammable() { return properties.isFlammable; }
    public float getHardness() { return properties.hardness; }
}