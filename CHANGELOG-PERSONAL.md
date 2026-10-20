# TNKTerraForged — development changelog

## 0.1.0 preview — TNK3-specific derivative (2026-10-20)
- Rebranded loaders' display names, creation screen and build archives to **TNKTerraForged**.
- Retained the original `ultraterraforged` mod ID, registry/data namespaces, version base and preset storage intentionally for world and integration compatibility; this fork **replaces**, never co-installs with, UltraTerraForged.
- Scoped unsafe static global structure terrain veto behind `-Dtnkterraforged.legacyGlobalStructureRules=true` (off by default), because this old village rule could reject **all** modded structure generation sites.
- Added generic structure-start bounding-box preservation during UTF cave carving, lining and local water repairs (configurable with `-Dtnkterraforged.structureProtection=false`).
- Compatible in design with IDAS, Integrated Dungeons Arise, Integrated Stronghold, Integrated Cataclysm, YUNG's Better Dungeons and Cataclysm; their actual JAR versions, placement rules, and multi-chunk overlaps still need in-game verification.
- Existing optional Alex's Caves 2.0.10 source integration retained for NeoForge; Fabric makes no such claim.
- Kept MIT upstream notices and GPL-3.0 integrated NeoForge compatibility obligations.
- No new dungeon structures, dungeon loot or progression bypasses are introduced.

## 2026-10-20 — Cave networks, failed-tile recovery and repository cleanup

### World generation
- **Giant cavern connectivity:** add deterministic, winding passages between neighboring active giant caverns, without dependency on the order in which chunks generate.
- **Cavern altitude weighting:** use block-space sea level for mountain weighting rather than comparing block elevation with a normalized water level.
- **Built-in caves:** reduce the spaghetti, noodle, regular/deep carver and ravine densities across six shipped presets; user-defined presets are not modified.
- **Cross-chunk fluid work:** use a primitive long flood queue and 2-byte bottom-relative ceilings, and only allocate the flood-height table when a water-connected cave is actually discovered. Dry cave chunks avoid this allocation entirely. True cross-chunk aquifer connectivity still requires live tests.

### Memory and reliability
- **Normal tile generation:** release pooled tile arrays when any async worker or filter fails; wait until all workers finish before closing.
- **Failed tile caching:** remove failed tile entries so later requests can retry generation rather than reusing a completed exceptional future indefinitely.
- **Idle scheduler:** disable scheduled polling for caches configured with infinite lifetime (such as active tile caches).
- **Regression:** add an asynchronous tile-failure retry JUnit test.

### Presentation and repository
- Rewrite README to describe actual fork features, supported loaders, credit and compatibility.
- Update NeoForge and Fabric mod metadata, project homepages, issue links and original/fork contributor credits; retain correct MIT and GPL license declarations.
- Consolidate development branches to NeoForge/Fabric 1.21.1 editions, retaining only necessary repository administration branch(es) if GitHub permissions prevent changing the default.

**Validation:** source compilation and targeted tests are required; geometry, fluid seams, Alex's Caves coexistence and user input still require actual Minecraft-world checks. Build results do not constitute a performance benchmark.

---

## 2026-10-20 — Audit completion implementation

### High-priority world generation
- **A04:** Stop resetting every active world's caches when another world initializes. Cache registration and scheduler polling now use weak references to prevent abandoned worlds being kept alive.
- **A05/C05:** Use smaller one-block water terraces and interpolated reach samples. Smooth forced sea/tributary junction drops backwards through the upstream profile, without raising water or exceeding feasible gorge depth.
- **A06:** Detect newly opened cave air touching filled water, propagate that water through connected air up to the source-water elevation, and mark chunk edges for postprocessing. Disconnected caves remain dry.
- **A09:** Reenable the serialized terrain-type exclusion in structure placement rules, including their existing village rule.
- **B03:** Prevent pooled tiles from being retired by timer/capacity while in-flight chunks may be using them; retire on the final chunk drop.

### Compatibility and stability
- **C01 (NeoForge source integrated):** Adapted the existing GPL-3.0 standalone compatibility implementation into the UTF NeoForge module: rare-cave biome routing, structure placement validation, verified `/locateacbiome`, world sampler binding and native cavern protection. Conditional Mixin plugin and compile-only Alex's Caves 2.0.10 dependency keep the port optional. **Remove the standalone tfacfix JAR** when testing this build. Runtime compatibility still requires validation.
- **C03:** Replace HEAD/TAIL surface region context with a scoped MixinExtras method wrapper and `try/finally`, so exceptions cannot leave a stale thread-local region.
- **C04/C05:** Add river mouth/gorge/terrace unit tests and a dual-loader Java 21 release-mode regression workflow.

