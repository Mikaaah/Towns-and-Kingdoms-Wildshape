package nl.townsandkingdoms.wildshape.world.worldgen.structure.rule;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.RandomState;
import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;

public interface StructureRule {
    public static final Codec<StructureRule> CODEC = WildshapeCodecs.dispatch(WildshapeBuiltInRegistries.STRUCTURE_RULE_TYPE, StructureRule::codec);

	// whether it has a say over where this structure goes
	boolean appliesTo(Holder<Structure> structure);

	boolean test(RandomState randomState, BlockPos pos);

	Codec<? extends StructureRule> codec();
}
