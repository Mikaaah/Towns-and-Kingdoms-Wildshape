package com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.neoforge;

import java.lang.ref.WeakReference;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.registries.UTFRegistries;

// The one NeoForge biome modifier UTF ships (data/ultraterraforged/neoforge/biome_modifier/preset.json): applies the
// modifiers of the world's preset, from UTF's own registry, which is empty in worlds without a preset. NeoForge runs
// biome modifiers as the server starts, once it's the current server.
public final class PresetBiomeModifier implements BiomeModifier {
	public static final PresetBiomeModifier INSTANCE = new PresetBiomeModifier();
	public static final MapCodec<PresetBiomeModifier> CODEC = MapCodec.unit(INSTANCE);
	private static WeakReference<MinecraftServer> reported = new WeakReference<>(null);

	private PresetBiomeModifier() {
	}

	@Override
	public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (phase == Phase.ADD && server.registryAccess().registry(UTFRegistries.BIOME_MODIFIER)
                .map(modifiers -> modifiers.size() > 0).orElse(false)) {
            // Run in ADD so datapack/pack REMOVE phases retain priority.
            NativeSpawnHabitatBridge.apply(server, biome, builder);
        }
        if (phase != Phase.AFTER_EVERYTHING) return;
		server.registryAccess().registry(UTFRegistries.BIOME_MODIFIER).ifPresent((modifiers) -> {
			if (reported.get() != server) {
				reported = new WeakReference<>(server);
				UTFCommon.LOGGER.info("Applying the {} biome modifiers of the world's preset", modifiers.size());
			}
			for (com.pandaismyname1.ultraterraforged.world.worldgen.biome.modifier.BiomeModifier modifier : modifiers) {
				if (modifier instanceof NeoForgeBiomeModifier neoForgeModifier) {
					neoForgeModifier.modify(biome, builder.getGenerationSettings());
				}
			}
		});
	}

	@Override
	public MapCodec<PresetBiomeModifier> codec() {
		return CODEC;
	}
}
