package nl.townsandkingdoms.wildshape;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import nl.townsandkingdoms.wildshape.compat.streams.StreamsCompat;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBCompat;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBSurfaceRules;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;
import nl.townsandkingdoms.wildshape.server.commands.WildshapeCommands;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier.BiomeModifier;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier.BiomeModifiers;
import nl.townsandkingdoms.wildshape.world.worldgen.densityfunction.WildshapeDensityFunctions;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.WildshapeFeatures;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.chance.WildshapeChanceModifiers;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.placement.WildshapePlacementModifiers;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.template.decorator.TemplateDecorators;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.template.placement.TemplatePlacements;
import nl.townsandkingdoms.wildshape.world.worldgen.floatproviders.WildshapeFloatProviderTypes;
import nl.townsandkingdoms.wildshape.world.worldgen.heightproviders.WildshapeHeightProviderTypes;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.domain.Domains;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.function.CurveFunctions;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noises;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.rule.StructureRule;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.rule.StructureRules;
import nl.townsandkingdoms.wildshape.world.worldgen.surface.condition.WildshapeSurfaceConditions;
import nl.townsandkingdoms.wildshape.world.worldgen.surface.rule.LayeredSurfaceRule;
import nl.townsandkingdoms.wildshape.world.worldgen.surface.rule.WildshapeSurfaceRules;

public class WildshapeCommon {
	public static final String MOD_ID = "wildshape";
	public static final Logger LOGGER = LogManager.getLogger("Wildshape");

	public static void bootstrap() {
		WildshapeBuiltInRegistries.bootstrap();
		TemplatePlacements.bootstrap();
		TemplateDecorators.bootstrap();
		WildshapeChanceModifiers.bootstrap();
		WildshapePlacementModifiers.bootstrap();
		WildshapeDensityFunctions.bootstrap();
		Noises.bootstrap();
		Domains.bootstrap();
		CurveFunctions.bootstrap();
		WildshapeFeatures.bootstrap();
		WildshapeHeightProviderTypes.bootstrap();
		WildshapeFloatProviderTypes.bootstrap();
		WildshapeSurfaceRules.bootstrap();
		WildshapeSurfaceConditions.bootstrap();
		BiomeModifiers.bootstrap();
		StructureRules.bootstrap();

		WildshapeCommands.bootstrap();

		if(TBCompat.isEnabled()) {
			TBCompat.bootstrap();
		}

		if(StreamsCompat.isEnabled()) {
			StreamsCompat.bootstrap();
		}

		WildshapeRegistries.createDataRegistry(WildshapeRegistries.NOISE, Noise.DIRECT_CODEC);
		WildshapeRegistries.createDataRegistry(WildshapeRegistries.BIOME_MODIFIER, BiomeModifier.CODEC);
		WildshapeRegistries.createDataRegistry(WildshapeRegistries.STRUCTURE_RULE, StructureRule.CODEC);
		WildshapeRegistries.createDataRegistry(WildshapeRegistries.SURFACE_LAYERS, LayeredSurfaceRule.Layer.CODEC);
		WildshapeRegistries.createDataRegistry(WildshapeRegistries.PRESET, Preset.CODEC);
	}

	public static ResourceLocation location(String name) {
		if (name.contains(":")) return ResourceLocation.parse(name);
		return ResourceLocation.fromNamespaceAndPath(WildshapeCommon.MOD_ID, name);
	}
}
