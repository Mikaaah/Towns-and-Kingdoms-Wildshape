package nl.townsandkingdoms.wildshape.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;

//TODO support different presets per dimension
public class PresetData {
	public static final ResourceKey<Preset> PRESET = WildshapeRegistries.createKey(WildshapeRegistries.PRESET, "preset");
	
	public static void bootstrap(Preset preset, BootstrapContext<Preset> ctx) {
		ctx.register(PRESET, preset);
	}
}
