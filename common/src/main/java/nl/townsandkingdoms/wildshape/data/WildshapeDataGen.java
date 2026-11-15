package nl.townsandkingdoms.wildshape.data;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataGenerator.PackGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import nl.townsandkingdoms.wildshape.client.data.WildshapeLanguageProvider;
import nl.townsandkingdoms.wildshape.client.data.WildshapeTranslationKeys;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.data.preset.tags.PresetBiomeTagsProvider;
import nl.townsandkingdoms.wildshape.data.preset.tags.PresetBlockTagsProvider;
import nl.townsandkingdoms.wildshape.data.preset.tags.PresetSurfaceLayerProvider;
import nl.townsandkingdoms.wildshape.platform.DataGenUtil;

public class WildshapeDataGen {
	public static final String DATAPACK_PATH = "data/wildshape/datapacks";
	
	public static void generateResourcePacks(ResourcePackFactory resourcePackFactory) {
		DataGenerator.PackGenerator pack = resourcePackFactory.createPack();

		pack.addProvider(WildshapeLanguageProvider.EnglishUS::new);
		pack.addProvider((PackOutput output) -> PackMetadataGenerator.forFeaturePack(output, Component.translatable(WildshapeTranslationKeys.METADATA_DESCRIPTION)));
	}
	
	@Deprecated
	public static DataGenerator makePreset(Preset preset, HolderLookup.Provider registryAccess, Path dataGenPath, Path dataGenOutputPath) {
		DataGenerator dataGenerator = new DataGenerator(dataGenPath, SharedConstants.getCurrentVersion(), true);
		PackGenerator packGenerator = dataGenerator.new PackGenerator(true, "preset", new PackOutput(dataGenOutputPath));
		// built up front rather than on the common pool: on Forge, classes first loaded from common pool threads can't see mod classes
		CompletableFuture<HolderLookup.Provider> lookup = CompletableFuture.completedFuture(preset.buildPatch(registryAccess));
		
		packGenerator.addProvider((output) -> {
			return DataGenUtil.createRegistryProvider(output, lookup);
		});
		packGenerator.addProvider((output) -> {
			return new PresetBlockTagsProvider(output, lookup);
		});
		packGenerator.addProvider((output) -> {
			return new PresetSurfaceLayerProvider(preset, output, lookup);
		});
		packGenerator.addProvider((output) -> {
			return new PresetBiomeTagsProvider(preset, output, CompletableFuture.completedFuture(registryAccess));
		});
		packGenerator.addProvider((output) -> {
			return PackMetadataGenerator.forFeaturePack(output, Component.translatable(WildshapeTranslationKeys.PRESET_METADATA_DESCRIPTION));
		});
		return dataGenerator;
	}
	
	public interface ResourcePackFactory {
		DataGenerator.PackGenerator createPack();
	}
	
	public interface DataPackFactory {
		DataGenerator.PackGenerator createPack(ResourceLocation id);
	}
}
