<p align="center"><img src="https://i.imgur.com/0yL04dQ.png" alt="Towns & Kingdoms Wildshape" width="100%"></p>

<h1 align="center">Towns & Kingdoms Wildshape</h1>

<p align="center"><strong>Big landscapes. Bigger caverns. Terrain made for long journeys and underground discoveries.</strong></p>

<p align="center"><a href="https://www.curseforge.com/members/mikaaah/projects">Downloads</a> · <a href="https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/wiki">Player guide</a> · <a href="https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/issues">Report an issue</a> · <a href="https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/blob/audit/tnk3-compat-1.0.12/CHANGELOG.md">Changelog</a> · <a href="https://discord.gg/CRYk3uDPVt">Discord</a></p>

<p align="center"><img src="https://img.shields.io/badge/Minecraft-1.21.1-5b8731?style=flat-square" alt="Minecraft 1.21.1"> <img src="https://img.shields.io/badge/NeoForge-d77828?style=flat-square" alt="NeoForge"> <a href="https://mikaaah.github.io/TownsAndKingdoms/projects/"><img src="https://img.shields.io/badge/Towns_%26_Kingdoms-Modding_Hub-7f384a?style=flat-square" alt="Towns & Kingdoms Modding Hub"></a></p>




## Terrain and compatibility

- Large, connected caverns with lush, dripstone and lantern styles, rock supports and rare coastal coves.
- Terrain-aware river banks, coastlines and native Streams Reflowing currents, including explicit Flow Control overrides. NeoForge supports the verified Streams 2.15.2 and 2.16.2 APIs; Flow Control 0.2.2 uses 2.16.2.
- Native mod biome surfaces, decorations and creatures. Nature’s Spirit, BOP and Regions Unexplored keep their biome identities and materials.
- Protected native cave habitats, including Alex’s Caves, tagged third-party underground biomes and Deep Dark.
- Dungeon carving protection uses individual structure pieces and neighboring chunk references. Selected large Overworld dungeons receive explicit spacing rules; resource landmarks and other dimensions retain native placement.
- Habitat tag bridges connect actual TNK3 biomes to native dungeon and creature rules without duplicating spawn tables or changing their weights.
- Ores cannot become synthetic stone layers; Create’s decorative rocks remain eligible. Terrain caches have bounded retention and do not recycle arrays still held by readers.

The [November audit](docs/WILDSHAPE_AUDIT_2026-11-08.md) records findings, implementation and verification. The [compatibility matrix](docs/TNK3_COMPATIBILITY.md) links the exact dependency inventory, structure sets and all 264 entries from the latest retrievable TNK3 export. These records distinguish static inspection, focused tests and runtime evidence.

## Installation

Install **one NeoForge 1.21.1 Wildshape JAR**. Remove the previous terrain JAR and superseded standalone adapters `utfstreams`, `tfacfix` and `utfoptimizer`; their relevant fixes are integrated. **Keep Flow Control** when using its tools. Optional mods remain separate installations.

Select **Wildshape** in Create World. On a dedicated server use:

```properties
level-type=wildshape:wildshape
```

The FML mod ID and resource namespace are **`wildshape`**. Configuration is written to **`config/wildshape/`**. Missing settings and custom presets are imported from the previous directories without replacing current settings or altering the originals. New share codes use `WILDSHAPE1:`; previously shared codes still import. Commands use `/wildshape`, with `/wildshape streams status` when Streams is installed.

**This version changes registry identity. Use a fresh world.** Existing saves and exported datapacks referring to the previous namespace require a separate migration; copying settings does not migrate a world's registries. The mod does not rewrite existing worlds. See [identity and migration details](docs/WILDSHAPE_IDENTITY.md).

| Build | Minecraft | Java | Scope |
|---|---|---|---|
| NeoForge | 1.21.1 | 21 | Primary TNK3 build; native Alex’s Caves and actual-water current corrections |

## Preserved TNK3 decisions

The [24 excluded BOP/RU biomes](docs/TNK3_BIOME_EXCLUSIONS.md), BOP Grassland, Cherry Grove promotion, enormous cavern geometry, native mod surfaces and gentle river transitions remain. Artificial underground water tunnels remain disabled. Other dimensions keep their own generators. Recipes, quest progression, loot and boss access stay with the pack’s existing scripts and mods.

See [upstream comparison](docs/UPSTREAM_1.0.3_COMPARISON.md), [Streams details](docs/TNK3_STREAMS_REFLOWING.md), [cavern and surface notes](docs/TNK_CAVERNS_BIOMES_1.0.7.md) and [structure protection details](docs/TNK_STRUCTURE_COMPAT_1.0.8.md).

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
