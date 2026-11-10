# Wildshape — TNK3 compatibility

Minecraft **1.21.1**, NeoForge **21.1.262**, Java **21**. This describes **1.0.0**, preserving the renamed Wildshape identity and previously audited terrain/compatibility work.

## Evidence

The supplied **Beta 1.0** export contains 315 CurseForge entries and three bundled JARs. It includes resource packs as inventory entries, so this is not a loaded-mod count. Its bundled terrain JAR is the older beta.1 binary; the release uses the newer renamed source. Actual exported dependency metadata, hashes and worldgen resources were inspected before updating habitats. Version aliases, including Flow Control’s public 0.2.0 label versus embedded 0.2.2, are matched from bytes.

- [Current export disposition](audit/beta-1.0/pack-inventory.csv)
- [Current exact dependency hashes](audit/beta-1.0/verified-dependencies.csv)
- [Beta 1.0 follow-up findings](WILDSHAPE_BETA_1_AUDIT_2026-11-10.md)
- [Historical initial audit](WILDSHAPE_AUDIT_2026-11-08.md)

## Integration matrix

| Mods / area | Implemented policy | Practical limits |
|---|---|---|
| BOP, Regions Unexplored, Nature’s Spirit, TerraBlender | Native biome identities, materials, decorations and spawn settings; existing exclusions and surface adapters preserved | Biome distribution follows the selected preset and installed biome regions |
| Alex’s Caves 2.0.10 | Existing native climate adapter; cave carving preserves its biome cells; acid pits and other landmarks keep native spacing | Requires its normal dependencies; remove standalone `tfacfix` |
| RU/BOP and other tagged cave biomes; Deep Dark | Native `c:is_cave` / `c:is_underground` habitats protected; extensible `wildshape:protected_cave_biomes` tag | Untagged third-party underground biomes require an explicit entry; ordinary vanilla lush/dripstone remain available for authored cavern styles |
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
| Streams Reflowing 2.15.2 / 2.16.2 / 2.17.1 and Flow Control | Version-specific native-current and loaded-water adapters; Flow Control 0.2.2 uses Streams 2.16.2 and Flow Control 1.0.0 uses Streams 2.17.1; manual overrides respected; terrain surveys avoid full tile generation | Remove old `utfstreams`; retain Flow Control; visual/boat behavior requires client playtesting |
| Nether / End / Twilight / Aether / Otherside / other dimensions | Their generator ownership retained; automatic dungeon spacing targets only the explicit Overworld list | Does not promise new compatibility with untested custom dimension generators |
| Epic Structures: Dungeons / Witch Huts / Jungle Temples | Verified matching RU/Nature’s Spirit habitats; native dungeon/obelisk pairing, spacing, templates and loot retained | Witch-hut common tags already cover the new swamps; no blanket spacing increase |
| Croptopia | 85 crop/tree habitat extensions into explicit RU/Nature’s Spirit counterparts of the native vanilla selectors | Native feature modifiers and placement rates retained; no habitat additions to protected caves or other dimensions |
| Modern Apple Cows | Native common-tag spawns retained; enchanted variant recognizes Nature’s Spirit tropical woods | Native rarity, spawn checks and weights retained |
| Deeper and Darker / Jaden’s Nether Expansion | Their dimension generators, terrain and surfaces retain native ownership | No Overworld terrain context, density override or progression lock introduced |
| Storage, UI, combat and utility mods | No invented worldgen integration; optional dependencies remain optional | Inventory presence alone is not a runtime certification |

Habitat bridges are append-only and optional. They are exported with the Wildshape world preset, so unrelated vanilla worlds are not given this pack’s habitat rules.

## Structure rules

Wildshape no longer increases the spacing of every modded random-spread set. The selected minimum spacings are defined in [`StructureSetPolicy`](../common/src/main/java/nl/townsandkingdoms/wildshape/data/preset/StructureSetPolicy.java). Native larger spacings are retained. Explicit saved preset structure overrides take priority. Placement salts, weights, exclusion zones and custom placement types are retained. Dedicated servers bind these changes while world registry resources load, so modded sets absent from the initial vanilla-only export receive the same policy.

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

Path: `data/wildshape/tags/worldgen/biome/protected_cave_biomes.json`.

## Installation and migration

Install one NeoForge 1.21.1 JAR. Remove the previous terrain JAR and superseded `utfstreams`, `tfacfix` and `utfoptimizer`; keep Flow Control. Version 1.0.0 retains `wildshape` for the FML ID, resource namespace, configuration folder and world type. Legacy settings import without overwriting current settings. Use a fresh world: old saves and exported world datapacks require a separate registry migration. See [identity migration](WILDSHAPE_IDENTITY.md).

The release build, 54 regressions and three completed dedicated-server fixtures passed. The focused exported HIGH-accuracy Streams run reached the native habitat assertions but exceeded its total time budget during later current planning; the larger combined fixture was interrupted. These results and exact scope are recorded in the audit. Exhaustive visual checks of every dungeon, mob habitat, seed, custom preset and full client pack remain separate work; a clean compile is not evidence of those outcomes.
