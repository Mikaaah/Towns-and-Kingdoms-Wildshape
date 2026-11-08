# Towns & Kingdoms: Wildshape

Expansive terrain, enormous connected caverns and mod-aware exploration for **Towns & Kingdoms 3 | Beyond the Forge**.

Wildshape is Mikaaah’s maintained Minecraft **1.21.1 / Java 21** fork of UltraTerraForged, ReTerraForged and TerraForged. Original authors, history and notices remain credited below. The current development build is **1.0.12-beta.1**; NeoForge is the TNK3 target.

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

Use the JAR for your loader and Minecraft version. Install **one** Wildshape/UltraTerraForged terrain JAR. Remove the superseded standalone adapters `utfstreams`, `tfacfix` and `utfoptimizer`; their relevant fixes are integrated. **Keep Flow Control** when using its tools. Optional mods remain separate installations.

Select **Wildshape** in Create World. On a dedicated server use:

```properties
level-type=ultraterraforged:ultraterraforged
```

Configuration remains in `config/tnkterraforged/`; existing preset names and the `ultraterraforged` mod ID/registry namespace remain stable. Missing settings are imported from legacy configuration directories without replacing current settings.

Use a fresh test world for this beta. Changes affect new chunks; existing chunks retain their terrain and can meet new terrain at seams. Existing saved world datapacks can retain earlier structure/tag rules, so replacing a JAR does not necessarily migrate their registries. Back up worlds before upgrading.

| Build | Minecraft | Java | Scope |
|---|---|---|---|
| NeoForge | 1.21.1 | 21 | Primary TNK3 build; native Alex’s Caves and actual-water current corrections |
| Fabric | 1.21.1 | 21 | Shared terrain core and native Streams producer; separate Fabric runtime validation required |

## Development

```sh
./gradlew :common:test :neoforge:build :fabric:build -Putf.perfMods=false
```

Set `RELEASE=1` for versioned release filenames. Optional performance mods are excluded from this build command so they are not silently made requirements. See [development notes](docs/DEVELOPMENT.md), [changelog](CHANGELOG.md) and the [JAR inventory utility](tools/audit-compat-jars.py).

The opt-in dedicated-server check is enabled with JVM argument `-Dwildshape.compatSmoke=true` (or `-Ptnk.compatSmoke=true` for the NeoForge development run). It checks live registries, habitats, dimension ownership and generation, writes `wildshape-compat-results.json`, then stops the server. Leave this argument out for normal play.

## Preserved TNK3 decisions

The [24 excluded BOP/RU biomes](docs/TNK3_BIOME_EXCLUSIONS.md), BOP Grassland, Cherry Grove promotion, enormous cavern geometry, native mod surfaces and gentle river transitions remain. Artificial underground water tunnels remain disabled. Other dimensions keep their own generators. Recipes, quest progression, loot and boss access stay with the pack’s existing scripts and mods.

See [upstream comparison](docs/UPSTREAM_1.0.3_COMPARISON.md), [Streams details](docs/TNK3_STREAMS_REFLOWING.md), [cavern and surface notes](docs/TNK_CAVERNS_BIOMES_1.0.7.md) and [structure protection details](docs/TNK_STRUCTURE_COMPAT_1.0.8.md).

## Authors and licensing

- **Wildshape / TNK integration:** Mikaaah / Towns & Kingdoms
- **UltraTerraForged:** [pandaismyname1](https://github.com/Pandaismyname1/ultraterraforged)
- **ReTerraForged:** [raccoonman](https://github.com/racoonman2/ReTerraForged) and contributors
- **TerraForged:** [dags](https://github.com/TerraForged/TerraForged), Won-Ton and contributors

Original MIT copyright notices are retained in [LICENSE](LICENSE). The NeoForge distribution retains GPL-3.0-licensed compatibility code; see [LICENSE-ALEX-COMPAT-GPL-3.0](LICENSE-ALEX-COMPAT-GPL-3.0). Optional third-party mods and dungeon templates are not bundled.
