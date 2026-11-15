# SkyLands compatibility — Wildshape 1.0.3

Minecraft **1.21.1**, **NeoForge**, **Java 21**. The inspected SkyLands release is **0.9.4**, CurseForge project 1735016 / file 9583195, source commit `5bea72f636ff52e74c2c559ef6705fd7735cc176`.

## Installation and existing worlds

Install `wildshape-1.0.3-neoforge-1.21.1.jar` on the server and clients, replacing the previous Wildshape JAR. Install [SkyLands NeoForge Port](https://www.curseforge.com/minecraft/mc-mods/skylands-neoforge-port) and its normal dependencies separately: Lithostitched 1.8.1, GeckoLib 4.7.6 or later in its supported range, and Player Animator on clients. Wildshape does not make any of them a required dependency.

Restart Minecraft/server after installing. A Wildshape save from 1.0.0–1.0.2 keeps its registry identity and saved terrain preset. The compatibility hook supplies the necessary build space when world registries load; no manual preset edits or fresh world are required for this adjustment.

**Islands generate only in new chunks.** Existing generated chunks, including all pregenerated chunks, are not regenerated above their old terrain. Explore beyond the pregenerated area for islands. This integration does not provide island retro-generation. Keep a backup before adding a worldgen mod, and retain SkyLands once its blocks/biomes are saved in a world. Worlds using the former `ultraterraforged` namespace still require their separate registry migration.

## What changes

SkyLands creates its islands as chunk-local features at `TOP_LAYER_MODIFICATION`, with Lithostitched selecting sky biomes from Y=290 upward. Its 0.9.4 dimension resource provides build space from -64 through Y=527. An existing Wildshape preset normally supplies its own Overworld dimension resource and hides this height expansion.

When SkyLands is installed **and** a Wildshape preset resource is active, the registry-loading hook expands only the `minecraft:overworld` dimension-type resource as necessary. It retains the preset's lower bound, rounds the total height to whole chunk sections, preserves higher enabled resource ceilings, and expands logical height alongside physical build height. It never reduces a taller existing ceiling.

The saved preset ZIP, terrain/noise height, density functions, biome exclusions, river/cave configuration and other dimension-type resources are not rewritten. An illustrative -64/384 preset receives total height 592; a -128/640 preset receives 656. The terrain noise in those examples stays 384 and 640 respectively. The current built-in default uses min Y=-64 and total noise height 576, reaching Y=511; compatibility raises only its physical/logical build space to Y=527. SkyLands retains its own biome injector, native island features, templates, density settings, lighting, loot and spawn rules. No copied island generator or duplicated spawn table is added.

The hook is inactive without SkyLands or without an active Wildshape preset. Ordinary SkyLands worlds keep their native dimension resource. Higher compatible resource requests are read from the enabled Overworld resource stack without assuming loader-specific pack names.

## Verification

The exact packaged JAR from clean implementation commit `228f0a6c6c6b941090ec14a31614c5011c55f41f` passed:

- **53 focused tests**, including seven SkyLands resource-policy regressions, existing preset exports, tagged structure placements, biome exclusions, cave protection, cache eviction and surface/water safeguards.
- The Java 21 / NeoForge 21.1.262 build, release identity check and **25 pure current/search checks**.
- An installed **NeoForge 21.1.266** upgrade from the verified 1.0.2 JAR: the actual default ceiling increased from Y=511 to Y=527 while min Y=-64 and total noise height 576 were preserved. The saved preset ZIP remained byte-identical, and decoded blocks/biomes in every original section of test chunk `[0,0]` remained identical.
- Live Lithostitched injection exposing **all six sky biomes**, native island feature registration, and **12,026 native SkyLands blocks** sampled from Y=320–387 in chunk `[446,356]` near `(7141,5709)`. This island's saved block and biome data remained identical after restart.
- A marker at **Y=527** survived save/reload. Nether and End dimension heights remained unchanged. A separate world started and generated normally with all optional fixture mods removed; its original ceiling stayed Y=511.

The fixture used seed `2031102026`, SkyLands 0.9.4, BOP 21.1.0.14, Regions Unexplored 0.6.3, Nature's Spirit 2.3.2, TerraBlender 4.1.0.8, Lithostitched 1.8.1 with nested Apollib 1.2.0, GeckoLib 4.9.3 and GlitchCore 2.1.0.2. The exact dependency JARs were checked against their recorded SHA-256 values. No third-party JARs are bundled in Wildshape.

[Build and scope](audit/skylands-1.0.3/build-verification.json) · [Runtime summary](audit/skylands-1.0.3/summary.json) · [Generation report](audit/skylands-1.0.3/skylands-generate.json) · [Reload report](audit/skylands-1.0.3/skylands-reload.json) · [Dependency fingerprints](audit/skylands-1.0.3/dependency-inspection.json) · [Successful workflow](https://github.com/Mikaaah/Towns-and-Kingdoms-Wildshape/actions/runs/43703887994)

Artifact: `wildshape-1.0.3-neoforge-1.21.1.jar` — SHA-256 `0cb34e6164f2758bef537b57349994157b8017e886ab9227ce9f1afcd9b0ddf8`.

The fixture uses the exact 1.0.2 release JAR to create and save a world, then installs 1.0.3 with SkyLands and the audited BOP, Regions Unexplored, Nature's Spirit, TerraBlender, Lithostitched and GeckoLib JARs. It reads the actual baseline dimension/noise settings, then checks the expanded build ceiling, original noise height, six injected sky biomes, native island feature registration, native island blocks at sky altitude, every original section's blocks/biomes in a saved test chunk, unchanged preset bytes, save/reload and startup without optional mods. A marker at Y=527 checks that the newly available layer saves and reloads. It uses an installed NeoForge 21.1.266 server and records dependency fingerprints.

Full graphical client inspection and the user's exact server seed/preset are outside this isolated fixture. Upstream [SkyLands #13](https://github.com/MacatMovie/SkyLands-NeoForge-Port/issues/13) reports a separate MoreCulling chest-face rendering issue; the server fixture does not exercise client culling. The compatibility adjustment does not promise migration of non-Wildshape registry identities or compatibility with arbitrary dimension/height overrides.
