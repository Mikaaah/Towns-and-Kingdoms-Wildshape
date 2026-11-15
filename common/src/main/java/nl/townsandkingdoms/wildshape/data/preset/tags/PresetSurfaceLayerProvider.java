package nl.townsandkingdoms.wildshape.data.preset.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import nl.townsandkingdoms.wildshape.data.preset.PresetSurfaceLayerData;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;
import nl.townsandkingdoms.wildshape.tags.WildshapeSurfaceLayerTags;
import nl.townsandkingdoms.wildshape.world.worldgen.surface.rule.LayeredSurfaceRule;

public class PresetSurfaceLayerProvider extends TagsProvider<LayeredSurfaceRule.Layer> {
	private Preset preset;
	
	public PresetSurfaceLayerProvider(Preset preset, PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, WildshapeRegistries.SURFACE_LAYERS, completableFuture);
		
		this.preset = preset;
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {

	}
}