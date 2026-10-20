# UltraTerraForged 1.21.1 audit completion matrix

The pinned original audit identified **23 findings**. This file tracks the live source changes, not merely the ability to compile. The original audit is `UltraTerraForged_1.21.1_Source_Audit.md` (first-pass static analysis).

| Audit | Implementation | Testing/remaining risk |
| --- | --- | --- |
| A01 | Scope spawn finder to UTF samplers | Compiled; vanilla sampler in-game test needed |
| A02 | Isolate density cache by WorldLookup identity | Compiled |
| A03 | Conservative density function bounds | Compiled |
| A04 | Remove global cache reset at RandomState construction; use weak cache registration and weak scheduler references | Needs repeated create/unload profiling |
| A05 | Redistribute forced river-mouth height discontinuities backwards respecting gorge limits | WaterContinuityTest; real seed checking still necessary |
| A06 | Post-carve connected fluid reconciliation; protect dry caves not connected to source water | In-game multi-chunk aquifer tests needed |
| A07 | Count preexisting source water in postprocessing | Compiled |
| A08 | Restore cell context in finally | Compiled |
| A09 | Honor terrain blacklist in structure rule | Need dedicated village/ancient-city seed validation |
| B01 | Parse performance_internal.conf; honor threadCount | Unit-tested on earlier branch |
| B02 | Check cache entry before capacity eviction under writer lock | Compiled |
| B03 | Never age/capacity evict pooled tiles still in use; explicit removal at final chunk drop | Failure/reconnect stress tests needed |
| B04 | Incremental site eviction | Unit-tested earlier |
| B05 | Compute lake warp only for intersecting river networks | Compiled |
| B06 | Bound/configure UTF worker pool | No comparative JFR benchmark |
| B07 | Close failed tile allocations after workers complete | Compiled |
| B08 | Keep pooled resources released after rejected restore | Compiled |
| B09 | Fix LazyCallable Runnable-null completion | Unit-tested earlier |
| C01 | Native Alex's Caves biome carving protection based on optional biome namespace | Biome placement, native structure integration and locator still require separate compatibility addon |
| C02 | Fork workflow no longer targets upstream releases | Upstream CurseForge/Modrinth publishing disabled |
| C03 | Wrap full buildSurface in exception-safe SurfaceRegion scope | Mixins on modded server need runtime check |
| C04 | Dedicated water regression tests and dual-loader CI | Full multi-mod NeoForge pack GameTests still needed |
| C05 | One-block water terraces and continuous reach interpolation with gorge-limited receiver smoothing | Must verify nonleaking rivers across seeds |

## Compatibility note

The optional Alex's Caves port is distributed separately and its biome-selection / chamber-placement internals cannot be compiled into the common UTF module without importing the port as a compile dependency. Native UTF carving protection has no hard dependency and only activates where Alex's cave biomes are already generated. Until the native biome injection has its own CI integration test, **retain the existing [Terraforged Alex's Caves Fix](https://github.com/Mikaaah/Terraforged-Alexs-Caves-Fix) as a separate required compatibility addon in the modpack**.

## Never claim runtime verification from CI compilation

In-game world generation on the actual Minecraft 1.21.1 NeoForge mod list, at known seeds/presets and newly generated chunks, is required before calling this fully validated.
