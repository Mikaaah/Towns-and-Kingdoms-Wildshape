package nl.townsandkingdoms.wildshape.world.worldgen.feature.template.placement;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.template.template.Dimensions;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.template.template.TemplateContext;

public interface TemplatePlacement<T extends TemplateContext> {
    public static final Codec<TemplatePlacement<?>> CODEC = WildshapeCodecs.dispatch(WildshapeBuiltInRegistries.TEMPLATE_PLACEMENT_TYPE, TemplatePlacement::codec);
    
    boolean canPlaceAt(LevelAccessor world, BlockPos pos, Dimensions dimensions);

    boolean canReplaceAt(LevelAccessor world, BlockPos pos);
    
    T createContext();
    
    Codec<? extends TemplatePlacement<T>> codec();
}
