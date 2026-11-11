package nl.townsandkingdoms.wildshape.data.preset;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.data.preset.settings.StructureSettings.StructureSetEntry;

/**
 * Overrides the structure sets a preset changes, starting from the set as registered so structures, weights and
 * other placement values are kept. A disabled set keeps its placement but loses its structures; vanilla skips
 * structure sets that have no structures. Custom placements are patched by PresetStructureResources while world
 * resources load, so tagged exclusion zones retain the registry owner supplied by their native codec.
 */
public class PresetStructureSets {

	public static void bootstrap(Preset preset, BootstrapContext<StructureSet> ctx, HolderLookup.Provider registries) {
        HolderLookup.RegistryLookup<StructureSet> registered = registries.lookupOrThrow(Registries.STRUCTURE_SET);
		HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);
		HolderGetter<StructureSet> structureSets = ctx.lookup(Registries.STRUCTURE_SET);
		HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
		// Spread verified Overworld dungeon sets only. These registries are also
		// shared by other dimensions: a namespace-wide rule alters Nether content,
		// cave landmarks, resources and wildlife as well as adventure structures.
		registered.listElements().forEach(holder -> {
			ResourceLocation id = holder.key().location();
			if (preset.structures().entries.containsKey(id)) return;
			if (!(holder.value().placement() instanceof RandomSpreadStructurePlacement spread)) return;
			if (!canExport(spread)) return;
			int spacing = StructureSetPolicy.spacing(id, spread.spacing());
			if (spacing == spread.spacing()) return;
			StructureSetEntry entry = new StructureSetEntry();
			entry.spacing = spacing;
			ctx.register(holder.key(), apply(holder.value(), entry, structures, structureSets, biomes));
		});

		preset.structures().entries.forEach((id, entry) -> {
			ResourceKey<StructureSet> key = ResourceKey.create(Registries.STRUCTURE_SET, id);
			Optional<StructureSet> original = registered.get(key).map(Holder::value);
			if (original.isEmpty()) {
				// e.g. the preset was made with a mod that isn't installed now
				WildshapeCommon.LOGGER.debug("Structure set {} is absent from this snapshot; resolve it when world resources load", id);
				return;
			}
			if (!canExport(original.get().placement())) {
				WildshapeCommon.LOGGER.debug("Structure set {} uses a custom placement; apply its preset settings when world resources load", id);
				return;
			}
			ctx.register(key, apply(original.get(), entry, structures, structureSets, biomes));
		});
	}

	private static boolean canExport(StructurePlacement placement) {
		// Decoding a custom placement with source registry ops retains source-owned HolderSets. Those cannot be
		// serialized by the new preset registry set, as Integrated API's tagged super_exclusion_zone demonstrates.
		return placement.getClass() == RandomSpreadStructurePlacement.class
				|| placement.getClass() == ConcentricRingsStructurePlacement.class;
	}

	public static StructureSet apply(StructureSet original, StructureSetEntry entry, HolderGetter<Structure> structures, HolderGetter<StructureSet> structureSets, HolderGetter<Biome> biomes) {
        List<StructureSet.StructureSelectionEntry> entries = Boolean.FALSE.equals(entry.enabled) ? List.of() : original.structures().stream().map((selection) -> {
			return new StructureSet.StructureSelectionEntry(reference(structures, selection.structure()), selection.weight());
		}).toList();
		return new StructureSet(entries, placement(original.placement(), entry, structureSets, biomes));
	}

	private static StructurePlacement placement(StructurePlacement original, StructureSetEntry entry, HolderGetter<StructureSet> structureSets, HolderGetter<Biome> biomes) {
		float frequency = entry.frequency != null ? entry.frequency : original.frequency;
		int salt = entry.salt != null ? entry.salt : original.salt;
		Optional<StructurePlacement.ExclusionZone> exclusionZone = original.exclusionZone.map((zone) -> new StructurePlacement.ExclusionZone(reference(structureSets, zone.otherSet()), zone.chunkCount()));

		if (!canExport(original)) return original;

		if (original instanceof RandomSpreadStructurePlacement randomSpread) {
			int spacing = entry.spacing != null ? entry.spacing : randomSpread.spacing();
			int separation = entry.separation != null ? entry.separation : randomSpread.separation();
			// vanilla rejects placements where separation isn't smaller than spacing
			spacing = Math.max(spacing, 1);
			separation = Math.max(0, Math.min(separation, spacing - 1));
			return new RandomSpreadStructurePlacement(original.locateOffset, original.frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, randomSpread.spreadType());
		}
		if (original instanceof ConcentricRingsStructurePlacement rings) {
			int distance = entry.distance != null ? entry.distance : rings.distance();
			int spread = entry.spread != null ? entry.spread : rings.spread();
			int count = entry.count != null ? entry.count : rings.count();
			return new ConcentricRingsStructurePlacement(original.locateOffset, original.frequencyReductionMethod, frequency, salt, exclusionZone, distance, spread, count, reference(biomes, rings.preferredBiomes()));
		}
		// other placement types (from mods) can only be switched on and off
		return original;
	}

	// holders from the registered values belong to another registry set, so look them up again by key
	private static <T> Holder<T> reference(HolderGetter<T> getter, Holder<T> holder) {
		return holder.unwrapKey().<Holder<T>>map(getter::getOrThrow).orElse(holder);
	}

	private static <T> HolderSet<T> reference(HolderGetter<T> getter, HolderSet<T> holders) {
		return holders.unwrapKey().<HolderSet<T>>map(getter::getOrThrow).orElseGet(() -> {
			return HolderSet.direct(holders.stream().map((holder) -> reference(getter, holder)).toList());
		});
	}

}
