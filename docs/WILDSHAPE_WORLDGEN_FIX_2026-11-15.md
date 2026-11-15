# Wildshape 1.0.2 — worldgen and Streams audit

Minecraft 1.21.1 / NeoForge / Java 21. Based on maintained Wildshape 1.0.1 at `88d7f9839e3376fc9a00de25b227d9bac959a46f`, preserving the earlier world-creation, biome, dungeon, cave and current integrations. Legacy `main` was not used as the implementation baseline.

## Findings and changes

| Report | Finding | Change |
| --- | --- | --- |
| [TNK3 issue #158](https://github.com/Mikaaah/TownsAndKingdoms/issues/158) | Cache eviction sorts access timestamps that other threads update. A concurrent-reader regression reproduces the reported TimSort contract exception. | Read each eligible timestamp once, sort immutable candidates and remove the selected entries by identity. Published tile contents remain usable and are never closed by eviction. |
| Water columns and irregular river cuts | The Streams guard rejects removal of every existing fluid, including Streams' own water and obsolete sources outside Wildshape's planned waterline. | Limit protection to native above-sea water columns and their real beds. Memoize columns for one feature placement and restore the scope after nested calls and exceptions. `WorldGenRegion.getHeight` returns the first free block; subtract one to locate the solid bed. |
| Floating cave clay, water and lava | A custom cave cut preserves every pre-existing fluid and the single block immediately below it. An intersected underground aquifer can survive as a suspended pool and bed. | Let the cut remove ordinary underground water/lava and their beds inside its geometry. Keep surface-water, authored-lake, waterlogged-block, structure and protected-cave safeguards. Schedule exposed water and lava for normal fluid updates. |
| Missing surface soil | The fallback's near-ground condition uses predicted terrain cells. Density shaping and structure adjustment can put actual ground outside that band. | Read the generation chunk's solid surface height. Soil checks reach actual land and sea floors while excluding cave floors below an intact roof. Native biome material rules retain priority. |
| Brown Woody Highlands | Nature's Spirit 2.3.2 explicitly uses terracotta/badlands bands in `natures_spirit:woody_highlands`; this is not sufficient evidence of missing grass. | Preserve its native bands, chert and other materials. Do not replace all highland surfaces with grass. |
| Updated Streams Reflowing | Wildshape 1.0.1's loader range and actual-water profile selection stop at 2.17.1. | Add inspected 2.17.3 and 2.17.4 profiles and the corresponding optional loader range. Retain 2.15.2, 2.16.2 and 2.17.1. Unknown releases cannot activate legacy actual-water hooks by falling through a version check. |

The new release does not rename registries, replace the terrain preset, add hard dependencies or produce a Fabric edition. Generation corrections affect new chunks; existing saved terrain is not rewritten.

## Dependency inspection

The actual NeoForge 1.21.1 Streams 2.17.1, 2.17.3 and 2.17.4 JARs were downloaded from the project's Modrinth API and checked against its published SHA-512 hashes. Inspection covers the layer constructor/producer, grid entry points, platform settings, both registered stream fluid classes and client particle target. The layer/producer API remains compatible; the inspected modern entry points keep their signatures. No third-party code or dependency JAR is bundled.

Nature's Spirit is the pack's verified 2.3.2 JAR, CurseForge file 9326605. Its native surface rules were inspected directly. The concurrency fixture uses C2ME `0.4.0-alpha.0.124+1.21.1` and an installed NeoForge 21.1.266 server, matching the issue's relevant versions. Exact files and fingerprints are recorded with the audit evidence.

## Validation

- **55 focused tests passed**, including the reproduced cache race, immutable timestamp sampling, identity-safe replacement, retained tile data, cave aquifer intersections, water/lava updates, surface-water and waterlogged-block protection, Streams ownership/scoping, actual ground height, protected cave biomes, giant-cavern supports, water continuity and tagged custom structure export.
- Complete built-in preset export, preset identity, structure preference, optional habitat, biome exclusion and Mixin annotation regression selections also passed.
- NeoForge build, 25 pure current/search checks and `tnkReleaseCheck` passed. Release preparation verified clean source commit `ac44f923b79d666975b766e854c98d2325fef766`.
- The exact packaged JAR passed installed NeoForge **21.1.266** generation, clean save and restart/reload checks with **Streams 2.17.4 + C2ME 0.124 + Nature's Spirit 2.3.2**, and with Wildshape alone. Both reported chunks `[-92,-238]`, `[-114,-224]` and screenshot-coordinate chunk `[-7,-24]` reached FULL. Direct region/NBT inspection confirmed `minecraft:full`; saved section palettes matched after reload.
- The live Streams check passed settings bindings, all eight headings, tile-free distant surveys, elevated native water, the public flow grid and server physics. The sampled river was at `(2085,75,-4528)`.
- Fixtures used the normal 64-tile cache limit and Streams MEDIUM / 768-block watersheds. The combined fixture disables the watchdog while its synchronous startup hook requests several distant chunks; this is a test setting, not a pack configuration change or a performance certification.

See [runtime summary](audit/worldgen-1.0.2/runtime-summary.json), [build verification](audit/worldgen-1.0.2/build-verification.json), [before-fix reproductions](audit/worldgen-1.0.2/regression-before.json), [focused results](audit/worldgen-1.0.2/regression-after.json) and [dependency fingerprints](audit/worldgen-1.0.2/inspected-dependencies.json).

## Artifact and limits

`wildshape-1.0.2-neoforge-1.21.1.jar`

SHA-256: `d26e23ec016b9d6f94a700bdc4d09ebf3bd64027b8b46295a450cb8add49339d`

The screenshot seed and exact preset were not supplied in issue #158. The isolated fixture uses seed `2031102026`; generating the reported chunk coordinates there is not the same as replaying the original server world. The geometry tests reproduce specific failure mechanisms, not the exact screenshots. Full-pack graphical inspection and a backup-copy replay of the original seed/preset remain separate verification steps. The issue should not be marked fully verified solely from these isolated checks.
