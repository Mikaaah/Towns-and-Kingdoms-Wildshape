# Wildshape — Beta 1.0 pack follow-up

Audit date: 10 November 2026. Target: Minecraft 1.21.1, NeoForge 21.1.262, Java 21.

This section was recorded before changing production code. The implementation and verification record below will identify subsequent changes separately.

## Baseline and identity

- Source baseline: `audit/tnk3-compat-1.0.12` at `d6509bde2995e88e3fbc6e6629ccf38c68f6e718`. Its later commits only change documentation; the full namespace rename and previously verified terrain/compatibility implementation remain present. Main is older and is not used as an implementation baseline.
- Input: the supplied **Towns & Kingdoms 3 — Beyond the Forge, Beta 1.0** export: 315 manifest entries plus three bundled JARs. Manifest and HTML inventory ordering were cross-checked against known project IDs and downloaded metadata. Resource packs and shaders in the manifest are inventory entries, not loaded mod IDs.
- The bundled Wildshape file is the exact earlier beta.1 binary, SHA-256 `bd173b362a8b9489751aa57d6590737f445b4547034d3dbab5313b3037433fb1`, embedded ID `ultraterraforged`. The latest delivered beta.2 is SHA-256 `ce842bf128433af690b5c993e362d0f17efaf9c122e552747c83206d86279889`, embedded ID `wildshape`. The older bundled file is not substituted for the renamed implementation.
- The supplied TNK version registry records beta.2 as a development fingerprint and no established public Wildshape artifact. The requested deliberate release identity is **1.0.0**, with filename `wildshape-1.0.0-neoforge-1.21.1.jar`, embedded version `1.0.0` and Java 21 bytecode. Earlier fingerprints and aliases remain immutable. This task does not publish a marketplace file or merge main.

## Findings before changes

| Finding | Evidence | Action |
|---|---|---|
| Export still contains the pre-rename terrain JAR | Packaged metadata and exact SHA-256, rather than filename alone | Deliver one replacement JAR; preserve `wildshape` identity and non-destructive settings import |
| Version inputs still append Minecraft and snapshot labels | Current Gradle configuration produces `1.0.12-beta.2+1.21.1` | Adopt the supplied release manifest/adapter and verify the final remapped JAR |
| Three Epic Structures projects replace several older YUNG entries | Exact exported structure/tag JSON | Extend matching biome habitats; retain native placement, templates and loot |
| Large Epic dungeons and their obelisks share placement salt, spacing 61 and separation 60 | Both native structure-set resources | Keep the paired placement and equivalent habitat additions together; no independent spacing override |
| Croptopia's crop/tree habitats predominantly list vanilla, BOP and other absent biome mods | Croptopia 4.2.4's native biome tags | Add optional, explicit RU/Nature's Spirit counterparts of the native vanilla habitats; retain native feature modifiers and rates |
| Nature's Spirit is now 2.3.2 | Metadata of file 9326605 | Recheck biome identifiers, tags, feature ordering and native generation |
| Knight Quest was updated to 1.9.4 | Metadata and all native spawn modifiers | Existing bridge source IDs still match; retain live native weights and duplicate prevention |
| Apple cows use common habitat tags, with enchanted cows limited to the jungle tag | Native modifiers/tags | Preserve common-tag spawning; bridge Nature's Spirit's native tropical-woods habitat for the enchanted variant |
| Deeper and Darker and Jaden's Nether Expansion add non-Overworld content | Export and actual worldgen resources | Keep their dimension generators/surfaces independent; verify dimension ownership in the runtime fixture |
| The export pins Streams 2.16.2 and a Flow Control file labelled 0.2.0 | Actual metadata says Streams 2.16.2 and Flow Control 0.2.2, matching the recorded alias | Preserve the verified ABI; do not infer incompatibility or upgrade dependencies from display labels |

The archive's configurations and native third-party resources are evidence, not instructions to modify unrelated mods. Recipes, loot pools, boss access, dimension progression, structures' NBT and entity AI remain outside this terrain follow-up.

