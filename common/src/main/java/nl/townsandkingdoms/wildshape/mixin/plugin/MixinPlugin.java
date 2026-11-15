package nl.townsandkingdoms.wildshape.mixin.plugin;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.google.common.collect.ImmutableList;

import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBCompat;
import nl.townsandkingdoms.wildshape.compat.worldpreview.WPCompat;
import nl.townsandkingdoms.wildshape.compat.c2me.C2MECompat;
import nl.townsandkingdoms.wildshape.compat.byepregen.ByePregenCompat;
import nl.townsandkingdoms.wildshape.compat.streams.StreamsCompat;
import nl.townsandkingdoms.wildshape.compat.alexscaves.AlexsCavesCompat;
import nl.townsandkingdoms.wildshape.compat.generatoraccelerator.GeneratorAcceleratorCompat;

public class MixinPlugin implements IMixinConfigPlugin {
	private static final String MIXIN_PACKAGE_PREFIX = "nl.townsandkingdoms.wildshape.mixin.";
	public static final List<String> TB_MIXINS = ImmutableList.of(mixinClassName("terrablender.MixinClimateSampler"), mixinClassName("terrablender.MixinNoiseChunk"), mixinClassName("terrablender.MixinParameterList"), mixinClassName("terrablender.MixinTargetPoint"));
	public static final List<String> WP_MIXINS = ImmutableList.of(mixinClassName("worldpreview.SampleUtilsMixin"));
	public static final List<String> C2ME_OPENCL_MIXINS = ImmutableList.of(mixinClassName("c2me.MixinChunkMapOpenCL"));
	public static final List<String> BYE_PREGEN_MIXINS = ImmutableList.of(mixinClassName("byepregen.AccessorNoiseChunk"), mixinClassName("byepregen.MixinArenaNoiseFiller"));
	public static final List<String> STREAMS_MIXINS = ImmutableList.of(mixinClassName("streams.MixinStreamFlowGrid"), mixinClassName("streams.MixinRiverCarverFeature"), mixinClassName("streams.MixinWorldGenRegion"), mixinClassName("streams.MixinStreamsTerrainSampler"));
	public static final List<String> AC_MIXINS = ImmutableList.of(mixinClassName("alexscaves.MixinBiomeGenerationNoiseCondition"), mixinClassName("alexscaves.MixinClimateSampler"));
	public static final List<String> GA_MIXINS = ImmutableList.of(mixinClassName("generatoraccelerator.MixinNoiseBasedChunkGenerator"));

	@Override
	public void onLoad(String mixinPackage) {
		log(TBCompat.isEnabled(), "TerraBlender");
		log(WPCompat.isEnabled(), "World Preview");
		log(C2MECompat.isOpenCLLoaded(), "C2ME OpenCL");
		log(ByePregenCompat.isEnabled(), "Bye Pregen");
		log(StreamsCompat.isEnabled(), "Streams Reflowing");
		log(AlexsCavesCompat.isEnabled(), "Alex's Caves");
		log(GeneratorAcceleratorCompat.isEnabled(), "Generator Accelerator");
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return TB_MIXINS.contains(mixinClassName) ? TBCompat.isEnabled() :
			   WP_MIXINS.contains(mixinClassName) ? WPCompat.isEnabled() :
			   C2ME_OPENCL_MIXINS.contains(mixinClassName) ? C2MECompat.isOpenCLLoaded() :
			   BYE_PREGEN_MIXINS.contains(mixinClassName) ? ByePregenCompat.isEnabled() :
			   STREAMS_MIXINS.contains(mixinClassName) ? StreamsCompat.isEnabled() :
			   AC_MIXINS.contains(mixinClassName) ? AlexsCavesCompat.isEnabled() :
			   GA_MIXINS.contains(mixinClassName) ? GeneratorAcceleratorCompat.isEnabled() :
			   true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return Stream.of(TB_MIXINS, WP_MIXINS, C2ME_OPENCL_MIXINS, BYE_PREGEN_MIXINS, STREAMS_MIXINS, AC_MIXINS, GA_MIXINS).flatMap(List::stream).map((str) -> {
			return str.replace(MIXIN_PACKAGE_PREFIX, "");
		}).toList();
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	private static String mixinClassName(String className) {
		return MIXIN_PACKAGE_PREFIX + className;
	}

	private static void log(boolean isModLoaded, String modName) {
		if(isModLoaded) {
			WildshapeCommon.LOGGER.info("Enabling {} compat", modName);
		} else {
			WildshapeCommon.LOGGER.info("Disabling {} compat", modName);
		}
	}
}
