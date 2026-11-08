package nl.townsandkingdoms.wildshape.world.worldgen.feature.template.decorator;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import com.mojang.serialization.Codec;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.feature.template.template.TemplateContext;

public interface TemplateDecorator<T extends TemplateContext> {
    public static final Codec<TemplateDecorator<?>> CODEC = WildshapeCodecs.dispatch(WildshapeBuiltInRegistries.TEMPLATE_DECORATOR_TYPE, TemplateDecorator::codec);
    
    void apply(LevelAccessor level, T buffer, RandomSource random, boolean modified);
    
    Codec<? extends TemplateDecorator<T>> codec();
}