## Implementation and verification

Production changes add optional habitat tags for the current Epic Structures and 85 Croptopia crop/tree selectors, plus the enchanted apple cow’s native tropical-woods tag. All 49 explicitly selected crop counterpart biome IDs were checked against RU 0.6.3 and Nature’s Spirit 2.3.2 resources, and none is in the approved exclusion list. Linked Epic dungeon/obelisk pairs receive identical additions and keep their native salt, spacing and separation. No native feature modifier, spawn weight, structure template, loot table or other-dimension generator is replaced.

The supplied TNK Gradle release adapter is installed at the end of the root build and targets `:neoforge:remapJar`. `release.json` is the version input; FML metadata and the final filename are checked by `tnkReleaseCheck`. Historical fingerprints are retained in `release-tools/registry.json`. A clean source commit is embedded in the candidate artifact. No Fabric artifact is built.

Static inspection covers 111 exact export dependency JARs, with 171 structure-set records, 345 structure records, 360 biome JSON records, 1,249 biome-tag records and 758 biome-modifier records; zero JSON parse errors. Counts are resource records across JARs, not distinct winning registry entries or loaded mod IDs. The audit utility now excludes NeoForge biome data maps from biome-definition counts. Required fixture dependency closure was checked across 510 relationships, including nested JAR metadata. The first fixture launch exposed an older `required=true` declaration for Aether’s nested Accessories dependency on oωo; the exact exported oωo file 9215303 was added, and both older dependency keys and modern `type=required` were included in the closure check. All server-side required dependency IDs are present.

Build and runtime outcomes are recorded below and in the linked machine-readable evidence. The earlier beta.1/beta.2 evidence remains historical; it is not relabelled as a test of 1.0.0.


The separate current Flow Control 1.0.0 artifact and Streams 2.17.1 were also inspected. Their shared platform/layer interfaces are byte-identical to 2.16.2, but two registered baked-fluid implementation classes were renamed. Exact version-gated hooks were added for those two targets; the common grid and particle signatures were checked. Supported ranges remain restricted to the three verified Streams releases. These are additional update fixtures, not substituted versions in the exported-mod-list evidence.

The initial release build passed 54 focused common tests, 25 pure flow checks, and `tnkReleaseCheck`; the version-system fixture suite passed 26 tests. Final artifact and dedicated-server evidence is recorded after building the reachable GitHub source commit. The build uses a private Gradle cache and a build-only Loom TCP fallback because this workspace disallows Unix-domain sockets. That helper does not modify game classes or ship in the mod JAR. Dedicated game launches use the ordinary packaged JAR without the helper.


## Release artifact and completed checks

The final remapped JAR is `wildshape-1.0.0-neoforge-1.21.1.jar`, SHA-256 `7f51a889a2df1f76776819b53c43a1e3776d6e2b3eb942ec412226c700a3143a`. It was built from reachable GitHub source commit `74a2f78cb22675415ff33fe57e5d0a7496842fe0`, with a clean source tree. The packaged FML ID is `wildshape`, packaged version is exactly `1.0.0`, and maximum Java class major is 65 (Java 21). All 837 class payloads match the compilation used for the focused tests. The source and release manifest are in that commit; subsequent evidence/ledger documentation does not change production code.

| Check | Observed result |
|---|---|
| NeoForge release build and `tnkReleaseCheck` | Passed |
| Focused common regression tests | 54 passed; zero failures/errors/skips |
| Pure water-search/flow checks | 25 passed |
| Supplied release-tool fixtures | 26 passed |
| Streams 2.17.1 + Flow Control 1.0.0 + Create 6.0.10 | Passed: new fluid hooks, elevated native river water/current, public grid and server physics, eight headings, tile-free survey, three sample chunks |
| Flow Control 1.0.0 integration with the final Wildshape JAR | All 19 functional groups passed, including both baked fluid variants, manual priority, boat stillness, persistence/networking and actual Create network behavior |
| No optional mods installed | Passed: identity, world type, command root, generator ownership, threaded heights and three sample chunks |
| Focused 27-dependency exported habitat fixture | Server started in 312.900 seconds; reached final native-river check after preceding assertions; exceeded the 600-second total budget during native Streams planning; not an end-to-end pass |
| Combined 111-dependency exported worldgen/creature fixture | Loaded dimensions, then interrupted before a final report; not counted as a pass |