**Licensing:** The embedded NeoForge Alex's Caves compat sources retain the GPL-3.0 obligations of the separately published fix; original UltraTerraForged MIT credits remain. See `LICENSE-ALEX-COMPAT-GPL-3.0`.

**Validation policy:** See `AUDIT-STATUS.md` for every one of the original 23 audit findings and outstanding runtime/integration tests. Static checks and builds alone do not validate the TNK3 modpack or resolve historic chunks.

---

## Unreleased (Minecraft 1.21.1; fork changes)

### Cave generation
- **Massive caverns:** increased chamber diameters to approximately 230–350 blocks from 80–150, with noise-ragged ceilings, stone pillars and deep lakes preserved.
- **Terrain-aware placement:** giant cavern attempts are more common below elevated terrain; the probability rises with sampled surface elevation instead of moving caverns higher.
- **Deep, protected roofs:** cavern roof limited to Y=30 or lower, with a 30-block overburden from measured terrain. Actual local column height also clips the carve, protecting valleys between samples.
- **Adaptive vertical fit:** cavern heights shrink to available world depth instead of breaking bedrock or poking through terrain; deeper world presets allow taller caverns.
- **Density:** grid reduced from 900 to 640 blocks. Increased default giant-cavern frequency 0.30→0.65; default small horizontal layer-cave feature disabled (players can re-enable).
- **Carver performance:** reject distant columns and vertically excluded blocks before expensive per-block wall-noise evaluation. Deterministic seed/grid placement, chunk independent.

### UI / preset editor
- **Slider mouse interaction:** explicitly process clicks and dragging inside nested scrolling panels instead of depending on indirect value forwarding. Apply accepted normalized values and update labels immediately.
- **Slider focus:** correctly associate clicked scrolling-panel child and release its focus; clear focus when rebuilding lists.
- **Precision:** avoid needlessly casting normalized slider positions to float when rendering value text. Protect stored slider values against NaN or outside-range callback values.

### Stability and correctness
- **Spawn safety:** only override climate spawn search for UltraTerraForged climate samplers, and guard a missing spawn center. Other dimensions use the original Minecraft finder.
- **World-state isolation:** invalidates fallback density cache when the world/generator lookup changes, not merely coordinates; also resets cache before a failed recomputation.
- **Density-function bounds:** advertise conservative numeric bounds because continentalness and weirdness can be negative.
- **Noise restoration:** always restore thread-local sample-cell state using `try/finally` if heightmap evaluation throws.
- **Concurrent cache insertion:** recheck under the write lock before evicting an old entry; avoids needless eviction on racing same-key inserts.
- **Object pooling:** pool resources remain marked released even when the array pool is full.
- **River-noise performance:** skip two lake-domain warp noises for columns outside every river network, retaining the same algorithm for intersecting networks.
- **Raised-water postprocessing:** count pre-existing water as filled when identifying river columns that may spill.

### Additional stability, memory and performance corrections
- **LazyCallable:** distinguish computed-null from not-yet-computed values; Runnable-based adapters now complete once without repeatedly executing or throwing after returning null.
- **Landform cache:** evict only one old site at capacity instead of clearing the entire 4096-entry per-thread cache, retaining hot locations and stable deterministic site geometry.
- **Tile generation:** on worker or filter failure, release allocated tile and pooled arrays after all workers have completed, for both regular terrain and zoomed previews.
- **Performance configuration:** `performance_internal.conf` now actually reads and validates `tileSize`, `batchCount` and `threadCount` rather than always ignoring the file.
- **Generation workers:** cap default worker threads at 8 (or fewer depending on the CPU) to reduce competition with C2ME and JVM memory pressure; `threadCount` is applied after generator initialization, avoiding platform-config access during static executor bootstrap.
- **Release safety:** disable automated publishing to the original project's CurseForge and Modrinth pages; remove the upstream project IDs.

### Validation and scope
- These modifications affect newly generated chunks, **not existing terrain**.
- Initial regression coverage: existing cave/river/option tests and new targeted cavern roof tests; real-world visual checking and mod compatibility tests remain necessary.
- **Integrated in later audit work:** optional NeoForge Alex's Caves source compatibility and cache lifecycle fixes. Cross-chunk aquifer behavior still requires testing in full Minecraft worlds.
- The existing `1.0.2+1.21.1` mod version is intentionally preserved in the test branch for dependencies that pin the version.
