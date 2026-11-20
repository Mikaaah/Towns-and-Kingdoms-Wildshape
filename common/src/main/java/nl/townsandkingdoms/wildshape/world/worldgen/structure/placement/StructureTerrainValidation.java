package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;

/** Bounded conservative screening, not a terrain-flattening replacement for a mod's own foundation builder. */
public final class StructureTerrainValidation {
    private static final int MAX_FOUNDATION_PIECES = 64;
    private StructureTerrainValidation() {}
    @FunctionalInterface public interface Sampler { Sample sample(int x, int z); }
    @FunctionalInterface public interface CavernSampler { boolean hollowAt(int x, int y, int z); }
    public record Sample(int groundY, int waterY) {}

    public static boolean valid(RandomState state, ResourceLocation id, BoundingBox box) {
        if (!( (Object) state instanceof WildshapeRandomState wildshape) || wildshape.generatorContext() == null) return true;
        StructurePlacementProfiles.Settings settings = StructurePlacementProfiles.settings();
        StructurePlacementProfiles.Profile profile = settings.structure(id);
        if (!settings.enabled() || profile == null || !profile.surface()) return true;
        var context = wildshape.generatorContext();
        // Raw deterministic terrain only. Never depend on whether a filtered tile happens to have been cached.
        // This avoids cold erosion tiles and river-network surveys for speculative structure footprints.
        return valid(box, profile, (x, z) -> {
            var cell = context.localHeightmap.get().sampleClimateGround(x, z);
            return new Sample(context.levels.scale(cell.height), Math.max(context.levels.waterY, context.levels.scale(cell.waterLevel)));
        });
    }

    /** Optional per-structure native anchor check. Zero disables it for basements, mountainside and sky designs. */
    public static boolean validAnchor(RandomState state, ResourceLocation id, net.minecraft.core.BlockPos anchor) {
        var settings = StructurePlacementProfiles.settings();
        var profile = settings.structure(id);
        if (!settings.enabled() || profile == null || !profile.surface() || profile.maxAnchorOffset() == 0) return true;
        if (!((Object)state instanceof WildshapeRandomState wildshape) || wildshape.generatorContext() == null) return true;
        var context = wildshape.generatorContext();
        var cell = context.localHeightmap.get().sampleClimateGround(anchor.getX(), anchor.getZ());
        return validAnchor(profile, anchor.getY(), context.levels.scale(cell.height));
    }
    public static boolean validAnchor(StructurePlacementProfiles.Profile profile, int anchorY, int predictedGroundY) {
        return profile.maxAnchorOffset() == 0 || Math.abs((long)anchorY - predictedGroundY) <= profile.maxAnchorOffset();
    }

    /** Reject a complete opt-in buried start before its pieces become protected floating rock islands. */
    public static boolean validCavernSupport(RandomState state, ResourceLocation id,
            java.util.List<net.minecraft.world.level.levelgen.structure.StructurePiece> pieces, int bottom) {
        var settings = StructurePlacementProfiles.settings();
        var profile = settings.structure(id);
        if (!settings.enabled() || profile == null || !profile.rejectCavernVoid()) return true;
        // Locators may inspect thousands of native stubs. Do not turn them into
        // cavern/river surveys; actual chunk generation performs this validation.
        if (nl.townsandkingdoms.wildshape.world.worldgen.WorldGenFlags.structureSearch()) return true;
        if (!((Object)state instanceof WildshapeRandomState wildshape) || wildshape.generatorContext() == null) return true;
        var caves = wildshape.generatorContext().caveFeatures;
        int count = Math.min(MAX_FOUNDATION_PIECES, pieces.size());
        java.util.List<BoundingBox> bounds = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) bounds.add(pieces.get((int)((long)i * pieces.size() / count)).getBoundingBox());
        return validCavernSupport(profile, bounds, (x, y, z) -> caves.wouldCarveFoundation(x, y, z, bottom));
    }

    /** Bounded per-piece support tests, rather than the empty space between separate jigsaw corridors. */
    public static boolean validCavernSupport(StructurePlacementProfiles.Profile profile,
            java.util.List<BoundingBox> pieces, CavernSampler sampler) {
        if (!profile.rejectCavernVoid() || profile.layer().equals("disabled")) return true;
        int count = Math.min(MAX_FOUNDATION_PIECES, pieces.size());
        for (int i = 0; i < count; i++) {
            BoundingBox box = pieces.get((int)((long)i * pieces.size() / count));
            int midX = (int)(((long)box.minX() + box.maxX()) >> 1);
            int midZ = (int)(((long)box.minZ() + box.maxZ()) >> 1);
            int supportY = box.minY() - 1;
            for (int ix = 0; ix < 3; ix++) for (int iz = 0; iz < 3; iz++) {
                int x = ix == 0 ? box.minX() : ix == 1 ? midX : box.maxX();
                int z = iz == 0 ? box.minZ() : iz == 1 ? midZ : box.maxZ();
                if (sampler.hollowAt(x, supportY, z)) return false;
            }
        }
        return true;
    }

    /** Nine samples cover the completed native bounding box, including its center and edge midpoints. */
    public static boolean valid(BoundingBox box, StructurePlacementProfiles.Profile profile, Sampler sampler) {
        if (!profile.surface()) return true;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        int midX = (int)(((long)box.minX() + box.maxX()) >> 1);
        int midZ = (int)(((long)box.minZ() + box.maxZ()) >> 1);
        for (int ix = 0; ix < 3; ix++) for (int iz = 0; iz < 3; iz++) {
            int x = ix == 0 ? box.minX() : ix == 1 ? midX : box.maxX();
            int z = iz == 0 ? box.minZ() : iz == 1 ? midZ : box.maxZ();
            Sample sample = sampler.sample(x, z);
            min = Math.min(min, sample.groundY); max = Math.max(max, sample.groundY);
            if (profile.rejectWater() && sample.waterY > sample.groundY) return false;
            if ((long)max - min > profile.maxSlopeDelta()) return false;
        }
        return true;
    }
}
