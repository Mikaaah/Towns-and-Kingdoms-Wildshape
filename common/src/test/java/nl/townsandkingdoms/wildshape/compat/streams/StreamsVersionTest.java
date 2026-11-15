package nl.townsandkingdoms.wildshape.compat.streams;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StreamsVersionTest {
    @Test void allInspectedReleasesSelectTheirActualABI() {
        assertEquals(StreamsVersion.LEGACY_215, StreamsVersion.of("2.15.2"));
        assertEquals(StreamsVersion.MODERN_216, StreamsVersion.of("2.16.2"));
        for (String release : new String[]{"2.17.1", "2.17.3", "2.17.4"}) {
            assertEquals(StreamsVersion.MODERN_217, StreamsVersion.of(release));
            assertTrue(StreamsVersion.of(release).modern());
        }
        assertEquals(StreamsVersion.MODERN_217, StreamsVersion.of("2.17.4+1.21.1"));
    }

    @Test void similarFutureAndPrereleaseNumbersCannotActivateTheWrongHooks() {
        for (String release : new String[]{"2.17.10", "2.17.40", "2.17.5", "2.18.0", "2.17.4-rc.1", "", "unknown"})
            assertEquals(StreamsVersion.UNSUPPORTED, StreamsVersion.of(release), release);
        assertEquals(StreamsVersion.UNSUPPORTED, StreamsVersion.of(null));
        assertFalse(StreamsVersion.UNSUPPORTED.modern());
    }
}
