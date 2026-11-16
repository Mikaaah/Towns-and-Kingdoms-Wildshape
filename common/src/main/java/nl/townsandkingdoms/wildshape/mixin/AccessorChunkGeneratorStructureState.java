package nl.townsandkingdoms.wildshape.mixin;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkGeneratorStructureState.class)
public interface AccessorChunkGeneratorStructureState {
    @Accessor("biomeSource") BiomeSource wildshape$biomeSource();
}
