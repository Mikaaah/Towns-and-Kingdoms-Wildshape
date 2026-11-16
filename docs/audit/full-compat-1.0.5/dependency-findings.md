# Dependency and placement inventory — Wildshape 1.0.5 audit

This record reports static inspection of exact JAR bytes on 2026-11-16. It is not a dedicated-server or graphical-client certification. The source list contains named groups; expanding the eleven YUNG's mods yields 54 target entries. Integrated API (a native dependency) and the separately named Generator Accelerator 2 make 56 inspected artifacts. The selected versions are recorded in `compatibility-matrix.csv`, with immutable download URLs and SHA-256 hashes in `dependency-lock.json`. Most match the previously supplied TNK Beta 1.0 export; Naturalist, Archie's Dinosaurs, preview/performance tools and certain missing YUNG's modules are explicitly selected public audit releases, not a claim about what is installed on the user's server.

## Findings that change implementation choices

1. **Independent placement sets do not reserve clearance from one another.** Their salts select repeatable candidate positions within their own grids. Different salts do not prevent neighboring candidates from separate grids. Changing salts alone cannot enforce a radial cross-mod clearance.
2. **Repeated salts are not automatically faults.** Twelve groups repeat a salt; five repeat salt, spacing, separation and spread type. `epic:large_dungeons` and `epic:large_dungeons_obelisks` intentionally share salt `3456745`, spacing `61`, separation `60`. Moving one independently breaks the surface marker's relationship with its dungeon. SkyLands also has paired inside/outside archipelago sets and arena theme variants. They remain under SkyLands' native policy.
3. **Integrated replacements must remain replacements.** Seven distinct structure-set IDs have multiple resource providers. Six are `cataclysm:*` replacements from Integrated Cataclysm; `mowziesmobs:wrought_chambers` is provided by both Mowzie's Mobs and Integrated Mowzie's Mobs. These are not fourteen independent placements. Five of the seven repeated JSON definitions are identical; the ancient-factory and wrought-chamber replacements differ. Actual winning resources are determined when Minecraft loads the resource stack, not by inventory ordering.
4. **Placement subclass behavior matters.** The inspected `integrated_api:advanced_random_spread` class and `cataclysm:cataclysm_random_spread` class extend `RandomSpreadStructurePlacement`, but both override `isStructureChunk` and `getPotentialStructureChunk`. A hook only in the superclass implementation cannot assume it observes every overridden entry point. Native exclusion zones, frequencies, placement codecs and origin restrictions must survive.
5. **Several apparent buried/floating placements are authored.** Epic dungeon definitions deliberately specify absolute start heights (`-25`, `-15`, `-5`, `5`) and `terrain_adaptation: none`; their obelisks project to `WORLD_SURFACE_WG`. Cataclysm acropolis specifies absolute height `200`. SkyLands generates its own floating content. A generic surface-flatness or water exclusion rule across these records would remove intended content and create dead areas.
6. **Native probability and strict rejection have a distribution tradeoff.** Rejecting an unsuitable native candidate does not automatically move it elsewhere in that region. Candidate-only minimum-distance arbitration can reserve space for a candidate whose structure later fails native biome, pool, piece or terrain validation. Therefore a bounded deterministic filter can reduce clusters, but it cannot promise both exact native frequencies and an absence of empty regions. A generation-order-dependent registry of already generated starts would introduce a separate reproducibility/thread-safety problem.
7. **Feature-based content does not pass through structure-set placement.** YUNG's Bridges and Extras have biome modifiers/placed features and no structure sets in the inspected JARs. Their native feature-stage logic must be audited separately. Ores, crops, mob additions and custom biome features are likewise not repaired by structure-spacing JSON.
8. **A list of desired mods does not specify an installed dependency closure.** The inventory has 56 JARs but omits some transitive libraries because it is a static worldgen-resource inspection. It must not be copied directly into a server's `mods` folder or described as a successfully loaded full-pack fixture. Generator Accelerator 1 and 2 are alternatives whose APIs and mod IDs differ; they are recorded separately, not recommended for simultaneous installation.

