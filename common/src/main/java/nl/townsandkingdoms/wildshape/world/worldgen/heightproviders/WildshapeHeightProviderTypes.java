package nl.townsandkingdoms.wildshape.world.worldgen.heightproviders;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import nl.townsandkingdoms.wildshape.platform.RegistryUtil;

public class WildshapeHeightProviderTypes {
	public static final HeightProviderType<LegacyCarverHeight> LEGACY_CARVER = register("legacy_carver", LegacyCarverHeight.CODEC);
	
	public static void bootstrap() {
	}
	
	private static <T extends HeightProvider> HeightProviderType<T> register(String name, Codec<T> codec) {
		MapCodec<T> mapCodec = WildshapeCodecs.asMap(codec);
		HeightProviderType<T> type = () -> mapCodec;
		RegistryUtil.register(BuiltInRegistries.HEIGHT_PROVIDER_TYPE, name, type);
		return type;
	}
}
