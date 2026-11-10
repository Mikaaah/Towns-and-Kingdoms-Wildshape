# Wildshape identity — 1.0.0

Version 1.0.0 retains the complete rename introduced in 1.0.12-beta.2 and standardizes the release version. The older beta.1 JAR bundled in the Beta 1.0 pack changed display branding while retaining the former internal identity; replace it with the 1.0.0 artifact. Current verification is recorded in the [Beta 1.0 follow-up audit](WILDSHAPE_BETA_1_AUDIT_2026-11-10.md).

| Component | Current identity |
|---|---|
| FML mod ID | `wildshape` |
| Display name | Towns & Kingdoms: Wildshape |
| Resource namespace | `wildshape` |
| Java packages / Maven group | `nl.townsandkingdoms.wildshape` |
| World type / `server.properties` | `level-type=wildshape:wildshape` |
| Configuration | `config/wildshape/` |
| World preset pack | `wildshape-preset.zip` |
| Commands | `/wildshape`; `/wildshape streams status`; `/wildshape streams inspect` |
| New preset share codes | `WILDSHAPE1:` |
| Mixin and access-widener filenames | `wildshape-*` / `wildshape.accesswidener` |
| Build target | Minecraft 1.21.1, NeoForge, Java 21 |

All source packages, class prefixes, Mixin interface prefixes, resource paths, translation keys, packet IDs, developer flags, current usage documentation and release artifact paths use the Wildshape identity. Fabric is excluded from the build and release workflows; its retained source is not a supported release target.

## Settings and preset import

On startup, missing files are copied into `config/wildshape/`, in this priority order: existing Wildshape settings, the previous TNK folder, the previous upstream folder, then older ReTerraForged/TerraForged folders. Current settings always win, nested custom preset folders are imported, symbolic links are skipped and originals remain untouched. Previous standalone compatibility TOML files are also imported if no current file exists. Previously shared `UTF1:` preset codes can still be read; newly generated codes always use `WILDSHAPE1:`.

The former names occur only where needed to recognize imports, reject duplicate terrain/adapter mods, preserve original authors' credits, or record historical evidence. Those occurrences do not register the old FML identity or generate resources under it. Original MIT/GPL notices are retained.

## World compatibility

**Use a fresh world when replacing a JAR that registered `ultraterraforged`.** Version 1.0.0 does not rename registry identities again relative to the renamed beta.2 build. A saved world or exported datapack may contain the former namespace in registry keys, density-function codecs, preset resources and `level.dat`. Copying settings does not migrate these references. This release does not rewrite existing worlds or claim that replacing only the JAR migrates them. Keep the earlier JAR with existing saves until a separate registry migration has been prepared and verified on a copy.

Remove the previous terrain JAR before installing this one. The old terrain mod and superseded `utfstreams`, `tfacfix`, and `utfoptimizer` adapters are explicitly incompatible. Flow Control remains optional and supported; the exported Flow Control 0.2.2 uses Streams Reflowing 2.16.2, and the separate Flow Control 1.0.0 update uses Streams Reflowing 2.17.1.

## Historical beta.2 verification

The following results describe the earlier beta.2 artifact, not 1.0.0. Current 1.0.0 results and hashes are in the [follow-up audit](WILDSHAPE_BETA_1_AUDIT_2026-11-10.md).

The Java 21 NeoForge release build passed, along with 50 focused common tests and both native flow-rule verification tasks (25 checks). The packaged JAR and generated preset were scanned: no former package/resource paths or unexpected old-identity references remain. All workflow YAML parses, and the renamed source-diagnostic parser was exercised against the new package.

The same final JAR passed four dedicated-server cases on NeoForge 21.1.262: the combined 111-ID/418-biome mod set with sample generation across seven dimensions; Flow Control/Create's 18 functional groups; legacy Streams 2.15.2; and a fresh world without optional mods. The live checks verify the FML ID, `wildshape:wildshape` world type, `/wildshape` commands, native habitats/dungeon placement, threaded heights, elevated native river currents and tile-free Streams surveys. Existing settings imported into the new folder during the combined launch.

Exact results and the JAR SHA-256 are in `docs/audit/identity/`. The earlier audit evidence remains a historical record of beta.1 and is not relabeled as a test of this build. Graphical client rendering, the complete 264-entry client installation and migration of old-namespace worlds were not tested.
