package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.VegetationPatchFeature;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

class LushPoolMixinTest {
    @Test void requiredCancellablePreflightTargetsDeclaredVanillaMethodBeforeGroundWrites() throws Exception {
        assertEquals(boolean.class, VegetationPatchFeature.class.getDeclaredMethod("placeGround",
                WorldGenLevel.class, VegetationPatchConfiguration.class, Predicate.class,
                RandomSource.class, BlockPos.MutableBlockPos.class, int.class).getReturnType());
        String mixin="nl/townsandkingdoms/wildshape/mixin/MixinVegetationPatchFeature.class";
        var node=new ClassNode();
        try(var stream=getClass().getClassLoader().getResourceAsStream(mixin)) {
            assertNotNull(stream);
            new ClassReader(stream).accept(node,ClassReader.SKIP_CODE);
        }
        var method=node.methods.stream().filter(value->value.name.equals("wildshape$preflightLushPool")).findFirst().orElseThrow();
        var annotations=new ArrayList<AnnotationNode>();
        if(method.visibleAnnotations!=null) annotations.addAll(method.visibleAnnotations);
        if(method.invisibleAnnotations!=null) annotations.addAll(method.invisibleAnnotations);
        var inject=annotations.stream().filter(value->value.desc.endsWith("/Inject;")).findFirst().orElseThrow();
        assertEquals(List.of("placeGround"),value(inject,"method"));
        assertEquals(1,value(inject,"require"));
        assertEquals(true,value(inject,"cancellable"));
        Object at=value(inject,"at");
        AnnotationNode point=at instanceof List<?> list?(AnnotationNode)list.getFirst():(AnnotationNode)at;
        assertEquals("HEAD",value(point,"value"));
        try(var stream=getClass().getClassLoader().getResourceAsStream("wildshape-common.mixins.json")) {
            assertNotNull(stream);
            assertTrue(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)
                    .contains("\"MixinVegetationPatchFeature\""));
        }
    }

    private static Object value(AnnotationNode annotation,String name) {
        for(int i=0;i<annotation.values.size();i+=2) if(annotation.values.get(i).equals(name)) return annotation.values.get(i+1);
        return null;
    }
}
