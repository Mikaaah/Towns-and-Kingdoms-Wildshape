package nl.townsandkingdoms.wildshape.data.preset;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import nl.townsandkingdoms.wildshape.WildshapeCommon;

/**
 * The "Wildshape" world type (data/wildshape/worldgen/world_preset/wildshape.json). It has the same
 * dimensions as vanilla's default; Wildshape terrain comes from the preset datapack a world is created with,
 * which overrides the overworld's noise settings.
 */
public final class WildshapeWorldPresets {
	public static final ResourceKey<WorldPreset> WILDSHAPE = ResourceKey.create(Registries.WORLD_PRESET, WildshapeCommon.location("wildshape"));

	private WildshapeWorldPresets() {
	}
}
