package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import nl.townsandkingdoms.wildshape.data.preset.StructureSetJsonPatcher;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** A real tag-owning registry and custom codec reproduce the client export's cross-registry HolderSet failure. */
class TaggedStructurePresetExportTest {
    private static final ResourceKey<StructureSet> VILLAGES = ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.withDefaultNamespace("villages"));
    private static final ResourceKey<StructureSet> PILLAGER_OUTPOSTS = ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.withDefaultNamespace("pillager_outposts"));
    private static final ResourceLocation CUSTOM_ID = ResourceLocation.parse("integrated_villages:regular_villages");
    private static final TagKey<StructureSet> AVOID = TagKey.create(Registries.STRUCTURE_SET,
            ResourceLocation.parse("integrated_villages:village_avoid"));
    private static StructurePlacementType<TaggedSpread> placementType;

    @BeforeAll static void bootstrap() throws Exception {
        TestBootstrap.init();
        Field frozen = MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        var registry = BuiltInRegistries.STRUCTURE_PLACEMENT;
        frozen.setBoolean(registry, false);
        try {
            placementType = Registry.register(registry, ResourceLocation.parse("wildshape_test:tagged_spread"),
                    (StructurePlacementType<TaggedSpread>) () -> TaggedSpread.CODEC);
        } finally {
            registry.freeze();
        }
    }

    @Test void taggedCustomPlacementDoesNotBreakClientPresetSerialization() {
        var source = source(CUSTOM_ID);
        assertNativeFields(nativeJson(source, CUSTOM_ID));
        var preset = preset();
        var pack = PresetDatapack.generate(preset, source);
        assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
        assertFalse(pack.files().containsKey(path(CUSTOM_ID)), "The custom placement stays in its native resource");
        var nativeJson = nativeJson(source, CUSTOM_ID);
        assertTrue(StructureSetJsonPatcher.patch(preset, CUSTOM_ID, nativeJson));
        assertEquals(40, nativeJson.getAsJsonObject("placement").get("spacing").getAsInt());
        assertNativeFields(nativeJson);
    }

    @Test void explicitCustomSettingsSurviveExportAndLateBinding() {
        var source = source(CUSTOM_ID);
        assertNativeFields(nativeJson(source, CUSTOM_ID));
        var preset = preset();
        var entry = preset.structures().getOrCreate(CUSTOM_ID);
        entry.spacing = 55;
        entry.frequency = 0.5F;
        entry.salt = 123;
        var pack = PresetDatapack.generate(preset, source);
        assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
        var stored = Preset.CODEC.parse(JsonOps.INSTANCE,
                pack.files().get("data/wildshape/wildshape/worldgen/preset/preset.json")).getOrThrow();
        var nativeJson = nativeJson(source, CUSTOM_ID);
        assertTrue(StructureSetJsonPatcher.patch(stored, CUSTOM_ID, nativeJson));
        var placement = nativeJson.getAsJsonObject("placement");
        assertEquals(55, placement.get("spacing").getAsInt());
        assertEquals(0.5F, placement.get("frequency").getAsFloat());
        assertEquals(123, placement.get("salt").getAsInt());
        assertNativeFields(nativeJson);
        assertFalse(StructureSetJsonPatcher.patch(stored, CUSTOM_ID, nativeJson));
    }

    @Test void customPlacementOnAVanillaRegistryKeyCanStillBeDisabled() {
        var id = ResourceLocation.withDefaultNamespace("villages");
        var source = source(id);
        assertNativeFields(nativeJson(source, id));
        var preset = preset();
        preset.structures().getOrCreate(id).enabled = false;
        var pack = PresetDatapack.generate(preset, source);
        assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
        assertFalse(pack.files().containsKey(path(id)));
        var nativeJson = nativeJson(source, id);
        assertTrue(StructureSetJsonPatcher.patch(preset, id, nativeJson));
        assertEquals(0, nativeJson.getAsJsonArray("structures").size());
        assertNativeFields(nativeJson);
    }

    private static void assertNativeFields(JsonObject json) {
        var placement = json.getAsJsonObject("placement");
        assertEquals("wildshape_test:tagged_spread", placement.get("type").getAsString());
        assertEquals("#integrated_villages:village_avoid",
                placement.getAsJsonObject("super_exclusion_zone").get("other_set").getAsString());
        assertEquals(6, placement.getAsJsonObject("super_exclusion_zone").get("chunk_count").getAsInt());
    }

    private static HolderLookup.Provider source(ResourceLocation customId) {
        var vanilla = VanillaRegistries.createLookup();
        var registry = new MappedRegistry<StructureSet>(Registries.STRUCTURE_SET, Lifecycle.stable());
        var villages = vanilla.lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(VILLAGES).value();
        var avoid = registry.getOrCreateTag(AVOID);
        if (!customId.equals(VILLAGES.location()))
            registry.register(VILLAGES, villages, RegistrationInfo.BUILT_IN);
        registry.register(PILLAGER_OUTPOSTS, new StructureSet(villages.structures(),
                new RandomSpreadStructurePlacement(32, 8, RandomSpreadType.LINEAR, 1234)), RegistrationInfo.BUILT_IN);
        registry.register(ResourceKey.create(Registries.STRUCTURE_SET, customId),
                new StructureSet(villages.structures(), new TaggedSpread(26, 19, 1982700828,
                        new Exclusion(avoid, 6))), RegistrationInfo.BUILT_IN);
        registry.freeze();
        registry.bindTags(Map.of(AVOID, List.of(registry.getHolderOrThrow(VILLAGES),
                registry.getHolderOrThrow(PILLAGER_OUTPOSTS))));
        return new HolderLookup.Provider() {
            @Override public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return vanilla.listRegistries();
            }

            @SuppressWarnings("unchecked")
            @Override public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                if (key.equals(Registries.STRUCTURE_SET))
                    return Optional.of((HolderLookup.RegistryLookup<T>) registry.asLookup());
                return vanilla.lookup(key);
            }

            // Vanilla's bootstrap uses a shared serialization owner. Preserve that context for unchanged registries.
            @Override public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> ops) {
                var original = vanilla.createSerializationContext(ops);
                return RegistryOps.create(ops, new RegistryOps.RegistryInfoLookup() {
                    @SuppressWarnings("unchecked")
                    @Override public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                        if (key.equals(Registries.STRUCTURE_SET))
                            return Optional.of((RegistryOps.RegistryInfo<T>) RegistryOps.RegistryInfo.fromRegistryLookup(registry.asLookup()));
                        return original.owner(key).flatMap(owner -> original.getter(key).map(getter ->
                                new RegistryOps.RegistryInfo<>(owner, getter, vanilla.lookupOrThrow(key).registryLifecycle())));
                    }
                });
            }
        };
    }

    private static JsonObject nativeJson(HolderLookup.Provider source, ResourceLocation id) {
        var set = source.lookupOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, id)).value();
        return StructureSet.DIRECT_CODEC.encodeStart(source.createSerializationContext(JsonOps.INSTANCE), set)
                .getOrThrow().getAsJsonObject();
    }

    private static Preset preset() { return BuiltinPresetRenderTest.presets().get("default").get(); }
    private static String path(ResourceLocation id) {
        return "data/" + id.getNamespace() + "/worldgen/structure_set/" + id.getPath() + ".json";
    }

    private record Exclusion(HolderSet<StructureSet> otherSet, int chunkCount) {
        private static final Codec<Exclusion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET).fieldOf("other_set").forGetter(Exclusion::otherSet),
                Codec.INT.fieldOf("chunk_count").forGetter(Exclusion::chunkCount)).apply(instance, Exclusion::new));
    }

    private static final class TaggedSpread extends RandomSpreadStructurePlacement {
        private static final MapCodec<TaggedSpread> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("spacing").forGetter(TaggedSpread::spacing),
                Codec.INT.fieldOf("separation").forGetter(TaggedSpread::separation),
                Codec.INT.fieldOf("salt").forGetter(value -> value.salt),
                Exclusion.CODEC.fieldOf("super_exclusion_zone").forGetter(value -> value.avoid)
        ).apply(instance, TaggedSpread::new));
        private final Exclusion avoid;

        TaggedSpread(int spacing, int separation, int salt, Exclusion avoid) {
            super(spacing, separation, RandomSpreadType.LINEAR, salt);
            this.avoid = avoid;
        }

        @Override public StructurePlacementType<?> type() { return placementType; }
    }
}
