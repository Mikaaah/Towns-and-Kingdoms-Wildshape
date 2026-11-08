package nl.townsandkingdoms.wildshape.tags;

import net.minecraft.tags.TagKey;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.surface.rule.LayeredSurfaceRule;

public class WildshapeSurfaceLayerTags {
	public static final TagKey<LayeredSurfaceRule.Layer> TERRABLENDER = resolve("terrablender");
	
    private static TagKey<LayeredSurfaceRule.Layer> resolve(String path) {
    	return TagKey.create(WildshapeRegistries.SURFACE_LAYERS, WildshapeCommon.location(path));
    }
}
