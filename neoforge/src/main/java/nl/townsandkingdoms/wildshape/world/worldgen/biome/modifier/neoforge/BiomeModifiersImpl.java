package nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier.neoforge;

import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.townsandkingdoms.wildshape.platform.RegistryUtil;
import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier.BiomeModifier;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier.Filter;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier.Order;

public class BiomeModifiersImpl {
	
	public static void bootstrap() {
		register("add", AddModifier.CODEC);
		register("replace", ReplaceModifier.CODEC);
		RegistryUtil.register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "preset", PresetBiomeModifier.CODEC);
	}

	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Optional<Pair<Filter.Behavior, HolderSet<Biome>>> biomes, HolderSet<PlacedFeature> features) {
		return new AddModifier(order, step, biomes.map((p) -> new Filter(p.getSecond(), p.getFirst())), features);
	}

	public static BiomeModifier replace(GenerationStep.Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return new ReplaceModifier(step, biomes, replacements);
	}
	
	public static void register(String name, Codec<? extends BiomeModifier> value) {
		RegistryUtil.register(WildshapeBuiltInRegistries.BIOME_MODIFIER_TYPE, name, value);
	}
}
