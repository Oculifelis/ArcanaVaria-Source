package com.arcanavaria.client;

import com.arcanavaria.exanimator.Exanimator;
import com.arcanavaria.exanimator.ExanimatorRegistry;
import com.arcanavaria.exanimator.ExanimatorState;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;

/**
 * -Pautotest=exanimator: the Exanimator on a superflat floor at night, fighting an unkillable survival player. Logs the
 * moves it uses, forces phase two (Eclipse) at half health, kills it and logs the drop. The rite itself needs a wizard
 * tower and two thurible clouds, so this calls the boss straight in the way the rite does (create, snapTo, awaken).
 */
final class ExanimatorAutotest {
    private static final Set<ExanimatorState> SEEN = EnumSet.noneOf(ExanimatorState.class);
    private static boolean sawPhaseTwo;

    static void setUp(ServerPlayer p, Logger log) {
        MinecraftServer server = p.level().getServer();
        for (String c : new String[] {"difficulty normal", "time set midnight", "gamemode survival @a",
            "effect give @a resistance infinite 4 true", "effect give @a regeneration infinite 2 true",
            "effect give @a saturation infinite 0 true", "effect give @a night_vision infinite 0 true", "clear @a",
            "item replace entity @a weapon.mainhand with arcanavaria:thaumium_scythe", "kill @e[type=minecraft:item]",
            "tp @a 0.5 -60 0.5 0 10"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        ServerLevel level = (ServerLevel) p.level();
        Exanimator boss = ExanimatorRegistry.EXANIMATOR.get().create(level, EntitySpawnReason.EVENT);
        if (boss == null) {
            log.error("AVTEST exanimator create returned null");
            return;
        }
        boss.snapTo(0.5, -60.0, 9.5, 180.0F, 0.0F);
        boss.awaken(BlockPos.containing(0.5, -60.0, 9.5));
        boolean added = level.addFreshEntity(boss);
        log.info("AVTEST exanimator summoned added={} hp={}/{} state={}", added, boss.getHealth(), boss.getMaxHealth(),
            boss.state().serialName());
    }

    static Exanimator find(ServerPlayer p) {
        List<Exanimator> list = p.level().getEntitiesOfClass(Exanimator.class, p.getBoundingBox().inflate(64.0));
        return list.isEmpty() ? null : list.get(0);
    }

    static void report(ServerPlayer p, Logger log, String label) {
        Exanimator b = find(p);
        if (b == null) {
            log.info("AVTEST exanimator {} boss=none player hp={}", label, p.getHealth());
            return;
        }
        SEEN.add(b.state());
        sawPhaseTwo |= b.phaseTwo();
        log.info("AVTEST exanimator {} state={} t={} hp={} phase2={} dist={} player hp={} target={}", label,
            b.state().serialName(), (int) b.stateTime(0), Math.round(b.getHealth()), b.phaseTwo(),
            String.format("%.1f", b.distanceTo(p)), Math.round(p.getHealth()), b.getTarget() == p);
    }

    /** Turns the player to face the boss so the next shot has it in frame (third person, behind the player). */
    static void face(ServerPlayer p) {
        Exanimator b = find(p);
        if (b == null) {
            return;
        }
        double dx = b.getX() - p.getX(), dz = b.getZ() - p.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        double back = 6.0;
        double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
        double x = p.getX(), z = p.getZ();
        if (len < back) {
            x = b.getX() - dx / len * back;
            z = b.getZ() - dz / len * back;
        }
        p.connection.teleport(x, p.getY(), z, yaw, 5.0F);
    }

    /** Brings it to just under half health with a kill-type hit (the only kind its invulnerable moves let through). */
    static void halve(ServerPlayer p, Logger log) {
        Exanimator b = find(p);
        if (b == null) {
            log.error("AVTEST exanimator halve: no boss");
            return;
        }
        ServerLevel level = (ServerLevel) p.level();
        float before = b.getHealth();
        float dmg = before - b.getMaxHealth() * 0.45F;
        boolean hit = b.hurtServer(level, level.damageSources().genericKill(), dmg);
        log.info("AVTEST exanimator halve hit={} hp {} -> {}", hit, Math.round(before), Math.round(b.getHealth()));
    }

    static void kill(ServerPlayer p, Logger log) {
        Exanimator b = find(p);
        if (b == null) {
            log.error("AVTEST exanimator kill: no boss");
            return;
        }
        ServerLevel level = (ServerLevel) p.level();
        boolean hit = b.hurtServer(level, level.damageSources().playerAttack(p), 10000.0F);
        log.info("AVTEST exanimator kill by player hit={} dead={} state={}", hit, b.isDeadOrDying(), b.state().serialName());
    }

    static void loot(ServerPlayer p, Logger log) {
        Exanimator b = find(p);
        if (b != null && !b.isDeadOrDying()) {
            ServerLevel level = (ServerLevel) p.level();
            b.hurtServer(level, level.damageSources().playerAttack(p), 10000.0F);
            log.info("AVTEST exanimator second kill try dead={} state={}", b.isDeadOrDying(), b.state().serialName());
        }
        List<ItemEntity> items = p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(48.0));
        StringBuilder sb = new StringBuilder();
        for (ItemEntity e : items) {
            sb.append(e.getItem().getCount()).append('x').append(BuiltInRegistries.ITEM.getKey(e.getItem().getItem())).append(' ');
        }
        log.info("AVTEST exanimator loot [{}]", sb.toString().trim());
    }

    static void summary(ServerPlayer p, Logger log) {
        Exanimator b = find(p);
        StringBuilder sb = new StringBuilder();
        for (ExanimatorState s : SEEN) {
            sb.append(s.serialName()).append(' ');
        }
        log.info("AVTEST exanimator summary moves seen [{}] phase2={} boss left={} player alive={} hp={}", sb.toString().trim(),
            sawPhaseTwo, b != null && b.isAlive(), p.isAlive(), Math.round(p.getHealth()));
    }

    private ExanimatorAutotest() {
    }
}
