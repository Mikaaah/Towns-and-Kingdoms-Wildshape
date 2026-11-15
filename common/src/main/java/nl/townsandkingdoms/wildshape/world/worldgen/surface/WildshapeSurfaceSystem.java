package nl.townsandkingdoms.wildshape.world.worldgen.surface;

import java.util.List;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import nl.townsandkingdoms.wildshape.world.worldgen.surface.rule.StrataStack;

public interface WildshapeSurfaceSystem {
	List<StrataStack> getOrCreateStrata(ResourceLocation name, Function<RandomSource, List<StrataStack>> factory);
}
