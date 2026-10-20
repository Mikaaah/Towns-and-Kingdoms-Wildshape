package com.pandaismyname1.ultraterraforged.test;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.pandaismyname1.ultraterraforged.config.PerformanceConfig;

public class PerformanceConfigTest {
    @Test void readsActualProperties() throws Exception {
        Path path = Files.createTempFile("utf-performance", ".conf");
        try {
            Files.writeString(path, "tileSize=2\nbatchCount=4\nthreadCount=3\n");
            var loaded = PerformanceConfig.read(path).result().orElseThrow();
            assertEquals(2, loaded.tileSize());
            assertEquals(4, loaded.batchCount());
            assertEquals(3, loaded.threadCount());
            Files.writeString(path, "tileSize=999\n");
            assertTrue(PerformanceConfig.read(path).result().isEmpty());
        } finally { Files.deleteIfExists(path); }
    }
}
