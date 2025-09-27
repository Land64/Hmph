package hmph.rendering.world.dimensions;

import hmph.math.PerlinNoise;
import hmph.rendering.BlockRegistry;
import hmph.rendering.world.biomes.BiomeManager;
import java.util.*;

public class DimensionCreator {
    private static final int AIR = 0;
    private final BlockRegistry registry;
    private final BiomeManager biomeManager;

    // Terrain generation parameters
    private static final double CONTINENT_SCALE = 0.0008;  // Large scale landmass
    private static final double TERRAIN_SCALE = 0.02;     // Medium scale hills
    private static final double DETAIL_SCALE = 0.08;      // Fine detail
    private static final double BIOME_SCALE = 0.004;      // Biome boundaries

    private static final int SEA_LEVEL = 62;
    private static final int MAX_HEIGHT = 140;
    private static final int MIN_HEIGHT = 1;

    // Noise generators for different terrain features
    private final PerlinNoise continentNoise;
    private final PerlinNoise terrainNoise;
    private final PerlinNoise detailNoise;
    private final PerlinNoise caveNoise;
    private final PerlinNoise oreNoise;

    public DimensionCreator(BlockRegistry registry) {
        this.registry = registry;
        this.biomeManager = new BiomeManager(registry);

        // Initialize noise generators with different seeds for variety
        this.continentNoise = new PerlinNoise(1234);
        this.terrainNoise = new PerlinNoise(5678);
        this.detailNoise = new PerlinNoise(9012);
        this.caveNoise = new PerlinNoise(3456);
        this.oreNoise = new PerlinNoise(7890);
    }

    public static class TerrainData {
        public final int[][][] blocks;
        public final int blocksGenerated;
        public final String biomeName;

        public TerrainData(int[][][] blocks, int blocksGenerated) {
            this.blocks = blocks;
            this.blocksGenerated = blocksGenerated;
            this.biomeName = null;
        }

        public TerrainData(int[][][] blocks, int blocksGenerated, String biomeName) {
            this.blocks = blocks;
            this.blocksGenerated = blocksGenerated;
            this.biomeName = biomeName;
        }
    }

    public TerrainData generateTerrain(String dimensionName, int chunkX, int chunkZ, PerlinNoise perlin) {
        switch (dimensionName.toLowerCase()) {
            case "overworld":
                return generateOverworldTerrain(chunkX, chunkZ, perlin);
            case "nether":
                return generateNetherTerrain(chunkX, chunkZ, perlin);
            case "end":
                return generateEndTerrain(chunkX, chunkZ, perlin);
            default:
                return generateOverworldTerrain(chunkX, chunkZ, perlin);
        }
    }

