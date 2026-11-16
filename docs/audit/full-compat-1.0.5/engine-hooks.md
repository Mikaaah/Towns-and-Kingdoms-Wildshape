# Optional engine hook audit — 1.0.5

Inspected against the 1.0.4 performance branch, then checked the dependency bytecode
using Java 21 `javap`. ABI inspection establishes that named entry points exist;
it does not establish runtime Mixin application, graphical rendering or full-pack
behavior.

| Engine | Inspected artifact | Findings |
|---|---|---|
| TerraBlender | 4.1.0.8, `cf-940057-6054947.jar` | `SurfaceRuleManager` namespaced surface access and `NamespacedSurfaceRuleSource` constructor/accessors match the bridge. Existing runtime wrap removes the Minecraft namespace default before applying the Wildshape fallback. Region weights remain owned by the biome mods. |
| Alex's Caves | 2.0.10, `cf-1420348-7981977.jar` | Both `BiomeGenerationNoiseCondition.test` overloads exist. Existing centre-climate adapter and biome namespace carving protection remain in place; do not substitute arbitrary density functions or overwrite native cave features. |
| Streams Reflowing | 2.17.3 local performance fixture; 2.15.2, 2.16.2, 2.17.1 and 2.17.4 retained from recorded release inspections | Explicit ABI profiles remain in `StreamsVersion`; an uninspected release is not silently treated as the legacy ABI. Loader metadata enforces the inspected ranges. Keep Streams' own hydrology/configuration; native river current data does not prove every terrain seam or fluid column is visually correct. |
| Flow Control | 1.0.0 NeoForge 1.21.1 | `FlowOverrides.at(BlockGetter,int,int,int)` matches the optional reflection bridge used by `ActualWaterFlow`. A non-null native override prevents correction of manual flow directions. This does not require Create or load Flow Control when it is absent. |
| SkyLands | 0.9.4.1 source and existing performance fixture | The implementation places Overworld islands as features. Existing patch expands only the Overworld dimension-type build space, retaining saved terrain noise settings, and uses scoped approximate terrain probes for distant anchors. It is not a separate-dimension generator replacement. |
| World Preview NeoForged | 1.3.1, `world_preview-1.3.1.jar` | **Defect found and fixed:** the inspected port moved `SampleUtils` under `com.caeruleusTait`, while the old bridge targeted only `caeruleusTait`. Both constructor signatures and `randomState` / `registryAccess` fields match. The `@Pseudo` bridge now lists both package paths. Graphical preview execution still needs a client run. |
| Bye Pregen | 1.1.3.4, `cf-1567378-9353269.jar` | `ArenaNoiseFiller.fill`, `Request` constructor, and all six reflective accessors required by the existing culling bridge match. Unknown arena API versions remain unsupported; the fallback warning does not guarantee a changed filler can safely fill a culled noise chunk. |
| Generator Accelerator | 1.21.1-1.6.2, `cf-1323569-8168434.jar` | Mod ID is `generator_accelerator`; `bts$iterateNoiseColumn` and overwritten `getBaseColumn` match the existing culling bridge. This is the original GA generation. |
| Generator Accelerator 2 | 1.21.1-2.0.21.259, `cf-1323569-9551894.jar` | Distinct mod ID `generatoraccelerator2`; not covered by the GA1 bridge. **Interception gap found and patched:** bytecode replaces/cancels vanilla `applyCarvers` after its own `CarverRunner.apply`. A separately guarded mixin now wraps both its CPU `NoiseChunk.aquifer()` and GPU `GpuTerrain.aquifer()` results with Wildshape's raised-water guard. Cavern decoration now wraps the complete carving invocation rather than depending on a skipped vanilla TAIL injection. Runtime compatibility remains subject to the recorded fixture result; this is not GPU/OpenCL certification. |

## Optional registration cleanup

The common JSON already declared TerraBlender, World Preview, C2ME OpenCL, Bye
Pregen, Streams, Alex's Caves and Generator Accelerator mixins. `MixinPlugin`
returned those same entries again from `getMixins()`. The plugin now dynamically
returns only the two SkyLands entries, which were not in the JSON. All existing
families retain their mod-presence guard. The new Integrated API and Cataclysm
placement hooks are independently guarded by `integrated_api` and `cataclysm`.
The new GA2 carver hook is independently guarded by `generatoraccelerator2`; its
classes are never referenced as Java types from the common bridge.

`OptionalEngineMixinsTest` verifies every absent family is disabled, each family
is enabled only by its own mod, registrations are unique, both World Preview
package names are retained, and post-carving decoration wraps the whole call. These tests validate guard/registration behavior;
only a launched game validates transformed third-party classes.

## Deliberately retained compatibility codecs

`TBSurfaceRules` is a legacy named codec with no current direct producer call.
Deleting it could invalidate saved preset datapacks using that codec. It is not
removed merely because static call-site searches do not find a current writer.
The retained Streams ABIs and Mixin 0.17.3 annotation constraint likewise exist
for inspected dependencies, rather than being unneeded old-port artifacts.
