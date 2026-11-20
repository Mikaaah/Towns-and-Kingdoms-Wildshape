# Giant cavern clay pools and unsupported source water

## Actual generation order

1. Noise terrain and aquifers are generated.
2. `MixinNoiseBasedChunkGenerator.wildshape$scopedSurface` fills raised water, builds the normal surface, and calls Wildshape's cave carving. Its existing repair/postprocessing pass runs here.
3. Native AIR carvers (or the accelerator's equivalent) run. The complete wrapping call then applies Wildshape cavern decorations and biome painting.
4. Biome decoration places native lush vegetation, including waterlogged clay patches, using those biome identities.

This order means an early cave-fluid repair cannot repair new clay/water that a later native vegetation feature places. Giant LUSH and LANTERN chambers deliberately use a lush biome identity: native ceiling moss, vegetation and ambience are retained by this patch, rather than suppressing the whole biome.

## Source finding

The declared 1.21.1 `VegetationPatchFeature.placeGround(WorldGenLevel, VegetationPatchConfiguration, Predicate<BlockState>, RandomSource, BlockPos.MutableBlockPos, int)` method allows partial replacement depth: encountering a nonreplaceable block after at least one replaced layer returns success. The waterlogged subclass first places that ground and later tests the four sides and only one bottom face before replacing interior positions with water. Thin rock/clay ledges can consequently become water trays suspended over a larger cave. This was checked against the actual 1.21.1 vanilla client classes with official Mojang mappings (`client.txt`, object `2244b6f072256667bcd9a73df124d6c58de77992`).

## Narrow native-pool correction

The new required HEAD injection into `placeGround` checks the complete sampled replacement depth plus one supporting block before the first write. It applies only to the native waterlogged vegetation feature, floor direction, an active Wildshape generator, and the unprotected vanilla lush biome. Every checked support block must be dry and upward-sturdy. Reads stay in the same X/Z column; no neighbors are generated. Supported native pools keep their original placements and random draws. Ordinary moss features, other dimensions, Alex's Caves and other modded cave biomes are outside scope. Failed preflight writes neither clay nor water.

## Giant lake source correction

A giant cavern can overlap an already-open cave, or a later Wildshape cut can remove a lake's bed. New source water written by the giant lake is now marked separately from other authored fluids. The carving pass drains only a marked giant-lake column whose bottom is air, continuing upward through its own now-unsupported sources. Supported lake basins remain. Sea caves, other authored waterfalls/rivers, natural aquifers, lava and waterlogged blocks are outside this policy. It does not delete fluids by block type or scan the whole chunk after foreign feature placement.

This pass runs at the end of Wildshape's cave carving. It does not claim to identify or rewrite arbitrary fluid sources introduced by later mods or saved older chunks. Existing terrain is not repaired automatically.

## Focused coverage

- Full requested clay depth and support below it are required.
- Thin ledges fail before any ground/fluids are written.
- Supported real pool beds remain eligible.
- Fluid and waterlogged support blocks are excluded from new clay beds.
- Vertical bounds and same-column reads are checked.
- Protected/foreign cave biomes are excluded.
- The actual native method signature, required cancellable HEAD hook and mixin registration are checked.
- Hanging giant-lake columns drain; supported giant basins, other authored water, natural water and lava remain.

The final build/runtime record owns execution results; these source tests alone do not establish full-modpack visual verification or repair previously generated chunks.
