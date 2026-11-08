package com.pandaismyname1.ultraterraforged.neoforge.compat;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;

/** Developer-only live-registry and generation checks. Never registered during normal play. */
public final class CompatibilitySmokeChecks {
    private CompatibilitySmokeChecks() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CompatibilitySmokeChecks::prepare);
        NeoForge.EVENT_BUS.addListener(CompatibilitySmokeChecks::started);
    }

    private static void prepare(ServerAboutToStartEvent event) {
        event.getServer().getWorldData().getGameRules()
                .getRule(net.minecraft.world.level.GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    private static void started(ServerStartedEvent event) {
        var server = event.getServer();
        var report = new LinkedHashMap<String, Object>();
        try {
            var level = server.overworld();
            var random = level.getChunkSource().randomState();
            require(((UTFRandomState) (Object) random).generatorContext() != null, "Wildshape preset is not active");
            report.put("mods", ModList.get().getMods().stream().map(mod ->
                    mod.getModId() + "@" + mod.getVersion()).sorted().toList());
            var dimensions = new java.util.ArrayList<String>();
            for (var other : server.getAllLevels()) {
                if (other == level) continue;
                require(((UTFRandomState) (Object) other.getChunkSource().randomState()).generatorContext() == null,
                        "Terrain context leaked into " + other.dimension().location());
                if (Boolean.getBoolean("wildshape.generateOtherDimensions")) other.getChunk(0, 0);
                dimensions.add(other.dimension().location().toString());
            }
            report.put("otherDimensionsRetainTheirGenerators", dimensions);
            report.put("generatedOtherDimensionSamples", Boolean.getBoolean("wildshape.generateOtherDimensions"));

            var sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
            var spacing = new LinkedHashMap<String, Integer>();
            Map<String, Integer> expected = Map.of("idas:idas_common", 36, "dungeons_arise:major_structures", 64,
                    "ae2:meteorite", 32, "alexscaves:acid_pit", 6, "hybrid_aquatic:whale_falls", 16,
                    "betterfortresses:fortress", 30);
            expected.forEach((id, wanted) -> sets.getOptional(ResourceLocation.parse(id)).ifPresent(set -> {
                if (set.placement() instanceof RandomSpreadStructurePlacement spread) {
                    spacing.put(id, spread.spacing());
                    require(spread.spacing() == wanted, "Unexpected spacing for " + id + ": " + spread.spacing());
                }
            }));
            report.put("structureSpacing", spacing);
            var customTypes = new LinkedHashMap<String, String>();
            Map.of("idas:idas_common", "integrated_api:advanced_random_spread",
                    "dungeons_arise:major_structures", "integrated_api:advanced_random_spread",
                    "betterjungletemples:jungle_temples", "betterjungletemples:jungle_temple",
                    "betterdeserttemples:desert_temples", "betterdeserttemples:desert_temple")
                    .forEach((id, wanted) -> sets.getOptional(ResourceLocation.parse(id)).ifPresent(set -> {
                        String type = BuiltInRegistries.STRUCTURE_PLACEMENT.getKey(set.placement().type()).toString();
                        require(type.equals(wanted), "Custom placement type was erased for " + id + ": " + type);
                        var json = net.minecraft.world.level.levelgen.structure.placement.StructurePlacement.CODEC
                                .encodeStart(level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),
                                        set.placement()).getOrThrow().getAsJsonObject();
                        if (id.startsWith("idas:") || id.startsWith("dungeons_arise:"))
                            require(json.has("super_exclusion_zone"), "Lost Integrated exclusion zone for " + id);
                        if (id.startsWith("betterjungletemples:"))
                            require(json.has("enhanced_exclusion_zone"), "Lost YUNG exclusion zone for " + id);
                        customTypes.put(id, type);
                    }));
            report.put("customStructurePlacementTypes", customTypes);

            var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            var aliases = Map.of("is_hot_overworld", "is_hot/overworld", "is_cold_overworld", "is_cold/overworld",
                    "is_dry_overworld", "is_dry/overworld", "is_dense_overworld", "is_dense_vegetation/overworld",
                    "is_coniferous", "is_tree/coniferous");
            var habitatSizes = new LinkedHashMap<String, Integer>();
            aliases.forEach((legacy, modern) -> {
                var source = biomes.getTag(TagKey.create(Registries.BIOME, ResourceLocation.parse("c:" + modern)));
                var target = TagKey.create(Registries.BIOME, ResourceLocation.parse("c:" + legacy));
                source.ifPresent(values -> {
                    values.forEach(biome -> require(biome.is(target), "Missing habitat bridge for " + biome.unwrapKey()));
                    habitatSizes.put(legacy, values.size());
                });
            });
            report.put("legacyHabitats", habitatSizes);
            if (ModList.get().isLoaded("hybrid_aquatic") && ModList.get().isLoaded("regions_unexplored")) {
                var fen = biomes.getOptional(ResourceLocation.parse("regions_unexplored:fen")).orElseThrow();
                long spawns = java.util.Arrays.stream(MobCategory.values()).flatMap(category ->
                        fen.getMobSettings().getMobs(category).unwrap().stream()).filter(spawn ->
                        BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type).toString()
                                .equals("hybrid_aquatic:african_butterflyfish")).count();
                require(spawns > 0, "HAPI native fish spawn modifier did not recognize RU fen");
                report.put("nativeFishHabitatApplied", true);
            }

            if (ModList.get().isLoaded("knightquest")) {
                var candidate = biomes.holders().filter(biome -> !biome.key().location().getNamespace().equals("minecraft")
                        && biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_forest")))
                        && biome.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD)
                        && !biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_cave")))
                        && !biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_underground")))
                        && !biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse("ultraterraforged:protected_cave_biomes"))))
                        .findFirst().orElseThrow();
                var nativeModifier = (net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier)
                        level.registryAccess().registryOrThrow(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS)
                        .get(ResourceLocation.parse("knightquest:spawn_eldknight"));
                var expectedSpawn = nativeModifier.spawners().getFirst();
                var actualSpawns = candidate.value().getMobSettings().getMobs(expectedSpawn.type.getCategory()).unwrap()
                        .stream().filter(spawn -> spawn.type == expectedSpawn.type).toList();
                require(actualSpawns.size() == 1, "Knight Quest bridge duplicated or omitted its spawn in " + candidate.key());
                var actualSpawn = actualSpawns.getFirst();
                require(actualSpawn.getWeight().asInt() == expectedSpawn.getWeight().asInt()
                        && actualSpawn.minCount == expectedSpawn.minCount && actualSpawn.maxCount == expectedSpawn.maxCount,
                        "Native Knight Quest spawn weights/groups changed");
                report.put("nativeSpawnDataPreservedWithoutDuplication", candidate.key().location().toString());
            }

            var generator = level.getChunkSource().getGenerator();
            var heights = new LinkedHashMap<String, Integer>();
            try (var workers = Executors.newFixedThreadPool(2)) {
                for (int[] point : new int[][] {{13, 2}, {2560, 2048}, {-4096, 5120}, {13312, 2144}}) {
                    int x = point[0], z = point[1];
                    int height = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                    int threaded = workers.submit(() -> generator.getBaseHeight(x, z,
                            Heightmap.Types.WORLD_SURFACE_WG, level, random)).get();
                    require(height == threaded, "Column height changed on a different thread");
                    heights.put(x + "," + z, height);
                }
            }
            report.put("threadIndependentColumnHeights", heights);
            generator.getBiomeSource().findBiomeHorizontal(0, 64, 0, 64, biome -> true,
                    level.getRandom(), random.sampler());
            require(!WorldGenFlags.fastLookups(), "Biome search leaked approximate lookup state");
            report.put("biomeSearchRestoresLookupScope", true);
            for (int[] point : new int[][] {{0, 0}, {2, -1}, {-2, 1}}) level.getChunk(point[0], point[1]);
            report.put("generatedSampleChunks", 3);
            report.put("registeredBiomes", biomes.size());
            report.put("protectedCaveTagMembers", biomes.getTag(TagKey.create(Registries.BIOME,
                    ResourceLocation.parse("ultraterraforged:protected_cave_biomes"))).map(values -> values.size()).orElse(0));
            if (ModList.get().isLoaded("streamsreflowing"))
                nl.townsandkingdoms.terraforged.compat.streams.StreamsSmokeChecks.verify(level, report);
            report.put("passed", true);
        } catch (Throwable failure) {
            report.put("passed", false);
            report.put("error", failure.toString());
            UTFCommon.LOGGER.error("Wildshape compatibility smoke failed", failure);
        }
        try {
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("wildshape-compat-results.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { UTFCommon.LOGGER.error("Cannot save compatibility smoke report", failure); }
        UTFCommon.LOGGER.info("Wildshape compatibility smoke complete: {}", report);
        server.halt(false);
    }
}
