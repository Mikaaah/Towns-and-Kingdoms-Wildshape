# Wildshape 1.0.5-beta.1 — worldgen engineering audit

Minecraft 1.21.1 / NeoForge / Java 21. Baseline: `fix/worldgen-performance-1.0.4`, commit `256e003`. Changes are isolated on `audit/structure-compat-1.0.5`; the default branch and live worlds are not changed by this audit.

## Executive diagnosis

The inspected code has several independent causes of clustering, missing content and stalls. A repeated salt is not automatically a defect. Independent structure sets do not share an exclusion rule, even when every salt is unique. Replacing all salts would move sites, break intentionally paired layouts and still would not provide cross-mod clearance.

The reproducible dependency inventory inspected **56 exact JARs**, expanding the requested named families into actual artifacts: **159 structure-set providers / 152 distinct set IDs**, **313 structure definitions**, **267 biomes**, **1,090 biome-tag files** and **706 biome modifiers**, with zero JSON parsing failures or artifact hash mismatches. These are static resource counts, not a certification of loading all artifacts together. Exact versions, URLs and SHA-256 values are in `docs/audit/full-compat-1.0.5/dependency-lock.json`; the compatibility matrix records each inspected artifact.

| Finding | Evidence / practical consequence | Change |
| --- | --- | --- |
| Independent native placement grids | Previous spacing policy worked per set. Several sets can choose nearby chunks independently. | Cross-set deterministic candidate screening, without loading adjacent chunks. |
| Intentional repeated salts/layouts | Five identical-layout groups include Epic dungeon/obelisk and SkyLands theme pairs; twelve salt-sharing groups exist. | Preserve native salts and paired layouts; no blanket salt rewrite. |
| Native overrides of placement methods | Integrated API and Cataclysm override the method used by ordinary vanilla random-spread placement. A base-only hook misses them. | Individually guarded hooks for verified overrides; unsupported algorithms remain native and cannot block filtered ones. |
| Native terrain assumptions | Large pieces can cross cliffs even if the origin passes a height check; native terrain adaptation alone does not guarantee a level footprint. | Bounded 3×3 footprint height-range/water screen for explicitly classified surface structures; retain native construction and adaptation. |
| Native validation rejects a candidate after arbitration | Biome tags, piece builders, terrain, exclusion zones and feature-specific rules can reject sites. | Keep those native rules. Approximate biome rejection is opt-in; no fabricated fallback dungeon or forced biome conversion. |
| Optional habitats omit newer biome families | Blooming Nature climate/river habitats and specific native selectors lack suitable modded equivalents. | Ninety additive optional tag overlays plus matching preset-export tables; native feature and spawn rates retained. |
| Pack-level Naturalist disable | A previously supplied KubeJS export replaces `naturalist:add_animals` with `neoforge:none`. | Documented; Wildshape does not undo an intentional pack override. Current server config still needs comparison. |
| Landmarks are not always structures | Thaumaturge totems, portals and hilltop stones are placed features. | Preserve native feature providers; structure-set arbitration cannot control these landmarks. |
| Shared mutable strata RNG | Concurrent first-use of different cached layer IDs consumes one RNG in unpredictable order. | Resource-keyed positional RNG; newly generated geology becomes independent of first-use order. |
| Lazy initialization publication race | A read could validate the stamp before observing initialization and return a stale null. | Volatile publication after complete initialization, locked single-writer slow path. |
| Avoidable retained erosion data | Each column retained duplicate index and weight arrays. | Share immutable relative kernels, preserving float arithmetic and iteration order. |
| Optional engine API differences | World Preview changed package path; Generator Accelerator 2 has a distinct mod ID and carver path. | Support both preview targets, separate GA2 CPU/GPU aquifer guards, cavern decoration around the complete carver call. |

## Structure implementation and configuration

`StructurePlacementCoordinator` builds an immutable, weak-world-indexed view of registered, supported random-spread placements. It evaluates neighboring **native candidate coordinates**, native frequency/exclusion rules and a stable seed/set/coordinate rank. It never queries neighboring chunks or reserves sites in a generation-order-dependent mutable map. Candidate calculation uses negative-coordinate-safe region division and bounded work. Default center clearance is **256 blocks for major structures** and **112 blocks for minor structures**, using the larger clearance for a pair.

Automatic legacy spacing floors are bypassed when unified screening is enabled, avoiding two simultaneous density reductions. Explicit settings already saved in a preset/datapack remain authoritative and are not silently migrated.

