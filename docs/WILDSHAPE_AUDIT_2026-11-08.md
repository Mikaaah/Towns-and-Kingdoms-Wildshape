# Wildshape — TNK3 code and compatibility audit

Audit date: 8 November 2026. Target: Minecraft 1.21.1, NeoForge, Java 21.

This first pass was written before changing production code. Findings describe the inspected baseline; the implementation and verification log at the end records subsequent work separately.

## Source and pack evidence

- Maintained repository: [Towns-and-Kingdoms-Wildshape](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape).
- Default `main`: `275776c`, version 1.0.2. The newer `fix/tnk3-biome-exclusions` head is `0f6c201`, version 1.0.11. The audit uses **1.0.11** for implementation, preserving its approved cave, river, shoreline, surface and biome decisions. The default branch's later changes are documentation/branding, not newer terrain code.
- Pack inventory: `Towns & Kingdoms 3 - Beyond the Forge-Package one.zip`, 21 October; 258 CurseForge entries and six bundled JARs. `TNK3_mods_grouped(1).csv`, 22 October, records the same 264 entries. The 25 October TNK3 `latest(1).log` provides a newer installed-version cross-check, including Nature's Spirit 2.3.1, Regions Unexplored 0.6.3, BOP 21.1.0.14, Alex's Caves 2.0.10 and Streams 2.15.2.
- November's supplied `Modsjars(1).zip` contains 20 concrete dependency JARs. Versioned structure/ecology JARs are inspected directly from the export's exact file IDs, rather than inferring registry names from display names. Actual newer JARs take priority where available.
- The newest `latest(3).log` belongs to **Planescape**, not TNK3; it is excluded. The September repository target list is also older than these pack artifacts. This is the latest retrievable TNK3 inventory, not proof that a November installation is byte-identical.
- Wildshape's own issue search returned no issues. Relevant upstream reports were inspected separately; their closed status is not accepted as proof of a fix in this fork.

## Findings before changes

| ID | Priority | Evidence and consequence | Required correction |
|---|---|---|---|
| W01 | High | Default branch 1.0.2 lacks substantial confirmed 1.0.11 work. Starting a cleanup from `main` would restore old river, biome and structure behavior. | Base the isolated audit branch on `0f6c201`; keep the default branch untouched. |
| W02 | High | `CaveCarving.isProtectedCaveBiome` protects only the `alexscaves` and `yungscavebiomes` namespaces. RU 0.6.3 and BOP 21.1.0.14 publish cave biomes through `c:is_cave`; those cells and Deep Dark can currently be carved, lined and repainted as generic lush/dripstone caverns. | Use an extensible protection tag plus native cave tags, preserving ordinary vanilla lush/dripstone cavern styles. Protect sensitive cave biome cells and their habitat. |
| W03 | High | `PresetStructureSets.bootstrap` increases **every** non-vanilla random-spread set below spacing 48. This also changes AC cave landmarks, AE2 meteorites, aquatic whale falls, small decorations and Nether structures. Example: AC acid pits go from 6 to 18 chunks; AE2 meteorites from 32 to 48. Structure density changes apply to a world's shared registry, not only its Overworld. | Replace blanket changes with verified, explicitly selected Overworld structure-set rules; preserve boss/quest, resource and other-dimension placement. Explicit user preset overrides must win. |
| W04 | High | `StrataRule.findMaterials` accepts any full block in broad stone tags. It rejects falling blocks and block entities, but not ores. Upstream issue 18 reports entire Zenalite ore strata; the same mechanism exists here. | Reject ore blocks by common ore tags and block identity traits, retaining a pack-extensible exclusion tag. Preserve valid Create decorative stones. |
| W05 | High | `FastPoissonModifier.getDensityNoise` checks the injected interface but directly dereferences nullable `generatorContext()`. All RandomStates receive that interface, including vanilla and other dimensions. Upstream issue 14 describes the same failure on another loader/version. | Null-safe fallback to ordinary placement density when no Wildshape context/cache exists. Regression against a non-Wildshape RandomState. |
| W06 | Medium | `PresetBiomeModifier.reported` statically retains a `MinecraftServer` after stopping a game. A logging guard should not hold the entire old server graph. | Store a weak reference; do not change feature or spawn modification semantics. |
| W07 | High | `WorldGenFlags` defaults to approximate lookups; only the chunk worker's structure-start step switches it off, through HEAD/TAIL injections. Off-thread height queries can use unfiltered cells, and a thrown exception skips restoration. Upstream PR 17 documents this family of structure-height mismatch. Its current patch targets a newer Minecraft API and cannot be copied wholesale. | Scope accurate structure column queries with exception-safe restoration; isolate approximate search behavior and test thread/cache independence against 1.21.1 APIs. |
| W08 | Medium | `Skerries.apply` initializes its maximum to zero, so the `best <= -2` no-islet exit cannot run. Cold shallow ocean cells can become broad sea-level flats. Upstream PR 16 identifies the identical line. | Start below all islet heights; verify water between islets and preserve intentional coastline shapes. |
| W09 | Medium | Structure-piece protection already resolves neighboring starts and uses a four-block margin in 1.0.11. It is valuable and must not be replaced with aggregate start boxes, which would wall off large mineshaft volumes. Feature-placed bridges and some later structures do not provide pieces here. | Retain the piece-based approach, test cross-chunk boundaries, document generation-stage limits. Do not claim universal overlap prevention. |
| W10 | Medium | Native spawn behavior comes from biome identities, biome tags, modifier phases and mod-specific placement checks. Adding a global Wildshape creature-spawn table would duplicate or bypass native ecology. | Audit native tags/conditions, bridge actual missing habitats where justified, preserve native weights, group sizes, predicates and bosses. |
| W11 | Medium | Integrated mods intentionally override resources in other namespaces: Integrated Cataclysm uses `cataclysm:*`; Integrated Mowzie's uses `mowziesmobs:*`; Dungeons Arise uses `dungeons_arise:*`. Matching display-name/mod-ID prefixes would miss these replacements or add duplicate structures. | Base rules on the winning registry resource and its actual type, not only the installing mod's ID. |
| W12 | Medium | Existing exclusions remove 24 exact BOP/RU IDs while keeping BOP Grassland. Cavern biome painting can affect native biome features and mob spawning. | Preserve all exclusions and the biome fallback; restrict cavern repainting to cells owned by the Wildshape style pass. |
| W13 | Medium | Optional source compatibility is not full-pack runtime proof. Closed river issues 7/9/10 and prior unit tests do not verify every seed, preset, third-party carver, village foundation or stream current. | Build, run focused regressions and launch fresh-world server tests with a recorded dependency set. Record limitations honestly. |

