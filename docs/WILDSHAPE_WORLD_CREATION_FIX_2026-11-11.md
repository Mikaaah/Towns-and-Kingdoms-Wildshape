# Wildshape 1.0.1 — client world-creation fix

Date: 11 November 2026. Target: Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21.

## Report and cause

The supplied `Archive(1).zip` contains `latest.log` and `debug.log`. It identifies Wildshape 1.0.0, Integrated API 1.9.2 and Integrated Villages 1.3.3. Both attempts to create a Wildshape world stop in `PresetApplier.prepareForCreate` → `PresetPacks.export` before terrain generation. The blocking exception names `data/integrated_villages/worldgen/structure_set/regular_villages.json`, `integrated_villages:village_avoid`, and a `HolderSet` that is not valid in the current registry set.

The delivered 1.0.0 JAR was retrieved and matched SHA-256 `7f51a889a2df1f76776819b53c43a1e3776d6e2b3eb942ec412226c700a3143a`. The source baseline is the maintained audit branch at `97c2613`; its changes after the built 1.0.0 source were documentation only. Legacy main was not used as an implementation baseline.

Integrated API’s custom `advanced_random_spread` placement retains a named structure-set tag in its `super_exclusion_zone`. The old exporter encoded and decoded that placement against the source registry snapshot, then inserted it into a different preset registry set. Its nested named holder set still belonged to the source registry owner. Exporting the new set therefore failed. A client’s snapshot already contains loaded modded structure sets, whereas initial dedicated-server export starts from vanilla registries; the previous server checks did not exercise this failing client export state.

The exact Integrated API 1.9.2 and Integrated Villages 1.3.3 NeoForge JAR metadata and village placement resource were inspected. No additional runtime dependency is introduced.

## Change

`PresetStructureSets` directly exports only the two supported vanilla placement classes. Custom placement instances remain in their native resources. The existing world-scoped `PresetStructureResources`/`StructureSetJsonPatcher` hook applies policy and explicit saved settings before the native codec decodes those resources, on clients and dedicated servers. Tagged avoidance zones, custom codec fields, structure weights and templates retain their native representation.

Explicit spacing, separation, frequency, salt and disable settings continue to be stored in the preset and applied at resource loading. This also handles custom placement types registered under a vanilla structure-set ID. Vanilla structure preferences retain their existing direct export behavior. The mod ID, commands, configuration, world type and saved registry namespace remain `wildshape`.

The release manifest advances to `1.0.1`. CI now includes the tagged-placement regression and complete built-in preset-export tests.

## Verification

- Three registry/codec reproduction tests were run before changing production code. All three failed with the same named-holder ownership error reported in the supplied logs; the source registry fixture itself serialized correctly.
- After the fix, all **41 focused tests passed**, with zero failures, errors or skips. This includes the three new reproductions, all built-in preset exports, vanilla structure settings, explicit custom settings, native tagged exclusion preservation, late binding, optional habitats, preset identity and dedicated-server preset configuration.
- [Before-fix evidence](audit/world-creation-1.0.1/regression-before.json), [after-fix evidence](audit/world-creation-1.0.1/regression-after.json), and [inspected dependency hashes](audit/world-creation-1.0.1/inspected-dependencies.json) retain exact scope.

The reproduction uses real Minecraft registries and codecs with a small custom-placement fixture matching the failing tag schema. It is not presented as a graphical-client run of the entire TNK pack. The final NeoForge build and `tnkReleaseCheck` passed from clean, reachable source commit `58b1ad3dead1cbeef725b69300f7b0e9b1ac4803`. Both pure water-flow tasks also passed: 7 search checks and 18 flow rules. The final JAR is `wildshape-1.0.1-neoforge-1.21.1.jar`, SHA-256 `5af33eb20acdfd2c24aa814cb3fdd6714dc0e7e11f62546950ccf3b5d7a56463`. Its FML ID is `wildshape`, version is exactly `1.0.1`, source marker is clean, and maximum class major is 65. Compared with the delivered 1.0.0 JAR, 835 gameplay classes are byte-identical, `PresetStructureSets` contains the fix, and the generated Architectury loader bridge has its normal new build identity. No build helper or test fixture is bundled. See [build verification](audit/world-creation-1.0.1/build-verification.json), [release receipt](audit/world-creation-1.0.1/release-receipt.json) and [checksums](audit/world-creation-1.0.1/SHA256SUMS). No marketplace upload or release tag was created. The private build environment uses a build-only Loom TCP fallback because Unix-domain sockets are unavailable; that helper does not change or ship in game code.

## Installation

Replace `wildshape-1.0.0-neoforge-1.21.1.jar` with `wildshape-1.0.1-neoforge-1.21.1.jar`, restart Minecraft, and create a Wildshape world. Configuration deletion is unnecessary. This patch introduces no further registry rename relative to 1.0.0; the existing separate migration requirement for worlds using the former `ultraterraforged` namespace still applies.
