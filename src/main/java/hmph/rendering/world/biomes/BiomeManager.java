package hmph.rendering.world.biomes;

import hmph.math.PerlinNoise;
import hmph.rendering.BlockRegistry;
import java.util.*;

public class BiomeManager {
    private static final int AIR = 0;
    private final BlockRegistry registry;
    private final Map<String, Biome> biomes = new HashMap<>();
    private final PerlinNoise temperatureNoise;
    private final PerlinNoise humidityNoise;
    private final PerlinNoise biomeNoise;

    public BiomeManager(BlockRegistry registry) {
        this.registry = registry;
        this.temperatureNoise = new PerlinNoise(12345);
        this.humidityNoise = new PerlinNoise(54321);
        this.biomeNoise = new PerlinNoise(99999);
        initBiomes();
    }

    public static class Biome {
        public final String name;
        public final Map<String, Integer> blockIds;
        public final BiomeProperties properties;

        public Biome(String name, Map<String, Integer> blockIds, BiomeProperties properties) {
            this.name = name;
            this.blockIds = blockIds;
            this.properties = properties;
        }
    }

    public static class BiomeProperties {
        public final double scale;
        public final int maxHeight;
        public final int seaLevel;
        public final float temperature;
        public final float humidity;
        public final boolean hasStructures;

        public BiomeProperties(double scale, int maxHeight, int seaLevel, float temperature, float humidity, boolean hasStructures) {
            this.scale = scale;
            this.maxHeight = maxHeight;
            this.seaLevel = seaLevel;
            this.temperature = temperature;
            this.humidity = humidity;
            this.hasStructures = hasStructures;
        }
    }

    public static class TerrainData {
        public final int[][][] blocks;
        public final int blocksGenerated;
        public final String biomeName;

        public TerrainData(int[][][] blocks, int blocksGenerated, String biomeName) {
            this.blocks = blocks;
            this.blocksGenerated = blocksGenerated;
            this.biomeName = biomeName;
        }
    }

    private int getBlockId(String blockName) {
        int id = registry.getIDFromName(blockName);
        if (id == 0) {
            String[] fallbacks = {"stone", "dirt", "grass"};
            for (String fallback : fallbacks) {
                int fallbackId = registry.getIDFromName(fallback);
                if (fallbackId != 0) {
                    System.err.println("Warning: Block '" + blockName + "' not found in registry, using " + fallback + " (ID " + fallbackId + ")");
                    return fallbackId;
                }
            }
            System.err.println("Warning: Block '" + blockName + "' not found, using fallback ID 1");
            return 1;
        }
        return id;
    }

    private void initBiomes() {
        
        
        for (int i = 1; i <= 10; i++) {
            String blockName = registry.getNameFromID(i);
            
        }


        Map<String, Integer> plainsBlocks = new HashMap<>();
        plainsBlocks.put("grass", getBlockId("grass"));    // This should match your registry
        plainsBlocks.put("dirt", getBlockId("dirt"));
        plainsBlocks.put("stone", getBlockId("stone"));
        biomes.put("plains", new Biome("plains", plainsBlocks,
                new BiomeProperties(0.05, 25, 10, 0.7f, 0.6f, false)));

        // Forest biome
        Map<String, Integer> forestBlocks = new HashMap<>();
        forestBlocks.put("grass", getBlockId("grass"));
        forestBlocks.put("dirt", getBlockId("dirt"));
        forestBlocks.put("stone", getBlockId("stone"));
        forestBlocks.put("oak_leaves", getBlockId("oak_leaves"));
        forestBlocks.put("beech_leaves", getBlockId("beech_leaves"));
        biomes.put("forest", new Biome("forest", forestBlocks,
                new BiomeProperties(0.04, 35, 12, 0.6f, 0.8f, true)));

        Map<String, Integer> mountainBlocks = new HashMap<>();
        mountainBlocks.put("stone", getBlockId("granite"));
        mountainBlocks.put("dirt", getBlockId("dirt"));
        mountainBlocks.put("grass", getBlockId("grass"));
        mountainBlocks.put("snow", getBlockId("snow"));
        mountainBlocks.put("marble", getBlockId("marble"));
        biomes.put("mountains", new Biome("mountains", mountainBlocks,
                new BiomeProperties(0.02, 80, 5, 0.2f, 0.3f, false)));

        Map<String, Integer> desertBlocks = new HashMap<>();
        desertBlocks.put("sand", getBlockId("sand_ugly"));
        desertBlocks.put("sand2", getBlockId("sand_ugly_2"));
        desertBlocks.put("sandstone", getBlockId("sandstone"));
        desertBlocks.put("stone", getBlockId("sandstone"));
        biomes.put("desert", new Biome("desert", desertBlocks,
                new BiomeProperties(0.06, 20, 8, 0.9f, 0.1f, false)));

    }

