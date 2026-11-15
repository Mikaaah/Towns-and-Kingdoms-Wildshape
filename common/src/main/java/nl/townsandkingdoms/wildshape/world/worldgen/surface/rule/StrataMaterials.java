package nl.townsandkingdoms.wildshape.world.worldgen.surface.rule;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Broad stone tags sometimes contain ores; they must never become free geological layers. */
final class StrataMaterials {
    private static final TagKey<net.minecraft.world.level.block.Block> ORES = tag("c", "ores");
    private static final TagKey<net.minecraft.world.level.block.Block> LEGACY_ORES = tag("forge", "ores");
    private static final TagKey<net.minecraft.world.level.block.Block> STORAGE = tag("c", "storage_blocks");

    private StrataMaterials() {}

    static boolean isResource(BlockState state) {
        return state.is(ORES) || state.is(LEGACY_ORES) || state.is(STORAGE)
                || state.getBlock() instanceof DropExperienceBlock
                || state.getBlock() instanceof RedStoneOreBlock
                || oreName(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
    }

    // Also covers custom ore blocks missing c:ores. Match path segments, not substrings such as "scoria".
    static boolean oreName(String path) {
        return path.equals("ore") || path.startsWith("ore_") || path.endsWith("_ore")
                || path.contains("_ore_") || path.contains("/ore_") || path.endsWith("/ore");
    }

    private static TagKey<net.minecraft.world.level.block.Block> tag(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
