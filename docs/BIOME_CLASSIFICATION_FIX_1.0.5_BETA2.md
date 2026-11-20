# River and mushroom-island biome classification fix

## Root causes

1. `Heightmap.applyClimate` correctly assigned valley weirdness to rivers, but `applyPost` subsequently chose continentalness from the broad continent-noise mask. A submerged inland channel or elevated river could therefore keep an ocean climate tuple. River width was incidental; the missing distinction was channel identity versus ocean-mask continentalness.
2. Mushroom island identity existed only as `terrain == MUSHROOM_FIELDS`, which downstream river/coast/landform labels could overwrite before final climate assignment. The island populator additionally withheld the mushroom label from its dry lowest two blocks of shoreline, allowing nonfungal shore biomes on actual islands.

## Implementation

- `Cell.mushroomIsland` records provenance only where the real rare mushroom-island populator raises dry land. It travels through ordinary cell copies and resets with pooled cells. Final climate preserves the mushroom continentalness band across subsequent terrain-label changes. Unraised submerged outer slopes and ordinary islands do not receive this marker.
- Dry mushroom-island shore joins the island habitat from sea level, replacing the previous sea-plus-two-block requirement. Island placement, rarity, radius and terrain rise remain unchanged.
- Final climate for actual river terrain is kept out of ocean bands if the water is raised above sea level or the channel is landward of the configured beach boundary. Low coastal mouths/estuaries retain their existing coastal/ocean classification. No global ocean-biome blacklist, per-column allocation or additional terrain sampling was added.

## Pinned compatibility inspection

The CB-1.1.4 manifest pins TerraBlender CF940057/6054947, BOP CF220318/8288121 and Nature's Spirit CF1044992/9326605. TerraBlender selects the closest parameter point from the active region tree; it does not always fall back to vanilla based on distance. Inspection was therefore necessary before relying only on shared climate values.

All five Nature's Spirit parameter builders explicitly include vanilla Mushroom Fields over continentalness -1.2 to -1.05 with full climate coverage. BOP's Overworld builder includes the same mushroom continentalness band and native Mushroom Fields fallback. Hybrid Aquatic's Lithostitched injectors target specific vanilla marine types and River; its native tropical river replacement remains eligible after the shared climate correction. No missing fungal region entry was found that would justify a final hard-coded biome override.

The supplied `GenOne.json` has deep/shallow/beach/coast/inland controls 0.1 / 0.25 / 0.326 / 0.448 / 0.5 and world height 512; the correction uses each world's own levels and control points rather than replacing those saved settings. Artifact hashes and method-level observations are recorded in `docs/audit/biome-classification/pinned-region-inspection.json`.

## Validation and migration scope

Focused regression cases exercise wide inland river beds below sea level, raised rivers beneath an ocean mask, cold rivers, real ocean/estuary negatives, actual mushroom provenance surviving river/beach/coast labels, ordinary-island/submerged-slope negatives and pooled-cell copy/reset. The existing mushroom island generation survey also checks actual provenance. Build and runtime results are recorded separately by the release workflow.

Replacing the JAR changes classification for newly generated chunks. It does not rewrite biome arrays or terrain in existing or pregenerated chunks. Old saved terrain settings, seed and preset remain in use; old/new chunk boundaries can show biome changes because already generated chunks retain their previous data.
