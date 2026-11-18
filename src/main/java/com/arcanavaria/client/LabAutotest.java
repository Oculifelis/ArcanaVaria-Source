package com.arcanavaria.client;

import com.arcanavaria.lab.AspectLockBlockEntity;
import com.arcanavaria.lab.LabRegistry;
import com.arcanavaria.lab.PhaseBlock;
import com.arcanavaria.lab.RuneLockBlock;
import com.arcanavaria.lab.RuneStoneBlock;
import com.arcanavaria.lab.WardCoreBlock;
import com.arcanavaria.lab.WardCoreBlockEntity;
import com.arcanavaria.lab.boss.GuardianAltarBlockEntity;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.content.item.PhialItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import org.slf4j.Logger;

/**
 * The lab stage of the client test, driven on the server: a ward with every puzzle in its box. The player walks in
 * (survival, kept alive) and is sealed; breaking the shell, a chorus teleport and stepping out are each refused; the
 * aspect lock takes a phial, the rune lock a wrong and then the right sequence, the beam reaches its receptor and
 * loses it when the mirror flips, the phase stones keep time; the guardian rises with its traits and its death opens
 * the ward. Then a second ward: the sealed player dies and must respawn at its exit with the ward dormant again.
 */
final class LabAutotest {
    private static final int Y = -60;
    private static final BlockPos CORE = new BlockPos(0, Y, 0), LOCK = new BlockPos(3, Y, 6), RUNE_LOCK = new BlockPos(-6, Y, 6),
            EMITTER = new BlockPos(-7, Y, -5), MIRROR = new BlockPos(-3, Y, -5), RECEPTOR = new BlockPos(-3, Y, -8),
            ALTAR = new BlockPos(0, Y, 6), SHELL = new BlockPos(2, Y, -2), CORE2 = new BlockPos(40, Y, 0);

    private LabAutotest() {
    }

