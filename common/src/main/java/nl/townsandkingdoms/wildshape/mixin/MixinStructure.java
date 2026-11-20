package nl.townsandkingdoms.wildshape.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.rule.StructureRule;

// each structure rule has its say only over the structures it's for; it once kept every structure of every mod away
// from rivers, Alex's Caves' underground chambers too (issue #7)
@Mixin(Structure.class)
public class MixinStructure {

	@Inject(
		at = @At("RETURN"),
		method = "findValidGenerationPoint",
		cancellable = true
	)
    private void findValidGenerationPoint(GenerationContext generationContext, CallbackInfoReturnable<Optional<GenerationStub>> callback) {
		Optional<GenerationStub> generationStub = callback.getReturnValue();
		if(generationStub.isEmpty()) {
			return;
		}
		RegistryAccess registry = generationContext.registryAccess();
		Holder<Structure> structure = registry.registryOrThrow(Registries.STRUCTURE).wrapAsHolder((Structure) (Object) this);
        var id = registry.registryOrThrow(Registries.STRUCTURE).getKey((Structure)(Object)this);
        if (id != null && !nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructureTerrainValidation.validAnchor(generationContext.randomState(), id, generationStub.get().position())) {
            callback.setReturnValue(Optional.empty());
            return;
        }
		for(StructureRule structureRule : registry.registryOrThrow(WildshapeRegistries.STRUCTURE_RULE)) {
			if(structureRule.appliesTo(structure) && !structureRule.test(generationContext.randomState(), generationStub.get().position())) {
				callback.setReturnValue(Optional.empty());
				return;
			}
		}
    }
    // Inspect the completed native footprint; preserve each mod's own jigsaw offsets and terrain adaptation.
    @Inject(method = "generate", at = @At("RETURN"), cancellable = true)
    private void wildshape$validateFootprint(RegistryAccess registry, net.minecraft.world.level.chunk.ChunkGenerator generator,
            net.minecraft.world.level.biome.BiomeSource source, net.minecraft.world.level.levelgen.RandomState state,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates, long seed,
            net.minecraft.world.level.ChunkPos chunkPos, int references, net.minecraft.world.level.LevelHeightAccessor heights,
            java.util.function.Predicate<Holder<net.minecraft.world.level.biome.Biome>> biomeTest,
            CallbackInfoReturnable<net.minecraft.world.level.levelgen.structure.StructureStart> cir) {
        var start = cir.getReturnValue();
        if (!start.isValid()) return;
        var id = registry.registryOrThrow(Registries.STRUCTURE).getKey((Structure)(Object)this);
        if (id != null && (!nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructureTerrainValidation.valid(state, id, start.getBoundingBox())
                || !nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructureTerrainValidation.validCavernSupport(
                        state, id, start.getPieces(), heights.getMinBuildHeight())))
            cir.setReturnValue(net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START);
    }

}
