package nl.townsandkingdoms.wildshape.mixin.structure;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructurePlacementCoordinator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.world.structures.placements.CataclysmRandomSpread", remap = false)
abstract class MixinCataclysmStructurePlacement {
    @WrapMethod(method = "isStructureChunk", remap = false)
    private boolean wildshape$regionalClearance(ChunkGeneratorStructureState state, int x, int z, Operation<Boolean> original) {
        return StructurePlacementCoordinator.filterNative((StructurePlacement)(Object)this, state, x, z,
            () -> original.call(state, x, z));
    }
}
