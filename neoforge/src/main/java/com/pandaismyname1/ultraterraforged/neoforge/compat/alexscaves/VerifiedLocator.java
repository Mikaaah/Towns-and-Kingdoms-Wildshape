package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

import java.util.Locale;
import java.util.Map;

public final class VerifiedLocator {
    public static final Map<String, ResourceKey<Biome>> NAMES = Map.of(
            "magnetic_caves", ACBiomeRegistry.MAGNETIC_CAVES,
            "primordial_caves", ACBiomeRegistry.PRIMORDIAL_CAVES,
            "toxic_caves", ACBiomeRegistry.TOXIC_CAVES,
            "abyssal_chasm", ACBiomeRegistry.ABYSSAL_CHASM,
            "forlorn_hollows", ACBiomeRegistry.FORLORN_HOLLOWS,
            "candy_cavity", ACBiomeRegistry.CANDY_CAVITY);
    private static final int[] HEIGHTS = {-16, 0, -32, 16, 32, -48, 48, 64, -64, 80, 96};
    private VerifiedLocator() {}

    public static BlockPos find(ServerLevel level, BlockPos origin, ResourceKey<Biome> key) {
        int originX = origin.getX() >> 4;
        int originZ = origin.getZ() >> 4;
        for (int radius = 0; radius < 500; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int x = (originX + dx) * 16 + 8;
                    int z = (originZ + dz) * 16 + 8;
                    if (!key.equals(ACBiomeRarity.getACBiomeForPosition(level.getSeed(), x, z))) continue;
                    var center = ACBiomeRarity.getACBiomeCenterForPosition(level.getSeed(), x, z);
                    if (center != null) {
                        BlockPos result = verify(level, (int) Math.floor(center.x * 4), (int) Math.floor(center.z * 4), key);
                        if (result != null) return result;
                    }
                    BlockPos result = verify(level, x, z, key);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    private static BlockPos verify(ServerLevel level, int x, int z, ResourceKey<Biome> key) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        for (int y : HEIGHTS) {
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;
            if (source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler).is(key)) {
                return new BlockPos(x, y, z);
            }
        }
        return null;
    }

    public static int run(CommandSourceStack source, String name) {
        var key = NAMES.get(name.toLowerCase(Locale.ROOT));
        if (key == null) {
            source.sendFailure(Component.literal("Unknown Alex's Caves biome: " + name));
            return 0;
        }
        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos found = find(source.getLevel(), origin, key);
        if (found == null) {
            source.sendFailure(Component.literal("No verified " + name + " biome found within approximately 8,000 blocks."));
            return 0;
        }
        String teleport = "/tp @s " + found.getX() + " " + found.getY() + " " + found.getZ();
        var coordinates = Component.literal("[" + found.getX() + ", " + found.getY() + ", " + found.getZ() + "]")
                .setStyle(Style.EMPTY.withColor(0x55ff55)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, teleport))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Verified biome-source location. Click to fill in teleport command."))));
        source.sendSuccess(() -> Component.literal("Verified " + name + " at ").append(coordinates), false);
        return 1;
    }
}