    public String getBiomeAt(int worldX, int worldZ) {
        double temp = temperatureNoise.noise(worldX * 0.001, 0, worldZ * 0.001);
        double humid = humidityNoise.noise(worldX * 0.001, 50, worldZ * 0.001);
        double biomeVariation = biomeNoise.noise(worldX * 0.002, 0, worldZ * 0.002);

        temp = (temp + 1.0) * 0.5;
        humid = (humid + 1.0) * 0.5;
        biomeVariation = (biomeVariation + 1.0) * 0.5;

        if (temp < 0.2) {
            return humid > 0.6 ? "taiga" : "mountains";
        } else if (temp > 0.8) {
            return humid < 0.3 ? "desert" : humid > 0.8 ? "swamp" : "swamp";
        } else if (humid < 0.3) {
            return "desert";
        } else if (temp > 0.6 && humid > 0.6) {
            return biomeVariation > 0.6 ? "forest" : "plains";
        } else if (biomeVariation > 0.8) {
            return "marble_caves";
        } else {
            return "plains";
        }
    }

    public TerrainData generateBiomeTerrain(int chunkX, int chunkZ, PerlinNoise perlin) {
        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;
        String biomeName = getBiomeAt(centerX, centerZ);

        Biome biome = biomes.get(biomeName);
        if (biome == null) {
            System.err.println("Warning: Biome '" + biomeName + "' not found, using plains");
            biome = biomes.get("plains");
        }

        return generateTerrainForBiome(chunkX, chunkZ, biome, perlin);
    }

    private TerrainData generateTerrainForBiome(int chunkX, int chunkZ, Biome biome, PerlinNoise perlin) {
        int[][][] blocks = new int[16][256][16];
        int blocksGenerated = 0;

        
        return generatePlains(chunkX, chunkZ, biome, perlin, blocks, blocksGenerated);
    }

    private TerrainData generatePlains(int chunkX, int chunkZ, Biome biome, PerlinNoise perlin, int[][][] blocks, int blocksGenerated) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double worldX = (chunkX * 16 + x) * biome.properties.scale;
                double worldZ = (chunkZ * 16 + z) * biome.properties.scale;

                double noise = perlin.noise(worldX, 0, worldZ);
                int height = (int) ((noise * 0.5 + 0.5) * biome.properties.maxHeight);
                height = Math.max(biome.properties.seaLevel, Math.min(height, 255));

                for (int y = 0; y <= height; y++) {
                    Integer blockId = null;
                    if (y == height) {
                        blockId = biome.blockIds.get("grass");
                    } else if (y >= height - 3) {
                        blockId = biome.blockIds.get("dirt");
                    } else {
                        blockId = biome.blockIds.get("stone");
                    }

                    
                    if (blockId == null) {
                        System.err.println("ERROR: Null block ID in biome '" + biome.name + "' at height " + y);
                        blockId = 1; 
                    }

                    blocks[x][y][z] = blockId;
                    blocksGenerated++;
                }
            }
        }
        return new TerrainData(blocks, blocksGenerated, biome.name);
    }

    public Collection<Biome> getAllBiomes() {
        return biomes.values();
    }

    public Biome getBiome(String name) {
        return biomes.get(name);
    }

    public Set<String> getBiomeNames() {
        return biomes.keySet();
    }

    public String getCurrentBiome(int worldX, int worldZ) {
        return getBiomeAt(worldX, worldZ);
    }
}