Native linked groups and separate surface, underground, ocean and sky profiles remain distinct. Concentric strongholds, feature-based landmarks, AE2 resource features and unknown custom placement algorithms retain their own rules. Nether and End terrain contexts are not given the Overworld policy. The complete maximum candidate bound is checked before enabling an index; an excessive configuration disables that world's screening with a warning rather than applying inconsistent partial filtering. This behavior is inspectable in the developer audit report.

Configuration is server-authoritative and snapshotted before generation. `config/wildshape/structures.toml` uses **NeoForge ModConfigSpec** for enablement, major/minor clearance and candidate budget. `config/wildshape/structure-placement.json` contains exact namespace/set/structure profiles, layer classification, linked groups, slope thresholds and water screening; it is created from the bundled JSON once and is never silently overwritten. Restart the server after changing either file. Existing preset and performance config formats are preserved rather than converted incompatibly.

Terrain screening uses the completed native `StructureStart` bounding box for specifically configured surface IDs. It evaluates a fixed nine-point raw-terrain grid, rejects excessive elevation range and optionally wet terrain. It does not mutate pieces, globally flatten terrain, or replace every `TerrainAdjustment`. Raw terrain is intentionally used here to avoid reintroducing filtered tile/river-network surveys during placement.

**Limits:** center clearance is not a guarantee of space between the edges of enormous bounding boxes. Native start rejection after arbitration can leave holes. The conservative footprint estimate does not inspect every final eroded column, raised river or native foundation; it cannot certify every floating/buried start or underground cavity/depth. Underground, underwater and sky specialists retain native validation. Screening changes future evaluated sites and does not rewrite previously generated structures or repair pre-existing clusters at the boundary of explored terrain.

## Biomes, rivers, caves and resources

- TerraBlender's native biome/surface pipeline is retained. No biome identities, world seed, saved terrain preset or climate splines are replaced.
- Optional append-only tags cover Blooming Nature climate/vegetation, Croptopia crops and fruit, Naturalist habitats, Archie’s Dinosaurs fossils/fauna, Thaumaturge selectors, and HAPI/Starcatcher rivers. All literal optional biome references were checked against inspected JARs; unavailable mods use `required:false` entries.
- Static tag overlays also merge into existing saved preset datapacks, so these additions do not require rewriting a world's preset. They extend tag membership in ordinary worlds when Wildshape is installed too. Higher-priority user packs using `replace:true` can intentionally override them.
- Native ore/meteorite providers remain responsible for AE2, Create, Create Nuclear and Immersive Engineering placement. Wildshape does not invent extra geodes or duplicate ore rates. The audit distinguishes configured/placed features from structure sets.
- Alex’s Caves native cave generation stays in control; the protected cave-biome family also includes Thaumaturge’s inspected underground biome. Generic surface checks are not imposed on cave ecosystems.
- Streams' native integration and Flow Control manual direction/stillwater precedence are preserved. Existing water/cave safety regressions remain in the test set; this audit does not establish that every river in every seed is defect-free.
- Generator Accelerator 1 and 2 are separate integrations; do not install both as a validation shortcut. The GA2 guard wraps both aquifers passed into its carving pipeline, and Wildshape cavern decoration runs after a successfully completed accelerated carving call.

See `HABITAT_AUDIT_1.0.5.md`, `CONCURRENCY_PERFORMANCE_AUDIT_1.0.5.md`, `docs/audit/full-compat-1.0.5/engine-hooks.md` and `dependency-findings.md` for exact APIs and retained behavior.

## Performance and cleanup

The density sampler no longer creates two weak-reference wrappers on every column cache miss; it keeps weak world identity until the world changes. Erosion shares at most 81 immutable brush pairs. For a 192×192 brush domain the calculated retained kernel array count falls **73,728 → 162**, with primitive kernel payload **13,050,720 → 19,656 bytes**; pointer lookup tables remain. This is a memory calculation, not a measured TPS improvement. The Cell pool now uses slot zero, prevents repeated close before reborrow, and avoids mutating another worker's local list. Reusable handles keep scoped ownership: callers must not reuse/close an old handle after it has been borrowed again.

The existing snapshot-sorted eviction, safe published tile lifetime, failed-tile retry, and 1.0.4 cheap search/probe paths are retained. Cold erosion/filtering and large Streams watershed planning can still be expensive. The process-wide generation executor has an unbounded queue; safely bounding in-flight futures needs lifecycle/scheduling work beyond a cache-size cap. No claim of zero allocations or resolved full-pack TPS is made.

