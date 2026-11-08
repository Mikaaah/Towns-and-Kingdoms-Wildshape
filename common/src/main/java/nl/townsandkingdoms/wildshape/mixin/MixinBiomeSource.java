package nl.townsandkingdoms.wildshape.mixin;

import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import nl.townsandkingdoms.wildshape.world.worldgen.WorldGenFlags;

/** Searches can scan ungenerated terrain without constructing filtered tiles at every sample. */
@Mixin(BiomeSource.class)
class MixinBiomeSource {
    @WrapMethod(method = "findClosestBiome3d")
    private Pair<BlockPos, Holder<Biome>> wildshape$closest(BlockPos origin, int radius, int horizontalStep,
            int verticalStep, Predicate<Holder<Biome>> predicate, Climate.Sampler sampler, LevelReader level,
            Operation<Pair<BlockPos, Holder<Biome>>> original) {
        return WorldGenFlags.approximately(() -> original.call(origin, radius, horizontalStep, verticalStep, predicate, sampler, level));
    }

    @WrapMethod(method = "findBiomeHorizontal(IIIIILjava/util/function/Predicate;Lnet/minecraft/util/RandomSource;ZLnet/minecraft/world/level/biome/Climate$Sampler;)Lcom/mojang/datafixers/util/Pair;")
    private Pair<BlockPos, Holder<Biome>> wildshape$horizontal(int x, int y, int z, int radius, int step,
            Predicate<Holder<Biome>> predicate, RandomSource random, boolean closest, Climate.Sampler sampler,
            Operation<Pair<BlockPos, Holder<Biome>>> original) {
        return WorldGenFlags.approximately(() -> original.call(x, y, z, radius, step, predicate, random, closest, sampler));
    }
}
