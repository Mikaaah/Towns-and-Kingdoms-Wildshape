# Upstream comparison — TNKTerraForged 1.0.11

Compared on 26 October 2026:

- TNK baseline: `7dbd54533b205fa1f350f9e8c2f81de5ad284b08` (1.0.10).
- Upstream `1.21.1`: `c0a12700e34bcf7e940ac248a4450a8e6225c2c3` (1.0.3).
- Shared source base: upstream `59bee7f` and TNK import `e7c6fd8` have identical trees (`6604f08c025b7ab32ab0f1c85143e977be946bb7`). The update applies changes since that source base, rather than replacing TNK with upstream.

| Area | Upstream change | TNK result |
| --- | --- | --- |
| River junctions | `c88a685`: lift tributary approaches to parent water level where banks permit; upstream measured forks arriving 3–18 blocks too low. | Imported, then run TNK mouth smoothing. Retain one-block terraces, finished drainage-ground sampling, and bounded 3–6-block drops per reach. This addresses both low tributaries and abrupt drops; it does not prohibit all natural waterfalls. |
| Cave water | `1992aab`, `2e08693`: protect ground beneath fluids and guard raised water against vanilla/modded carvers. | Imported; retain TNK prevention of later pools filling already open cavern air and its authored-fluid tracking. |
| Giant caverns | Upstream fixed lake pillars but retains smaller, generic noise columns. | Keep TNK's much larger caverns, Y30 roof cap, curved supports, decoration styles, clipped corridor iteration, and per-piece structure protection. TNK already preserves pillar foundations through lakes. |
| Alex's Caves | `2a39561`, `7ebc1c5`: native climate distribution adaptation and NeoForge-compatible injection selectors. | Use upstream's native integration. Remove the duplicate older NeoForge placement engine. Keep TNK cave-biome protection and copy terrain activation into chunk-local climate samplers. |
| Streams | `b54b4cc`: native current layers through Streams' API; `23c0a97`, `65da8dd`: preserve water and allow Streams to seal beds. | Use one upstream native-current producer. Keep TNK's bounded river snapshots and avoid blocking terrain generation for current queries. Remove old grid composition and blanket stream-feature cancellation. Retain NeoForge actual-water corrections, manual-override handling and diagnostics. |
| Structures | `304cae8`: rules explicitly name their target structures. | Replace TNK's temporary disabled global veto with targeted rules; default legacy rules target villages. Retain TNK spacing adjustments and piece-level cave protection. |
| Biomes and surfaces | Upstream does not include TNK's pack-specific changes. | Retain all 24 exclusions, BOP Grassland, Cherry Grove promotion, Nature's Spirit surface adaptation, mushroom islands, stony shore relief, ocean climate repairs and river/ocean separation. |
| Canyon walls | `3952767`: constrain narrow canyon depth by distance from rim. | Imported, retaining TNK drainage sampling. |
| Performance and caches | `94835a7`, `4e4e915`, `52179ba`: independent cache lifetimes, world-keyed cell lookup, duplicate chunk-drop handling, per-world preview culling. | Combine with existing TNK failure recovery, worker limits, non-expiring in-flight tiles and cache invalidation on failed lookups. Keep the used LazyCallable implementation. |
| Other compatibility | Bye Pregen, Generator Accelerator, Experimental continents, Fabric decoration ordering. | Imported along with upstream tests and the vendored compile-only MixinSquared annotation dependency. |

## Installation and scope

Use the replacement TNK JAR; do not install the original UltraTerraForged alongside it. Streams Reflowing and Alex's Caves/Citadel remain separate optional mods. Remove old standalone `utfstreams` and `tfacfix` adapters. The existing TNK biome, surface and terrain configuration remains active. Native Streams speed now follows upstream's defaults; TNK's additional actual-water correction settings remain in `config/tnkterraforged/streams.toml`.

Terrain changes affect new chunks. Existing chunks are not rewritten. Automated checks cover selected river junctions, cave water, mod surfaces, biome selection, optional-mod startup and current grids; they do not establish that every seed and full TNK3 structure combination is free of artifacts.