Unused `LoadBalanceLongMap` and excluded Fabric port material are identified rather than removed blindly; serialized/codec/mixin classes were not deleted from text-search results alone. Duplicate optional mixin registrations were removed, and retired KnightQuest/Alex’s Mobs habitat aliases were cleaned up. No unnecessary hard dependencies were introduced.

## Verification and release status

This is **1.0.5-beta.1**, not a stable full-pack certification. The machine-readable verification record contains actual completed build/test/runtime results; pending or failed checks must not be read as passed.

The pre-CI evidence was: all 56 dependency hashes/resources parsed; all 90 overlays optional/additive with zero missing literal biome IDs; Java 21 habitat exporter compiled and ran; four standalone Java concurrency tests passed. The regression workflow includes clearance order/concurrency, exact erosion brush equivalence, lazy publication, pool lifetime, strata determinism, optional guards, old preset export, cave/water behavior and habitat parity. Installed-server checks exercise save upgrade, native SkyLands generation, ceiling persistence, reload and startup without optional mods. Additional full-modpack/player-teleport, pregeneration stress, visual structure placement and original-world TPS verification remain necessary before stable deployment.

Newly generated strata can differ from earlier versions because the race-prone random sequence was replaced. Existing chunks are not rewritten. Use a copy of the server world to evaluate the changed structure distribution and native foundations before adopting this beta on the live server.

## Completed verification

- Java 21 NeoForge build, **115 regression tests**, **18 flow rules**, **7 downhill-search checks**, and release identity validation passed for source commit `1e0cbc8bc48cd7a3fc6cfdcbaaeac643bac67cf4`.
- Installed server on NeoForge 21.1.266 with the TNK biome stack, SkyLands 0.9.4, Cataclysm 3.33, Integrated API 1.9.1, Better Dungeons 5.1.4 and Better Desert Temples 4.1.5 passed upgrade, generation, save/reload and optional-mod-absence checks.
- The fixture evaluated **117 native candidate sites**, accepted **27**, found **zero same-layer clearance violations** and **zero budget bypasses**. The bounded sampling took **5 ms** in that run. The Integrated API case uses a clearly marked test-only synthetic set with a vanilla village reference; it does not certify every IDAS template or dependency combination.
- The sampled existing chunk's blocks/biomes, saved preset and other dimension heights remained unchanged. New sky island blocks and the Y=527 marker survived reload. These are server data assertions, not graphical or player-teleport validation.
- Bye Pregen 1.1.3.4 independently passed the same upgrade/generation/reload/absence sequence.
- **Generator Accelerator 1.6.2 + Regions Unexplored 0.6.3 fails at startup.** GA1's core `WorldCarver.carveBlock` overwrite prevents RU's `fixGrassQuery` injection. Its exposed configuration cannot disable that core overwrite; `chunkPipelineCarvers` does not resolve it. This conflict is upstream of Wildshape generation. Keep RU and leave GA1 out of this combination. GA2 has a separate API and its own runtime record; it is not assumed equivalent.

The delivered JAR is the exact artifact used by the structure server fixture, SHA-256 `31d0dde5d20d67fb151a40240c1f074fafdbebe3235aa6857ab4b65a1fc722c6`. Its owned classes/data match the regression build; independent builds differ only in an Architectury-generated bridge UUID and access-transformer line ordering. See the machine-readable evidence under `docs/audit/full-compat-1.0.5/runtime/`.

### Optional engine runtime outcomes

| Utility | Exact inspected/tested version | Result |
| --- | --- | --- |
| Bye Pregen | 1.1.3.4 | Upgrade, native island generation, save/reload and optional absence passed. |
| World Preview NeoForged | 1.3.1 | Upgrade/generation/reload/absence passed; the relocated `SampleUtils` constructor and slow-height sample actually ran with an independent approximate context (sample height 40). Graphical client preview remains untested. |
| Generator Accelerator 2 | 1.21.1-2.0.21.259 | Upgrade/generation/reload/absence passed using its safe vanilla fallback: FAST=0, VANILLA=25 during generation. Optimized CPU and GPU carvers were not exercised, so their acceleration and optional aquifer bridge are not runtime-certified. |
| Generator Accelerator 1 | 1.21.1-1.6.2 | Failed startup with Regions Unexplored 0.6.3 because of the third-party overwrite/injection conflict described above. |

Fixture phase wall times include server startup and checks; they are not chunk-generation latency or a comparative TPS benchmark. The fixtures use isolated copies and preserve sampled saved data. They do not demonstrate complete interoperability of all 56 artifacts loaded simultaneously. `runtime/verification.json` records all outcomes and remaining limits.
