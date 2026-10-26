# TNKTerraForged × Streams Reflowing

**Built-in optional integration — Minecraft 1.21.1, TNKTerraForged 1.0.11**

Native river layers now use upstream UltraTerraForged 1.0.3's built-in producer. NeoForge retains additional actual-water corrections derived from Mikaaah's own [UltraTerraForged Streams Currents](https://github.com/Mikaaah/UltraTerraForged-Streams-Currents). Streams Reflowing and its water-flow implementation remain a separate installed mod.

## What is integrated

- Height-aware currents generated from TNKTerraForged's own native river route frames, in bounded vertical sections of the current grid.
- Final downhill correction using the water actually present in **already loaded** chunks. Does not force chunk loading.
- Better matching of server water physics, baked stream blocks, boat movement and client surface particles.
- On NeoForge, final water and boat vectors follow connected loaded water rather than an uphill baked direction.
- Manual edits from the optional [Streams Flow Control](https://github.com/Mikaaah/Streams-Flow-Control) are preserved through its `FlowOverrides` hook.
- No replacement of river terrain, water blocks or other dimension worldgen. Existing Streams Reflowing-generated streams remain Streams' responsibility.

## Requirements and installation

| Component | Required? | Version |
| --- | --- | --- |
| TNKTerraForged | Required | Minecraft 1.21.1 / NeoForge 21.1.x |
| Streams Reflowing | **Optional** | **2.15.2** NeoForge |
| Streams Flow Control | Optional | Its own compatible version |
| Standalone UltraTerraForged Streams Currents / `utfstreams` | **Do not install** | Included in TNKTerraForged |

Use the original Streams JAR from [Modrinth](https://modrinth.com/mod/streams-reflowing/version/vbJ5meb8) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/streams-reflowing). TNKTerraForged **does not redistribute it**, and still works with Streams absent.

Upstream's native producer supports TNK terrain, including legacy sea-level rivers. NeoForge's additional actual-water corrections need at least one of **winding rivers** or **raised water** enabled. In `config/streamsreflowing.properties`, keep:

```properties
vanillaWaterFlow=true
flowVanillaRivers=true
```

On NeoForge, `config/tnkterraforged/streams.toml` controls the additional actual-water correction and scan radius. Native layers use upstream speeds (main river 1.0, tributary 0.7) and the complete river water column. Retired native speed/depth/fast-scan keys are no longer used. Compact river snapshots survive tile retirement, and missing snapshots fall back to a non-blocking terrain query.

## Diagnostics

`/utfstreams status` shows additional-correction activation, the upstream native provider, and how often actual-water corrections happen.
`/utfstreams inspect` shows the stream-grid angle and final water flow at your feet.

Neither command is registered if Streams Reflowing is missing.

A dedicated Gradle integration run can be started using `-Ptnk.streamsSmoke=true` with `:neoforge:runServer`. This enables the original 0.2.1 source's server-side verification harness and writes `neoforge/run/utfstreams-integration-results.json`.

## Loader and version boundaries

Native currents use upstream's common implementation on **NeoForge and Fabric**. Additional actual-water corrections, diagnostics and the integration harness remain NeoForge-only. The exact Streams API of **2.15.2** is targeted; later releases require explicit compatibility verification before widening the dependency range.

**Never load both the old standalone addon and this integrated variant.** TNKTerraForged marks the `utfstreams` mod ID as incompatible, because applying both sets of Mixins could corrupt river physics or crash during initialization.

## Licensing

Integration source is from [Mikaaah's GPL-3.0-only repository](https://github.com/Mikaaah/UltraTerraForged-Streams-Currents/blob/main/LICENSE); the remaining NeoForge actual-water corrections retain that license. The former duplicate Alex placement adapter has been replaced by upstream's built-in climate adaptation. Upstream TerraForged/ReTerraForged/UltraTerraForged notices remain intact.

The Streams Reflowing JAR is licensed **All Rights Reserved** by its author. It is a compile-time API dependency and optional runtime mod **not shaded, bundled or republished in TNKTerraForged**.

## Performance and limitations

Native layer building probes the terrain generator; runtime loaded-water correction scans a bounded number of positions, with short-lived cached results. Rendering and chunk-generation load may increase on enormous river systems. Measure generation time, memory use and boat motion in the full TNK3 pack rather than treating compilation as performance validation.

Per-chunk calculation uses predicted terrain for native streams and actual loaded water for final current correction. River terrain altered by datapacks, players or other mods may differ from predicted flow. Old saved stream blocks are not regenerated by the adapter.
