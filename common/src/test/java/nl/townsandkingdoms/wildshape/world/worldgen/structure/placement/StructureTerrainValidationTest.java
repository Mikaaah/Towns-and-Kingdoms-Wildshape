package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

class StructureTerrainValidationTest {
    private final StructurePlacementProfiles.Profile surface = new StructurePlacementProfiles.Profile("surface",24,true);
    @Test void checksCompletedFootprintAndRejectsCliff() {
        BoundingBox box = new BoundingBox(-100,60,-100,100,100,100);
        assertFalse(StructureTerrainValidation.valid(box,surface,(x,z) -> new StructureTerrainValidation.Sample(x>0 ? 100 : 64,63)));
        assertTrue(StructureTerrainValidation.valid(box,surface,(x,z) -> new StructureTerrainValidation.Sample(70,63)));
    }
    @Test void underwaterCornerRejectsLandProfileWithoutFlattening() {
        BoundingBox box = new BoundingBox(0,50,0,30,80,30);
        assertFalse(StructureTerrainValidation.valid(box,surface,(x,z) -> new StructureTerrainValidation.Sample(x==30 && z==30 ? 50 : 70,63)));
    }
    @Test void giantBoundingBoxStillOnlySamplesNinePointsAndNativeLayersArePreserved() {
        AtomicInteger calls = new AtomicInteger();
        BoundingBox box = new BoundingBox(-1000000,-64,-1000000,1000000,300,1000000);
        assertTrue(StructureTerrainValidation.valid(box,surface,(x,z) -> { calls.incrementAndGet(); return new StructureTerrainValidation.Sample(70,63); }));
        assertEquals(9,calls.get());
        var cave = new StructurePlacementProfiles.Profile("underground",0,true);
        assertTrue(StructureTerrainValidation.valid(box,cave,(x,z) -> { fail("Cave profile must retain native terrain handling"); return null; }));
    }
    @Test void optionalNativeAnchorRejectsFloatingAndBuriedStarts() {
        var grounded = new StructurePlacementProfiles.Profile("surface",24,true,12);
        assertTrue(StructureTerrainValidation.validAnchor(grounded,70,64));
        assertFalse(StructureTerrainValidation.validAnchor(grounded,100,64));
        assertFalse(StructureTerrainValidation.validAnchor(grounded,32,64));
        assertTrue(StructureTerrainValidation.validAnchor(surface,100,64), "Disabled by default for unknown native anchor conventions");
    }

    @Test void giantBuriedStartsHaveBoundedSupportSampling() {
        var buried = new StructurePlacementProfiles.Profile("underground", 0, false, 0, true);
        var bounds = java.util.Collections.nCopies(10000, new BoundingBox(0, -30, 0, 15, -10, 15));
        AtomicInteger probes = new AtomicInteger();
        assertTrue(StructureTerrainValidation.validCavernSupport(buried, bounds,
                (x, y, z) -> { probes.incrementAndGet(); return false; }));
        assertEquals(64 * 9, probes.get());
    }

}
