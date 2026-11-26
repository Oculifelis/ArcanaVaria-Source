package com.arcanavaria.client;

import com.arcanavaria.AVRegistry;
import com.fundamentum.whip.Whips;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * -Pautotest=fxpreview: look previews for the Elemental Scythe and Elemental Whip effects (client/SwingFx), filmed as
 * short frame strips from behind the player. Test-only: nothing here runs in normal play. Scythe looks: 0 primal
 * crescent, 1 prism sweep, 2 crescent with an aspect ring on each hit. Whip looks (each with the cord glowing in its
 * primal): 0 vis arc, 1 seeker mote, 2 linked rings, each jumping from the first enemy to a second.
 */
final class FxPreviewAutotest {
    private static final int PER = 70, CLIPS = 6;
    /** The primal shown in each clip: ignis, aqua, ordo for the scythe; ignis, aqua, perditio for the whip. */
    private static final int[] SHOWN = {1, 2, 4, 1, 2, 5};
    private static final int[] SCYTHE_FRAMES = {2, 4, 6, 9};
    private static final int[] WHIP_FRAMES = {5, 8, 11, 15};

    private FxPreviewAutotest() {
    }

    static boolean step(Minecraft mc, int t, BiConsumer<Minecraft, Consumer<ServerPlayer>> onServer, BiConsumer<Minecraft, String> shot) {
        if (t == 20) {
            onServer.accept(mc, FxPreviewAutotest::build);
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (t < 60) {
            return false;
        }
        int clip = (t - 60) / PER, at = (t - 60) % PER;
        if (clip >= CLIPS) {
            SwingFx.cordRgb = -1;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            return true;
        }
        boolean scythe = clip < 3;
        int look = clip % 3, primal = SHOWN[clip];
        String name = (scythe ? "fxscythe" : "fxwhip") + look;
        if (at == 0) {
            SwingFx.cordRgb = scythe ? -1 : SwingFx.PRIMALS[primal];
            onServer.accept(mc, p -> setUp(p, scythe));
        } else if (at == 30) {
            if (scythe) {
                mc.player.swing(InteractionHand.MAIN_HAND);
                List<Vec3> hits = new ArrayList<>();
                for (Zombie z : zombies(mc, 3.5)) {
                    hits.add(z.position().add(0, z.getBbHeight() * 0.6, 0));
                }
                SwingFx.scythe(look, primal, mc.player.position().add(0, mc.player.getBbHeight() * 0.5, 0), mc.player.getYRot(), hits);
            } else {
                onServer.accept(mc, Whips::swing);
            }
        } else if (!scythe && at == 36) {
            List<Zombie> near = zombies(mc, 9.0);
            near.sort((a, b) -> Double.compare(a.distanceToSqr(mc.player), b.distanceToSqr(mc.player)));
            if (near.size() > 1) {
                SwingFx.chain(look, primal, chest(near.get(0)), chest(near.get(1)));
            }
        }
        int[] frames = scythe ? SCYTHE_FRAMES : WHIP_FRAMES;
        for (int i = 0; i < frames.length; i++) {
            if (at == 30 + frames[i]) {
                shot.accept(mc, name + "_f" + i);
            }
        }
        return false;
    }

    private static Vec3 chest(Zombie z) {
        return z.position().add(0, z.getBbHeight() * 0.6, 0);
    }

    private static List<Zombie> zombies(Minecraft mc, double r) {
        return new ArrayList<>(mc.level.getEntitiesOfClass(Zombie.class, new AABB(mc.player.blockPosition()).inflate(r)));
    }

    private static void build(ServerPlayer p) {
        for (String c : new String[] {"difficulty easy", "time set noon", "weather clear", "gamemode survival @a",
            "kill @e[type=!minecraft:player]", "fill -8 -60 -8 8 -50 12 minecraft:air", "fill -8 -61 -8 8 -61 12 minecraft:grass_block"}) {
            run(p, c);
        }
    }

    private static void setUp(ServerPlayer p, boolean scythe) {
        run(p, "tp @e[type=minecraft:husk] 0.5 -400 0.5");
        run(p, "kill @e[type=minecraft:item]");
        run(p, "tp @a 0.5 -60 0.5 0 15");
        String z = " {NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Rotation:[180f,0f]}";
        if (scythe) {
            run(p, "summon minecraft:husk -0.9 -60 2.2" + z);
            run(p, "summon minecraft:husk 0.5 -60 2.6" + z);
            run(p, "summon minecraft:husk 1.9 -60 2.2" + z);
        } else {
            run(p, "summon minecraft:husk 0.5 -60 4.0" + z);
            run(p, "summon minecraft:husk 3.0 -60 6.5" + z);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(scythe ? AVRegistry.ELEMENTAL_SCYTHE.get() : AVRegistry.ELEMENTAL_WHIP.get()));
    }

    private static void run(ServerPlayer p, String c) {
        MinecraftServer server = p.level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
    }
}
