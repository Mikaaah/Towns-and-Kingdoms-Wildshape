package nl.townsandkingdoms.wildshape.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.DungeonDensityPolicy;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructurePlacementProfiles;

/** Server-authoritative policy, snapshotted before a world's generation begins. */
public final class StructurePlacementSettings {
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue MAJOR;
    private static final ModConfigSpec.IntValue MINOR;
    private static final ModConfigSpec.IntValue BUDGET;
    private static final ModConfigSpec.DoubleValue DUNGEON_DENSITY;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Cross-set structure screening. Restart the server to apply changes.",
                "Per-structure profiles are in config/wildshape/structure-placement.json.",
                "Clearance is between candidate centers; native linked layouts remain linked.")
                .push("placement");
        ENABLED = builder.define("enabled", true);
        MAJOR = builder.comment("Major dungeon/boss clearance in blocks.")
                .defineInRange("majorClearance", 256, 0, 512);
        MINOR = builder.comment("Minor structure/tower clearance in blocks.")
                .defineInRange("minorClearance", 112, 0, 512);
        BUDGET = builder.comment("Maximum native candidate checks per screening call.",
                "Screening preserves native placement if this budget would be exceeded.")
                .defineInRange("maxCandidateChecks", 8192, 64, 32768);
        DUNGEON_DENSITY = builder.comment("Expected native candidate opportunities per area for selected dungeon sets; not final spawn counts.",
                "1.25 means roughly 25% more candidate opportunities: grid spacing is divided by sqrt(1.25).",
                "Set 1.0 to preserve native grids. Integer chunk steps, terrain checks and cross-set clearance affect results.",
                "Exact set overrides and linked companions are configured in wildshape/dungeon-density.json.")
                .defineInRange("candidateOpportunityMultiplier", DungeonDensityPolicy.DEFAULT_MULTIPLIER, 0.25D, 4.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private StructurePlacementSettings() {}

    public static void register(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "wildshape/structures.toml");
        // COMMON config loads before worldgen registry resources are decoded, on dedicated and integrated servers.
        modBus.addListener(StructurePlacementSettings::loading);
        NeoForge.EVENT_BUS.addListener(StructurePlacementSettings::starting);
    }

    private static void loading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) apply();
    }

    private static void starting(ServerAboutToStartEvent event) { apply(); }

    private static void apply() {
        try {
            DungeonDensityPolicy.configure(DUNGEON_DENSITY.get(),
                    FMLPaths.CONFIGDIR.get().resolve("wildshape/dungeon-density.json"));
            StructurePlacementProfiles.configure(ENABLED.get(), MAJOR.get(), MINOR.get(), BUDGET.get(),
                    FMLPaths.CONFIGDIR.get().resolve("wildshape/structure-placement.json"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read Wildshape structure placement policy", e);
        }
    }
}