    private TerrainData generateOverworldTerrain(int chunkX, int chunkZ, PerlinNoise perlin) {
        int[][][] blocks = new int[16][256][16];
        int blocksGenerated = 0;
        String primaryBiome = null;

        // Get block IDs
        int stoneId = registry.getIDFromName("stone");
        int dirtId = registry.getIDFromName("dirt");
        int grassId = registry.getIDFromName("grass");
        int sandId = registry.getIDFromName("sand_ugly");
        int sandstoneId = registry.getIDFromName("sandstone");
        int graniteId = registry.getIDFromName("granite");
        int marbleId = registry.getIDFromName("marble");
        int oakLeavesId = registry.getIDFromName("oak_leaves");
        int pineLeavesId = registry.getIDFromName("pine_leaves");
        int snowId = registry.getIDFromName("snow");
        int gravelId = registry.getIDFromName("gravel");

        // Fallback to stone if blocks don't exist
        if (stoneId == 0) stoneId = 1;
        if (dirtId == 0) dirtId = stoneId;
        if (grassId == 0) grassId = stoneId;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                // Generate height using multiple noise layers
                double continentHeight = continentNoise.noise(worldX * CONTINENT_SCALE, 0, worldZ * CONTINENT_SCALE);
                double terrainHeight = terrainNoise.noise(worldX * TERRAIN_SCALE, 0, worldZ * TERRAIN_SCALE);
                double detailHeight = detailNoise.noise(worldX * DETAIL_SCALE, 0, worldZ * DETAIL_SCALE);

                // Combine noise layers with different weights
                double combinedNoise =
                        continentHeight * 0.5 +     // Large scale landmasses
                                terrainHeight * 0.3 +       // Medium scale hills
                                detailHeight * 0.2;         // Fine detail

                // Convert to height
                int baseHeight = (int) ((combinedNoise * 0.5 + 0.5) * (MAX_HEIGHT - SEA_LEVEL)) + SEA_LEVEL;
                baseHeight = Math.max(MIN_HEIGHT, Math.min(baseHeight, MAX_HEIGHT));

                // Determine biome
                String biome = determineBiome(worldX, worldZ, baseHeight);
                if (primaryBiome == null) primaryBiome = biome;

                // Adjust height based on biome
                int finalHeight = adjustHeightForBiome(baseHeight, biome, worldX, worldZ);

                // Generate column
                for (int y = 0; y <= finalHeight && y < 256; y++) {
                    int blockId = determineBlockType(worldX, y, worldZ, finalHeight, biome);
                    if (blockId != AIR) {
                        blocks[x][y][z] = blockId;
                        blocksGenerated++;
                    }

                    // Add caves
                    if (y > 5 && y < 50) {
                        double caveValue = caveNoise.noise(worldX * 0.05, y * 0.05, worldZ * 0.05);
                        if (caveValue > 0.6) {
                            blocks[x][y][z] = AIR;
                            if (blocks[x][y][z] != AIR) blocksGenerated--;
                        }
                    }
                }

                // Add water for areas below sea level
                for (int y = finalHeight + 1; y <= SEA_LEVEL && y < 256; y++) {
                    // In a real implementation, you'd add water blocks here
                    // For now, we'll leave it as air
                }

                // Add surface features (trees, etc.)
                if (finalHeight >= SEA_LEVEL) {
                    generateSurfaceFeatures(blocks, x, z, finalHeight, biome, worldX, worldZ);
                }
            }
        }

        return new TerrainData(blocks, blocksGenerated, primaryBiome);
    }

    private String determineBiome(int worldX, int worldZ, int height) {
        double temperature = continentNoise.noise(worldX * BIOME_SCALE, 100, worldZ * BIOME_SCALE);
        double humidity = terrainNoise.noise(worldX * BIOME_SCALE, 200, worldZ * BIOME_SCALE);
        double biomeNoise = detailNoise.noise(worldX * BIOME_SCALE * 2, 0, worldZ * BIOME_SCALE * 2);

        // Normalize values
        temperature = (temperature + 1.0) * 0.5;
        humidity = (humidity + 1.0) * 0.5;
        biomeNoise = (biomeNoise + 1.0) * 0.5;

        // Height influences biome (mountains are colder)
        if (height > 90) {
            return temperature < 0.3 ? "mountains" : "taiga";
        }

        if (height < SEA_LEVEL + 5) {
            return "plains";
        }

        if (temperature < 0.2) {
            return humidity > 0.5 ? "taiga" : "mountains";
        } else if (temperature > 0.8) {
            return humidity < 0.3 ? "desert" : "swamp";
        } else if (humidity < 0.3) {
            return "desert";
        } else if (humidity > 0.7 && temperature > 0.4) {
            return biomeNoise > 0.6 ? "forest" : "swamp";
        } else {
            return biomeNoise > 0.5 ? "forest" : "plains";
        }
    }

    private int adjustHeightForBiome(int baseHeight, String biome, int worldX, int worldZ) {
        switch (biome) {
            case "mountains":
                double mountainNoise = terrainNoise.noise(worldX * 0.01, 0, worldZ * 0.01);
                return baseHeight + (int)(mountainNoise * 30);
            case "desert":
                return baseHeight - 5;
            case "swamp":
                return Math.min(baseHeight, SEA_LEVEL + 2);
            case "plains":
                double plainsNoise = detailNoise.noise(worldX * 0.03, 0, worldZ * 0.03);
                return baseHeight + (int)(plainsNoise * 5);
            default:
                return baseHeight;
        }
    }

    private int determineBlockType(int worldX, int worldY, int worldZ, int surfaceHeight, String biome) {
        // Get block IDs with proper fallback handling
        int stoneId = getBlockIdSafe("stone", 1);
        int dirtId = getBlockIdSafe("dirt", stoneId);
        int grassId = getBlockIdSafe("grass", stoneId);
        int sandId = getBlockIdSafe("sand_ugly", stoneId);
        int sandstoneId = getBlockIdSafe("sandstone", stoneId);
        int graniteId = getBlockIdSafe("granite", stoneId);
        int marbleId = getBlockIdSafe("marble", stoneId);
        int snowId = getBlockIdSafe("snow", stoneId);
        int basaltId = getBlockIdSafe("basalt", stoneId);
        int mudId = getBlockIdSafe("mud", dirtId);
        int oakLeavesId = getBlockIdSafe("oak_leaves", stoneId);

        if (worldY == 0) {
            return stoneId;
        }

        if (worldY < surfaceHeight - 8) {
            if (worldY < 16) {
                double oreChance = oreNoise.noise(worldX * 0.1, worldY * 0.1, worldZ * 0.1);
                if (oreChance > 0.85) {
                    return graniteId;
                } else if (oreChance > 0.75) {
                    return marbleId;
                }
            }

            switch (biome) {
                case "volcanic":
                    return basaltId;
                case "mountains":
                    return graniteId;
                case "marble_caves":
                    return marbleId;
                default:
                    return stoneId;
            }
        }

        if (worldY == surfaceHeight) {
            switch (biome) {
                case "desert":
                    return sandId;
                case "mountains":
                    if (surfaceHeight > 100) {
                        return snowId;
                    } else {
                        return stoneId;
                    }
                case "swamp":
                    return mudId;
                case "volcanic":
                    return basaltId;
                case "taiga":
                    return snowId;
                default:
                    return grassId;
            }
        } else if (worldY >= surfaceHeight - 4 && worldY < surfaceHeight) {
            switch (biome) {
                case "desert":
                    if (worldY >= surfaceHeight - 2) {
                        return sandId;
                    } else {
                        return sandstoneId;
                    }
                case "mountains":
                    return stoneId;
                case "swamp":
                    return mudId;
                case "volcanic":
                    return basaltId;
                default:
                    return dirtId;
            }
        }

        return stoneId;
    }

    /**
     * Helper method to safely get block IDs with fallback
     */
    private int getBlockIdSafe(String blockName, int fallback) {
        int id = registry.getIDFromName(blockName);
        if (id == 0) {
            return fallback;
        }
        return id;
    }

    private void generateSurfaceFeatures(int[][][] blocks, int x, int z, int surfaceHeight, String biome, int worldX, int worldZ) {
        int oakLeavesId = registry.getIDFromName("oak_leaves");
        int pineLeavesId = registry.getIDFromName("pine_leaves");
        int oakLogId = registry.getIDFromName("oak_log");
        int pineLogId = registry.getIDFromName("pine_log");

        if ((biome.equals("forest") || biome.equals("taiga")) && surfaceHeight >= SEA_LEVEL) {
            double treeChance = detailNoise.noise(worldX * 0.1, 0, worldZ * 0.1);
            if (treeChance > 0.7 && surfaceHeight + 6 < 256) {
                int logId = biome.equals("taiga") && pineLogId != 0 ? pineLogId :
                        (oakLogId != 0 ? oakLogId : registry.getIDFromName("stone"));
                int leavesId = biome.equals("taiga") && pineLeavesId != 0 ? pineLeavesId :
                        (oakLeavesId != 0 ? oakLeavesId : registry.getIDFromName("oak_leaves"));

                if (leavesId == 0) leavesId = registry.getIDFromName("stone");

                for (int y = surfaceHeight + 1; y <= surfaceHeight + 4; y++) {
                    if (y < 256) blocks[x][y][z] = logId;
                }

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            int leafX = x + dx;
                            int leafZ = z + dz;
                            int leafY = surfaceHeight + 4 + dy;

                            if (leafX >= 0 && leafX < 16 && leafZ >= 0 && leafZ < 16 &&
                                    leafY < 256 && (dx != 0 || dz != 0 || dy != 0)) {
                                blocks[leafX][leafY][leafZ] = leavesId;
                            }
                        }
                    }
                }
            }
        }
    }

    private TerrainData generateNetherTerrain(int chunkX, int chunkZ, PerlinNoise perlin) {
        // Placeholder for Nether generation
        int[][][] blocks = new int[16][256][16];
        return new TerrainData(blocks, 0, "nether");
    }

    private TerrainData generateEndTerrain(int chunkX, int chunkZ, PerlinNoise perlin) {
        int[][][] blocks = new int[16][256][16];
        return new TerrainData(blocks, 0, "end");
    }

    public Set<String> getAvailableDimensions() {
        return Set.of("overworld", "nether", "end");
    }

    public BiomeManager getBiomeManager() {
        return biomeManager;
    }

    public String getCurrentBiome(int worldX, int worldZ) {
        return determineBiome(worldX, worldZ, SEA_LEVEL + 20);
    }
}