package nl.townsandkingdoms.wildshape.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.data.WildshapeDataGen;

public class WildshapeFabric implements ModInitializer, DataGeneratorEntrypoint {

	@Override
	public void onInitialize() {
		WildshapeCommon.bootstrap();
	}

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		WildshapeDataGen.generateResourcePacks(fabricDataGenerator::createPack);
	}
}