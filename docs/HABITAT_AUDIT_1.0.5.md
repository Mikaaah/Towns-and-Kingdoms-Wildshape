# Wildshape 1.0.5 beta habitat and feature audit

This is an implementation and static-resource audit, not a claim that every version combination or the full TNK server has passed a graphical/runtime test. Dependency metadata and hashes are recorded in the accompanying full compatibility inventory. The 1.0.4 saved terrain/preset identity remains the baseline.

## Confirmed gaps corrected

| Gap | Evidence | Correction |
| --- | --- | --- |
| Blooming Nature's biome climate tags do not cover the scoped selectors used by Farmer's Delight | Blooming Nature 1.1.12 provides `c:is_hot` / `c:is_wet`; Farmer's Delight 1.3.4 consumes `c:is_hot/overworld` / `c:is_wet/overworld` | Add optional exact Blooming surface IDs to the scoped common tags. Keep native temperatures, exclusions and feature rates. |
| Croptopia's RU/Nature's Spirit habitat extensions omitted Blooming Nature | Current preset habitat family tables | Add verified Blooming plains/forest/aspen/flower/taiga/swamp/desert/savanna IDs to the existing families. Native crop/tree modifiers remain the only feature providers. |
| Blooming Nature's lily/pampas/fern/vine selectors miss appropriate modded habitats | Native selectors restrict to vanilla IDs or older Minecraft families | Append suitable known surface equivalents/common jungle and savanna tags. Never duplicate the feature modifiers. |
| Thaumaturge's cinderpearl and brainy-husk selectors only list vanilla deserts | Inspected Thaumaturge 1.21.1 native tag JSON | Append exact matching RU, Nature's Spirit and Blooming desert IDs. Preserve native weights and placement. |
| Thaumaturge's `magical_forest_caves` is marked Overworld without common underground classification | Inspected native `minecraft:is_overworld` tag | Add optional `c:is_cave` and `c:is_underground` classification and Wildshape carving protection. |
| Retired pack integrations remained active | Eight `knightquest:spawn_*` rules and Alex's Mobs legacy climate aliases | Remove retired KnightQuest extensions, flat legacy Alex's Mobs tag exports and obsolete smoke assertions. |

The climate bridge uses explicit Overworld surface IDs. Making `c:is_hot/overworld` include all of `c:is_hot` would also admit hot biomes from other dimensions and is deliberately avoided. Native ore/crop/spawn modifiers are retained instead of re-registering them under Wildshape, which would change rates and can introduce decoration ordering cycles.

## Native behavior retained

| Mod group | Resource-level observation | Result |
| --- | --- | --- |
| Farmer's Delight 1.3.4 | Native wild carrots/onions/potatoes already select Overworld and temperature; beetroots/cabbage select beaches; rice/tomatoes select climate tags | Keep native selectors and filters; bridge scoped Blooming climates only. Village processors are not replaced. |
| Ars Nouveau (inspected current manifest artifact) | Archwood adds its own Overworld/forest/village tags. Creature and mixed-archwood modifiers already select Overworld, berries select taiga | No duplicate berry, creature, tree or ore modifiers added. |
| Create Nuclear 2.0.0 | Lead/nitrate/thorium/uranium/striated ores all select `minecraft:is_overworld` | Keep native height ranges, ore targets and generation step. |
| Blooming Nature 1.1.12 | Supplies 16 native biomes, Minecraft Overworld/forest/river/savanna/taiga tags and its own surface features | Preserve native surfaces and biome identities. No extra regions or biome IDs are registered. |
| HAPI / Hybrid Aquatic / Starcatcher | HAPI 1.1.3 explicit lists omitted Blooming cold/tropical rivers and marshes; those IDs now extend HAPI and Starcatcher native selectors | Preserve native modifiers and protected cave-ocean exclusions. |
| Epic Dungeons / Epic Witch Huts / Epic Temples | Actual IDs remain `mr_epic_structuresdungeons`, `mr_epic_structureswitchhuts`, `mr_epic_structuresjungletemples`; resource namespace `epic` | These are the requested separate Epic mods. Retain paired dungeon/obelisk habitat additions and native placement relationships. |
| Alex's Caves | Native cave-biome collection is already optional in the shared protected cave tag | Retain native cave worlds/biomes and climate hooks. Surface compatibility does not turn cave biomes into crop/dungeon habitats. |

## Pack-level causes outside Wildshape

A previously exported TNK KubeJS override explicitly replaces `naturalist:add_animals` with `{"type":"neoforge:none"}`. In that pack state, adding habitat tags cannot restore native wildlife. Wildshape deliberately does not bypass this pack decision. Check the **currently installed server's** KubeJS override before diagnosing missing animals; the local export is evidence of a previous pack state, not proof of the live server's current files.

Several Thaumaturge objects that resemble small structures (including `obsidian_totem`, `hilltop_stones` and `crimson_portal`) are placed features registered through biome modifiers, not `StructureSet`s. A cross-StructureSet exclusion rule cannot guarantee clearance for those features. Native placed-feature validation remains in control.

## Optional-mod isolation and saved-world scope

New references are emitted with `required:false` both in the world's generated preset datapack and in 90 append-only common habitat-tag resources. No optional mod classes, registries, features or spawn data are hard-coded as mandatory. Native optional modifiers remain responsible for their own registry access and rates.

Existing saved presets receive these additions through common append-only tag resources, without rewriting their preset, terrain settings or source datapack. New presets export the same references. These additive resources affect common/mod tags whenever Wildshape is installed, including ordinary non-Wildshape worlds; no native feature or spawn modifiers are duplicated. Higher-priority datapack replacements/removals retain normal registry/tag priority. Only newly introduced bridges and Blooming additions are overlaid; the older preset-specific habitat tables are not globally re-exported in full.

## Verification scope

`OptionalHabitatDatapackTest` now checks generated optional references with no optional mods installed, paired Epic habitats, no duplicate feature modifiers, single Blooming crop references, scoped climates, underground classification and removal of retired aliases. `HabitatOverlayTest` additionally verifies packaged saved-world overlays against current tables, optional references and paired habitats. The pure-Java table exporter compiled and ran successfully under Java 21, generating 90 resource files. Build/test results are reported by the release workflow separately; no unrun test is claimed here.

Naturalist 2.0.7 and Archie's Dinosaurs 1.0.6 were subsequently inspected. Naturalist's turkey habitats and ant-hill common families now receive additive extensions while its native add_animals modifier and blacklists retain control. Archie's actual namespace is `ec_ad`; `amber_fossil` is a template structure using ocean-floor placement and native bone processing. Exact desert fossil habitats, common jungle therimoss habitats and verified Hybrid Aquatic warm coral biomes receive extensions. Its lush-cave, Deep Dark, Nether and End specialists retain native selectors. The coralosaur structure explicitly requests sunken placement, depth -42 and forced water; generic dry-surface validation must not replace this behavior.
