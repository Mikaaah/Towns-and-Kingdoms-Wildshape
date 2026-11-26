package nl.townsandkingdoms.wildshape.test;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import nl.townsandkingdoms.wildshape.mixin.plugin.MixinPlugin;
import nl.townsandkingdoms.wildshape.platform.ModLoaderUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Optional engine targets must be gated before Mixin resolves their classes. */
class OptionalEngineMixinsTest {
    private record Family(List<String> mixins, String mod) {}

    private static final List<Family> FAMILIES = List.of(
            new Family(MixinPlugin.TB_MIXINS, "terrablender"),
            new Family(MixinPlugin.WP_MIXINS, "world_preview"),
            new Family(MixinPlugin.C2ME_OPENCL_MIXINS, "c2me_opts_accel_opencl"),
            new Family(MixinPlugin.BYE_PREGEN_MIXINS, "byepregen"),
            new Family(MixinPlugin.STREAMS_MIXINS, "streamsreflowing"),
            new Family(MixinPlugin.AC_MIXINS, "alexscaves"),
            new Family(MixinPlugin.GA_MIXINS, "generator_accelerator"),
            new Family(MixinPlugin.GA2_MIXINS, "generatoraccelerator2"),
            new Family(MixinPlugin.SKYLANDS_MIXINS, "skylands"));

    @Test void absentEnginesDisableAllTheirMixins() {
        var plugin = new MixinPlugin();
        try (var platform = mockStatic(ModLoaderUtil.class)) {
            for (Family family : FAMILIES)
                for (String mixin : family.mixins())
                    assertFalse(plugin.shouldApplyMixin("unused", mixin), mixin);
            assertTrue(plugin.shouldApplyMixin("unused",
                    "nl.townsandkingdoms.wildshape.mixin.MixinChunkGenerator"));
        }
    }

    @Test void installingOneEngineEnablesOnlyItsFamily() {
        var plugin = new MixinPlugin();
        for (Family installed : FAMILIES) {
            try (var platform = mockStatic(ModLoaderUtil.class)) {
                platform.when(() -> ModLoaderUtil.isLoaded(installed.mod())).thenReturn(true);
                for (Family family : FAMILIES)
                    for (String mixin : family.mixins())
                        assertEquals(family == installed,
                                plugin.shouldApplyMixin("unused", mixin), installed.mod() + ": " + mixin);
            }
        }
    }

    @Test void optionalMixinsAreRegisteredExactlyOnce() throws Exception {
        String prefix = "nl.townsandkingdoms.wildshape.mixin.";
        List<String> registered = new ArrayList<>();
        try (var stream = getClass().getResourceAsStream("/wildshape-common.mixins.json")) {
            assertNotNull(stream);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            json.getAsJsonArray("mixins").forEach(value -> registered.add(prefix + value.getAsString()));
        }
        new MixinPlugin().getMixins().forEach(name -> registered.add(prefix + name));
        assertEquals(registered.size(), new HashSet<>(registered).size(), "duplicate Mixin registrations");
        for (String retired : List.of("MixinStructurePlacement", "AccessorChunkGeneratorStructureState",
                "structure.MixinIntegratedStructurePlacement", "structure.MixinCataclysmStructurePlacement"))
            assertFalse(registered.contains(prefix + retired), "native placements must not be intercepted: " + retired);
        assertTrue(registered.contains(prefix + "MixinStructure"), "independent terrain suitability checks remain");
        for (Family family : FAMILIES)
            for (String mixin : family.mixins())
                assertTrue(registered.contains(mixin), "unregistered optional mixin: " + mixin);
    }

    @Test void previewBridgeTargetsOriginalAndRelocatedNeoForgePackages() throws Exception {
        // Read CLASS-retained annotations without loading either optional SampleUtils class.
        try (var stream = getClass().getResourceAsStream(
                "/nl/townsandkingdoms/wildshape/mixin/worldpreview/SampleUtilsMixin.class")) {
            assertNotNull(stream);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE);
            var annotation = node.invisibleAnnotations.stream()
                    .filter(value -> value.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                    .findFirst().orElseThrow();
            Object targets = annotation.values.get(annotation.values.indexOf("targets") + 1);
            assertEquals(List.of(
                    "caeruleusTait.world.preview.backend.worker.SampleUtils",
                    "com.caeruleusTait.world.preview.backend.worker.SampleUtils"), targets);
        }
    }

    @Test void acceleratedCarversCannotSkipCaveDecorationViaHeadCancellation() throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/nl/townsandkingdoms/wildshape/mixin/MixinNoiseBasedChunkGenerator.class")) {
            assertNotNull(stream);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            var decorate = node.methods.stream().filter(method -> method.name.equals("wildshape$decorateCaverns"))
                    .findFirst().orElseThrow();
            assertTrue(java.util.stream.Stream.of(decorate.visibleAnnotations, decorate.invisibleAnnotations)
                    .filter(java.util.Objects::nonNull).flatMap(List::stream).anyMatch(annotation ->
                    annotation.desc.equals("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;")));
            int original = -1, decoration = -1, index = 0;
            for (var instruction : decorate.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.equals("com/llamalad7/mixinextras/injector/wrapoperation/Operation")
                            && call.name.equals("call")) original = index;
                    if (call.owner.endsWith("/CaveFeatures") && call.name.equals("decorate")) decoration = index;
                }
                index++;
            }
            assertTrue(original >= 0 && decoration > original,
                    "the complete carving call must return before cavern decoration");
        }
    }
}
