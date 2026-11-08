package nl.townsandkingdoms.wildshape.world.worldgen.surface;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public interface WildshapeSurfaceContext {
	@Nullable
	Set<ResourceKey<Biome>> getSurroundingBiomes();
}
