package com.arcanavaria.client;

import static com.arcanavaria.ArcanaVaria.MODID;

import com.arcanavaria.lab.AspectLockBlockEntity;
import com.arcanavaria.lab.BeamBlocks;
import com.arcanavaria.lab.FloorGateBlock;
import com.arcanavaria.lab.LabRegistry;
import com.arcanavaria.lab.LabScan;
import com.arcanavaria.lab.RuneLockBlockEntity;
import com.arcanavaria.lab.WardCoreBlockEntity;
import com.arcanavaria.lab.WardState;
import com.arcanavaria.lab.boss.GuardianAltarBlockEntity;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The structures stage of the client test (-Pautotest=structures, a normal world): finds the nearest Wizard Tower and
 * Forsaken Laboratory the world created, as a player would with /locate, travels to each, and walks in. Checks that
 * the marker processor turned Worldforge's markers into lab blocks (ward core, altar, locks, hatches...), that walking
 * in seals the ward, and takes shots outside and inside.
 */
final class StructureAutotest {
    private static @Nullable BlockPos towerAt, labAt;
    private static @Nullable String labName;
    private static @Nullable BoundingBox towerBox, labBox;
    private static final List<BlockPos> labsMore = new ArrayList<>();

    private StructureAutotest() {
    }

    static void locate(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        run(level, "gamemode survival @a");
        for (String e : new String[] {"resistance infinite 4", "regeneration infinite 2", "saturation infinite 0", "night_vision infinite 0",
            "water_breathing infinite 0", "slow_falling infinite 0", "fire_resistance infinite 0"}) {
            run(level, "effect give @a " + e + " true");
        }
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> towers = new ArrayList<>(), labs = new ArrayList<>();
        reg.listElements().forEach(h -> {
            if (!h.key().identifier().getNamespace().equals(MODID)) {
                return;
            }
            String path = h.key().identifier().getPath();
            (path.startsWith("wizard_tower") ? towers : path.startsWith("forsaken_lab") ? labs : new ArrayList<Holder<Structure>>()).add(h);
        });
        log.info("AVTEST structures registered: towers={} labs={}", towers.size(), labs.size());
        BlockPos from = p.blockPosition();
        Pair<BlockPos, Holder<Structure>> t = towers.isEmpty() ? null
                : level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(towers), from, 100, false);
        Pair<BlockPos, Holder<Structure>> l = labs.isEmpty() ? null
                : level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(labs), from, 100, false);
        // more labs, in case the nearest one rolled no boss chamber (its ward lives there)
        labsMore.clear();
        if (!labs.isEmpty()) {
            for (int[] d : new int[][] {{1500, 0}, {-1500, 0}, {0, 1500}, {0, -1500}}) {
                var more = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(labs), from.offset(d[0], 0, d[1]), 60, false);
                if (more != null) {
                    labsMore.add(more.getFirst());
                }
            }
        }
        towerAt = t == null ? null : t.getFirst();
        labAt = l == null ? null : l.getFirst();
        labName = l == null ? null : l.getSecond().unwrapKey().map(k -> k.identifier().toString()).orElse("?");
        log.info("AVTEST locate tower={} dist={} lab={} {} dist={}", towerAt == null ? "none" : towerAt.toShortString(),
                towerAt == null ? -1 : (int) Math.sqrt(towerAt.distSqr(from)), labName, labAt == null ? "none" : labAt.toShortString(),
                labAt == null ? -1 : (int) Math.sqrt(labAt.distSqr(from)));
    }

    /** Generates the structure's chunks, stands the player outside it facing in, and returns its box. */
    static void approach(ServerPlayer p, Logger log, boolean tower) {
        ServerLevel level = (ServerLevel) p.level();
        if (!tower) {
            // the nearest lab first, then the others, until one has a boss chamber (a ward core)
            List<BlockPos> tries = new ArrayList<>();
            if (labAt != null) {
                tries.add(labAt);
            }
            tries.addAll(labsMore);
            for (BlockPos q : tries) {
                labAt = q;
                if (visit(p, log, false)) {
                    return;
                }
            }
            return;
        }
        visit(p, log, true);
    }

    private static boolean visit(ServerPlayer p, Logger log, boolean tower) {
        ServerLevel level = (ServerLevel) p.level();
        BlockPos at = tower ? towerAt : labAt;
        if (at == null) {
            log.error("AVTEST structures FAIL nothing located ({})", tower ? "tower" : "lab");
            return false;
        }
        StructureStart start = startNear(level, at, tower);
        if (start == null) {
            log.error("AVTEST structures FAIL no start at {} ({})", at.toShortString(), tower ? "tower" : "lab");
            return false;
        }
        BoundingBox box = start.getBoundingBox();
        for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++) {
            for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) {
                level.getChunk(cx, cz);
            }
        }
        if (tower) {
            towerBox = box;
        } else {
            labBox = box;
        }
        WardCoreBlockEntity core = core(level, box);
        log.info("AVTEST {} at {} pieces={} box={} core={}", tower ? "tower" : "lab", at.toShortString(),
                start.getPieces().size(), box, core == null ? "none" : core.getBlockPos().toShortString());
        if (tower && core != null) {
            // out past the porch, looking back at the door
            Vec3 exit = core.exit(), c = Vec3.atCenterOf(core.getBlockPos());
            Vec3 away = exit.subtract(c).multiply(1, 0, 1).normalize();
            Vec3 stand = exit.add(away.scale(14));
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(stand.x), (int) Math.floor(stand.z));
            float yaw = (float) (Math.toDegrees(Math.atan2(-(c.x - stand.x), c.z - stand.z)));
            p.teleportTo(level, stand.x, y + 1, stand.z, java.util.Set.of(), yaw, -28.0F, true);
        } else {
            Vec3 c = box.getCenter().getCenter();
            p.teleportTo(level, c.x, box.maxY() + 4, c.z, java.util.Set.of(), 0.0F, 60.0F, true);
        }
        return tower || core != null;
    }

    /** Walks the player in (to the ward's entry, or the lab's middle) and reports what the markers became. */
    static void enter(ServerPlayer p, Logger log, boolean tower) {
        ServerLevel level = (ServerLevel) p.level();
        BoundingBox box = tower ? towerBox : labBox;
        if (box == null) {
            return;
        }
        WardCoreBlockEntity core = core(level, box);
        if (core != null) {
            Vec3 e = core.entry();
            Vec3 c = Vec3.atCenterOf(core.getBlockPos());
            float yaw = (float) (Math.toDegrees(Math.atan2(-(c.x - e.x), c.z - e.z)));
            p.teleportTo(level, e.x, e.y, e.z, java.util.Set.of(), yaw, 5.0F, true);
        }
        Map<String, Integer> found = new TreeMap<>();
        AABB all = AABB.of(box).inflate(2);
        for (BlockEntity be : LabScan.in(level, all, BlockEntity.class)) {
            String id = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString();
            if (id.startsWith(MODID + ":") || id.contains("spawner") || id.contains("jar") || id.contains("lectern")) {
                found.merge(id.replace(MODID + ":", ""), 1, Integer::sum);
            }
        }
        int stones = 0;
        for (BlockPos q : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            stones += level.getBlockState(q).is(LabRegistry.RUNE_STONE.get()) ? 1 : 0;
        }
        log.info("AVTEST {} entered at {} blocks {} rune_stones={}", tower ? "tower" : "lab " + labName, p.blockPosition().toShortString(), found, stones);
    }

    static void report(ServerPlayer p, Logger log, boolean tower) {
        ServerLevel level = (ServerLevel) p.level();
        BoundingBox box = tower ? towerBox : labBox;
        if (box == null) {
            return;
        }
        WardCoreBlockEntity core = core(level, box);
        if (core == null) {
            log.info("AVTEST {} ward none (want {})", tower ? "tower" : "lab", tower ? "sealed" : "sealed if it has a boss chamber");
            return;
        }
        log.info("AVTEST {} ward={} sealed={} box={} (want sealed, true)", tower ? "tower" : "lab", core.state().getSerializedName(),
                WardCoreBlockEntity.isSealed(p), core.box());
        if (tower) {
            StringBuilder gates = new StringBuilder();
            for (BlockPos q : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                var s = level.getBlockState(q);
                if (s.is(LabRegistry.FLOOR_GATE.get())) {
                    gates.append(q.getY() - core.getBlockPos().getY()).append(s.getValue(FloorGateBlock.OPEN) ? "o " : "s ");
                }
            }
            log.info("AVTEST tower hatches by height [{}] (o open, s shut: shut over uncleared floors)", gates.toString().trim());
        }
        if (tower) {
            // the climb frees the player the real way: every floor cleared, the guardian killed
            return;
        }
        // free the player for the next structure: as if the guardian fell
        core.clear(level);
        log.info("AVTEST {} ward after clear={} sealed={}", tower ? "tower" : "lab", core.state().getSerializedName(), WardCoreBlockEntity.isSealed(p));
    }

    // ---- the climb: every floor of the created tower cleared as a player would, each hatch checked, the guardian killed

    private static final List<BlockPos> hatches = new ArrayList<>();
    private static int climbFloor, climbWait, climbOpened;
    private static boolean climbStarted;
    private static @Nullable BlockPos altarAt;
    /** Set once the climb has ended (pass or fail); the client stage then moves on. */
    static volatile boolean climbDone;

    /** One step of the climb, every 20 ticks from the client stage. */
    static void climb(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        if (towerBox == null || climbDone) {
            climbDone = true;
            return;
        }
        WardCoreBlockEntity core = core(level, towerBox);
        if (!climbStarted) {
            climbStarted = true;
            for (BlockPos q : BlockPos.betweenClosed(towerBox.minX(), towerBox.minY(), towerBox.minZ(), towerBox.maxX(), towerBox.maxY(), towerBox.maxZ())) {
                if (level.getBlockState(q).is(LabRegistry.FLOOR_GATE.get())) {
                    hatches.add(q.immutable());
                }
            }
            hatches.sort(java.util.Comparator.comparingInt(BlockPos::getY));
            List<GuardianAltarBlockEntity> altars = LabScan.in(level, AABB.of(towerBox).inflate(2), GuardianAltarBlockEntity.class);
            altarAt = altars.isEmpty() ? null : altars.get(0).getBlockPos();
            log.info("AVTEST climb hatches={} altar={} ward={}", hatches.size(), altarAt == null ? "none" : altarAt.toShortString(),
                    core == null ? "none" : core.state().getSerializedName());
        }
        if (climbFloor < hatches.size()) {
            BlockPos gate = hatches.get(climbFloor);
            if (climbWait == 0) {
                boolean openBefore = level.getBlockState(gate).getValue(FloorGateBlock.OPEN);
                String solved = clearBelow(level, gate, towerBox);
                standAt(p, level, gate.below(2), gate);
                log.info("AVTEST climb floor {} hatch {} was {} cleared [{}]", climbFloor + 1, gate.toShortString(), openBefore ? "open" : "shut", solved);
            }
            climbWait += 20;
            if (level.getBlockState(gate).getValue(FloorGateBlock.OPEN)) {
                climbOpened++;
                standAt(p, level, gate.above(), gate);
                log.info("AVTEST climb floor {} hatch OPEN after {} ticks; through it, now at y={} sealed={}", climbFloor + 1, climbWait,
                        p.blockPosition().getY(), WardCoreBlockEntity.isSealed(p));
                climbFloor++;
                climbWait = 0;
            } else if (climbWait >= 120) {
                log.error("AVTEST climb FAIL floor {} hatch {} still shut after clearing; still blocking: {}", climbFloor + 1,
                        gate.toShortString(), blocking(level, gate));
                climbFloor++;
                climbWait = 0;
            }
            return;
        }
        // the top: the guardian
        if (altarAt == null || !(level.getBlockEntity(altarAt) instanceof GuardianAltarBlockEntity altar)) {
            log.error("AVTEST climb FAIL no guardian altar in the tower");
            climbDone = true;
            return;
        }
        climbWait += 20;
        if (climbWait == 20) {
            standAt(p, level, altarAt.above(), altarAt);
            killMobs(level, towerBox);
            log.info("AVTEST climb top reached y={} altar phase={}", p.blockPosition().getY(), altar.phase().getSerializedName());
            return;
        }
        if (climbWait < 400 && altar.phase() == GuardianAltarBlockEntity.Phase.FIGHTING) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, AABB.of(towerBox).inflate(4))) {
                var data = m.getExistingDataOrNull(LabRegistry.BOSS.get());
                if (data != null) {
                    log.info("AVTEST climb guardian {} archetype={} traits={} hp={}", m.getName().getString(), data.archetype().identifier(),
                            data.traits().stream().map(k -> k.identifier().getPath()).toList(), m.getHealth());
                    m.kill(level);
                    climbWait = 400;
                    return;
                }
            }
        }
        if (climbWait >= 380 && climbWait < 400) {
            log.error("AVTEST climb FAIL guardian never rose (altar {})", altar.phase().getSerializedName());
            climbWait = 440;
        } else if (climbWait >= 460) {
            // the reward drops on the altar, where the player stands, so it may already be in the inventory
            List<String> got = new ArrayList<>();
            level.getEntitiesOfClass(ItemEntity.class, new AABB(altarAt).inflate(6)).forEach(e -> got.add(e.getItem().toString()));
            p.getInventory().forEach(st -> { if (!st.isEmpty()) { got.add(st.toString()); } });
            int loot = got.size();
            log.info("AVTEST climb loot {}", got);
            log.info("AVTEST climb end: hatches opened {}/{} altar={} ward={} sealed={} loot stacks={} (want all, defeated, cleared, false, >0)",
                    climbOpened, hatches.size(), altar.phase().getSerializedName(), core == null ? "none" : core.state().getSerializedName(),
                    WardCoreBlockEntity.isSealed(p), loot);
            if (core != null && core.state() != WardState.CLEARED) {
                core.clear(level);
            }
            climbDone = true;
        }
    }

    /** Clears the floor under a hatch as a player would: spawners broken, aspect locks filled, rune codes found, mirrors set. */
    private static String clearBelow(ServerLevel level, BlockPos gate, BoundingBox box) {
        AABB below = below(gate);
        int spawners = 0, aspects = 0, runes = 0;
        for (BlockEntity be : LabScan.in(level, below, BlockEntity.class)) {
            if (be instanceof SpawnerBlockEntity) {
                level.destroyBlock(be.getBlockPos(), false);
                spawners++;
            } else if (be instanceof AspectLockBlockEntity lock && !lock.open()) {
                lock.pour(lock.aspect(), lock.need() - lock.have());
                aspects++;
            } else if (be instanceof RuneLockBlockEntity lock && lock.state() != RuneLockBlockEntity.LockState.OPEN) {
                runes += solveRunes(level, lock) ? 1 : 0;
            }
        }
        String beams = solveBeams(level, below);
        killMobs(level, box);
        return "spawners=" + spawners + " aspect=" + aspects + " rune=" + runes + " beam=" + beams;
    }

    private static AABB below(BlockPos gate) {
        return new AABB(gate.getX() - 13, gate.getY() - 13, gate.getZ() - 13, gate.getX() + 14, gate.getY(), gate.getZ() + 14);
    }

    /** Finds the code as a player would, by trying each rune at each step (a wrong one starts it over). */
    private static boolean solveRunes(ServerLevel level, RuneLockBlockEntity lock) {
        List<Integer> known = new ArrayList<>();
        while (lock.state() != RuneLockBlockEntity.LockState.OPEN && known.size() < 16) {
            boolean found = false;
            for (int r = 0; r < 8 && !found; r++) {
                lock.labReset();
                for (int k : known) {
                    lock.press(level, k);
                }
                lock.press(level, r);
                if (lock.state() == RuneLockBlockEntity.LockState.OPEN || progress(lock) == known.size() + 1) {
                    known.add(r);
                    found = true;
                }
            }
            if (!found) {
                return false;
            }
        }
        return lock.state() == RuneLockBlockEntity.LockState.OPEN;
    }

    private static int progress(RuneLockBlockEntity lock) {
        String s = lock.snapshot();
        int i = s.indexOf("progress=") + 9;
        return Integer.parseInt(s.substring(i, s.indexOf(' ', i)));
    }

    /** Tries mirror settings until every emitter's beam ends on a receptor; returns receptors reached / emitters. */
    private static String solveBeams(ServerLevel level, AABB area) {
        List<BlockPos> mirrors = new ArrayList<>(), emitters = new ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed((int) area.minX, (int) area.minY, (int) area.minZ, (int) area.maxX - 1, (int) area.maxY - 1, (int) area.maxZ - 1)) {
            Block b = level.getBlockState(q).getBlock();
            if (b instanceof BeamBlocks.Mirror) {
                mirrors.add(q.immutable());
            } else if (b instanceof BeamBlocks.Emitter) {
                emitters.add(q.immutable());
            }
        }
        if (emitters.isEmpty()) {
            return "0/0";
        }
        int n = Math.min(mirrors.size(), 14), best = 0, bestHits = -1;
        for (int mask = 0; mask < (1 << n); mask++) {
            setMirrors(level, mirrors, mask, n);
            int hits = 0;
            for (BlockPos e : emitters) {
                BlockPos[] hit = new BlockPos[1];
                BeamBlocks.trace(level, e, level.getBlockState(e).getValue(BlockStateProperties.HORIZONTAL_FACING), hit);
                hits += hit[0] != null ? 1 : 0;
            }
            if (hits > bestHits) {
                bestHits = hits;
                best = mask;
            }
            if (hits == emitters.size()) {
                break;
            }
        }
        setMirrors(level, mirrors, best, n);
        return bestHits + "/" + emitters.size() + " (" + mirrors.size() + " mirrors)";
    }

    private static void setMirrors(ServerLevel level, List<BlockPos> mirrors, int mask, int n) {
        for (int i = 0; i < n; i++) {
            BlockState s = level.getBlockState(mirrors.get(i));
            boolean want = (mask >> i & 1) == 1;
            if (s.getValue(BeamBlocks.Mirror.FLIPPED) != want) {
                level.setBlock(mirrors.get(i), s.setValue(BeamBlocks.Mirror.FLIPPED, want), Block.UPDATE_ALL);
            }
        }
    }

    /** What under a hatch still keeps it shut, for the failure line. */
    private static String blocking(ServerLevel level, BlockPos gate) {
        List<String> out = new ArrayList<>();
        for (BlockEntity be : LabScan.in(level, below(gate), BlockEntity.class)) {
            BlockState s = be.getBlockState();
            if (be instanceof SpawnerBlockEntity || s.hasProperty(BlockStateProperties.LIT) && !s.getValue(BlockStateProperties.LIT)
                    && BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace().equals(MODID)) {
                out.add(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath() + "@" + be.getBlockPos().toShortString());
            }
        }
        return out.toString();
    }

    private static void killMobs(ServerLevel level, BoundingBox box) {
        for (Entity e : level.getEntitiesOfClass(Entity.class, AABB.of(box).inflate(4),
                e -> e instanceof Enemy && e.getExistingDataOrNull(LabRegistry.BOSS.get()) == null)) {
            e.discard();
        }
    }

    /** Puts the player on the nearest standable spot (two air over a solid floor) around {@code at}, facing {@code look}. */
    private static void standAt(ServerPlayer p, ServerLevel level, BlockPos at, BlockPos look) {
        for (int r = 0; r <= 3; r++) {
            for (int dy = 0; dy >= -3; dy--) {
                for (Direction d : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    BlockPos q = at.offset(0, dy, 0).relative(d, r);
                    if (level.getBlockState(q).getCollisionShape(level, q).isEmpty()
                            && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()
                            && !level.getBlockState(q.below()).getCollisionShape(level, q.below()).isEmpty()) {
                        Vec3 c = Vec3.atBottomCenterOf(q), l = Vec3.atCenterOf(look);
                        float yaw = (float) Math.toDegrees(Math.atan2(-(l.x - c.x), l.z - c.z));
                        p.teleportTo(level, c.x, c.y, c.z, java.util.Set.of(), yaw, 10.0F, true);
                        return;
                    }
                }
            }
        }
        Vec3 c = Vec3.atBottomCenterOf(at);
        p.teleportTo(level, c.x, c.y, c.z, java.util.Set.of(), p.getYRot(), 10.0F, true);
    }

    private static @Nullable StructureStart startNear(ServerLevel level, BlockPos at, boolean tower) {
        var chunk = level.getChunk(at.getX() >> 4, at.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS);
        for (var e : chunk.getAllStarts().entrySet()) {
            var key = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(e.getKey());
            if (key != null && key.getNamespace().equals(MODID) && key.getPath().startsWith(tower ? "wizard_tower" : "forsaken_lab")
                    && e.getValue().isValid()) {
                return e.getValue();
            }
        }
        return null;
    }

    private static @Nullable WardCoreBlockEntity core(ServerLevel level, BoundingBox box) {
        List<WardCoreBlockEntity> cores = LabScan.in(level, AABB.of(box).inflate(2), WardCoreBlockEntity.class);
        return cores.isEmpty() ? null : cores.get(0);
    }

    private static void run(ServerLevel level, String cmd) {
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), cmd);
    }
}
