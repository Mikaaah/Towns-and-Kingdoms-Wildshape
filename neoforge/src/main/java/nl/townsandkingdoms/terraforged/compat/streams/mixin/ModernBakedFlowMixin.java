package nl.townsandkingdoms.terraforged.compat.streams.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.terraforged.compat.streams.ActualWaterFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = {"streamsreflowing.proprietary.SourceGivenToAuthorizedOnly_006$ProprietaryNiceJohn_001",
        "streamsreflowing.proprietary.SourceGivenToAuthorizedOnly_006$SeeAiNoticeConstant_001"}, remap = false, priority = 400)
public abstract class ModernBakedFlowMixin {
    @ModifyReturnValue(method = "getFlow", at = @At("RETURN"), remap = false)
    private Vec3 wildshape$actualFlow(Vec3 original, BlockGetter level, BlockPos pos, FluidState state) {
        return ActualWaterFlow.apply(original, level, pos, state);
    }
}
