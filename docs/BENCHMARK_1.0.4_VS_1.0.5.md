# Wildshape 1.0.4 vs 1.0.5-beta.1 — measured fixture results

Status: **complete**; 6/6 successful paired-sequence trials.

Values below are **median (minimum–maximum)**. Startup and saving are excluded from generation/load windows.

| Metric | 1.0.4 | 1.0.5-beta.1 | Candidate change from baseline median |
| --- | ---: | ---: | ---: |
| First cold FULL chunk (s) | 36.75 (33.37–38.93) | 32.68 (32.44–32.88) | -11.1% |
| All 81 requested FULL chunks (s) | 66.58 (65.94–71.87) | 62.96 (62.12–64.97) | -5.4% |
| Requested chunks per second | 1.22 (1.13–1.23) | 1.29 (1.25–1.30) | +5.8% |
| Fresh-JVM saved-area loading (s) | 3.13 (2.84–3.75) | 3.70 (3.20–4.90) | +18.0%; ranges overlap |
| Sampled generation RSS peak (MiB) | 2123.03 (2115.21–2132.80) | 2070.71 (2067.51–2102.02) | -2.5% |
| Sampled reload RSS peak (MiB) | 1579.57 (1447.00–1588.10) | 1575.37 (1417.27–1644.47) | -0.3%; ranges overlap |
| Generation process CPU time (s) | 221.77 (211.42–238.56) | 215.94 (210.55–219.20) | -2.6%; ranges overlap |
| Measured generation GC pauses (ms) | 1232.77 (1118.99–1391.93) | 1109.67 (1075.14–1197.39) | -10.0%; ranges overlap |
| Observed after-GC heap maximum during generation (MiB) | 1268.06 (1259.06–1284.06) | 1286.77 (1220.06–1287.06) | +1.5%; ranges overlap |

A negative elapsed-time change means less time; a positive throughput change means more requested chunks per second. Those are different percentages.

## Per-trial measurements

| Trial | Version | First cold (s) | 81-chunk total (s) | Saved load (s) | Sampled generation RSS peak (MiB) | Recorded native starts |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| 01-A | 1.0.4 | 36.750 | 66.579 | 2.840 | 2115.2 | 0 |
| 02-B | 1.0.5-beta.1 | 32.880 | 64.967 | 4.898 | 2102.0 | 0 |
| 03-B | 1.0.5-beta.1 | 32.683 | 62.957 | 3.695 | 2070.7 | 0 |
| 04-A | 1.0.4 | 38.929 | 71.867 | 3.745 | 2123.0 | 0 |
| 05-A | 1.0.4 | 33.370 | 65.937 | 3.133 | 2132.8 | 0 |
| 06-B | 1.0.5-beta.1 | 32.436 | 62.119 | 3.198 | 2067.5 | 0 |

## Adjacent paired generation comparisons

| Pair | Baseline trial | Candidate trial | Less elapsed time | More requested throughput |
| --- | --- | --- | ---: | ---: |
| 1 | 01-A | 02-B | +2.42% | +2.48% |
| 2 | 04-A | 03-B | +12.40% | +14.15% |
| 3 | 05-A | 06-B | +5.79% | +6.15% |

## Limitations

- Only three repetitions per version: medians/ranges are descriptive, not a robust confidence interval.
- One fixed seed, preset, area and reduced dependency stack; not complete live-pack TPS or all-seed behavior.
- Requested-target throughput includes FULL completion and request/poll overhead; Minecraft also generates neighbors.
- RSS is sampled every 100ms process memory, not an absolute peak; it includes JVM heap policy, native buffers and metaspace, and differs from committed/reserved heap or post-GC heap use.
- GC events spanning a measurement boundary are excluded because their individual pauses cannot be located precisely.
- Changed placement policy/strata can change actual generated work; this is shipped-version comparison, not an isolated erosion microbenchmark.
- No speed percentage should be presented as conclusive if the trial-to-trial range/order effects are comparable to it.
- No native structure starts are recorded in these target chunks; this area primarily measures terrain/biome/island generation rather than completed dungeon construction.

## Test setup and interpretation

- **Three trials per version**, counterbalanced A–B–B–A–A–B, with fresh JVMs and identical saved-world copies. A separate calibration run used a different request protocol and is excluded from these measurements.
- Seed **2031102026**; unchanged saved preset SHA-256 `db4745f9bf850de99b79fb9b6d64ed5c490cbb8ad2c55c012efb14eef8425d4c`.
- **Java 21, NeoForge 21.1.266**, G1 GC, 512 MiB initial/3 GiB maximum heap, four processors exposed to Java and three Wildshape workers. The shared host exposes nine logical CPUs; this is not a dedicated production-server benchmark.
- **16 dependency JARs plus Wildshape**, including BOP, Regions Unexplored, Nature’s Spirit, TerraBlender, Streams 2.17.3, SkyLands 0.9.4.1, Cataclysm 3.33, Integrated API 1.9.1, Better Dungeons 5.1.4 and Better Desert Temples 4.1.5. Their exact hashes are recorded. C2ME, Bye Pregen and both Generator Accelerators were absent from both versions.
- First cold usable/FULL chunk at (446,356), then 81 targets spanning 442–450 by 352–360, requested in nine 3×3 batches. **The 81-chunk total includes the first cold chunk.** Minecraft also generates neighboring chunks, so throughput is requested-target throughput rather than a count of every generated chunk.
- Generation timing ends after in-game loaded checks; saved NBT independently confirms every target is FULL. Fresh-JVM reload timing excludes startup. Save/flush, world copying, JFR parsing and verification are outside the timed windows. Persistent RCON, JVM profiling and polling are identical for both versions.
- Every trial preserved all saved target blocks, biome data and preset bytes on reload. All six runs also generated identical biome digests across 81 chunks / 2,997 sections. None of the target chunks recorded a native structure start; the test therefore primarily measures terrain/biome/island generation plus placement screening, not the construction of large dungeons.
- Peak RSS is **sampled process resident memory during generation**, not a retained-heap or total-server-memory guarantee. Post-GC heap figures are separately observed samples rather than a forced-GC retained-memory test.

The measured overall generation improvement is modest: **5.4% less elapsed time**, equivalent to **5.8% higher requested-target throughput** in this fixture. The first cold chunk improved by 11.1% in elapsed time. Saved-area loading did not improve: its median was 0.56seconds longer, with overlapping run ranges. Full-modpack player TPS, other seeds/biomes and sustained Chunky pregeneration were not measured.

## Exact delivered artifacts

| Version | SHA-256 |
| --- | --- |
|1.0.4 |`a1838c6acdeb7492f0baccf050a4d257e9875efffccd9b6ff2a61d5d2dc469c0`|
|1.0.5-beta.1 |`31d0dde5d20d67fb151a40240c1f074fafdbebe3235aa6857ab4b65a1fc722c6`|

Neither JAR was rebuilt or modified for this benchmark. The harness is in `tools/benchmark-104-105.py`, the postprocessor in `tools/analyze-benchmark.py`, and condensed measurements/independent checks in `docs/audit/benchmark-104-105/`. Full raw measurement files are retained in the accompanying evidence archive.
