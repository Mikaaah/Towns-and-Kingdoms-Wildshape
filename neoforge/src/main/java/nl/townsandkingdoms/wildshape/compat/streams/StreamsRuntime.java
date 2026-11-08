package nl.townsandkingdoms.wildshape.compat.streams;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.platform.services.IPlatformHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLLoader;

/** Explicit bindings for the two verified Streams ABIs. No third-party classes are bundled. */
public final class StreamsRuntime {
    private StreamsRuntime() {}

    private static final boolean MODERN = detectModern();

    public static boolean modern() { return MODERN; }

    private static boolean detectModern() {
        var file = FMLLoader.getLoadingModList().getModFileById("streamsreflowing");
        return file != null && file.getMods().getFirst().getVersion().toString().startsWith("2.16.2");
    }

    public static boolean waterFlowEnabled() {
        return modern() ? Modern.flag(Modern.WATER) : Services.PLATFORM.vanillaWaterFlow();
    }

    public static boolean riverFlowEnabled() {
        return modern() ? Modern.flag(Modern.RIVERS) : Services.PLATFORM.flowVanillaRivers();
    }

    public static double vx(byte angle) {
        return modern() ? Modern.vector(Modern.X, angle) : StreamFlowGrid.vx(angle);
    }

    public static double vz(byte angle) {
        return modern() ? Modern.vector(Modern.Z, angle) : StreamFlowGrid.vz(angle);
    }

    public static byte angleAt(int x, int y, int z) {
        if (!modern()) return StreamFlowGrid.angleOf(StreamFlowGrid.peek(x >> 4, z >> 4), x, z, y);
        try {
            byte[] grid = (byte[]) Modern.PEEK.invokeExact(x >> 4, z >> 4);
            return (byte) Modern.ANGLE.invokeExact(grid, x, z, y);
        } catch (Throwable failure) { throw new IllegalStateException("Streams grid API changed", failure); }
    }

    public static byte bakedAngle(BlockState state) {
        return modern() ? (byte) -1 : LegacyBaked.angle(state);
    }

    private static final class LegacyBaked {
        static byte angle(BlockState state) {
            if (state.getBlock() instanceof dev.streamsreflowing.neoforge.fluid.StreamLiquidBlock) {
                int direction = state.getValue(dev.streamsreflowing.neoforge.fluid.StreamLiquidBlock.FLOW);
                if (direction < 8) {
                    var vector = dev.streamsreflowing.neoforge.fluid.StreamLiquidBlock.DIRS[direction];
                    return StreamFlowGrid.encode(vector.x, vector.z);
                }
            }
            return -1;
        }
    }

    private static final class Modern {
        // Names checked against the exact 2.16.2 NeoForge JAR. Do not widen the supported range blindly.
        static final MethodHandle PEEK = bind("niceJohn1", MethodType.methodType(byte[].class, int.class, int.class));
        static final MethodHandle ANGLE = bind("proprietary1", MethodType.methodType(byte.class, byte[].class, int.class, int.class, int.class));
        static final MethodHandle X = bindStatic("proprietary1");
        static final MethodHandle Z = bindStatic("seeAiNotice1");
        static final MethodHandle WATER = bindFlag("q");
        static final MethodHandle RIVERS = bindFlag("o");

        static MethodHandle bindStatic(String name) {
            return bind(name, MethodType.methodType(double.class, byte.class));
        }

        static MethodHandle bind(String name, MethodType type) {
            try { return MethodHandles.publicLookup().findStatic(StreamFlowGrid.class, name, type); }
            catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }

        static MethodHandle bindFlag(String name) {
            try {
                return MethodHandles.publicLookup().findVirtual(IPlatformHelper.class, name,
                        MethodType.methodType(boolean.class));
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }

        static double vector(MethodHandle method, byte angle) {
            try { return (double) method.invokeExact(angle); }
            catch (Throwable failure) { throw new IllegalStateException("Streams vector API changed", failure); }
        }

        static boolean flag(MethodHandle method) {
            try { return (boolean) method.invokeExact(Services.PLATFORM); }
            catch (Throwable failure) { throw new IllegalStateException("Streams settings API changed", failure); }
        }
    }
}
