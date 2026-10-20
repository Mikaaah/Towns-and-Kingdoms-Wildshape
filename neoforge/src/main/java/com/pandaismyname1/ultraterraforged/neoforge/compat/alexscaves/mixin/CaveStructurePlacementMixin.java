package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.level.structure.AbstractCaveGenerationStructure;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.FixConfig;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.TerraforgedAlexCavesFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = Structure.class, remap = false)
public abstract class CaveStructurePlacementMixin {
    @Shadow protected abstract Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context);

    @Inject(method = "findValidGenerationPoint", at = @At("HEAD"), cancellable = true)
    private void tfacfix$validateChamber(Structure.GenerationContext context,
                                       CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        if (!((Object) this instanceof AbstractCaveGenerationStructure)
                || TerraforgedAlexCavesFix.context(context.randomState().sampler()) == null
                || !FixConfig.FIX_CHAMBERS.get()) return;
        var matching = ((CaveStructureAccessor) this).tfacfix$matchingBiome();
        // Keep the port's own cave candidate/piece builder. Only avoid the unrelated surface-rule hook.
        cir.setReturnValue(findGenerationPoint(context).filter(stub -> {
            var pos = stub.position();
            var biome = context.chunkGenerator().getBiomeSource().getNoiseBiome(
                    QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()),
                    context.randomState().sampler());
            return biome.is(matching) && context.validBiome().test(biome);
        }));
    }
}
