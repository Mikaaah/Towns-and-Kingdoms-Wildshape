package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;

class CaveBiomeProtectionTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @SuppressWarnings("unchecked")
    private static Holder<Biome> biome(String id) {
        Holder<Biome> holder = mock(Holder.class);
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(id));
        when(holder.unwrapKey()).thenReturn(Optional.of(key));
        when(holder.is(key)).thenReturn(true);
        return holder;
    }

    @Test void nativeCavesAndDeepDarkRetainOwnership() {
        assertTrue(CaveBiomeProtection.protects(biome("alexscaves:magnetic_caves")));
        assertTrue(CaveBiomeProtection.protects(biome("yungscavebiomes:frosted_caves")));
        assertTrue(CaveBiomeProtection.protects(biome("minecraft:deep_dark")));
        var ru = biome("regions_unexplored:prismachasm");
        when(ru.is(TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_cave")))).thenReturn(true);
        assertTrue(CaveBiomeProtection.protects(ru));
        var bop = biome("biomesoplenty:spider_nest");
        when(bop.is(TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_underground")))).thenReturn(true);
        assertTrue(CaveBiomeProtection.protects(bop));
    }

    @Test void stylesKeepOrdinaryVanillaCavesAndRespectPackExtensions() {
        assertFalse(CaveBiomeProtection.protects(biome("minecraft:plains")));
        assertFalse(CaveBiomeProtection.protects(biome("minecraft:lush_caves")));
        assertFalse(CaveBiomeProtection.protects(biome("minecraft:dripstone_caves")));
        assertFalse(CaveBiomeProtection.protects(null));
        var custom = biome("example:custom_cavern");
        when(custom.is(CaveBiomeProtection.PROTECTED)).thenReturn(true);
        assertTrue(CaveBiomeProtection.protects(custom));
    }
}
