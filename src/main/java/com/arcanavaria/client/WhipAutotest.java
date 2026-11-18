package com.arcanavaria.client;

import com.arcanavaria.AVRegistry;
import com.fundamentum.curios.Slots;
import com.fundamentum.summon.Summons;
import com.fundamentum.whip.WhipTags;
import com.fundamentum.whip.Whips;
import com.fundamentum.whip.WhipsClient;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The whip stage of ClientAutotest (only with Fundamentum): for each whip, hold it, stand a frozen zombie 3.5 blocks
 * ahead, swing (Whips.swing), check the client sees the swing, screenshot it and log the zombie's health lost.
 * Separate from ClientAutotest so that class never touches Fundamentum.
 */
final class WhipAutotest {
    static final int TICKS_PER_WHIP = 80;

    static List<DeferredItem<Item>> whips() {
        return List.of(AVRegistry.THAUMIUM_WHIP, AVRegistry.VOID_WHIP, AVRegistry.ELEMENTAL_WHIP, AVRegistry.AMBERBARK_LASH);
    }

    static void setUp(ServerPlayer p, int i) {
        MinecraftServer server = p.level().getServer();
        // the Elemental Whip swings at midnight, so its full-bright cord shows against the dark
        String time = whips().get(i) == AVRegistry.ELEMENTAL_WHIP ? "time set midnight" : "time set noon";
        for (String c : new String[] {time, "kill @e[type=minecraft:zombie]", "kill @e[type=minecraft:item]", "tp @a 0.5 -60 0.5 0 5",
            "summon minecraft:zombie 0.5 -60 4.0 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f]}"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(whips().get(i).get()));
    }

    static void face(ServerPlayer p, int yaw) {
        MinecraftServer server = p.level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 -60 0.5 " + yaw + " 5");
    }

    static void swing(ServerPlayer p, org.slf4j.Logger log) {
        log.info("AVTEST whip {} swing started={}", p.getMainHandItem().getItem(), Whips.swing(p));
    }

    static boolean swingingOnClient(Minecraft mc) {
        return WhipsClient.swinging(mc.player);
    }

    static void result(ServerPlayer p, org.slf4j.Logger log) {
        List<Zombie> z = p.level().getEntitiesOfClass(Zombie.class, p.getBoundingBox().inflate(8.0));
        log.info("AVTEST whip {} zombie hp={} effects={} fire={}", p.getMainHandItem().getItem(),
            z.isEmpty() ? "dead" : String.valueOf(z.get(0).getHealth()),
            z.isEmpty() ? "-" : z.get(0).getActiveEffectsMap().keySet().toString(), !z.isEmpty() && z.get(0).isOnFire());
    }

    // ---- the six primals: the Elemental Whip swung again and again at a rooted zombie until each primal has shown

    static final int TICKS_PER_PRIMAL = 30, MAX_PRIMAL_SWINGS = 40;
    static final String[] PRIMAL_NAMES = {"aer", "ignis", "aqua", "terra", "ordo", "perditio"};
    private static final TreeSet<String> primalsSeen = new TreeSet<>();
    private static double primalRise, primalShift;

    /** A zombie that can't walk (speed 0) but still takes knockback, with 1000 hp so it lives through every swing. */
    static void primalsSetUp(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        for (String c : new String[] {"time set midnight", "kill @e[type=minecraft:zombie]", "tp @a 0.5 -60 0.5 0 5",
            "summon minecraft:zombie 0.5 -60 4.0 {Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],Health:1000f,"
                + "attributes:[{id:\"minecraft:movement_speed\",base:0.0},{id:\"minecraft:max_health\",base:1000.0},"
                + "{id:\"minecraft:attack_damage\",base:0.0}]}"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AVRegistry.ELEMENTAL_WHIP.get()));
        primalsSeen.clear();
    }

    private static Zombie zombie(ServerPlayer p) {
        List<Zombie> z = p.level().getEntitiesOfClass(Zombie.class, p.getBoundingBox().inflate(16.0));
        return z.isEmpty() ? null : z.get(0);
    }

    /** Puts the zombie back on its spot, clean of effects and fire, and swings at it. */
    static void primalSwing(ServerPlayer p, org.slf4j.Logger log) {
        Zombie z = zombie(p);
        if (z == null) {
            log.error("AVTEST primal FAIL zombie gone");
            return;
        }
        z.removeAllEffects();
        z.clearFire();
        z.setDeltaMovement(Vec3.ZERO);
        z.teleportTo(0.5, -60.0, 4.0);
        z.setHealth(z.getMaxHealth());
        primalRise = 0.0;
        primalShift = 0.0;
        Whips.swing(p);
    }

    /** Every tick after the swing: the highest the zombie rose and the farthest it was shoved. */
    static void primalSample(ServerPlayer p) {
        Zombie z = zombie(p);
        if (z != null) {
            primalRise = Math.max(primalRise, z.getY() + 60.0);
            primalShift = Math.max(primalShift, Math.hypot(z.getX() - 0.5, z.getZ() - 4.0));
        }
    }

    /** Reads what the hit carried: fire, Slowness, Glowing, Wither, a lift or a shove. Returns true once all six were seen. */
    static boolean primalRead(ServerPlayer p, int swing, org.slf4j.Logger log) {
        Zombie z = zombie(p);
        if (z == null) {
            return false;
        }
        List<String> now = new ArrayList<>();
        if (z.isOnFire()) {
            now.add("ignis");
        }
        if (z.hasEffect(MobEffects.SLOWNESS)) {
            now.add("aqua");
        }
        if (z.hasEffect(MobEffects.GLOWING)) {
            now.add("ordo");
        }
        if (z.hasEffect(MobEffects.WITHER)) {
            now.add("perditio");
        }
        if (primalRise > 1.2) {
            now.add("aer");
        } else if (primalShift > 2.0) {
            now.add("terra");
        }
        primalsSeen.addAll(now);
        log.info("AVTEST primal swing {} hp lost={} rise={} shift={} -> {} seen {}/6 {}", swing, z.getMaxHealth() - z.getHealth(),
            String.format("%.2f", primalRise), String.format("%.2f", primalShift), now, primalsSeen.size(), primalsSeen);
        return primalsSeen.size() == 6;
    }

    static void primalsEnd(org.slf4j.Logger log) {
        List<String> missing = new ArrayList<>(List.of(PRIMAL_NAMES));
        missing.removeAll(primalsSeen);
        if (missing.isEmpty()) {
            log.info("AVTEST primals all six seen");
        } else {
            log.error("AVTEST primals FAIL never seen: {}", missing);
        }
    }

    // ---- summon marks: real summons (Ostentum's Bone Whistle in the Curios summon slot) on a target the Amberbark Lash marked

    private static float marksStartHp;

    /** Amberbark Lash in hand, Bone Whistle in the summon slot, a 1000 hp rooted zombie ahead, daylight. */
    static void marksSetUp(ServerPlayer p, org.slf4j.Logger log) {
        primalsSetUp(p);
        MinecraftServer server = p.level().getServer();
        for (String c : new String[] {"time set noon", "curios replace summon 0 @p with ostentum:bone_whistle"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AVRegistry.AMBERBARK_LASH.get()));
        log.info("AVTEST marks ostentum={} summon slot={}", ModList.get().isLoaded("ostentum"), Slots.allEquipped(p, Slots.SUMMON));
    }

    static void marksLash(ServerPlayer p, org.slf4j.Logger log) {
        Zombie z = zombie(p);
        if (z != null) {
            z.teleportTo(0.5, -60.0, 4.0);
        }
        log.info("AVTEST marks minions before lash={} lash started={}", Summons.minions(p).size(), Whips.swing(p));
    }

    /** Whether the mark took, what a summon's 4-damage bite is worth on the marked target, and the summons set on it. */
    static void marksCheck(ServerPlayer p, org.slf4j.Logger log) {
        Zombie z = zombie(p);
        if (z == null) {
            log.error("AVTEST marks FAIL no target");
            return;
        }
        List<Mob> minions = Summons.minions(p);
        marksStartHp = z.getHealth();
        log.info("AVTEST marks target marked={} minion 4-dmg bite on it={} minions={} {}", WhipTags.marked(z),
            WhipTags.minionHit(p.getUUID(), z, 4.0F, p.getRandom()), minions.size(),
            minions.stream().map(m -> m.getType().toShortString()).toList());
    }

    static void marksResult(ServerPlayer p, org.slf4j.Logger log) {
        Zombie z = zombie(p);
        if (z == null) {
            log.error("AVTEST marks FAIL no target");
            return;
        }
        float lost = marksStartHp - z.getHealth();
        boolean stillMarked = WhipTags.marked(z);
        WhipTags.clear(z);
        log.info("AVTEST marks result: summons took {} hp off the marked target in 8 s; still marked={}; same bite unmarked={}",
            lost, stillMarked, WhipTags.minionHit(p.getUUID(), z, 4.0F, p.getRandom()));
    }

    private WhipAutotest() {
    }
}