## Pack compatibility decisions

| Area | Audited integration | Policy |
|---|---|---|
| Surface biomes | BOP, RU, Nature's Spirit, TerraBlender; vanilla biome additions from Autumnity, BloomingNature, Alpine Whispers and Upgrade Aquatic | Preserve native biome objects, surface materials, feature order, grass/water colors and spawn settings. Keep the existing Nature's Spirit surface-height adapter. |
| Cave ecosystems | Alex's Caves, YUNG's Cave Biomes when installed, RU, BOP, Deep Dark and tagged third-party underground biomes | Leave their cave cells/materials/spawn habitats under their owning mod. Keep Wildshape's enormous connected caverns in eligible terrain. |
| Major exploration | IDAS, Integrated Dungeons Arise, Integrated Villages, Integrated Stronghold, Integrated Cataclysm, Integrated Mowzie's, Integrated Seven Seas, YUNG structures, Iron's Spells | Preserve original structures, boss entities, template pools and loot. Apply selective density control only to verified sets. Keep No Structure Overlap's own checks in control of cross-mod overlap. |
| Creatures | Alex's Mobs, Mowzie's, Cataclysm, Hybrid Aquatic/HAPI, WilderNature, Ars creatures, Iron's mobs, vanilla wildlife | Prefer native habitat tags and spawn predicates. Any additions must be grounded in actual JAR resources; do not indiscriminately add every creature to every biome. |
| Automation resources | Create decorative rock, AE2 meteorites, ores from IE/Ars/Iron's and other installed mods | Keep ores out of synthetic stone strata; retain native ore and meteorite generation. Recipe and quest balance remains with the existing pack scripts. |
| Rivers | Streams Reflowing 2.15.2 and Flow Control | Retain integrated native current hooks and explicit Flow Control overrides; preserve the approved no-artificial-underground-rivers policy. |
| Other dimensions | Twilight Forest, Aether/Deep Aether, BetterEnd, BetterNether, Amplified Nether, later Duskdelvers | Do not replace their generators, scatter their mobs into the Overworld, or apply automatic Overworld density rules to them. |
| UI/combat/storage and libraries | Remaining listed mods | No invented terrain integration. Keep dependencies optional and loader-specific; validate startup compatibility where the installed test set permits. |

## Issue triage