    static void setUp(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        run(level, "kill @e[type=!minecraft:player]");
        run(level, "fill -12 -60 -12 12 -45 12 minecraft:air");
        run(level, "fill 28 -60 -12 52 -45 12 minecraft:air");
        p.getInventory().clearContent();
        p.setGameMode(GameType.SURVIVAL);
        keepAlive(p);
        set(level, CORE, LabRegistry.WARD_CORE.get().defaultBlockState());
        set(level, LOCK, LabRegistry.ASPECT_LOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        for (int r = 0; r < 4; r++) {
            set(level, new BlockPos(-5 + r, Y, 6), LabRegistry.RUNE_STONE.get().defaultBlockState().setValue(RuneStoneBlock.RUNE, r));
        }
        set(level, RUNE_LOCK, LabRegistry.RUNE_LOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        set(level, EMITTER, LabRegistry.BEAM_EMITTER.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        set(level, MIRROR, LabRegistry.BEAM_MIRROR.get().defaultBlockState());
        set(level, RECEPTOR, LabRegistry.BEAM_RECEPTOR.get().defaultBlockState());
        for (int i = 0; i < 4; i++) {
            set(level, new BlockPos(4 + i, Y, -6), LabRegistry.PHASE_BLOCK.get().defaultBlockState().setValue(PhaseBlock.OFFSET, i));
        }
        set(level, new BlockPos(6, Y, 2), LabRegistry.VIS_LIFT.get().defaultBlockState());
        set(level, ALTAR, LabRegistry.GUARDIAN_ALTAR.get().defaultBlockState());
        set(level, SHELL, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        run(level, "tp @a 0.5 -59 -2.5 0 20");
    }

    static void sealed(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        log.info("AVTEST lab ward={} sealed={} (want sealed, true)", level.getBlockState(CORE).getValue(WardCoreBlock.WARD).getSerializedName(),
                WardCoreBlockEntity.isSealed(p));
        boolean broke = p.gameMode.destroyBlock(SHELL);
        var chorus = new EntityTeleportEvent.ItemConsumption(p, new ItemStack(Items.CHORUS_FRUIT), 30, Y + 1, 30);
        NeoForge.EVENT_BUS.post(chorus);
        log.info("AVTEST lab shell broke={} stands={} chorus cancelled={} (want false, true, true)", broke,
                level.getBlockState(SHELL).is(Blocks.POLISHED_DEEPSLATE), chorus.isCanceled());
        p.teleportTo(0.5, Y + 1, -14.5);       // out through the wall, as a mod's teleport might
    }

    static void puzzles(ServerPlayer p, Logger log, int step) {
        ServerLevel level = (ServerLevel) p.level();
        switch (step) {
            case 0 -> {
                log.info("AVTEST lab pulled back to z={} (want inside, z > -8)", Math.round(p.getZ() * 10) / 10.0);
                var ordo = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).getOrThrow(AspectLockBlockEntity.DEFAULT);
                ItemStack phial = PhialItem.makeFilled(ordo);
                p.setItemInHand(InteractionHand.MAIN_HAND, phial);
                level.getBlockState(LOCK).useItemOn(phial, level, p, InteractionHand.MAIN_HAND, hit(LOCK, Direction.NORTH));
                AspectLockBlockEntity lock = (AspectLockBlockEntity) level.getBlockEntity(LOCK);
                log.info("AVTEST lab aspect lock have={}/{} lit={} signal={} (want 10/10, true, 15)", lock.have(), lock.need(),
                        level.getBlockState(LOCK).getValue(BlockStateProperties.LIT), level.getSignal(LOCK, Direction.SOUTH));   // what the block north of the lock receives
                press(level, p, 0);
                press(level, p, 2);
                log.info("AVTEST lab runes wrong: pips={} (want 0)", level.getBlockState(RUNE_LOCK).getValue(RuneLockBlock.PIPS));
                log.info("AVTEST lab beam receptor lit={} (want true)", level.getBlockState(RECEPTOR).getValue(BlockStateProperties.LIT));
                log.info("AVTEST lab phase solid now: {}", phases(level));
            }
            case 1 -> {
                for (int r = 0; r < 4; r++) {
                    press(level, p, r);
                }
                log.info("AVTEST lab runes right: pips={} open={} (want 4, true)", level.getBlockState(RUNE_LOCK).getValue(RuneLockBlock.PIPS),
                        level.getBlockState(RUNE_LOCK).getValue(RuneLockBlock.LIT));
                level.getBlockState(MIRROR).useWithoutItem(level, p, hit(MIRROR, Direction.UP));
                log.info("AVTEST lab phase solid 1s later: {}", phases(level));
            }
            default -> log.info("AVTEST lab beam after flip lit={} (want false)", level.getBlockState(RECEPTOR).getValue(BlockStateProperties.LIT));
        }
    }

    static void boss(ServerPlayer p, Logger log, boolean kill) {
        ServerLevel level = (ServerLevel) p.level();
        GuardianAltarBlockEntity altar = (GuardianAltarBlockEntity) level.getBlockEntity(ALTAR);
        List<Mob> bosses = level.getEntitiesOfClass(Mob.class, new AABB(ALTAR).inflate(16), m -> m.hasData(LabRegistry.BOSS.get()));
        if (!kill) {
            for (Mob m : bosses) {
                var d = m.getData(LabRegistry.BOSS.get());
                log.info("AVTEST lab guardian {} hp={} scale={} archetype={} traits={}", m.getType().builtInRegistryHolder().key().identifier(),
                        m.getMaxHealth(), m.getScale(), d.archetype().identifier(), d.traits().stream().map(k -> k.identifier().getPath()).toList());
            }
            log.info("AVTEST lab altar phase={} guardians={} (want fighting, 1)", altar.phase().getSerializedName(), bosses.size());
            return;
        }
        bosses.forEach(m -> m.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE));
        int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(ALTAR).inflate(4)).size();
        log.info("AVTEST lab guardian slain: altar={} ward={} sealed={} reward stacks={} (want defeated, cleared, false, >0)",
                altar.phase().getSerializedName(), level.getBlockState(CORE).getValue(WardCoreBlock.WARD).getSerializedName(),
                WardCoreBlockEntity.isSealed(p), drops);
        log.info("AVTEST lab shell breakable after clear={} (want true)", p.gameMode.destroyBlock(SHELL) && level.getBlockState(SHELL).isAir());
    }

    static void deathSetUp(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        set(level, CORE2, LabRegistry.WARD_CORE.get().defaultBlockState());
        p.teleportTo(40.5, Y + 1, 2.5);
    }

    static void die(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        log.info("AVTEST lab ward2={} sealed={} (want sealed, true)", level.getBlockState(CORE2).getValue(WardCoreBlock.WARD).getSerializedName(),
                WardCoreBlockEntity.isSealed(p));
        p.removeAllEffects();
        p.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
    }

    static void afterRespawn(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 at = p.position();
        log.info("AVTEST lab respawned at {} {} {} ward2={} linked={} (want 40.5 -60 -11.5, dormant, false)", at.x, at.y, at.z,
                level.getBlockState(CORE2).getValue(WardCoreBlock.WARD).getSerializedName(), p.hasData(LabRegistry.WARD_LINK.get()));
        p.setGameMode(GameType.CREATIVE);
    }

    private static void press(ServerLevel level, ServerPlayer p, int rune) {
        BlockPos at = new BlockPos(-5 + rune, Y, 6);
        level.getBlockState(at).useWithoutItem(level, p, hit(at, Direction.UP));
    }

    private static String phases(ServerLevel level) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            s.append(level.getBlockState(new BlockPos(4 + i, Y, -6)).getValue(PhaseBlock.SOLID) ? '#' : '.');
        }
        return s.toString();
    }

    private static void keepAlive(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60, 4, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 4, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20 * 60, 0, false, false));
    }

    private static BlockHitResult hit(BlockPos at, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(at).relative(face, 0.5), face, at, false);
    }

    private static void set(ServerLevel level, BlockPos at, BlockState state) {
        level.setBlock(at, state, 3);
    }

    private static void run(ServerLevel level, String cmd) {
        var server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
    }
}
