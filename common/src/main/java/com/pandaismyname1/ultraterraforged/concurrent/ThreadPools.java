package com.pandaismyname1.ultraterraforged.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.pandaismyname1.ultraterraforged.config.PerformanceConfig;

public class ThreadPools {
    // Cap default worker count to avoid saturating C2ME / vanilla schedulers.
    // Explicit threadCount in performance_internal.conf overrides this setting.
    public static final ExecutorService WORLD_GEN = Executors.newFixedThreadPool(
        PerformanceConfig.read(PerformanceConfig.DEFAULT_FILE_PATH)
            .result().orElseGet(PerformanceConfig::makeDefault).threadCount(),
        daemonFactory("UTF-WorldGen"));

    public static int availableProcessors() {
        return Math.max(2, Runtime.getRuntime().availableProcessors());
    }

    public static ThreadFactory daemonFactory(String name) {
        AtomicInteger counter = new AtomicInteger();
        return (runnable) -> {
            Thread thread = new Thread(runnable, name + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
