package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;
import com.pandaismyname1.ultraterraforged.world.worldgen.structure.rule.StructureRule;

@Mixin(Structure.class)
public class MixinStructure {

	@Inject(
		at = @At("HEAD"), 
		method = "isValidBiome",
		cancellable = true
	)
    private static void isValidBiome(GenerationStub generationStub, GenerationContext generationContext, CallbackInfoReturnable<Boolean> callback) {
		// Original rules were intended for villages, but this static Minecraft
        // hook has no current Structure identity. Applying a village-only
        // terrain rule here rejects every modded dungeon, arena or stronghold.
        // Disable the global veto in TNK3 until structure-target-aware rules
        // are implemented. Retain an explicit compatibility/debug switch.
        if (!Boolean.getBoolean("tnkterraforged.legacyGlobalStructureRules")) return;
        if (!((Object) generationContext.randomState() instanceof
              com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState)) return;
		RegistryAccess registry = generationContext.registryAccess();
		RegistryLookup<StructureRule> structureRules = registry.lookupOrThrow(UTFRegistries.STRUCTURE_RULE);
		
		for(StructureRule structureRule : structureRules.listElements().map(Holder::value).toList()) {
			if(!structureRule.test(generationContext.randomState(), generationStub.position())) {
				callback.setReturnValue(false);
			}
		}
    }
}
