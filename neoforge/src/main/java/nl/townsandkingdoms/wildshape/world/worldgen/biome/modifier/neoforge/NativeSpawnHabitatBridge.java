package nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier.neoforge;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Extend verified vanilla-only selectors using the installed native modifier's actual spawn data. */
final class NativeSpawnHabitatBridge {
    private record Rule(ResourceLocation source, List<TagKey<Biome>> habitats) {
        Rule(String source, String... habitats) {
            this(ResourceLocation.parse(source), java.util.Arrays.stream(habitats)
                    .map(id -> TagKey.create(Registries.BIOME, ResourceLocation.parse(id))).toList());
        }
    }

    private static final List<Rule> RULES = List.of(
            new Rule("knightquest:spawn_bad_patch", "c:is_jungle"),
            new Rule("knightquest:spawn_eldbomb", "c:is_forest", "c:is_taiga", "c:is_snowy_plains"),
            new Rule("knightquest:spawn_eldknight", "c:is_forest", "c:is_taiga", "c:is_snowy_plains"),
            new Rule("knightquest:spawn_fallen_knight", "c:is_forest", "c:is_taiga", "c:is_desert"),
            new Rule("knightquest:spawn_ghosty", "c:is_forest", "c:is_taiga", "c:is_badlands"),
            new Rule("knightquest:spawn_lizzy", "c:is_forest", "c:is_taiga", "c:is_desert"),
            new Rule("knightquest:spawn_ratman", "c:is_desert", "c:is_jungle"),
            new Rule("knightquest:spawn_swampman", "c:is_beach", "minecraft:is_river", "c:is_jungle"),
            new Rule("legendary_monsters:bomber", "c:is_desert")
    );
    private static final TagKey<Biome> CAVE = TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_cave"));
    private static final TagKey<Biome> UNDERGROUND = TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_underground"));
    private static final TagKey<Biome> PROTECTED = TagKey.create(Registries.BIOME,
            ResourceLocation.parse("wildshape:protected_cave_biomes"));

    private NativeSpawnHabitatBridge() {}

    static void apply(MinecraftServer server, Holder<Biome> biome, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (!biome.is(BiomeTags.IS_OVERWORLD) || biome.is(CAVE) || biome.is(UNDERGROUND) || biome.is(PROTECTED)) return;
        if (biome.unwrapKey().map(key -> key.location().getNamespace().equals("minecraft")).orElse(true)) return;
        var modifiers = server.registryAccess().registry(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        if (modifiers.isEmpty()) return;
        var spawns = builder.getMobSpawnSettings();
        for (Rule rule : RULES) {
            if (rule.habitats.stream().noneMatch(biome::is)) continue;
            var source = modifiers.get().getOptional(rule.source).orElse(null);
            // Removed/disabled/absent modifiers stay disabled; never invent default weights.
            if (!(source instanceof AddSpawnsBiomeModifier nativeSpawns) || nativeSpawns.biomes().contains(biome)) continue;
            for (var spawn : nativeSpawns.spawners()) {
                var category = spawn.type.getCategory();
                if (spawns.getSpawner(category).stream().noneMatch(existing -> existing.type == spawn.type))
                    spawns.addSpawn(category, spawn);
            }
        }
    }
}
