package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.mixin.ConditionAccessor;

import java.util.Map;

public final class PlacementEngine {
    private PlacementEngine() {}

    public static Holder<Biome> pick(WorldContext world, Climate.Sampler sampler, int x, int y, int z) {
        int blockY = QuartPos.toBlock(y);
        if (blockY < FixConfig.MIN_CAVE_Y.get() || blockY > FixConfig.MAX_CAVE_Y.get()) return null;
        var info = ACBiomeRarity.getRareBiomeInfoForQuad(world.seed, x, z);
        if (info == null) return null;
        int offset = ACBiomeRarity.getRareBiomeOffsetId(info);
        var center = ACBiomeRarity.getRareBiomeCenter(info);
        if (center == null) return null;
        int cx = (int) Math.floor(center.x);
        int cz = (int) Math.floor(center.z);
        Climate.TargetPoint local = sampler.sample(x, y, z);
        Climate.TargetPoint centerPoint = sampler.sample(cx, y, cz);
        float depth = Climate.unquantizeCoord(local.depth());
        float continentalness = Climate.unquantizeCoord(centerPoint.continentalness());

        for (Map.Entry<ResourceKey<Biome>, BiomeGenerationNoiseCondition> entry : BiomeGenerationConfig.getBiomesSnapshot().entrySet()) {
            BiomeGenerationNoiseCondition condition = entry.getValue();
            if (condition.getRarityOffset() != offset || condition.isDisabledCompletely()) continue;
            ConditionAccessor fields = (ConditionAccessor) condition;
            if (!PlacementRules.outsideSpawnExclusion((long) cx * 4, (long) cz * 4, condition.getDistanceFromSpawn())) continue;
            var dimensions = fields.tfacfix$dimensions();
            if (dimensions != null && !dimensions.isEmpty() && !dimensions.contains(world.dimension.location().toString())) continue;
            if (!PlacementRules.matchesDepth(depth, fields.tfacfix$depth(), FixConfig.DEFAULT_MAX_DEPTH.get().floatValue())) continue;
            if (!PlacementRules.matches(Climate.unquantizeCoord(centerPoint.erosion()), fields.tfacfix$erosion())) continue;
            if (!PlacementRules.matches(Climate.unquantizeCoord(centerPoint.temperature()), fields.tfacfix$temperature())) continue;
            if (!PlacementRules.matches(Climate.unquantizeCoord(centerPoint.humidity()), fields.tfacfix$humidity())) continue;
            if (!PlacementRules.matches(Climate.unquantizeCoord(centerPoint.weirdness()), fields.tfacfix$weirdness())) continue;
            String name = entry.getKey().location().getPath();
            boolean ocean = world.isOcean(QuartPos.toBlock(cx), QuartPos.toBlock(cz));
            if (!PlacementRules.matchesContinentalness(name, continentalness, fields.tfacfix$continentalness(), ocean,
                    FixConfig.ADAPT_INLAND_FILTERS.get())) continue;
            // Preserve the port's ocean-only intent without its extra hardcoded vanilla-noise threshold.
            if (name.equals("abyssal_chasm") && (!ocean || !world.isOcean(QuartPos.toBlock(x), QuartPos.toBlock(z)))) continue;
            return world.biomes.get(entry.getKey());
        }
        return null;
    }
}
