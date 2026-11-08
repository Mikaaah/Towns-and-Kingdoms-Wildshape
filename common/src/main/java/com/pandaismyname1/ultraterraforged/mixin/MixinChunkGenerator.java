package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;

@Mixin(ChunkGenerator.class)
class MixinChunkGenerator {
    @WrapMethod(method = "findNearestMapStructure")
    private Pair<BlockPos, Holder<Structure>> wildshape$locate(ServerLevel level, HolderSet<Structure> structures,
            BlockPos origin, int radius, boolean skipKnown, Operation<Pair<BlockPos, Holder<Structure>>> original) {
        return WorldGenFlags.approximately(() -> original.call(level, structures, origin, radius, skipKnown));
    }
}
