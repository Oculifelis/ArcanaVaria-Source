package com.arcanavaria.client;

import com.arcanavaria.lab.LabRegistry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * -Pautotest=sunbeam: a dark deepslate room with a Sun Lens, a mirror and a receptor. The beam is shot from a wide
 * angle (three frames a few ticks apart, to show the motion) and from close beside the mirror.
 */
final class SunbeamAutotest {
    private static final int Y = -58, START = 120, PER_LOOK = 130, LOOKS = 1;
    private static final BlockPos EMITTER = new BlockPos(0, Y, 0), MIRROR = new BlockPos(9, Y, 0), RECEPTOR = new BlockPos(9, Y, -6);

    private SunbeamAutotest() {
    }

    /** One client tick; true once every shot is taken. */
    static boolean step(Minecraft mc, int t, BiConsumer<Minecraft, Consumer<ServerPlayer>> onServer, BiConsumer<Minecraft, String> shot) {
        if (t == 60) {
            onServer.accept(mc, SunbeamAutotest::build);
            mc.options.hideGui = true;
        }
        if (t < START) {
            return false;
        }
        int look = (t - START) / PER_LOOK, at = (t - START) % PER_LOOK;
        if (look >= LOOKS) {
            return true;
        }
        if (at == 0) {
            onServer.accept(mc, p -> run(p, "tp @a -3.5 -57.4 5.5 facing 7 -57.8 -2"));
        } else if (at == 40 || at == 46 || at == 52) {
            shot.accept(mc, "sun" + look + "_wide_" + (at - 40) / 6);
        } else if (at == 60) {
            onServer.accept(mc, p -> run(p, "tp @a 5.5 -57.2 2.8 facing 9.5 -57.6 -1.5"));
        } else if (at == 100) {
            shot.accept(mc, "sun" + look + "_close");
        } else if (at == 106) {
            shot.accept(mc, "sun" + look + "_close_1");
        }
        return false;
    }

    private static void build(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        for (String c : new String[] {"difficulty peaceful", "time set midnight", "weather clear", "gamemode spectator @a",
            "kill @e[type=!minecraft:player]", "fill -6 -61 -10 14 -52 9 minecraft:polished_deepslate hollow",
            "fill -5 -60 -9 13 -60 8 minecraft:deepslate_tiles", "setblock -4 -56 7 minecraft:light[level=5]",
            "setblock 12 -56 -8 minecraft:light[level=5]"}) {
            run(p, c);
        }
        level.setBlock(EMITTER, LabRegistry.BEAM_EMITTER.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
        level.setBlock(MIRROR, LabRegistry.BEAM_MIRROR.get().defaultBlockState(), 3);
        level.setBlock(RECEPTOR, LabRegistry.BEAM_RECEPTOR.get().defaultBlockState(), 3);
    }

    private static void run(ServerPlayer p, String c) {
        MinecraftServer server = p.level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
    }
}
