package nl.townsandkingdoms.terraforged.compat.streams.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.world.level.BlockGetter;
import nl.townsandkingdoms.terraforged.compat.streams.ActualWaterFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Streams 2.16.2 targets also used by Flow Control 0.2.2; manual overrides retain priority. */
@Mixin(value = StreamFlowGrid.class, remap = false, priority = 400)
public abstract class ModernGridMixin {
    @ModifyReturnValue(method = {"niceJohn1(Lnet/minecraft/world/level/BlockGetter;III)B",
            "noDecompilePermission1(Lnet/minecraft/world/level/BlockGetter;III)B"}, at = @At("RETURN"), remap = false)
    private static byte wildshape$actualAngle(byte original, BlockGetter level, int x, int y, int z) {
        return ActualWaterFlow.angle(level, x, y, z, original);
    }
}
