package nl.townsandkingdoms.terraforged.compat.streams.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.ClientLevel;
import nl.townsandkingdoms.terraforged.compat.streams.ActualWaterFlow;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "dev.streamsreflowing.neoforge.client.ProprietaryNiceJohn_001", remap = false, priority = 400)
public abstract class ModernParticleMixin {
    @WrapMethod(method = "proprietary1(Lnet/minecraft/client/multiplayer/ClientLevel;IIIBDZ)V", remap = false)
    private static void wildshape$downhillParticles(ClientLevel level, int x, int y, int z, byte angle,
            double drift, boolean debris, Operation<Void> original) {
        byte corrected = ActualWaterFlow.angle(level, x, y, z, angle);
        if (corrected != -1) original.call(level, x, y, z, corrected, drift, debris);
    }
}
