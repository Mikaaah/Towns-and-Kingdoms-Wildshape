# Wildshape 1.0.4 versus 1.0.5-beta.1 benchmark

This benchmark compares the existing, delivered JARs; it never rebuilds either version. Both use identical copies of one saved world/preset, the same dependency JARs, Java 21 / NeoForge21.1.266, G1 GC, three Wildshape workers, four exposed processors and512MiB–3GiB heap. C2ME, Bye Pregen and both Generator Accelerators are absent to isolate Wildshape.

The reduced stack includes Biomes O' Plenty, Regions Unexplored, Nature's Spirit, TerraBlender, SkyLands0.9.4.1, Streams Reflowing2.17.3, Cataclysm3.33, Integrated API1.9.1, Better Dungeons5.1.4, Better Desert Temples4.1.5 and their exact required libraries. This is not the complete live TNK modpack.

## Method

- Six independent trials in balanced order **A B B A A B**, three per version. Each starts a new JVM and an untouched copy of the same world template. A separate untimed/calibration pilot is excluded from these results.
- First cold FULL chunk at(446,356), then81 target chunks covering442–450 by352–360. Nine3×3 request batches avoid one giant synchronous command becoming the watchdog bottleneck.
- Timers end only after in-game checks show the requested chunks loaded. Saved NBT must independently show **FULL** for all81 targets. Minecraft may generate additional neighboring chunks, so throughput is expressed as requested target chunks per second.
- After save and clean shutdown, a fresh JVM reloads the same saved area. Startup, saving, world copying, data verification and JFR parsing are outside the measured generation/load windows.
- Process RSS sampled every100ms during each window; CPU time sampled from/proc; JFR and G1 logs are enabled identically on both versions. Reserved heap, resident memory and post-GC live heap are different metrics.
- Existing preset bytes and all saved biome data must survive reload unchanged. Block snapshots are recorded separately: normal fluid/neighbor updates can change block states in a ticking loaded area. Exact before/after regions are retained for the first trial so differences can be diagnosed instead of silently ignored.
- Record native structure starts. Intentional placement-policy differences can change the actual generation work; a shipped-version comparison is not an isolated erosion microbenchmark.

Results, exact hashes, per-trial timings, run ranges and limitations will be recorded after the paired runs complete. No speed percentage is claimed before measurement. The fixture measures a fixed seed and area, not full-server player TPS or performance across all seeds.
