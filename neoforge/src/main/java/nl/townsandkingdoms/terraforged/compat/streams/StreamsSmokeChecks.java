package nl.townsandkingdoms.terraforged.compat.streams;

import java.util.Map;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import com.pandaismyname1.ultraterraforged.compat.streams.RiverCurrents;

/** Opt-in live checks shared by both supported Streams releases. */
public final class StreamsSmokeChecks {
    private StreamsSmokeChecks() {}

    public static void verify(ServerLevel level, Map<String, Object> report) throws Exception {
        boolean enabled = StreamsRuntime.waterFlowEnabled() && StreamsRuntime.riverFlowEnabled();
        boolean expectDisabled = Boolean.getBoolean("wildshape.expectStreamsInactive");
        if (enabled == expectDisabled) throw new AssertionError("Streams setting bindings disagree with fixture");
        if ((UtfStreams.context(level) != null) != enabled) throw new AssertionError("Streams context ignores settings");
        report.put("streamsSettingsRespected", true);
        if (!enabled) return;
        for (int heading = 0; heading < 8; heading++) {
            double radians = heading * Math.PI / 4;
            byte encoded = StreamFlowGrid.encode(Math.cos(radians), Math.sin(radians));
            if (StreamsRuntime.vx(encoded) * Math.cos(radians) + StreamsRuntime.vz(encoded) * Math.sin(radians) < .99)
                throw new AssertionError("Streams angle binding changed for heading " + heading);
        }
        report.put("streamsEightHeadingBindings", true);
        var context = UtfStreams.context(level);
        var engine = dev.streamsreflowing.worldgen.StreamsWorldgen.engine(level);
        if (context.cache.provideAtChunkIfPresent(42000 >> 4, -36000 >> 4) != null)
            throw new AssertionError("Survey probe was already generated");
        double surveyed = engine.sampler().height(42000, -36000);
        if (!Double.isFinite(surveyed) || context.cache.provideAtChunkIfPresent(42000 >> 4, -36000 >> 4) != null)
            throw new AssertionError("Streams survey allocated a full terrain tile");
        if (com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags.fastLookups())
            throw new AssertionError("Streams survey leaked its lookup scope");
        report.put("streamsSurveyDoesNotGenerateTiles", true);
        var sample = IntegrationChecks.findElevatedMain(context);
        if (sample == null) throw new AssertionError("No elevated native river sample");
        var pos = new BlockPos(sample.x(), sample.surfaceY(), sample.z());
        level.getChunkAt(pos);
        if (!level.getFluidState(pos).is(FluidTags.WATER)) throw new AssertionError("Elevated river has no actual water");
        var layers = RiverCurrents.INSTANCE.layers(level, null, sample.x() >> 4, sample.z() >> 4);
        if (layers.isEmpty()) throw new AssertionError("No native producer layer");
        var layer = layers.getFirst();
        byte[] angles = (byte[]) layer.getClass().getMethod(StreamsRuntime.modern() ? "proprietary1" : "angle").invoke(layer);
        int index = (sample.x() & 15) + (sample.z() & 15) * 16;
        if (angles[index] == -1) throw new AssertionError("Elevated river lacks a current");
        if (StreamsRuntime.modern()) {
            var build = java.util.Arrays.stream(StreamFlowGrid.class.getMethods()).filter(method ->
                    method.getName().equals("proprietary1") && method.getReturnType() == byte[].class
                    && method.getParameterCount() == 7).findFirst().orElseThrow();
            // Wildshape supplies its own native river layer; vanilla-river classification is disabled.
            build.invoke(null, level, engine, null, null, false, sample.x() >> 4, sample.z() >> 4);
            if (StreamsRuntime.angleAt(sample.x(), sample.surfaceY(), sample.z()) == -1)
                throw new AssertionError("Native current did not reach the public cached grid");
            var physics = StreamFlowGrid.class.getMethod("niceJohn1", net.minecraft.world.level.BlockGetter.class,
                    int.class, int.class, int.class);
            if ((byte) physics.invoke(null, level, sample.x(), sample.surfaceY(), sample.z()) == -1)
                throw new AssertionError("Native current did not reach server physics");
            report.put("streamsPublicGridAndPhysics", true);
        } else {
            StreamsRuntime.angleAt(sample.x(), sample.surfaceY(), sample.z());
        }
        var vector = ActualWaterFlow.apply(new net.minecraft.world.phys.Vec3(sample.dx(), 0, sample.dz()),
                level, pos, level.getFluidState(pos));
        if (!Double.isFinite(vector.x) || !Double.isFinite(vector.z)) throw new AssertionError("Invalid corrected current");
        report.put("elevatedNativeRiver", Map.of("x", sample.x(), "z", sample.z(), "surfaceY", sample.surfaceY()));
        report.put("streamsNativeLayerAndWaterCorrection", true);
    }
}
