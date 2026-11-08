# Wildshape — TNK3 compatibility

Minecraft **1.21.1**, Java **21**. NeoForge is the TNK3 target. This describes **1.0.12-beta.1**, based on the latest terrain branch, 1.0.11, with current main-branch branding and documentation preserved.

## Evidence

The latest retrievable pack export has 264 entries (258 CurseForge manifest entries and six bundled JARs). The 25 October TNK3 launch log and November’s 20 uploaded JARs supplement it. The newer Planescape log is unrelated and excluded. This is an evidence-based inventory, not a claim that every November installation is identical.

- [Full 264-entry disposition](audit/tnk3-mod-compatibility.csv)
- [Inspected dependency versions and SHA-256 hashes](audit/verified-dependencies.csv)
- [Verified structure-set resources](audit/verified-structure-sets.csv)
- [Findings and validation record](WILDSHAPE_AUDIT_2026-11-08.md)

## Integration matrix

| Mods / area | Implemented policy | Practical limits |
|---|---|---|
| BOP, Regions Unexplored, Nature’s Spirit, TerraBlender | Native biome identities, materials, decorations and spawn settings; existing exclusions and surface adapters preserved | Biome distribution follows the selected preset and installed biome regions |
| Alex’s Caves 2.0.10 | Existing native climate adapter; cave carving preserves its biome cells; acid pits and other landmarks keep native spacing | Requires its normal dependencies; remove standalone `tfacfix` |
| RU/BOP and other tagged cave biomes; Deep Dark | Native `c:is_cave` / `c:is_underground` habitats protected; extensible `ultraterraforged:protected_cave_biomes` tag | Untagged third-party underground biomes require an explicit entry; ordinary vanilla lush/dripstone remain available for authored cavern styles |
| IDAS / Integrated Dungeons Arise / Integrated Villages | Explicit spacing for verified sets; native templates, weights and loot retained | Runtime placement and biome availability still come from those mods |
| Integrated API | Append modern common habitat tags to its legacy biome collections | Optional tags cannot create an absent biome |
| Integrated Stronghold / Cataclysm / Mowzie’s / Seven Seas | Native registry replacements retained; known structure pieces protected during cavern carving | No copied templates or global boss spacing rewrite |
| YUNG structures | Explicit Overworld spacing for verified dungeon/temple/hut/monument sets; mineshaft, stronghold, Nether and End placement retained | Feature-placed geometry and inaccurate piece boxes cannot be protected universally |
| Cataclysm | Desert, snowy-plains and explicit deep-ocean habitat bridges; native arenas, mob weights and predicates | Alex’s Caves’ abyssal habitat is not broadly admitted into ordinary ocean tags |
| Alex’s Mobs 1.22.17 | Modern common tags bridged into its five verified legacy habitat tags | Native placement checks and configuration still determine actual spawns |
| Hybrid Aquatic / HAPI | RU deep ocean/bayou/fens and Nature’s Spirit marsh, tropical rivers and beaches added to matching native habitat tags | Spawn frequency, group size and water requirements remain native; RU reefs retain their coral classification |
| Starcatcher / Bosses’Rise / Legendary Monsters | Verified fishing and arena habitat tags extended to matching modded biomes; native encounter rules retained | No automatic global boss density change |
| Knight Quest / Legendary Monsters fixed spawn lists | Read installed native spawn modifiers; extend to matching modded habitats during ADD, avoiding existing entries | Later REMOVE rules retain priority; native weights/groups and placement predicates remain |
| WilderNature | Plains, desert and forest habitat bridges to native creature tags | No duplicate biome modifier or replacement spawn table |
| BloomingNature / Alpine Whispers / Autumnity / Upgrade Aquatic | Native vanilla-biome modifications retained | No global habitat or feature replacement |
| Ars / Iron’s Spells / Apotheosis | Native mobs, features and loot; selected Iron’s/Apotheosis Overworld tower spacing only | Gameplay, progression and loot stay with the pack |
| AE2 / Create / Immersive Engineering / modded ores | Meteorites and ore modifiers retain native placement; ore/storage blocks excluded from decorative strata; Create stone remains eligible | This does not modify ore yields or recipes |
| Streams Reflowing 2.15.2 or 2.16.2 / Flow Control | Version-specific native-current and loaded-water adapters; Flow Control 0.2.2 uses Streams 2.16.2; manual overrides respected | Remove old `utfstreams`; retain Flow Control; visual/boat behavior requires client playtesting |
| Nether / End / Twilight / Aether / other dimensions | Their generator ownership retained; automatic dungeon spacing targets only the explicit Overworld list | Does not promise new compatibility with untested custom dimension generators |
| Storage, UI, combat and utility mods | No invented worldgen integration; optional dependencies remain optional | Inventory presence alone is not a runtime certification |

Habitat bridges are append-only and optional. They are exported with the Wildshape world preset, so unrelated vanilla worlds are not given this pack’s habitat rules.

## Structure rules

Wildshape no longer increases the spacing of every modded random-spread set. The selected minimum spacings are defined in [`StructureSetPolicy`](../common/src/main/java/com/pandaismyname1/ultraterraforged/data/preset/StructureSetPolicy.java). Native larger spacings are retained. Explicit saved preset structure overrides take priority. Placement salts, weights, exclusion zones and custom placement types are retained.

Resource landmarks such as **AE2 meteorites**, **Alex’s Caves acid pits**, aquatic whale falls and Nether/End structures keep their own spacing. This matters because worldgen registries are shared across dimensions.

Carving protection follows individual structure pieces and neighboring chunk references with a four-block margin. It avoids carving through known dungeon walls without shielding a huge mineshaft’s entire aggregate volume. Structures that do not provide pieces at that generation stage, feature-generated buildings and inaccurate boxes remain limitations. No Structure Overlap retains its own checks.

## Extending native cave protection

Append entries or optional references to this biome tag in a datapack:

```json
{
  "replace": false,
  "values": [
    {"id": "example:native_cave", "required": false}
  ]
}
```

Path: `data/ultraterraforged/tags/worldgen/biome/protected_cave_biomes.json`.

## Installation and migration

Install one matching loader JAR. Remove superseded `utfstreams`, `tfacfix` and `utfoptimizer`; do not remove Flow Control. Internal IDs and `config/tnkterraforged/` remain stable. Use a fresh world to evaluate this beta: saved world preset datapacks can retain earlier structure/tag registries, and existing chunks do not regenerate. New chunks can have seams after terrain fixes.

Automated checks and dedicated-server evidence are recorded in the audit. Exhaustive visual checks of every dungeon, mob habitat, seed, custom preset and full client pack remain separate work; a clean compile is not evidence of those outcomes.
