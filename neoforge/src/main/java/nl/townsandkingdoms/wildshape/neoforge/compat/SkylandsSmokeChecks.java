package nl.townsandkingdoms.wildshape.neoforge.compat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.WorldGenFlags;

/** Opt-in developer fixture. No SkyLands classes or checks load during ordinary play. */
public final class SkylandsSmokeChecks {
    private SkylandsSmokeChecks() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkylandsSmokeChecks::prepare);
        NeoForge.EVENT_BUS.addListener(SkylandsSmokeChecks::started);
    }

    private static void prepare(ServerAboutToStartEvent event) {
        event.getServer().getWorldData().getGameRules()
                .getRule(net.minecraft.world.level.GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);
    }

    private static void require(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    private static void started(ServerStartedEvent event) {
        var server = event.getServer();
        String mode = System.getProperty("wildshape.skylandsSmoke");
        var report = new LinkedHashMap<String, Object>();
        report.put("mode", mode);
        try {
            var level = server.overworld();
            var random = level.getChunkSource().randomState();
            require(((WildshapeRandomState) (Object) random).generatorContext() != null,
                    "Wildshape terrain was replaced");
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            int noiseHeight = generator.generatorSettings().value().noiseSettings().height();
            var baseline = JsonParser.parseString(Files.readString(FMLPaths.GAMEDIR.get()
                    .resolve("skylands-baseline.json"))).getAsJsonObject();
            require(noiseHeight == baseline.get("noiseHeight").getAsInt(),
                    "The saved baseline noise terrain height changed: " + noiseHeight);
            require(level.getMinBuildHeight() == baseline.get("minY").getAsInt(),
                    "The saved baseline world bottom changed");
            report.put("noiseHeight", noiseHeight);
            report.put("minY", level.getMinBuildHeight());
            report.put("maxYExclusive", level.getMaxBuildHeight());
            report.put("previousMaxYExclusive", baseline.get("maxYExclusive").getAsInt());
            report.put("mods", ModList.get().getMods().stream()
                    .map(mod -> mod.getModId() + "@" + mod.getVersion()).sorted().toList());
            var dimensions = new LinkedHashMap<String, Object>();
            for (var other : server.getAllLevels()) {
                if (other == level) continue;
                require(((WildshapeRandomState) (Object) other.getChunkSource().randomState()).generatorContext() == null,
                        "Wildshape terrain leaked into " + other.dimension().location());
                dimensions.put(other.dimension().location().toString(),
                        Map.of("minY", other.getMinBuildHeight(), "height", other.getHeight()));
            }
            report.put("otherDimensions", dimensions);

            if (mode.equals("absent")) {
                require(!ModList.get().isLoaded("skylands"), "Absent fixture has SkyLands installed");
                require(level.getMaxBuildHeight() == baseline.get("maxYExclusive").getAsInt(),
                        "Height expanded without SkyLands");
                level.getChunk(0, 0);
            } else {
                require(ModList.get().isLoaded("skylands"), "SkyLands is missing");
                require(level.getMaxBuildHeight() >= 528, "Saved preset still clips the sky layer");
                var source = generator.getBiomeSource();
                long skyBiomes = source.possibleBiomes().stream()
                        .filter(b -> b.unwrapKey().orElseThrow().location().getNamespace().equals("skylands")).count();
                require(skyBiomes == 6, "Lithostitched did not expose all six sky biomes: " + skyBiomes);
                report.put("skyBiomes", skyBiomes);
                var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
                var placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                        .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE,
                                ResourceLocation.parse("skylands:procedural_overworld_terrain")));
                require(biomeRegistry.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS)
                        .value().getGenerationSettings().features().stream()
                        .anyMatch(set -> set.contains(placed)), "Native island feature is missing from host biomes");

                int x, z;
                Path previous = FMLPaths.GAMEDIR.get().resolve("skylands-generate.json");
                if (mode.equals("reload")) {
                    var saved = JsonParser.parseString(Files.readString(previous)).getAsJsonObject();
                    x = saved.get("islandX").getAsInt();
                    z = saved.get("islandZ").getAsInt();
                } else {
                    // Consult the installed mod's actual deterministic site API rather
                    // than reproducing or redistributing its terrain algorithm.
                    Class<?> shape = Class.forName("net.deepcanion.skylands.worldgen.SkyIslandShape");
                    var siteX = shape.getMethod("siteX", long.class, int.class, int.class);
                    var siteZ = shape.getMethod("siteZ", long.class, int.class, int.class);
                    var hasSite = shape.getMethod("hasSite", long.class, int.class, int.class);
                    int[] found = null;
                    search: for (int gx = 28; gx < 52; gx++) for (int gz = 28; gz < 52; gz++) {
                        if (!(boolean) hasSite.invoke(null, level.getSeed(), gx, gz)) continue;
                        int sx = (int) siteX.invoke(null, level.getSeed(), gx, gz);
                        int sz = (int) siteZ.invoke(null, level.getSeed(), gx, gz);
                        var biome = WorldGenFlags.approximately(() -> source.getNoiseBiome(
                                QuartPos.fromBlock(sx), QuartPos.fromBlock(350), QuartPos.fromBlock(sz), random.sampler()));
                        if (biome.unwrapKey().orElseThrow().location().getNamespace().equals("skylands")) {
                            found = new int[] {sx, sz};
                            break search;
                        }
                    }
                    require(found != null, "No injected sky-region island site found");
                    x = found[0]; z = found[1];
                }
                report.put("islandX", x); report.put("islandZ", z);
                var chunk = level.getChunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
                var ceilingMarker = new BlockPos(x, 527, z);
                if (mode.equals("generate"))
                    require(level.setBlock(ceilingMarker, Blocks.EMERALD_BLOCK.defaultBlockState(), 2),
                            "Cannot write the newly available Y=527 build layer");
                require(level.getBlockState(ceilingMarker).is(Blocks.EMERALD_BLOCK),
                        "The new-ceiling marker did not survive reload");
                report.put("newCeilingMarkerY", 527);
                int blocks = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                var position = new BlockPos.MutableBlockPos();
                int startX = chunk.getPos().getMinBlockX(), startZ = chunk.getPos().getMinBlockZ();
                for (int bx = startX; bx < startX + 16; bx++) for (int bz = startZ; bz < startZ + 16; bz++)
                    for (int y = 320; y < level.getMaxBuildHeight(); y++) {
                        var state = chunk.getBlockState(position.set(bx, y, bz));
                        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("skylands")) {
                            blocks++; min = Math.min(min, y); max = Math.max(max, y);
                        }
                    }
                require(blocks > 100, "Native islands did not generate in the sky layer: " + blocks);
                report.put("skyBlocksInSkyLayer", blocks);
                report.put("skyBlockMinY", min); report.put("skyBlockMaxY", max);
                require(level.getBlockState(new BlockPos(8, 90, 8)).is(Blocks.DIAMOND_BLOCK),
                        "Baseline existing-world marker was lost");
                report.put("existingWorldMarkerPreserved", true);
                report.put("chunk", List.of(chunk.getPos().x, chunk.getPos().z));
            }
            report.put("structurePlacement", nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructurePlacementCoordinator.audit(level));
            report.put("passed", true);
        } catch (Throwable failure) {
            report.put("passed", false);
            report.put("error", failure.toString());
            WildshapeCommon.LOGGER.error("SkyLands compatibility fixture failed", failure);
        } finally {
            try {
                String filename = mode.equals("reload") ? "skylands-reload.json"
                        : mode.equals("absent") ? "skylands-absent.json" : "skylands-generate.json";
                Files.writeString(FMLPaths.GAMEDIR.get().resolve(filename),
                        new GsonBuilder().setPrettyPrinting().create().toJson(report));
            } catch (Exception failure) {
                WildshapeCommon.LOGGER.error("Cannot save SkyLands verification report", failure);
            }
            server.halt(false);
        }
    }
}
