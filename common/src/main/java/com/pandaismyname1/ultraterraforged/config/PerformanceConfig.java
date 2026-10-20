package com.pandaismyname1.ultraterraforged.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.mojang.serialization.DataResult;
import com.pandaismyname1.ultraterraforged.concurrent.ThreadPools;
import com.pandaismyname1.ultraterraforged.platform.ConfigUtil;

/**
 * Optional UTF performance_internal.conf (Java properties syntax):
 * tileSize=3
 * batchCount=6
 * threadCount=8
 *
 * Loading this file only changes scheduling/cache dimensions, not terrain.
 * A restart is required to change the world-generation worker count.
 */
public record PerformanceConfig(int tileSize, int batchCount, int threadCount) {
    public static final Path DEFAULT_FILE_PATH = ConfigUtil.utf("performance_internal.conf");

    public static final int MAX_TILE_SIZE = 6;
    public static final int MAX_BATCH_COUNT = 20;
    public static final int MAX_THREAD_COUNT = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);

    public static DataResult<PerformanceConfig> read(Path path) {
        if (!Files.exists(path)) return DataResult.success(makeDefault());
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            props.load(reader);
            PerformanceConfig defaults = makeDefault();
            int tile = number(props, "tileSize", defaults.tileSize(), 1, MAX_TILE_SIZE);
            int batch = number(props, "batchCount", defaults.batchCount(), 1, MAX_BATCH_COUNT);
            int threads = number(props, "threadCount", defaults.threadCount(), 1, MAX_THREAD_COUNT);
            return DataResult.success(new PerformanceConfig(tile, batch, threads));
        } catch (IOException | IllegalArgumentException e) {
            return DataResult.error(() -> "Invalid UltraTerraForged performance config " + path + ": " + e.getMessage());
        }
    }

    private static int number(Properties props, String key, int fallback, int minimum, int maximum) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        int value = Integer.parseInt(raw.trim());
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(key + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    public static PerformanceConfig makeDefault() {
        return new PerformanceConfig(3, 6, Math.max(2, Math.min(8, ThreadPools.availableProcessors() - 1)));
    }
}
