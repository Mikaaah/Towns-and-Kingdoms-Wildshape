# TNKTerraForged — Towns & Kingdoms 3 compatibility plan

Minecraft **1.21.1**; primary loader **NeoForge 21.1.x**. This is the pack-oriented fork based on UltraTerraForged, ReTerraForged, and TerraForged, not an official upstream update.

## Implemented in this preview

- **Generic protection of known structures:** Before carving a block, TNKTerraForged checks bounding boxes of valid structure starts recorded in the current chunk and protects the footprint plus a two-block safety margin. Works by vanilla structure API; no hard dependency on a dungeon mod. Structures *not yet recorded* at build-surface stage, structures starting outside this chunk, and structure pieces with inaccurate boxes are **not guaranteed protected**.
- **Prevent unintended global structure rejection:** the original `Structure.isValidBiome` mixin applied village-specific terrain restrictions to all structures. Disabled by default for this fork; reenable temporarily with `-Dtnkterraforged.legacyGlobalStructureRules=true`. Terrain-safe placement should eventually be a **targeted** per-structure policy, not a global veto.
- **Large caves:** existing configurable enormous caverns and passage connections are preserved. Cave-water fixes and optional protected Alex's Caves biomes are preserved.
- **Optional Alex's Caves NeoForge integration:** already authored as the TNK-owned compatibility module. Uses the port 2.0.10; don't include the separate tfacfix JAR at the same time.
- **Separate dimensions:** No TNK overrides to Twilight Forest, Amplified Nether/BetterNether or BetterEnd world generators.

## TNK3 mods to validate next

The names below come from TNK3 project mod-list documentation and the Alpha 1.0 project listing; **they do not guarantee installed JAR versions in the currently running instance**.

| Mod / group | Goal | Current status |
| --- | --- | --- |
| Integrated Dungeons & Structures (IDAS), Integrated Dungeons Arise | Large Create-heavy dungeons should not be cut by caves; sparse placement | Generic start protection implemented; spacing profiles pending verified structure-set IDs |
| Integrated Stronghold, Integrated Villages | Retain accessibility and foundations; don't reject sites globally | Generic start protection + global veto removed; multi-chunk validation pending |
| Integrated Cataclysm, Cataclysm, Integrated Mowzie's Mobs | Boss arenas should not intersect cavern carving or water unexpectedly | Generic start protection; verify arena footprint and spawn conditions |
| YUNG's Better Dungeons | Preserve underground dungeon pieces, spawners, corridors | Generic start protection; verify where starts are registered relative to surface stage |
| Alex's Caves port | Native biome placement and landmark chambers survive UTF terrain | NeoForge optional source integration exists; verify port version |
| Biomes O' Plenty, Oh The Biomes We've Gone, TerraBlender | Biomes retained across TNK's terrain/climate | Existing TerraBlender hooks retained; full pack integration test pending |
| Atmospheric, Environmental, Autumnity, Upgrade Aquatic, BloomingNature | Keep vegetation, aquatic ecosystems, vanilla-biome additions | Do not overwrite mod features; verify feature placement by biome |
| Twilight Forest, BetterEnd: New Dawn, BetterNether: New Dawn, Amplified Nether | Preserve other dimensions' own generation and structures | TNKTerraForged customization is Overworld-focused |
| ModernFix, FerriteCore, Lithium, C2ME, AllTheLeaks | Avoid memory contention, thread oversubscription and async gen hazards | Existing bounded worker pool; heap/JFR and pregeneration benchmarks pending |

## Avoid accidentally changing progression

- Do **not** bundle or transplant Integrated or YUNG structure NBT files into TNKTerraForged.
- Do **not** alter structure loot tables or inject extra Create machines; TNK3 should handle loot, difficulty and chapter progression with its existing scripts/mods.
- Large Create-rich dungeons may need *rarer* structure-set placement; implement this with a **TNK-owned datapack once exact registry keys and versioned structure-set files are verified**.
- Keep new integration classes optional and narrowly scoped; missing mods should not crash startup.

## Compatibility constraints

- **Only one terrain fork:** TNKTerraForged keeps the registry ID `ultraterraforged` for compatibility and **must replace**, not sit alongside, the original UltraTerraForged. FreeTerraForged is a separate terrain generator; do not expect its custom world type and TNK's world type to compose automatically.
- Minecraft **1.21.1**, Java **21**. Use the loader-specific JAR.
- Pack testing must compare seed and preset with a matching mod list, fresh chunks, a structure locator, server memory snapshots and repeated / reload tests.
- For structure protection troubleshooting, use JVM arg `-Dtnkterraforged.structureProtection=false` temporarily to isolate differences. This is not an in-game GUI setting.
- Original game IDs and preset files stay `ultraterraforged` until an explicit migration version is designed.
