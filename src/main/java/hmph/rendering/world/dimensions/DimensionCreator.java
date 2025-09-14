package hmph.rendering.world.dimensions;
import hmph.math.PerlinNoise;
import hmph.rendering.BlockRegistry;
import java.util.*;
import java.util.function.BiFunction;
public class DimensionCreator {
    private static final int AIR = 0;
    private final BlockRegistry registry;
    private final Map<String, DimensionConfig> dimensions = new HashMap<>();
    public DimensionCreator(BlockRegistry registry) {
        this.registry = registry;
        initDefaultDimensions();
    }
    public static class DimensionConfig {
        public final String name;
        public final BiFunction<Integer, Integer, TerrainData> generator;
        public final Map<String, Integer> blockIds;
        public final TerrainSettings settings;
        public DimensionConfig(String name, BiFunction<Integer, Integer, TerrainData> generator, Map<String, Integer> blockIds, TerrainSettings settings) {
            this.name = name;
            this.generator = generator;
            this.blockIds = blockIds;
            this.settings = settings;
        }
    }
    public static class TerrainSettings {
        public final double scale;
        public final int maxHeight;
        public final int seaLevel;
        public final int surfaceDepth;
        public final int subSurfaceDepth;
        public TerrainSettings(double scale, int maxHeight, int seaLevel, int surfaceDepth, int subSurfaceDepth) {
            this.scale = scale;
            this.maxHeight = maxHeight;
            this.seaLevel = seaLevel;
            this.surfaceDepth = surfaceDepth;
            this.subSurfaceDepth = subSurfaceDepth;
        }
    }
    public static class TerrainData {
        public final int[][][] blocks;
        public final int blocksGenerated;
        public TerrainData(int[][][] blocks, int blocksGenerated) {
            this.blocks = blocks;
            this.blocksGenerated = blocksGenerated;
        }
    }
    private void initDefaultDimensions() {
        Map<String, Integer> overworldBlocks = new HashMap<>();
        overworldBlocks.put("grass", registry.getIDFromName("grass_block"));
        overworldBlocks.put("dirt", registry.getIDFromName("dirt_block"));
        overworldBlocks.put("stone", registry.getIDFromName("stone_block"));
        TerrainSettings overworldSettings = new TerrainSettings(0.05, 30, 10, 1, 3);
        registerDimension("overworld", this::generateOverworld, overworldBlocks, overworldSettings);
        Map<String, Integer> greenlandBlocks = new HashMap<>();
        greenlandBlocks.put("grass", registry.getIDFromName("grass_block"));
        greenlandBlocks.put("dirt", registry.getIDFromName("dirt_block"));
        greenlandBlocks.put("stone", registry.getIDFromName("stone_block"));
        TerrainSettings greenlandSettings = new TerrainSettings(0.03, 50, 15, 2, 5);
        //registerDimension("greenland", this::generateGreenland, greenlandBlocks, greenlandSettings);
        Map<String, Integer> mountainBlocks = new HashMap<>();
        mountainBlocks.put("stone", registry.getIDFromName("stone_block"));
        mountainBlocks.put("dirt", registry.getIDFromName("dirt_block"));
        mountainBlocks.put("grass", registry.getIDFromName("grass_block"));
        TerrainSettings mountainSettings = new TerrainSettings(0.02, 80, 5, 1, 2);
        //registerDimension("mountains", this::generateMountains, mountainBlocks, mountainSettings);
        Map<String, Integer> flatBlocks = new HashMap<>();
        flatBlocks.put("grass", registry.getIDFromName("grass_block"));
        flatBlocks.put("dirt", registry.getIDFromName("dirt_block"));
        flatBlocks.put("stone", registry.getIDFromName("stone_block"));
        TerrainSettings flatSettings = new TerrainSettings(0.1, 5, 20, 1, 3);
        //registerDimension("flatlands", this::generateFlatlands, flatBlocks, flatSettings);
    }
    public void registerDimension(String name, BiFunction<Integer, Integer, TerrainData> generator, Map<String, Integer> blockIds, TerrainSettings settings) {
        dimensions.put(name, new DimensionConfig(name, generator, blockIds, settings));
    }
    public TerrainData generateTerrain(String dimensionName, int chunkX, int chunkZ, PerlinNoise perlin) {
        DimensionConfig config = dimensions.get(dimensionName);
        if (config == null) config = dimensions.get("overworld");
        
        // Pass the shared perlin noise to the generation methods
        if (dimensionName.equals("overworld")) {
            return generateStandardTerrain(chunkX, chunkZ, config, perlin);
        } else if (dimensionName.equals("greenland")) {
            return generateGreenland(chunkX, chunkZ, perlin);
        } else if (dimensionName.equals("mountains")) {
            return generateMountains(chunkX, chunkZ, perlin);
        } else if (dimensionName.equals("flatlands")) {
            return generateFlatlands(chunkX, chunkZ, perlin);
        } else {
            return generateStandardTerrain(chunkX, chunkZ, config, perlin);
        }
    }
    private TerrainData generateOverworld(int chunkX, int chunkZ) {
        // This method is kept for backward compatibility but should use the shared perlin version
        PerlinNoise tempPerlin = new PerlinNoise(); // Only for backward compatibility
        return generateStandardTerrain(chunkX, chunkZ, dimensions.get("overworld"), tempPerlin);
    }
    private TerrainData generateGreenland(int chunkX, int chunkZ, PerlinNoise perlin) {
        DimensionConfig config = dimensions.get("greenland");
        int[][][] blocks = new int[16][256][16];
        int blocksGenerated = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double worldX = (chunkX * 16 + x) * config.settings.scale;
                double worldZ = (chunkZ * 16 + z) * config.settings.scale;
                double noise1 = perlin.noise(worldX, 0, worldZ);
                double noise2 = perlin.noise(worldX * 2, 0, worldZ * 2) * 0.5;
                double combinedNoise = (noise1 + noise2) * 0.6;
                int height = (int) ((combinedNoise * 0.5 + 0.5) * config.settings.maxHeight);
                height = Math.max(config.settings.seaLevel, Math.min(height, 255));
                for (int y = 0; y <= height; y++) {
                    if (y == height && height >= config.settings.seaLevel) {
                        blocks[x][y][z] = config.blockIds.get("grass");
                    } else if (y >= height - config.settings.subSurfaceDepth && y < height && height >= config.settings.seaLevel) {
                        blocks[x][y][z] = config.blockIds.get("dirt");
                    } else {
                        blocks[x][y][z] = config.blockIds.get("stone");
                    }
                    blocksGenerated++;
                }
            }
        }
        return new TerrainData(blocks, blocksGenerated);
    }
    private TerrainData generateMountains(int chunkX, int chunkZ, PerlinNoise perlin) {
        DimensionConfig config = dimensions.get("mountains");
        int[][][] blocks = new int[16][256][16];
        int blocksGenerated = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double worldX = (chunkX * 16 + x) * config.settings.scale;
                double worldZ = (chunkZ * 16 + z) * config.settings.scale;
                double noise1 = perlin.noise(worldX, 0, worldZ);
                double noise2 = perlin.noise(worldX * 0.5, 0, worldZ * 0.5) * 2;
                double ridgeNoise = Math.abs(perlin.noise(worldX * 4, 0, worldZ * 4)) * 0.3;
                double combinedNoise = noise1 + noise2 + ridgeNoise;
                int height = (int) ((combinedNoise * 0.5 + 0.5) * config.settings.maxHeight);
                height = Math.max(config.settings.seaLevel, Math.min(height, 255));
                for (int y = 0; y <= height; y++) {
                    if (y == height && height >= config.settings.seaLevel + 10) {
                        blocks[x][y][z] = config.blockIds.get("stone");
                    } else if (y == height && height >= config.settings.seaLevel) {
                        blocks[x][y][z] = config.blockIds.get("grass");
                    } else if (y >= height - config.settings.subSurfaceDepth && y < height) {
                        blocks[x][y][z] = config.blockIds.get("dirt");
                    } else {
                        blocks[x][y][z] = config.blockIds.get("stone");
                    }
                    blocksGenerated++;
                }
            }
        }
        return new TerrainData(blocks, blocksGenerated);
    }
    private TerrainData generateFlatlands(int chunkX, int chunkZ, PerlinNoise perlin) {
        DimensionConfig config = dimensions.get("flatlands");
        int[][][] blocks = new int[16][256][16];
        int blocksGenerated = 0;
        int height = config.settings.seaLevel;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= height; y++) {
                    if (y == height) {
                        blocks[x][y][z] = config.blockIds.get("grass");
                    } else if (y >= height - config.settings.subSurfaceDepth) {
                        blocks[x][y][z] = config.blockIds.get("dirt");
                    } else {
                        blocks[x][y][z] = config.blockIds.get("stone");
                    }
                    blocksGenerated++;
                }
            }
        }
        return new TerrainData(blocks, blocksGenerated);
    }
    private TerrainData generateStandardTerrain(int chunkX, int chunkZ, DimensionConfig config, PerlinNoise perlin) {
        int[][][] blocks = new int[16][256][16];
        int blocksGenerated = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double worldX = (chunkX * 16 + x) * config.settings.scale;
                double worldZ = (chunkZ * 16 + z) * config.settings.scale;
                double noiseValue = perlin.noise(worldX, 0, worldZ);
                int height = (int) ((noiseValue * 0.5 + 0.5) * config.settings.maxHeight);
                height = Math.max(config.settings.seaLevel, Math.min(height, 255));
                for (int y = 0; y <= height; y++) {
                    if (y == height && height >= config.settings.seaLevel) {
                        blocks[x][y][z] = config.blockIds.get("grass");
                    } else if (y >= height - config.settings.subSurfaceDepth && y < height && height >= config.settings.seaLevel) {
                        blocks[x][y][z] = config.blockIds.get("dirt");
                    } else {
                        blocks[x][y][z] = config.blockIds.get("stone");
                    }
                    blocksGenerated++;
                }
            }
        }
        return new TerrainData(blocks, blocksGenerated);
    }
    public Set<String> getAvailableDimensions() {
        return dimensions.keySet();
    }
    public DimensionConfig getDimensionConfig(String name) {
        return dimensions.get(name);
    }
    public TerrainData generateCustomTerrain(int chunkX, int chunkZ, String[] blockTypes, double scale, int maxHeight, int seaLevel, PerlinNoise perlin) {
        int[][][] blocks = new int[16][256][16];
        int blocksGenerated = 0;
        Map<String, Integer> blockIds = new HashMap<>();
        for (String blockType : blockTypes) {
            blockIds.put(blockType, registry.getIDFromName(blockType));
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double worldX = (chunkX * 16 + x) * scale;
                double worldZ = (chunkZ * 16 + z) * scale;
                double noiseValue = perlin.noise(worldX, 0, worldZ);
                int height = (int) ((noiseValue * 0.5 + 0.5) * maxHeight);
                height = Math.max(seaLevel, Math.min(height, 255));
                for (int y = 0; y <= height; y++) {
                    String blockType = blockTypes[Math.min(y * blockTypes.length / (height + 1), blockTypes.length - 1)];
                    blocks[x][y][z] = blockIds.get(blockType);
                    blocksGenerated++;
                }
            }
        }
        return new TerrainData(blocks, blocksGenerated);
    }
}
