package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import nl.townsandkingdoms.wildshape.data.preset.settings.WorldSettings.ControlPoints;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.Continentalness;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Heightmap;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Levels;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noises;
import nl.townsandkingdoms.wildshape.world.worldgen.terrain.TerrainType;

/** Exercise the final shared climate tuple consumed by vanilla and TerraBlender, not a biome-ID replacement. */
class SurfaceWaterBiomeClassificationTest {
    private static final Levels LEVELS = new Levels(384, 63);
    private static final ControlPoints POINTS = new ControlPoints(-1, -1, 0.1F, 0.25F,
            0.327F, 0.448F, 0.502F, 1.327F, 2.758F);
    private static Climate.ParameterList<ResourceKey<Biome>> biomes;
    private static Heightmap heightmap;

    @BeforeAll static void setup() throws ReflectiveOperationException {
        TestBootstrap.init();
        List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> points = new ArrayList<>();
        Method add = OverworldBiomeBuilder.class.getDeclaredMethod("addBiomes", Consumer.class);
        add.setAccessible(true);
        add.invoke(new OverworldBiomeBuilder(), (Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>) points::add);
        biomes = new Climate.ParameterList<>(points);
        heightmap = new Heightmap(null, null, null, null, null, LEVELS, POINTS, 1,
                null, Noises.zero(), null);
    }

    @Test void wideInlandRiverBedBelowSeaRemainsRiverInsteadOfOcean() {
        Cell channel = cell(TerrainType.RIVER, -18, 0.05F, 0.75F);
        channel.riverWidth = 100;
        channel.weirdness = 0.45F;
        heightmap.applyPost(channel, 0, 0);
        assertEquals(Biomes.RIVER, biome(channel, 0.5F));
        assertEquals(Biomes.FROZEN_RIVER, biome(channel, -0.8F));
        assertEquals(0, channel.weirdness);
        assertTrue(channel.continentalness >= Continentalness.NEAR_INLAND.min());
    }

    @Test void elevatedChannelRetainsRiverClimateEvenUnderAnOceanContinentMask() {
        Cell channel = cell(TerrainType.RIVER, 25, 0.05F, 0.05F);
        channel.waterLevel = LEVELS.water(40);
        channel.riverWidth = 80;
        heightmap.applyPost(channel, 0, 0);
        assertEquals(Biomes.RIVER, biome(channel, 0.5F));
    }

    @Test void lowCoastalEstuaryAndRealOceanRetainOceanClimate() {
        Cell estuary = cell(TerrainType.RIVER, -12, 0.2F, 0.2F);
        estuary.weirdness = 0;
        heightmap.applyPost(estuary, 0, 0);
        assertTrue(estuary.continentalness >= Continentalness.OCEAN.min()
                && estuary.continentalness <= Continentalness.OCEAN.max());
        assertNotEquals(Biomes.RIVER, biome(estuary, 0.5F));
        Cell sea = cell(TerrainType.DEEP_OCEAN, -45, 0.05F, 0.05F);
        heightmap.applyPost(sea, 0, 0);
        assertTrue(sea.continentalness >= Continentalness.DEEP_OCEAN.min()
                && sea.continentalness <= Continentalness.DEEP_OCEAN.max());
        assertNotEquals(Biomes.RIVER, biome(sea, 0.5F));
    }

    @Test void realMushroomIslandProvenanceSurvivesRiverAndShoreLabels() {
        for (var terrain : List.of(TerrainType.RIVER, TerrainType.BEACH, TerrainType.COAST)) {
            Cell island = cell(terrain, terrain.isRiver() ? -5 : 1, 0.05F, 0.05F);
            island.mushroomIsland = true;
            heightmap.applyPost(island, 0, 0);
            assertEquals(Biomes.MUSHROOM_FIELDS, biome(island, 0.5F), terrain.getName());
        }
        Cell ordinary = cell(TerrainType.BEACH, 1, 0.2F, 0.2F);
        heightmap.applyPost(ordinary, 0, 0);
        assertNotEquals(Biomes.MUSHROOM_FIELDS, biome(ordinary, 0.5F), "ordinary islands must not become fungal");
        Cell slope = cell(TerrainType.DEEP_OCEAN, -10, 0.05F, 0.05F);
        heightmap.applyPost(slope, 0, 0);
        assertNotEquals(Biomes.MUSHROOM_FIELDS, biome(slope, 0.5F), "submerged outer slopes remain sea");
    }

    @Test void mushroomProvenanceCopiesWithTerrainAndResetsWithPooledCells() {
        Cell original = new Cell();
        original.mushroomIsland = true;
        Cell copy = new Cell();
        copy.copyFrom(original);
        assertTrue(copy.mushroomIsland);
        copy.reset();
        assertFalse(copy.mushroomIsland);
    }

    private static Cell cell(nl.townsandkingdoms.wildshape.world.worldgen.terrain.Terrain terrain,
                             int height, float continentNoise, float continentEdge) {
        Cell cell = new Cell();
        cell.terrain = terrain;
        cell.height = LEVELS.water(height);
        cell.continentNoise = continentNoise;
        cell.continentEdge = continentEdge;
        cell.erosion = 0.4F;
        cell.weirdness = 0.2F;
        return cell;
    }

    private static ResourceKey<Biome> biome(Cell cell, float temperature) {
        return biomes.findValue(Climate.target(temperature, 0.5F, cell.continentalness,
                cell.erosion, 0, cell.weirdness));
    }
}
