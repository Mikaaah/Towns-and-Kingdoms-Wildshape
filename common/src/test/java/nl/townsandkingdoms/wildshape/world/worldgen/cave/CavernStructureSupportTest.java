package nl.townsandkingdoms.wildshape.world.worldgen.cave;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import nl.townsandkingdoms.wildshape.test.TestBootstrap;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Levels;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructurePlacementProfiles;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructureTerrainValidation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CavernStructureSupportTest {
    private static final GiantCaverns.Cavern CHAMBER = new GiantCaverns.Cavern(8, -10, 8, 140, 40, 135, 30, 100);
    private static final StructurePlacementProfiles.Profile BURIED =
            new StructurePlacementProfiles.Profile(256, "underground", 0, false, "", 0, true);
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    private static final class Slice {
        final ChunkAccess chunk = mock(ChunkAccess.class);
        final GeneratorContext context = mock(GeneratorContext.class);
        final Map<BlockPos, BlockState> changed = new HashMap<>();
        Slice() {
            context.levels = new Levels(384, 63);
            when(chunk.getPos()).thenReturn(new ChunkPos(0, 0));
            when(chunk.getMinBuildHeight()).thenReturn(-64);
            when(chunk.getMaxBuildHeight()).thenReturn(320);
            when(chunk.getAllStarts()).thenReturn(Map.of());
            when(chunk.getHeight(any(), anyInt(), anyInt())).thenReturn(100);
            when(chunk.getBlockState(any())).thenAnswer(call -> {
                BlockPos at = call.getArgument(0);
                assertEquals(0, at.getX() >> 4); assertEquals(0, at.getZ() >> 4);
                return get(at);
            });
            when(chunk.setBlockState(any(), any(), anyBoolean())).thenAnswer(call -> {
                BlockPos at = call.<BlockPos>getArgument(0).immutable();
                BlockState old = get(at);
                changed.put(at, call.getArgument(1));
                return old;
            });
        }
        BlockState get(BlockPos at) {
            return changed.getOrDefault(at, at.getY() <= -64 ? Blocks.BEDROCK.defaultBlockState() : Blocks.STONE.defaultBlockState());
        }
        void carve(GiantCaverns generator) { generator.hollow(CHAMBER, new CaveCarving(chunk, context)); }
    }

    @Test void cavernPredictionMatchesObservedLargeSliceIncludingWallsAndPillars() {
        var generator = new GiantCaverns(4321, 1);
        var slice = new Slice(); slice.carve(generator);
        int cuts = 0, retained = 0;
        for (int x = 0; x < 16; x++) for (int y = -55; y <= 30; y++) {
            boolean observed = !slice.get(new BlockPos(x, y, 8)).is(Blocks.STONE);
            boolean predicted = generator.carvesAt(CHAMBER, -64, 100, x, y, 8);
            assertEquals(observed, predicted, "cut mismatch at " + x + "," + y);
            if (observed) cuts++; else retained++;
        }
        assertTrue(cuts > 100); assertTrue(retained > 0, "native roof and pillars remain");
    }

    @Test void unsupportedBuriedStartCannotLeaveAnIsolatedProtectedRockFragment() {
        var generator = new GiantCaverns(4321, 1);
        var observed = new Slice(); observed.carve(generator);
        BlockPos cut = observed.changed.entrySet().stream()
                .filter(entry -> entry.getKey().getX() >= 4 && entry.getKey().getX() <= 11
                        && entry.getKey().getZ() >= 4 && entry.getKey().getZ() <= 11
                        && entry.getKey().getY() >= -20 && entry.getKey().getY() <= 0)
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        BoundingBox box = new BoundingBox(cut.getX(), cut.getY() + 1, cut.getZ(), cut.getX(), cut.getY() + 8, cut.getZ());
        assertFalse(StructureTerrainValidation.validCavernSupport(BURIED, java.util.List.of(box),
                (x, y, z) -> !observed.get(new BlockPos(x, y, z)).is(Blocks.STONE)));

        var fragment = new Slice();
        var piece = mock(StructurePiece.class); when(piece.getBoundingBox()).thenReturn(box);
        var valid = mock(StructureStart.class);
        when(valid.isValid()).thenReturn(true); when(valid.getPieces()).thenReturn(java.util.List.of(piece));
        when(fragment.chunk.getAllStarts()).thenReturn(Map.of(mock(Structure.class), valid));
        fragment.carve(generator);
        assertTrue(fragment.get(cut).is(Blocks.STONE), "old AABB mask preserves a suspended rock island");

        var rejected = new Slice();
        when(rejected.chunk.getAllStarts()).thenReturn(Map.of(mock(Structure.class), StructureStart.INVALID_START));
        rejected.carve(generator);
        assertEquals(observed.get(cut), rejected.get(cut), "invalidating the whole start leaves no AABB terrain fragment");
    }

    @Test void foundationEdgesAreCheckedButEmptySpaceBetweenPiecesIsNotReserved() {
        var box = new BoundingBox(0, -10, 0, 15, 15, 15);
        assertFalse(StructureTerrainValidation.validCavernSupport(BURIED, java.util.List.of(box),
                (x, y, z) -> x == 15 && z == 15));
        var next = new BoundingBox(100, -10, 0, 115, 15, 15);
        assertTrue(StructureTerrainValidation.validCavernSupport(BURIED, java.util.List.of(box, next),
                (x, y, z) -> x > 15 && x < 100));
    }

    @Test void otherNativeUndergroundStructuresKeepTheirOwnCavePlacement() {
        var nativeCave = new StructurePlacementProfiles.Profile(256, "underground", 0, true, "");
        assertTrue(StructureTerrainValidation.validCavernSupport(nativeCave,
                java.util.List.of(new BoundingBox(0, -40, 0, 15, 20, 15)),
                (x, y, z) -> { fail("unrelated native cave structure was screened"); return true; }));
    }
}
