package com.arcanavaria.client;

import com.arcanavaria.warden.GreatwoodWarden;
import com.mojang.logging.LogUtils;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

/**
 * -Pautotest=wardenview: a still Greatwood Warden at noon, shot close from the front, three-quarter, side, back,
 * head and a low angle, and its hitbox logged (it comes from the boss_moves data map).
 */
final class WardenViewAutotest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String AT = "70.5 -60 -14.5";

    private WardenViewAutotest() {
    }

    /** One client tick; true once every shot is taken. */
    static boolean step(Minecraft mc, int t, BiConsumer<Minecraft, Consumer<ServerPlayer>> onServer, BiConsumer<Minecraft, String> shot) {
        switch (t) {
            case 1 -> {
                mc.options.hideGui = true;
                onServer.accept(mc, p -> {
                    for (String c : new String[] {"difficulty easy", "time set noon", "weather clear", "gamemode spectator @a",
                        "kill @e[type=!minecraft:player]", "fill 50 -60 -40 92 -40 12 minecraft:air",
                        "summon arcanavaria:greatwood_warden " + AT + " {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}",
                        "tp @a 70.5 -56.4 -4.5 facing 70.5 -56.6 -14.5"}) {
                        run(p, c);
                    }
                });
            }
            case 100 -> shot.accept(mc, "warden_front");
            case 104 -> onServer.accept(mc, p -> run(p, "tp @a 77.8 -55.6 -7.6 facing 70.5 -56.6 -14.5"));
            case 140 -> shot.accept(mc, "warden_three");
            case 144 -> onServer.accept(mc, p -> run(p, "tp @a 80.5 -56.4 -14.5 facing 70.5 -56.6 -14.5"));
            case 180 -> shot.accept(mc, "warden_side");
            case 184 -> onServer.accept(mc, p -> run(p, "tp @a 64.5 -55.6 -23.5 facing 70.5 -56.6 -14.5"));
            case 220 -> shot.accept(mc, "warden_back");
            case 224 -> onServer.accept(mc, p -> run(p, "tp @a 72.2 -53.6 -10.2 facing 70.5 -54.4 -14.5"));
            case 260 -> shot.accept(mc, "warden_head");
            case 264 -> onServer.accept(mc, p -> run(p, "tp @a 73.5 -59.2 -9 facing 70.5 -55.5 -14.5"));
            case 300 -> shot.accept(mc, "warden_low");
            case 304 -> onServer.accept(mc, p -> {
                for (GreatwoodWarden w : p.level().getEntitiesOfClass(GreatwoodWarden.class, new AABB(p.blockPosition()).inflate(64.0))) {
                    AABB b = w.getBoundingBox();
                    LOG.info("AVTEST wardenview hitbox w={} h={} eye={} heart_y={}", b.getXsize(), b.getYsize(),
                        w.getEyeHeight(), w.heart().y - w.getY());
                }
            });
            default -> {
            }
        }
        return t >= 310;
    }

    private static void run(ServerPlayer p, String c) {
        MinecraftServer server = p.level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
    }
}
