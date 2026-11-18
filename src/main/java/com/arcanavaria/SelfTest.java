package com.arcanavaria;

import com.arcanavaria.lab.LabRegistry;
import com.arcanavaria.lab.RuneLockBlockEntity;
import com.arcanavaria.warden.GreatwoodWarden;
import com.arcanavaria.warden.WardenRegistry;
import com.arcanavaria.warden.WardenState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Save/load round trips of the Greatwood Warden fight and the rune lock, run once the dev server has started when the
 * JVM has {@code -Darcanavaria.selftest=true} (only the Gradle server run sets it). Logs one {@code SELFTEST PASS} or
 * {@code SELFTEST FAIL} line per check.
 */
final class SelfTest {
    private SelfTest() {
    }

    static void register() {
        if (AVRegistry.FUNDAMENTUM && Boolean.getBoolean("arcanavaria.scythetest")) {
            com.arcanavaria.scythe.ScytheSelfTest.register();
        }
        if (Boolean.getBoolean("arcanavaria.selftest")) {
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
                run(e.getServer().overworld());
                if (Boolean.getBoolean("arcanavaria.lenstest")) {
                    e.getServer().halt(false);
                }
            });
        }
    }

    private static void check(String name, boolean ok, String detail) {
        ArcanaVaria.LOG.info("SELFTEST {} {}: {}", ok ? "PASS" : "FAIL", name, detail);
    }

    private static GreatwoodWarden roundTrip(ServerLevel level, GreatwoodWarden w) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        w.saveWithoutId(out);
        CompoundTag tag = out.buildResult();
        GreatwoodWarden loaded = WardenRegistry.GREATWOOD_WARDEN.get().create(level, EntitySpawnReason.COMMAND);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        return loaded;
    }

    private static void warden(ServerLevel level) {
        GreatwoodWarden fresh = WardenRegistry.GREATWOOD_WARDEN.get().create(level, EntitySpawnReason.COMMAND);
        GreatwoodWarden freshBack = roundTrip(level, fresh);
        check("warden_roundtrip_fresh", freshBack.finale() == WardenState.Finale.PENDING && !freshBack.phaseTwo(), freshBack.snapshot());

        // Mid-RAY, phase two, finale earned but not played: the attack resumes in IDLE, everything else comes back.
        GreatwoodWarden mid = WardenRegistry.GREATWOOD_WARDEN.get().create(level, EntitySpawnReason.COMMAND);
        mid.selfTestMidFight(new BlockPos(12, 70, -5));
        GreatwoodWarden midBack = roundTrip(level, mid);
        String want = mid.snapshot().replace("state=ray", "state=idle");
        check("warden_roundtrip_queued_finale", midBack.state() == WardenState.IDLE && midBack.finale() == WardenState.Finale.QUEUED
                && want.equals(midBack.snapshot()), midBack.snapshot());

        // The finale ROOTS ending marks it DONE (one value, so queued and done can never both hold), and DONE is saved.
        midBack.selfTestFinishFinale();
        GreatwoodWarden doneBack = roundTrip(level, midBack);
        check("warden_finale_done_saved", midBack.finale() == WardenState.Finale.DONE && doneBack.finale() == WardenState.Finale.DONE,
                doneBack.snapshot());
        fresh.discard();
        freshBack.discard();
        mid.discard();
        midBack.discard();
        doneBack.discard();
    }

    private static void runeLock(ServerLevel level, String name, int progress, RuneLockBlockEntity.LockState expect) {
        BlockPos pos = new BlockPos(0, 64, 0);
        RuneLockBlockEntity lock = new RuneLockBlockEntity(pos, LabRegistry.RUNE_LOCK.get().defaultBlockState());
        lock.selfTestSet(List.of(3, 1, 4, 1, 5), progress);
        CompoundTag tag = lock.saveWithoutMetadata(level.registryAccess());
        RuneLockBlockEntity loaded = new RuneLockBlockEntity(pos, LabRegistry.RUNE_LOCK.get().defaultBlockState());
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        check(name, loaded.state() == expect && lock.snapshot().equals(loaded.snapshot()), loaded.snapshot());
    }

    private static void run(ServerLevel level) {
        try {
            warden(level);
            runeLock(level, "runelock_roundtrip_locked", 0, RuneLockBlockEntity.LockState.LOCKED);
            runeLock(level, "runelock_roundtrip_opening", 3, RuneLockBlockEntity.LockState.OPENING);
            runeLock(level, "runelock_roundtrip_open", 5, RuneLockBlockEntity.LockState.OPEN);
            if (Boolean.getBoolean("arcanavaria.lenstest")) {
                com.arcanavaria.lens.LensUndoSelfTest.run(level);
                com.arcanavaria.lens.LensGadgetSelfTest.run(level);
            }
        } catch (Exception ex) {
            ArcanaVaria.LOG.error("SELFTEST FAIL crashed", ex);
        }
    }
}