## Exact result

- 56 JARs inspected, all SHA-256 identities matched.
- 159 structure-set resource providers, representing 152 distinct resource IDs.
- 313 structure definitions, 267 biome definitions, 1,090 biome tag resources and 706 biome modifiers.
- 118 resource IDs have more than one provider across all resource kinds; this includes appendable tags and intentional replacements.
- Zero unparsed selected worldgen JSON resources.
- Full live registry resolution, dependency closure, terrain/clipping surveys, distributions across seeds and full-pack TPS remain separate runtime checks.

## Correct identities

| Requested name | Actual mod ID / resource namespace |
| --- | --- |
| IDAS — Integrated Dungeons and Structures | `idas` |
| Integrated Dungeons Arise | mod `integrated_dungeons_arise`; structure sets `dungeons_arise:*` |
| Integrated Seven Seas | `integrated_seven_seas` |
| Integrated Mowzie's Mobs | `integrated_mowzies_mobs`; replacement structures `mowziesmobs:*` |
| Integrated Cataclysm | `integrated_cataclysm`; replacement structures `cataclysm:*` |
| Epic Dungeons | mod `mr_epic_structuresdungeons`; structures `epic:*` |
| Epic Witch Huts | mod `mr_epic_structureswitchhuts`; structures `epic:*` |
| Epic Temples | inspected supplied pack artifact is **Epic Structures: Jungle Temples**, mod `mr_epic_structuresjungletemples`; structures `epic:*` |
| Archie's Dinosaurs | `ec_ad`; 1.0.6 public audit artifact |
| Modern Apple Cows | `apple_cows` |
| Bosses'Rise | `block_factorys_bosses` |
| Flow Control | `streamcontrol` |
| Generator Accelerator 1 | `generator_accelerator` |
| Generator Accelerator 2 | `generatoraccelerator2` |

The removed Epic Structures: Villages mod must not be confused with the three retained Epic dungeon/temple/witch-hut modules. The older displayed “Archy's Dinosaurs” spelling was incorrect; the inspected project is **Archie's Dinosaurs**.

## Reproduction

```sh
python tools/full-compat-inventory.py \
  --download-dir /tmp/wildshape-pinned-deps \
  --output-dir build/full-compat-inventory \
  --download
```

Without `--download`, only existing local bytes are checked. The tool emits a nonzero result for missing/mismatched JARs or worldgen JSON parse failures. `.github/workflows/full-compat-inventory.yml` runs this static task independently from the build, on explicit dispatch or audit-branch changes to its script/lock. Its artifact includes reports only, excluding third-party JARs. `resource-inventory.json` is the detailed local output; compact CSVs and collision/summary reports are sufficient for ordinary review.

## Primary release sources for newly selected artifacts

- Naturalist 2.0.7: https://modrinth.com/mod/naturalist/version/9Yn8ig4c
- Archie's Dinosaurs 1.0.6: https://www.curseforge.com/minecraft/mc-mods/archies-dinosaurs/files/9611961
- World Preview NeoForged 1.3.1: https://modrinth.com/mod/world-preview-neoforged-unofficial/version/AQCgiv4K
- Bye?Pregen! 1.1.3.4: https://www.curseforge.com/minecraft/mc-mods/bye-pregen/files/9353269
- Generator Accelerator 1.6.2: https://www.curseforge.com/minecraft/mc-mods/generator-accelerator/files/8168434
- Generator Accelerator 2 2.0.21.259: https://www.curseforge.com/minecraft/mc-mods/generator-accelerator/files/9551894
- Thaumaturge 1.1.0: https://www.curseforge.com/minecraft/mc-mods/thaumaturge/files/9452543

These links establish artifact identity. The JAR metadata and JSON inspection establish the local observations above; marketplace claims do not establish Wildshape runtime compatibility.