Evidence: [build verification](audit/beta-1.0/build-verification.json), [Streams update](audit/beta-1.0/runtime-streams-2171.json), [Flow integration](audit/beta-1.0/runtime-flow-100.json), [optional absence](audit/beta-1.0/runtime-optional-absence.json), [release receipt](audit/beta-1.0/release-receipt.json), [checksums](audit/beta-1.0/SHA256SUMS), and [fixture dependency closure](audit/beta-1.0/fixture-dependency-closure.json).

The receipt's `git_tag` and upload fields describe prepared release metadata; they do not mean a tag, marketplace upload or merge was performed. Earlier beta.2 fingerprints remain in the registry, and the 1.0.0 fingerprint is appended as a verified development artifact.

The first combined launch lacked the fixture's nested oωo dependency and was corrected. A later 5 GB-heap attempt ended with signal 9 before producing a result; it is not counted as a pass. The 3 GB-heap combined run used the exact exported dependencies and configuration with a bounded worker count. It progressed through live dimension initialization after a temporary execution-environment disconnect, but no final report or exit status was recoverable; that run is unverified. The smaller 27-dependency fixture included the newly added biome/dungeon/dimension mods, Knight Quest 1.9.4, Hybrid Aquatic, Alex’s Caves and the exact exported Streams 2.16.2 / Flow Control 0.2.2 pair. It also included the separately declared Kotlin language provider. Its only fixture configuration adjustment removed the FML override for absent Sable; native worldgen settings were retained. A first narrowed-fixture boot missing that language provider was corrected before the generation run. Third-party optional-Mixin, POI and unavailable external-network warnings are not presented as a clean warning-free startup.

Full graphical-client rendering, every dungeon/seed, multiplayer sessions, the entire 318-entry client installation, and migration of old-namespace worlds are not certified by these checks. Use a fresh world for the bundled pre-rename `ultraterraforged` build; 1.0.0 retains the renamed beta.2 namespace. Do not install both terrain JARs.


### Focused habitat fixture outcome

The [targeted fixture record](audit/beta-1.0/runtime-targeted-beta-habitats.json) and [log excerpt](audit/beta-1.0/runtime-targeted-log-excerpt.txt) preserve the incomplete result. It reached `Done (312.900s)` and the final `inspecting native main routes` stage. Source order in `CompatibilitySmokeChecks` shows that the preceding live identity, generator/dimension, Epic placement/pairing, Croptopia native-feature, fish habitat, Knight Quest spawn-data, threaded-height and sample-chunk assertions returned before that marker. This is evidence of those completed stages, not a fabricated final passing report.

The export uses Streams **HIGH** terrain accuracy with a **1,536-block watershed**. Under this fixture’s two active processors, native planning exceeded the 600-second total budget while preparing the elevated river sample in region `(4,1)`. The process was stopped by its timeout; no final smoke report was written. These settings were not lowered to manufacture a pass. The separate Streams 2.17.1/Flow Control 1.0.0 fixture used its own default configuration and passed; it does not certify the exported HIGH profile.

The narrowed fixture also logged native Nether Expansion recipe/advancement/tag warnings and ocelots listed under the wrong native mob category in several BOP/RU biomes. Wildshape’s bridge has no ocelot rule and derives each added spawn category from the installed entity type. These observations are retained as third-party/pack findings; no unrelated recipe, entity definition or native configuration was silently rewritten. All completed and incomplete cases are summarized in [runtime results](audit/beta-1.0/runtime-results.json).
