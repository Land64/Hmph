package hmph.rendering.world.dimensions;
import hmph.math.PerlinNoise;
import hmph.rendering.BlockRegistry;
public class GreenLand {
    private final DimensionCreator dimensionCreator;
    public GreenLand(BlockRegistry registry) {
        this.dimensionCreator = new DimensionCreator(registry);
    }
    public DimensionCreator.TerrainData generateChunk(int chunkX, int chunkZ) {
        return dimensionCreator.generateTerrain("greenland", chunkX, chunkZ, new PerlinNoise());
    }
    public DimensionCreator getDimensionCreator() {
        return dimensionCreator;
    }
}
