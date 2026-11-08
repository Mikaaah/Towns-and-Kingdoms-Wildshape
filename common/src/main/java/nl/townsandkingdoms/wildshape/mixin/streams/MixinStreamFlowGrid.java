package nl.townsandkingdoms.wildshape.mixin.streams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.server.level.ServerLevel;
import nl.townsandkingdoms.wildshape.compat.streams.StreamsCompat;

/*
 * Streams Reflowing gives rivers it finds by their biome a current towards the sea, at sea level, ahead of the layers
 * of other mods. In a world Wildshape makes it leaves rivers to Wildshape's own currents (RiverCurrents),
 * which know which way each river runs; Streams' own streams keep theirs.
 */
@Mixin(value = StreamFlowGrid.class, remap = false)
class MixinStreamFlowGrid {

	@ModifyVariable(
		method = { "getOrBuild", "buildProvisional",
                "proprietary1(Lnet/minecraft/server/level/ServerLevel;Ldev/streamsreflowing/core/RiverEngine;Ldev/streamsreflowing/core/river/SourceGivenToAuthorizedOnly_001$ProprietaryNiceJohn_001;Ldev/streamsreflowing/core/river/SourceGivenToAuthorizedOnly_001$SourceGivenToAuthorizedOnly_001;ZII)[B",
                "seeAiNotice1(Lnet/minecraft/server/level/ServerLevel;Ldev/streamsreflowing/core/RiverEngine;Ldev/streamsreflowing/core/river/SourceGivenToAuthorizedOnly_001$ProprietaryNiceJohn_001;Ldev/streamsreflowing/core/river/SourceGivenToAuthorizedOnly_001$SourceGivenToAuthorizedOnly_001;ZII)[B" },
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0,
		require = 0
	)
	private static boolean flowRivers(boolean flowRivers, @Local(argsOnly = true) ServerLevel level) {
		return flowRivers && StreamsCompat.context(level) == null;
	}
}
