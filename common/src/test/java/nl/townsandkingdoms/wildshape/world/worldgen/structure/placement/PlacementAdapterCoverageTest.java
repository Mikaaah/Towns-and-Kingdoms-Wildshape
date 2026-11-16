package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import nl.townsandkingdoms.wildshape.test.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlacementAdapterCoverageTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }
    static class InheritedPlacement extends RandomSpreadStructurePlacement {
        InheritedPlacement() { super(32,8,RandomSpreadType.LINEAR,123); }
    }
    static class UnknownOverride extends InheritedPlacement {
        @Override public boolean isStructureChunk(ChunkGeneratorStructureState state,int x,int z) { return true; }
    }
    static class UnknownGeometry extends InheritedPlacement {
        @Override protected boolean isPlacementChunk(ChunkGeneratorStructureState state,int x,int z) { return true; }
    }
    @Test void nativeExclusionsAreScopedAndRestoredEvenAfterExceptions() {
        assertFalse(StructurePlacementCoordinator.nativeScopeActive());
        assertFalse(StructurePlacementCoordinator.filterNative(null,null,0,0,(args) -> {
            assertTrue(StructurePlacementCoordinator.nativeScopeActive());
            assertTrue(StructurePlacementCoordinator.filterNative(null,null,0,0,(nestedArgs) -> true));
            return false;
        }));
        assertFalse(StructurePlacementCoordinator.nativeScopeActive());
        assertThrows(IllegalStateException.class, () -> StructurePlacementCoordinator.filterNative(null,null,0,0,(args) -> { throw new IllegalStateException("native exclusion failed"); }));
        assertFalse(StructurePlacementCoordinator.nativeScopeActive());
    }
    @Test void indexedCandidatesMustHaveASymmetricGenerationHook() {
        assertTrue(StructurePlacementCoordinator.supportsPlacement(new RandomSpreadStructurePlacement(32,8,RandomSpreadType.LINEAR,123)));
        assertTrue(StructurePlacementCoordinator.supportsPlacement(new InheritedPlacement()));
        assertFalse(StructurePlacementCoordinator.supportsPlacement(new UnknownGeometry()), "Unknown non-grid candidate algorithms remain native");
        assertFalse(StructurePlacementCoordinator.supportsPlacement(new UnknownOverride()),"An unhooked override must not become an invisible blocker");
    }
}
