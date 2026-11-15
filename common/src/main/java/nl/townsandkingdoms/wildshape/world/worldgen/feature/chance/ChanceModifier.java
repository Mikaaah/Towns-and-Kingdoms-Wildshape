package nl.townsandkingdoms.wildshape.world.worldgen.feature.chance;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;

public interface ChanceModifier {
	public static final Codec<ChanceModifier> CODEC = WildshapeCodecs.dispatch(WildshapeBuiltInRegistries.CHANCE_MODIFIER_TYPE, ChanceModifier::codec);
	
	float getChance(ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx);
	
	Codec<? extends ChanceModifier> codec();
}
