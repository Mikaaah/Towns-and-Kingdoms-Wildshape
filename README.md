<p align="center"><img src="https://i.imgur.com/0yL04dQ.png" alt="Towns & Kingdoms Wildshape" width="100%"></p>

<h1 align="center">Towns & Kingdoms Wildshape</h1>

<p align="center"><strong>Big landscapes. Bigger caverns. Terrain made for long journeys and underground discoveries.</strong></p>

<p align="center"><a href="https://www.curseforge.com/members/mikaaah/projects">Downloads</a> · <a href="https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/wiki">Player guide</a> · <a href="https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/issues">Report an issue</a> · <a href="https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/CHANGELOG.md">Changelog</a> · <a href="https://discord.gg/CRYk3uDPVt">Discord</a></p>

<p align="center"><img src="https://img.shields.io/badge/Minecraft-1.21.1-5b8731?style=flat-square" alt="Minecraft 1.21.1"> <img src="https://img.shields.io/badge/NeoForge-d77828?style=flat-square" alt="NeoForge"> <a href="https://mikaaah.github.io/TownsAndKingdoms/projects/"><img src="https://img.shields.io/badge/Towns_%26_Kingdoms-Modding_Hub-7f384a?style=flat-square" alt="Towns & Kingdoms Modding Hub"></a></p>

**Audit preview: Wildshape 1.0.5-beta.1 · Minecraft 1.21.1 · NeoForge · Java 21.**

This branch adds configurable cross-mod structure screening, current habitat bridges and concurrency fixes. See the [engineering report](docs/WORLDGEN_AUDIT_1.0.5.md) for scope, configuration, verification and remaining limits. This preview has not been certified against the complete live server pack.

Wildshape is Mikaaah’s maintained fork of UltraTerraForged, ReTerraForged and TerraForged. Upstream authors and licenses remain credited below.

The maintained Wildshape source is on [`main`](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/tree/main). Builds target **NeoForge 1.21.1 and Java 21**.

## Terrain and compatibility

- Large, connected caverns with lush, dripstone and lantern styles, rock supports and rare coastal coves.
- Terrain-aware river banks, coastlines and native Streams Reflowing currents, including explicit Flow Control overrides. NeoForge contains inspected bindings for Streams 2.15.2, 2.16.2, 2.17.1, 2.17.3 and 2.17.4; optional mods remain separate installations.
- Native mod biome surfaces, decorations and creatures. Nature’s Spirit, BOP and Regions Unexplored keep their biome identities and materials.
- Protected native cave habitats, including Alex’s Caves, tagged third-party underground biomes and Deep Dark.
- Dungeon carving protection uses individual structure pieces and neighboring chunk references. Selected large Overworld dungeons receive explicit spacing rules; resource landmarks and other dimensions retain native placement.
- Habitat tag bridges connect actual TNK3 biomes to native dungeon and creature rules without duplicating spawn tables or changing their weights.
- Ores cannot become synthetic stone layers; Create’s decorative rocks remain eligible. Terrain caches have bounded retention and do not recycle arrays still held by readers.
- Optional [SkyLands 0.9.4 compatibility](docs/SKYLANDS_COMPATIBILITY.md) expands Overworld build space for native sky islands, including existing Wildshape saves. The saved terrain preset and noise height stay intact. Islands appear in new chunks, including beyond any pregenerated area.

The [Beta 1.0 follow-up](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/WILDSHAPE_BETA_1_AUDIT_2026-11-10.md) compares the new pack export, while the [initial audit](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/WILDSHAPE_AUDIT_2026-11-08.md) retains the original findings and evidence. The [compatibility matrix](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/TNK3_COMPATIBILITY.md) links the exact dependency inventory, structure sets and the current Beta 1.0 export’s 318 entries. These records distinguish static inspection, focused tests and runtime evidence.

World-creation fix in **1.0.1**: custom structure placements keep their native tags during preset export; see the [fix and regression evidence](docs/WILDSHAPE_WORLD_CREATION_FIX_2026-11-11.md).

