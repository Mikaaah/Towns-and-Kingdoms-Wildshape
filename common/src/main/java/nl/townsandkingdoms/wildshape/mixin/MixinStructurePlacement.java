package nl.townsandkingdoms.wildshape.mixin;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructurePlacementCoordinator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(StructurePlacement.class)
abstract class MixinStructurePlacement {
    @WrapMethod(method = "isStructureChunk")
    private boolean wildshape$regionalClearance(ChunkGeneratorStructureState state, int x, int z, Operation<Boolean> original) {
        return StructurePlacementCoordinator.filterNative((StructurePlacement)(Object)this, state, x, z,
            () -> original.call(state, x, z));
    }
}
