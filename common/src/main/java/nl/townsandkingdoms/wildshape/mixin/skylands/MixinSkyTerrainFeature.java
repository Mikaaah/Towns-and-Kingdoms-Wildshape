package nl.townsandkingdoms.wildshape.mixin.skylands;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import nl.townsandkingdoms.wildshape.world.worldgen.WorldGenFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Island anchor probes must not generate filtered terrain tiles at every remote anchor. */
@Pseudo
@Mixin(targets = "net.deepcanion.skylands.worldgen.SkyTerrainFeature", remap = false)
class MixinSkyTerrainFeature {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/biome/BiomeSource;getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"), require = 1)
    private static Holder<Biome> wildshape$surveyIsland(BiomeSource source, int x, int y, int z,
            Climate.Sampler sampler, Operation<Holder<Biome>> original) {
        return WorldGenFlags.approximately(() -> original.call(source, x, y, z, sampler));
    }
}
