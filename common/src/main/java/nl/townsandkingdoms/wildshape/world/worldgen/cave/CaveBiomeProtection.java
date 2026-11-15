package nl.townsandkingdoms.wildshape.world.worldgen.cave;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import nl.townsandkingdoms.wildshape.WildshapeCommon;

/** Native cave habitats own their terrain, features and spawn settings. */
final class CaveBiomeProtection {
    static final TagKey<Biome> PROTECTED = TagKey.create(Registries.BIOME,
            WildshapeCommon.location("protected_cave_biomes"));
    private static final TagKey<Biome> CAVE = common("is_cave");
    private static final TagKey<Biome> UNDERGROUND = common("is_underground");

    private CaveBiomeProtection() {}

    static boolean protects(Holder<Biome> biome) {
        if (biome == null) return false;
        if (biome.is(Biomes.DEEP_DARK) || biome.is(PROTECTED)) return true;
        return biome.unwrapKey().map(key -> {
            String namespace = key.location().getNamespace();
            // These mods predate consistent common cave tags. Keep their existing safeguards.
            if (namespace.equals("alexscaves") || namespace.equals("yungscavebiomes")) return true;
            // Vanilla lush/dripstone caves remain eligible for the approved cavern styles.
            return !namespace.equals("minecraft") && (biome.is(CAVE) || biome.is(UNDERGROUND));
        }).orElse(false);
    }

    private static TagKey<Biome> common(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
