package nl.townsandkingdoms.wildshape.world.worldgen;

import org.jetbrains.annotations.Nullable;

import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.world.worldgen.cave.CaveFeatures;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Heightmap;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Levels;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.WorldLookup;
import nl.townsandkingdoms.wildshape.world.worldgen.tile.TileCache;
import nl.townsandkingdoms.wildshape.world.worldgen.tile.TileGenerator;
import nl.townsandkingdoms.wildshape.world.worldgen.tile.filter.WorldFilters;
import nl.townsandkingdoms.wildshape.world.worldgen.util.Seed;

public class GeneratorContext {
    // Enabled by the optional native-current integration before worlds are created.
    public static volatile boolean retainRiverData;
    public final nl.townsandkingdoms.wildshape.world.worldgen.rivermap.RiverDataCache riverData =
        new nl.townsandkingdoms.wildshape.world.worldgen.rivermap.RiverDataCache();
    public Seed seed;
    public Levels levels;
    public Preset preset;
    
    @Deprecated
    public ThreadLocal<Heightmap> localHeightmap;
    public TileGenerator generator;
    @Nullable
    public TileCache cache;
    public WorldLookup lookup;
    public CaveFeatures caveFeatures;
    
    public GeneratorContext(Preset preset, int seed, int tileSize, int tileBorder, int batchCount, @Nullable TileCache cache) {
        this.preset = preset;
        this.seed = new Seed(seed);
        this.levels = new Levels(preset.world().properties.terrainScaler(), preset.world().properties.seaLevel);

        Heightmap globalHeightmap = Heightmap.make(this);
        this.localHeightmap = ThreadLocal.withInitial(globalHeightmap::cache);
        this.generator = new TileGenerator(this.localHeightmap, new WorldFilters(this, globalHeightmap), tileSize, tileBorder, batchCount);
        this.cache = cache;
        this.lookup = new WorldLookup(this);
        this.caveFeatures = new CaveFeatures(this);
    }

    public static GeneratorContext makeCached(Preset preset, int seed, int tileSize, int batchCount, boolean queue) {
    	GeneratorContext ctx = makeUncached(preset, seed, tileSize, Math.min(2, Math.max(1, preset.filters().erosion.dropletLifetime / 16)), batchCount);
    	ctx.cache = new TileCache(tileSize, queue, ctx.generator);
    	ctx.lookup = new WorldLookup(ctx);
    	return ctx;
    }
    
    public static GeneratorContext makeUncached(Preset preset, int seed, int tileSize, int tileBorder, int batchCount) {
    	return new GeneratorContext(preset, seed, tileSize, tileBorder, batchCount, null);
    }
}
