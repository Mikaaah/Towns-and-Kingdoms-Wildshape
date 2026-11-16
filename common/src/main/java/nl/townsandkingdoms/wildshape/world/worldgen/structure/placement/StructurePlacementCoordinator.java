package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import nl.townsandkingdoms.wildshape.mixin.AccessorChunkGeneratorStructureState;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.WorldGenFlags;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructurePlacementProfiles.Profile;

/**
 * Deterministic thinning of native candidate sites; never queries or creates neighboring chunks.
 * A lower-ranked candidate cannot suppress its higher-ranked neighbor. Native biome/geometry rejection may still
 * leave a hole: this is deliberately not a claim that every reserved candidate becomes a dungeon.
 */
public final class StructurePlacementCoordinator {
    private static final ThreadLocal<Boolean> NATIVE = ThreadLocal.withInitial(() -> false);
    private static final Map<ChunkGeneratorStructureState, Index> INDEXES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final LongAdder BUDGET_BYPASSES = new LongAdder();
    private StructurePlacementCoordinator() {}

    /** Keep the complete native call (including other-set exclusion checks) outside our arbitration. */
    public static boolean filterNative(StructurePlacement placement, ChunkGeneratorStructureState state, int x, int z,
            java.util.function.BooleanSupplier nativeCall) {
        boolean nested = NATIVE.get();
        NATIVE.set(true);
        boolean result;
        try { result = nativeCall.getAsBoolean(); }
        finally { NATIVE.set(nested); }
        return result && (nested || accepts(placement, state, x, z));
    }

    static boolean nativeScopeActive() { return NATIVE.get(); }