Worldgen fixes in **1.0.2**: stable concurrent cache eviction, underground aquifer cleanup, scoped Streams water protection and actual-ground surface checks. See the [changelog](CHANGELOG.md) and [audit with save/reload evidence](docs/WILDSHAPE_WORLDGEN_FIX_2026-11-15.md). Generation corrections affect new chunks; saved terrain is not rewritten. Nature's Spirit's native highland terracotta remains part of those biomes.

## Installation

Install **one NeoForge 1.21.1 Wildshape JAR**. Remove the previous terrain JAR and superseded standalone adapters `utfstreams`, `tfacfix` and `utfoptimizer`; their relevant fixes are integrated. **Keep Flow Control** when using its tools. Optional mods remain separate installations.

Select **Wildshape** in Create World. On a dedicated server use:

```properties
level-type=wildshape:wildshape
```

The FML mod ID and resource namespace are **`wildshape`**. Configuration is written to **`config/wildshape/`**. Missing settings and custom presets are imported from the previous directories without replacing current settings or altering the originals. New share codes use `WILDSHAPE1:`; previously shared codes still import. Commands use `/wildshape`, with `/wildshape streams status` when Streams is installed.

**Worlds from the former `ultraterraforged` namespace require a fresh world or separate migration.** Wildshape 1.0.3 keeps the registry identity used by 1.0.0–1.0.2. Existing saves and exported datapacks referring to the previous namespace require a separate migration; copying settings does not migrate a world's registries. The mod does not rewrite existing worlds. See [identity and migration details](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/WILDSHAPE_IDENTITY.md).

| Build | Minecraft | Java | Scope |
|---|---|---|---|
| NeoForge | 1.21.1 | 21 | Primary TNK3 build; native Alex’s Caves and actual-water current corrections |

## Preserved TNK3 decisions

The [24 excluded BOP/RU biomes](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/TNK3_BIOME_EXCLUSIONS.md), BOP Grassland, Cherry Grove promotion, enormous cavern geometry, native mod surfaces and gentle river transitions remain. Artificial underground water tunnels remain disabled. Other dimensions keep their own generators. Recipes, quest progression, loot and boss access stay with the pack’s existing scripts and mods.

See [upstream comparison](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/UPSTREAM_1.0.3_COMPARISON.md), [Streams details](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/TNK3_STREAMS_REFLOWING.md), [cavern and surface notes](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/TNK_CAVERNS_BIOMES_1.0.7.md) and [structure protection details](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/main/docs/TNK_STRUCTURE_COMPAT_1.0.8.md).

## Authors and licensing

- **Wildshape / TNK integration:** Mikaaah / Towns & Kingdoms
- **UltraTerraForged:** [pandaismyname1](https://github.com/Pandaismyname1/ultraterraforged)
- **ReTerraForged:** [raccoonman](https://github.com/racoonman2/ReTerraForged) and contributors
- **TerraForged:** [dags](https://github.com/TerraForged/TerraForged), Won-Ton and contributors

Original MIT copyright notices are retained in [LICENSE](LICENSE). The NeoForge distribution retains GPL-3.0-licensed compatibility code; see [LICENSE-ALEX-COMPAT-GPL-3.0](LICENSE-ALEX-COMPAT-GPL-3.0). Optional third-party mods and dungeon templates are not bundled.
## Pull up a chair

Questions, builds, feedback, server news and the occasional wildly overambitious factory: join the **Towns & Kingdoms Modding Hub**.

[![Join the Towns & Kingdoms Discord](https://i.imgur.com/Ojgcccm.png)](https://discord.gg/CRYk3uDPVt)

**[Join Discord](https://discord.gg/CRYk3uDPVt)** · [Explore all projects](https://mikaaah.github.io/TownsAndKingdoms/projects/) · [CurseForge projects](https://www.curseforge.com/members/mikaaah/projects)
