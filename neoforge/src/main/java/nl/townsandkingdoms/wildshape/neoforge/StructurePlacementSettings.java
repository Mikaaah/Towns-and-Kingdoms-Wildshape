package nl.townsandkingdoms.wildshape.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import nl.townsandkingdoms.wildshape.world.worldgen.structure.placement.StructurePlacementProfiles;

/** Terrain suitability only; structure grids and native exclusion rules belong to their registering mods. */
public final class StructurePlacementSettings {
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Independent structure terrain suitability checks. Restart the server to apply changes.",
                "Per-structure profiles are in config/wildshape/structure-placement.json.",
                "The legacy placement.enabled key is retained for existing installations.",
                "This setting controls slope, water, anchor and cavern-foundation checks only.")
                .push("placement");
        ENABLED = builder.define("enabled", true);
        builder.pop();
        SPEC = builder.build();
    }

    private StructurePlacementSettings() {}

    public static void register(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "wildshape/structures.toml");
        modBus.addListener(StructurePlacementSettings::loading);
        NeoForge.EVENT_BUS.addListener(StructurePlacementSettings::starting);
    }

    private static void loading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) apply();
    }

    private static void starting(ServerAboutToStartEvent event) { apply(); }

    private static void apply() {
        try {
            StructurePlacementProfiles.configure(ENABLED.get(),
                    FMLPaths.CONFIGDIR.get().resolve("wildshape/structure-placement.json"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read Wildshape structure terrain profiles", e);
        }
    }
}