    public static boolean accepts(StructurePlacement placement, ChunkGeneratorStructureState state, int x, int z) {
        if (NATIVE.get() || !((Object)state.randomState() instanceof WildshapeRandomState wildshape) || wildshape.generatorContext() == null) return true;
        Index index = index(state);
        if (!index.settings.enabled() || !index.withinBudget) return true;
        Site self = index.byPlacement.get(placement);
        if (self == null || self.profile.clearance() == 0) return true;
        if (index.settings.approximateBiomeScreening() && !eligible(self, state, index.biomeSource, wildshape.generatorContext(), new ChunkPos(x, z))) return false;
        long priority = RegionalClearance.priority(state.getLevelSeed(), self.id.toString(), x, z);
        int checks = 0;
        for (Site other : index.sites) {
            if (!compatible(self.profile, other.profile)) continue;
            int radius = Math.max(self.profile.clearance(), other.profile.clearance());
            int chunks = (radius + 15) >> 4;
            int spacing = other.placement.spacing();
            int firstX = Math.floorDiv(x - chunks, spacing), lastX = Math.floorDiv(x + chunks, spacing);
            int firstZ = Math.floorDiv(z - chunks, spacing), lastZ = Math.floorDiv(z + chunks, spacing);
            for (int rx = firstX; rx <= lastX; rx++) for (int rz = firstZ; rz <= lastZ; rz++) {
                if (++checks > index.settings.maxCandidateChecks()) { BUDGET_BYPASSES.increment(); return true; }
                ChunkPos candidate = other.placement.getPotentialStructureChunk(state.getLevelSeed(), rx * spacing, rz * spacing);
                if (self == other && candidate.x == x && candidate.z == z) continue;
                if (!RegionalClearance.conflicts(x, z, self.profile.clearance(), candidate.x, candidate.z, other.profile.clearance())) continue;
                long otherPriority = RegionalClearance.priority(state.getLevelSeed(), other.id.toString(), candidate.x, candidate.z);
                if (RegionalClearance.wins(priority, self.id.toString(), x, z, otherPriority, other.id.toString(), candidate.x, candidate.z)) continue;
                if (nativeCandidate(other.placement, state, candidate.x, candidate.z) && (!index.settings.approximateBiomeScreening() || eligible(other, state, index.biomeSource, wildshape.generatorContext(), candidate))) return false;
            }
        }
        return true;
    }
    private static boolean nativeCandidate(StructurePlacement placement, ChunkGeneratorStructureState state, int x, int z) {
        boolean previous = NATIVE.get(); NATIVE.set(true);
        try { return placement.isStructureChunk(state, x, z); }
        finally { NATIVE.set(previous); }
    }
    private static boolean compatible(Profile a, Profile b) {
        if (!a.linkedGroup().isEmpty() && a.linkedGroup().equals(b.linkedGroup())) return false;
        // Native underwater, underground and sky layouts form separate strata. Surface and unclassified land share clearance.
        String aa = a.surface() ? "native" : a.layer(), bb = b.surface() ? "native" : b.layer();
        return !aa.equals("disabled") && aa.equals(bb);
    }
    private static boolean eligible(Site site, ChunkGeneratorStructureState state, BiomeSource source, GeneratorContext context, ChunkPos pos) {
        var ground = context.localHeightmap.get().sampleClimateGround(pos.getMiddleBlockX(), pos.getMiddleBlockZ());
        int surface = context.levels.scale(ground.height);
        int y = switch (site.profile.layer()) {
            case "sky" -> Math.max(256, surface + 64);
            case "ocean" -> context.levels.waterY - 16;
            case "underground" -> Math.max(-32, surface - 48);
            default -> surface;
        };
        // Cheap candidate-biome screening. It does not run a mod's native start builder or reserve a successful start.
        return WorldGenFlags.locatingStructure(() -> {
            int[] heights = {y, y - 16, y + 16, 0, context.levels.waterY};
            for (int sampleY : heights) {
                var biome = source.getNoiseBiome(QuartPos.fromBlock(pos.getMiddleBlockX()), QuartPos.fromBlock(sampleY), QuartPos.fromBlock(pos.getMiddleBlockZ()), state.randomState().sampler());
                for (StructureSet.StructureSelectionEntry entry : site.set.structures()) if (entry.structure().value().biomes().contains(biome)) return true;
            }
            return false;
        });
    }
    private static Index index(ChunkGeneratorStructureState state) {
        synchronized (INDEXES) { return INDEXES.computeIfAbsent(state, Index::new); }
    }
    private record Site(ResourceLocation id, StructureSet set, RandomSpreadStructurePlacement placement, Profile profile) {}
    private static final class Index {
        final StructurePlacementProfiles.Settings settings = StructurePlacementProfiles.settings();
        final List<Site> sites;
        final IdentityHashMap<StructurePlacement, Site> byPlacement = new IdentityHashMap<>();
        final BiomeSource biomeSource;
        final boolean withinBudget;
        final long candidateUpperBound;
        Index(ChunkGeneratorStructureState state) {
            biomeSource = ((AccessorChunkGeneratorStructureState)(Object)state).wildshape$biomeSource();
            List<Site> found = new ArrayList<>();
            for (Holder<StructureSet> holder : state.possibleStructureSets().stream().sorted(java.util.Comparator.comparing(h -> h.unwrapKey().map(k -> k.location().toString()).orElse(""))).toList()) {
                ResourceLocation id = holder.unwrapKey().map(k -> k.location()).orElse(null);
                if (id == null || !(holder.value().placement() instanceof RandomSpreadStructurePlacement random) || random.spacing() <= 0 || !supportsPlacement(random)) continue;
                Profile profile = settings.set(id);
                if (profile == null || profile.layer().equals("disabled") || profile.clearance() == 0) continue;
                Site site = new Site(id, holder.value(), random, profile);
                // Two registered sets may deliberately share a placement object; keep their native pairing intact.
                if (!byPlacement.containsKey(random)) { found.add(site); byPlacement.put(random, site); }
            }
            found.sort(java.util.Comparator.comparing(s -> s.id.toString()));
            sites = List.copyOf(found);
            long worst = 0;
            for (Site self : sites) {
                long total = 0;
                for (Site other : sites) if (compatible(self.profile, other.profile)) {
                    int radius = (Math.max(self.profile.clearance(), other.profile.clearance()) + 15) >> 4;
                    long regions = (2L * radius + other.placement.spacing() - 1) / other.placement.spacing() + 2;
                    total += regions * regions;
                }
                worst = Math.max(worst, total);
            }
            candidateUpperBound = worst;
            withinBudget = worst <= settings.maxCandidateChecks();
            if (!withinBudget) {
                BUDGET_BYPASSES.increment();
                nl.townsandkingdoms.wildshape.WildshapeCommon.LOGGER.warn("Structure clearance disabled for this world: bounded candidate work {} exceeds configured budget {}", worst, settings.maxCandidateChecks());
            }
        }
    }
    public static boolean supportsPlacement(StructurePlacement placement) {
        String name = placement.getClass().getName();
        if (name.equals("com.craisinlord.integrated_api.world.structures.placements.AdvancedRandomSpread") || name.equals("com.github.L_Ender.cataclysm.world.structures.placements.CataclysmRandomSpread")) return true;
        try {
            // Unknown overrides are not indexed as invisible blockers. Subclasses inheriting the base hook are safe.
            if (placement.getClass().getMethod("isStructureChunk", ChunkGeneratorStructureState.class, int.class, int.class).getDeclaringClass() != StructurePlacement.class) return false;
            for (Class<?> owner = placement.getClass(); owner != null; owner = owner.getSuperclass()) {
                try {
                    owner.getDeclaredMethod("isPlacementChunk", ChunkGeneratorStructureState.class, int.class, int.class);
                    return owner == RandomSpreadStructurePlacement.class;
                } catch (NoSuchMethodException inherited) { /* Continue to the native grid algorithm. */ }
            }
            return false;
        } catch (NoSuchMethodException impossible) { return false; }
    }
    /** Installed-server introspection: bounded metadata, without forcing a single chunk or full terrain tile. */
    public static Map<String, Object> audit(ServerLevel level) {
        var state = level.getChunkSource().getGeneratorState();
        Index index = index(state);
        List<Accepted> accepted = new ArrayList<>();
        int evaluated = 0;
        long began = System.nanoTime();
        outer: for (int rx = -1; rx <= 1; rx++) for (int rz = -1; rz <= 1; rz++) {
            for (Site site : index.sites) {
                if (!compatible(site.profile, site.profile)) continue;
                int spacing = site.placement.spacing();
                if (evaluated++ >= 128) break outer;
                ChunkPos pos = site.placement.getPotentialStructureChunk(state.getLevelSeed(), rx * spacing, rz * spacing);
                if (site.placement.isStructureChunk(state, pos.x, pos.z)) accepted.add(new Accepted(site, pos));
            }
        }
        int conflicts = 0;
        for (int a = 0; a < accepted.size(); a++) for (int b = 0; b < a; b++) {
            Accepted aa = accepted.get(a), bb = accepted.get(b);
            if (compatible(aa.site.profile, bb.site.profile) && RegionalClearance.conflicts(aa.pos.x, aa.pos.z, aa.site.profile.clearance(), bb.pos.x, bb.pos.z, bb.site.profile.clearance())) conflicts++;
        }
        Map<String,Object> result = new java.util.LinkedHashMap<>();
        result.put("enabled", index.settings.enabled()); result.put("approximateBiomeScreening", index.settings.approximateBiomeScreening()); result.put("withinBudget", index.withinBudget);
        result.put("indexedSets", index.sites.stream().map(site -> site.id.toString()).toList());
        result.put("candidateBudget", index.settings.maxCandidateChecks()); result.put("candidateUpperBound",index.candidateUpperBound);
        result.put("budgetBypasses", BUDGET_BYPASSES.sum()); result.put("registeredSets",state.possibleStructureSets().size());
        result.put("sampledNativeCandidates",Math.min(evaluated,128)); result.put("sampledAcceptedCandidates",accepted.size());
        result.put("sampledClearanceViolations",conflicts); result.put("sampleMillis", (System.nanoTime()-began)/1_000_000);
        if (index.settings.enabled() && index.withinBudget && conflicts != 0) throw new IllegalStateException("Cross-set structure clearance violations in installed runtime: " + result);
        return Map.copyOf(result);
    }
    private record Accepted(Site site, ChunkPos pos) {}
}
