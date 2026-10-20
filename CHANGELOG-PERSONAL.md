# UltraTerraForged Personal — development changelog
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

### Validation and scope
- These modifications affect newly generated chunks, **not existing terrain**.
- Initial regression coverage: existing cave/river/option tests and new targeted cavern roof tests; real-world visual checking and mod compatibility tests remain necessary.
- **Not yet integrated:** Alex's Caves standalone compat implementation, invasive aquifer/cross-chunk flood repair, unsafe global cache lifecycle rewrites, and speculative terrain re-profiling. These need separate, reproducible tests before merging.
- The existing `1.0.2+1.21.1` mod version is intentionally preserved in the test branch for dependencies that pin the version.
