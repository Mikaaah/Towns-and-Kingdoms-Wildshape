package com.pandaismyname1.ultraterraforged.world.worldgen.surface;

import java.util.HashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBCompat;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition.UTFSurfaceConditions;

/** Runtime bridge: native biome surfaces first, TNK terrain surfaces as fallback. */
public final class ModSurfaceRules {
    private ModSurfaceRules() {}

    @SuppressWarnings("unchecked")
    public static SurfaceRules.RuleSource wrap(SurfaceRules.RuleSource base, RegistryAccess registries) {
        if (TBCompat.isEnabled()) base = terraBlender(base);
        if (!ModLoaderUtil.isLoaded("natures_spirit")) return base;
        ResourceKey<Biome>[] biomes = registries.registryOrThrow(Registries.BIOME).registryKeySet().stream()
                .filter(key -> key.location().getNamespace().equals("natures_spirit")).toArray(ResourceKey[]::new);
        if (biomes.length == 0) return base;
        var nativeRules = natureSpirit();
        // Cypress uses the vanilla grass/dirt default; its native rule set only
        // describes exceptional surfaces. Actual TNK ground can be below the
        // vanilla preliminary surface estimate, so provide its soil explicitly.
        var cypress = SurfaceRules.ifTrue(SurfaceRules.isBiome(ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("natures_spirit","cypress_fields"))),
                SurfaceRules.ifTrue(UTFSurfaceConditions.nearGround(5), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0,0), SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())),
                        SurfaceRules.state(Blocks.DIRT.defaultBlockState()))),
                    SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.DIRT.defaultBlockState())))));
        return nativeRules == null ? SurfaceRules.sequence(cypress,base)
                : SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes),nativeRules),cypress,base);
    }

    private static SurfaceRules.RuleSource natureSpirit() {
        try {
            // No dependency on Nature's Spirit classes when the mod is absent.
            // Its own rules retain chalk, sandy soil, kaolin and other native materials.
            return (SurfaceRules.RuleSource)Class.forName("net.hibiscus.naturespirit.world.NSSurfaceRules")
                    .getMethod("makeRules").invoke(null);
        } catch (ReflectiveOperationException | LinkageError error) {
            UTFCommon.LOGGER.warn("Could not load Nature's Spirit surface rules; retaining terrain fallback", error);
            return null;
        }
    }

    private static SurfaceRules.RuleSource terraBlender(SurfaceRules.RuleSource base) {
        var all = terrablender.api.SurfaceRuleManager.getNamespacedRules(
                terrablender.api.SurfaceRuleManager.RuleCategory.OVERWORLD,base);
        if (all instanceof terrablender.worldgen.surface.NamespacedSurfaceRuleSource namespaced) {
            var sources = new HashMap<>(namespaced.sources());
            // Minecraft's default rules must not replace the authored TNK surfaces.
            sources.remove("minecraft");
            return new terrablender.worldgen.surface.NamespacedSurfaceRuleSource(base,sources);
        }
        return base;
    }
}
