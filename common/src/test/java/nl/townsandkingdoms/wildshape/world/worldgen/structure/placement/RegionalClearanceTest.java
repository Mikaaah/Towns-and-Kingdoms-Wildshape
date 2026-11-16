package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class RegionalClearanceTest {
    @Test
    void radialBoundaryAndMixedClassesUseBlocks() {
        assertFalse(RegionalClearance.conflicts(0, 0, 256, 16, 0, 112));
        assertTrue(RegionalClearance.conflicts(0, 0, 256, 15, 0, 112));
        assertTrue(RegionalClearance.conflicts(0, 0, 112, 15, 0, 256));
        assertFalse(RegionalClearance.conflicts(0, 0, 256, 12, 12, 256));
        assertTrue(RegionalClearance.conflicts(0, 0, 256, 11, 11, 256));
        assertFalse(RegionalClearance.conflicts(0, 0, 0, 0, 0, 0));
    }

    @Test
    void negativeAndExtremeCoordinatesCannotOverflowDistance() {
        assertTrue(RegionalClearance.conflicts(-16, -16, 256, -1, -16, 256));
        assertFalse(RegionalClearance.conflicts(Integer.MIN_VALUE, 0, 256,
                Integer.MAX_VALUE, 0, 256));
        assertFalse(RegionalClearance.conflicts(Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertTrue(RegionalClearance.conflicts(Integer.MAX_VALUE, Integer.MIN_VALUE,
                256, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1, 256));
        assertFalse(RegionalClearance.conflicts(0, 0, Integer.MAX_VALUE,
                134217727, 134217727, Integer.MAX_VALUE));
    }

    @Test
    void hashTiesHaveAStableStrictOrder() {
        assertTrue(RegionalClearance.wins(0, "a:set", 0, 0, 0, "b:set", 0, 0));
        assertTrue(RegionalClearance.wins(0, "a:set", -1, 0, 0, "a:set", 0, 0));
        assertTrue(RegionalClearance.wins(0, "a:set", 0, -1, 0, "a:set", 0, 0));
        assertFalse(RegionalClearance.wins(0, "a:set", 0, 0, 0, "a:set", 0, 0));
        assertTrue(RegionalClearance.wins(0, "a:set", 0, 0, -1L, "a:set", 0, 0));
    }

    @Test
    void thousandCandidatesHaveTheSameClearanceInAnyGenerationOrder() {
        List<Candidate> candidates = candidates();
        Set<Candidate> original = accepted(candidates);
        assertFalse(original.isEmpty());
        assertTrue(original.size() < candidates.size(), "Fixture needs real collisions");
        Collections.reverse(candidates);
        assertEquals(original, accepted(candidates));
        Collections.shuffle(candidates, new Random(17));
        assertEquals(original, accepted(candidates));
        for (Candidate a : original) {
            for (Candidate b : original) {
                if (a != b) assertFalse(conflicts(a, b), "Accepted candidates overlap");
            }
        }
    }

    @Test
    void concurrentEvaluationDoesNotRetainRandomOrMutableState() {
        List<Candidate> candidates = candidates();
        Set<Candidate> expected = accepted(candidates);
        Set<Candidate> parallel = candidates.parallelStream()
                .filter(a -> survives(a, candidates)).collect(java.util.stream.Collectors.toSet());
        assertEquals(expected, parallel);
        long[] serial = IntStream.range(0, 4096)
                .mapToLong(i -> RegionalClearance.priority(9844, "test:dungeon", i - 2048, -i)).toArray();
        long[] concurrent = IntStream.range(0, 4096).parallel()
                .mapToLong(i -> RegionalClearance.priority(9844, "test:dungeon", i - 2048, -i)).toArray();
        assertArrayEquals(serial, concurrent);
        assertNotEquals(RegionalClearance.priority(1, "test:a", 4, -4),
                RegionalClearance.priority(2, "test:a", 4, -4));
        assertNotEquals(RegionalClearance.priority(1, "test:a", 4, -4),
                RegionalClearance.priority(1, "test:b", 4, -4));
    }

    private static List<Candidate> candidates() {
        Random random = new Random(14788);
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String id = "test:set_" + i;
            int x = random.nextInt(257) - 128;
            int z = random.nextInt(257) - 128;
            candidates.add(new Candidate(id, x, z, i % 2 == 0 ? 256 : 112,
                    RegionalClearance.priority(54742, id, x, z)));
        }
        return candidates;
    }

    private static Set<Candidate> accepted(List<Candidate> candidates) {
        Set<Candidate> result = new HashSet<>();
        for (Candidate a : candidates) if (survives(a, candidates)) result.add(a);
        return result;
    }

    private static boolean survives(Candidate a, List<Candidate> candidates) {
        for (Candidate b : candidates) {
            if (a != b && conflicts(a, b) && RegionalClearance.wins(b.rank, b.id, b.x, b.z,
                    a.rank, a.id, a.x, a.z)) return false;
        }
        return true;
    }

    private static boolean conflicts(Candidate a, Candidate b) {
        return RegionalClearance.conflicts(a.x, a.z, a.clearance, b.x, b.z, b.clearance);
    }

    private record Candidate(String id, int x, int z, int clearance, long rank) {}
}
