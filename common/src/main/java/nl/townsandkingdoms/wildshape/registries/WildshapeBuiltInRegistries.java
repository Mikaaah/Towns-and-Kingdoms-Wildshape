package nl.townsandkingdoms.wildshape.registries;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import nl.townsandkingdoms.wildshape.platform.RegistryUtil;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier.BiomeModifier;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.chance.ChanceModifier;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.template.decorator.TemplateDecorator;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.template.placement.TemplatePlacement;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.domain.Domain;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.function.CurveFunction;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.rule.StructureRule;

public class WildshapeBuiltInRegistries {
	public static final Registry<Codec<? extends Noise>> NOISE_TYPE = RegistryUtil.createRegistry(WildshapeRegistries.NOISE_TYPE);
	public static final Registry<Codec<? extends Domain>> DOMAIN_TYPE = RegistryUtil.createRegistry(WildshapeRegistries.DOMAIN_TYPE);
	public static final Registry<Codec<? extends CurveFunction>> CURVE_FUNCTION_TYPE = RegistryUtil.createRegistry(WildshapeRegistries.CURVE_FUNCTION_TYPE);
	public static final Registry<Codec<? extends ChanceModifier>> CHANCE_MODIFIER_TYPE = RegistryUtil.createRegistry(WildshapeRegistries.CHANCE_MODIFIER_TYPE);
	public static final Registry<Codec<? extends TemplatePlacement<?>>> TEMPLATE_PLACEMENT_TYPE = RegistryUtil.createRegistry(WildshapeRegistries.TEMPLATE_PLACEMENT_TYPE);
	public static final Registry<Codec<? extends TemplateDecorator<?>>> TEMPLATE_DECORATOR_TYPE = RegistryUtil.createRegistry(WildshapeRegistries.TEMPLATE_DECORATOR_TYPE);
	public static final Registry<Codec<? extends BiomeModifier>> BIOME_MODIFIER_TYPE = RegistryUtil.createRegistry(WildshapeRegistries.BIOME_MODIFIER_TYPE);
	public static final Registry<Codec<? extends StructureRule>> STRUCTURE_RULE_TYPE = RegistryUtil.createRegistry(WildshapeRegistries.STRUCTURE_RULE_TYPE);

	public static void bootstrap() {
	}
}