Relevant primary evidence: [river embankments #6](https://github.com/Pandaismyname1/ultraterraforged/issues/6), [river direction #5](https://github.com/Pandaismyname1/ultraterraforged/issues/5), [Alex's Caves #7](https://github.com/Pandaismyname1/ultraterraforged/issues/7), [lake bubbles #9](https://github.com/Pandaismyname1/ultraterraforged/issues/9), [null context #14](https://github.com/Pandaismyname1/ultraterraforged/issues/14), [skerries #16](https://github.com/Pandaismyname1/ultraterraforged/pull/16), [column determinism #17](https://github.com/Pandaismyname1/ultraterraforged/pull/17), [ore strata #18](https://github.com/Pandaismyname1/ultraterraforged/issues/18). The save-screen report #2 targets another Minecraft generation and needs separate reproduction; its current upstream locking changes are not assumed applicable without comparison. The VSS report #15 targets Forge 1.20.1 and is not a reason to add unsupported reflection shims here.

## Acceptance and implementation log

### Additional findings exposed during implementation

| ID | Priority | Evidence | Resolution |
|---|---|---|---|
| W14 | High | Completed terrain tiles were retained indefinitely unless every constituent chunk reached the drop hook. Lookup-only or partially generated tiles could exhaust the 2 GiB regression JVM. Published cell arrays could also be recycled while late readers still held them. | Bound completed tile retention (64 by default); identity-based eviction; do not recycle published arrays. In-flight work may temporarily exceed the retention limit. Add eviction/churn and late-reader regressions. |
| W15 | High | Custom Integrated and YUNG placements extend `RandomSpreadStructurePlacement`; reconstructing them with the vanilla constructor erased their placement type, enhanced/super exclusion zones and custom checks. | Reconstruct custom placements with their registered codecs and alter only requested fields. Check type and exclusion-zone retention in live registries. |
| W16 | High | Flow Control 0.2.2 requires Streams 2.16.2. The existing Wildshape adapter and October export target 2.15.2. The two JARs have different binary APIs; simply widening the dependency range is insufficient. | Add explicit NeoForge bindings and gated Mixins for each verified version. Preserve the existing 2.15.2 path. Verify 2.16.2 with the actual newer Flow Control JAR; no dependency JAR is bundled. |
| W17 | Medium | Alex’s Mobs 1.22.17 queries five legacy flat `c:` tag names; modern BOP/RU/NS tags use hierarchical names. Several Integrated API collections have similar legacy references. | Append modern habitats to the verified legacy tags in the world’s preset datapack. |
| W18 | Medium | Several Knight Quest and Legendary Monsters native spawn modifiers name only vanilla biomes. Tag-only bridges cannot extend a fixed biome list. | During NeoForge’s ADD phase, reuse the installed native modifier’s spawn data for matching modded Overworld habitats. Avoid existing entries, protected cave habitats and vanilla biomes. Missing/disabled modifiers stay disabled; later REMOVE phases retain priority. |
| W19 | Medium | The spawn-finder interface is injected into every climate sampler; checking only interface presence intercepts non-Wildshape worlds as well. | Require an active terrain context before replacing the spawn finder. |
| W20 | Low | Historical coast and drumlin tests assume fixed density thresholds that already fail on 1.0.11: coastline 1934 vs 1817; 604 drumlin cells, a seven-block rise and zero lowered cells. The October golden file also predates later TNK terrain fixes. | Preserve production terrain. Compare feature-on/off behavior, retain height/no-lowering checks, and refresh terrain fingerprints after baseline comparison. |

### Implemented changes

- Native cave protection is tag-driven and extensible; Deep Dark and known native cave namespaces are protected. Vanilla lush/dripstone remain eligible for the confirmed cavern styles.
- Explicit Overworld structure-set rules replace namespace-wide spacing changes. User preset entries take priority; custom placement types and exclusion zones survive adjustment.
- Ore/storage tags, experience-dropping ore types and ore-name segments are excluded from decorative strata. Create decorative stones remain eligible.
- Accurate generated column sampling is independent of cache contents and thread identity. Approximate biome/structure searches use nested, exception-safe scopes. World Preview has its own approximation setting.
- Tile retention, failed-entry removal, Poisson context fallback, server logging references and non-Wildshape spawn lookup are corrected.
- Habitat bridges cover Integrated API, Alex’s Mobs, Cataclysm, HAPI/Hybrid Aquatic, WilderNature, Starcatcher, Bosses’Rise and Legendary Monsters. Knight Quest’s fixed selectors use live native spawn data on NeoForge. No boss catalysts, recipes, dungeon loot, template NBT or entity AI are replaced.
- Wildshape display/build branding is consistent. Existing mod ID, registry namespace, config directory and preset filenames remain stable. Superseded `utfstreams`, `tfacfix` and `utfoptimizer` adapters are marked incompatible; Flow Control remains supported.
- CI exercises focused terrain/compatibility regressions. An opt-in dedicated-server check records loaded versions, live spacing/types, habitat application, dimension ownership, thread-independent heights, generation and Streams integration.

### Verification record

Final build, test counts, runtime manifests and deliverable hashes are recorded below after the last verification pass. The initial no-optional-mod NeoForge 21.1.262 server passed generation and clean shutdown; larger runtime tests use the actual versioned dependency JARs and are reported separately.
