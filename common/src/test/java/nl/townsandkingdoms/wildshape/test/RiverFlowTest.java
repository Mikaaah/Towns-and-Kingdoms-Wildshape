package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Heightmap;
import nl.townsandkingdoms.wildshape.world.worldgen.rivermap.RiverFlow;
import nl.townsandkingdoms.wildshape.world.worldgen.rivermap.RiverRoutes;
import nl.townsandkingdoms.wildshape.world.worldgen.rivermap.Rivermap;
import nl.townsandkingdoms.wildshape.world.worldgen.rivermap.river.Network;
import nl.townsandkingdoms.wildshape.world.worldgen.terrain.populator.RiverPopulator;
import nl.townsandkingdoms.wildshape.world.worldgen.util.PosUtil;

/**
 * The way a river's water runs, for Streams Reflowing's currents (compat/streams): along the river, from its source to
 * its mouth, wherever it winds.
 */
public class RiverFlowTest {

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void waterRunsDownTheRiver() throws ReflectiveOperationException {
		GeneratorContext context = GeneratorContext.makeUncached(BuiltinPresetRenderTest.presets().get("default").get(), PresetRenderer.SEED, PresetRenderer.TILE_SIZE, 0, 6);
		Heightmap heightmap = context.localHeightmap.get();
		Field networks = Rivermap.class.getDeclaredField("networks");
		networks.setAccessible(true);
		int[] counts = new int[3];
		for (float[] at : new float[][] { { 0.0F, 0.0F }, { 6000.0F, 2000.0F }, { -5000.0F, 4000.0F } }) {
			Cell cell = new Cell();
			heightmap.applyContinent(cell, at[0], at[1]);
			Rivermap rivermap = Rivermap.get(cell, null, heightmap);
			for (Network network : (Network[]) networks.get(rivermap)) {
				check(rivermap, network, counts);
			}
		}
		int places = counts[0];
		int missed = counts[1];
		int wrong = counts[2];
		System.out.println(places + " places in rivers, " + missed + " with no river found, " + wrong + " running the wrong way");
		assertTrue(places > 200, "only " + places + " places");
		// where rivers meet, the place may be in the other river
		assertTrue(missed <= places / 50, missed + " of " + places + " places in the middle of a river found none");
		assertTrue(wrong <= places / 20, wrong + " of " + places + " places ran the wrong way");
	}

	private static void check(Rivermap rivermap, Network network, int[] counts) {
		RiverPopulator carver = network.riverCarver();
		for (float t = 0.05F; t < 0.95F; t += 0.05F) {
			long here = RiverRoutes.channelInWorld(carver, t);
			long up = RiverRoutes.channelInWorld(carver, t - 0.02F);
			long down = RiverRoutes.channelInWorld(carver, t + 0.02F);
			float x = PosUtil.unpackLeftf(here);
			float z = PosUtil.unpackRightf(here);
			float dx = PosUtil.unpackLeftf(down) - PosUtil.unpackLeftf(up);
			float dz = PosUtil.unpackRightf(down) - PosUtil.unpackRightf(up);
			float length = (float) Math.sqrt(dx * dx + dz * dz);
			counts[0]++;
			RiverFlow.Sample sample = RiverFlow.sample(rivermap, x, z);
			if (sample == null) {
				counts[1]++;
			} else if ((sample.dirX() * dx + sample.dirZ() * dz) / length < 0.7F) {
				counts[2]++;
			}
		}
		for (Network child : network.children()) {
			check(rivermap, child, counts);
		}
	}
}
