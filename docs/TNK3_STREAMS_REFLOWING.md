# Wildshape × Streams Reflowing

**Built-in optional integration — Minecraft 1.21.1, Wildshape 1.0.0**

Native river layers use the built-in terrain producer. NeoForge retains additional actual-water corrections derived from Mikaaah's own [Wildshape Streams Currents](https://github.com/Mikaaah/Wildshape-Streams-Currents). Streams Reflowing and its water-flow implementation remain a separate installed mod.

## What is integrated

- Height-aware currents generated from Wildshape's own native river route frames, in bounded vertical sections of the current grid.
- Final downhill correction using the water actually present in **already loaded** chunks. Does not force chunk loading.
- Better matching of server water physics, baked stream blocks, boat movement and client surface particles.
- On NeoForge, final water and boat vectors follow connected loaded water rather than an uphill baked direction.
- Manual edits from the optional [Streams Flow Control](https://github.com/Mikaaah/Streams-Flow-Control) are preserved through its `FlowOverrides` hook.
- No replacement of river terrain, water blocks or other dimension worldgen. Existing Streams Reflowing-generated streams remain Streams' responsibility.

## Requirements and installation

| Component | Required? | Version |
| --- | --- | --- |
| Wildshape | Required | Minecraft 1.21.1 / NeoForge 21.1.x |
| Streams Reflowing | **Optional** | Verified NeoForge releases **2.15.2**, **2.16.2**, **2.17.1** |
| Streams Flow Control | Optional | Exported 0.2.2 with Streams 2.16.2; 1.0.0 with Streams 2.17.1 |
| Standalone Wildshape Streams Currents / `utfstreams` | **Do not install** | Included in Wildshape |

Use the matching original Streams JAR from [Modrinth](https://modrinth.com/mod/streams-reflowing) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/streams-reflowing). Wildshape **does not redistribute it**, and still works with Streams absent.

Upstream's native producer supports TNK terrain, including legacy sea-level rivers. NeoForge's additional actual-water corrections need at least one of **winding rivers** or **raised water** enabled. In `config/streamsreflowing.properties`, keep:

```properties
vanillaWaterFlow=true
flowVanillaRivers=true
```

On NeoForge, `config/wildshape/streams.toml` controls the additional actual-water correction and scan radius. Native layers use upstream speeds (main river 1.0, tributary 0.7) and the complete river water column. Retired native speed/depth/fast-scan keys are no longer used. Compact river snapshots survive tile retirement, and missing snapshots fall back to a non-blocking terrain query.

## Diagnostics

`/wildshape streams status` shows additional-correction activation, the upstream native provider, and how often actual-water corrections happen.
`/wildshape streams inspect` shows the stream-grid angle and final water flow at your feet.

Neither command is registered if Streams Reflowing is missing.

For developer verification only, start a disposable dedicated world with JVM flag `-Dwildshape.compatSmoke=true`. The live identity, generation and optional Streams checks write `wildshape-compat-results.json` in the game directory and stop the server afterward. Current artifact hashes and completed fixtures are recorded in the [1.0.0 audit](WILDSHAPE_BETA_1_AUDIT_2026-11-10.md).

## Loader and version boundaries

This release targets **NeoForge 1.21.1 / Java 21**. It uses version-specific hooks for the verified Streams **2.15.2**, **2.16.2** and **2.17.1** APIs. The two baked-fluid classes renamed in 2.17.1 have separate hooks; unverified later versions are outside the supported ranges. Flow Control remains optional, and manual overrides retain priority over automatic downhill correction.

**Never load both the old standalone addon and this integrated variant.** Wildshape marks the `utfstreams` mod ID as incompatible, because applying both sets of Mixins could corrupt river physics or crash during initialization.

## Licensing

Integration source is from [Mikaaah's GPL-3.0-only repository](https://github.com/Mikaaah/Wildshape-Streams-Currents/blob/main/LICENSE); the remaining NeoForge actual-water corrections retain that license. The former duplicate Alex placement adapter has been replaced by upstream's built-in climate adaptation. Upstream TerraForged/ReTerraForged/Wildshape notices remain intact.

The Streams Reflowing JAR is licensed **All Rights Reserved** by its author. It is a compile-time API dependency and optional runtime mod **not shaded, bundled or republished in Wildshape**.

## Performance and limitations

Native layer building probes the terrain generator; runtime loaded-water correction scans a bounded number of positions, with short-lived cached results. Rendering and chunk-generation load may increase on enormous river systems. Measure generation time, memory use and boat motion in the full TNK3 pack rather than treating compilation as performance validation.

Per-chunk calculation uses predicted terrain for native streams and actual loaded water for final current correction. River terrain altered by datapacks, players or other mods may differ from predicted flow. Old saved stream blocks are not regenerated by the adapter.
