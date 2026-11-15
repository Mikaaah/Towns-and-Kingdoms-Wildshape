package nl.townsandkingdoms.wildshape.world.worldgen.floatproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviderType;
import nl.townsandkingdoms.wildshape.platform.RegistryUtil;

public class WildshapeFloatProviderTypes {
	public static final FloatProviderType<LegacyCanyonYScale> LEGACY_CANYON_Y_SCALE = register("legacy_canyon_y_scale", LegacyCanyonYScale.CODEC);
	
	public static void bootstrap() {
	}
	
	private static <T extends FloatProvider> FloatProviderType<T> register(String name, Codec<T> codec) {
		MapCodec<T> mapCodec = WildshapeCodecs.asMap(codec);
		FloatProviderType<T> type = () -> mapCodec;
		RegistryUtil.register(BuiltInRegistries.FLOAT_PROVIDER_TYPE, name, type);
		return type;
	}
}
