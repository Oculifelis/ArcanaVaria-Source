package com.arcanavaria.lab;

import static com.arcanavaria.ArcanaVaria.MODID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * The ward's anti-cheese, all on the game bus: no breaking or building on a standing ward's shell (blocks tagged
 * #arcanavaria:lab_mutable excepted, for the lattice puzzles), explosions spare it, and no pearl, chorus fruit or other
 * teleport gets a player in or out. Death inside: you respawn at the ward's exit, and the tower resets once nobody
 * sealed is left. Coming back to a ward that reset or fell while you were away puts you at its exit.
 */
public final class WardEvents {
    private WardEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WardEvents::broken);
        NeoForge.EVENT_BUS.addListener(WardEvents::placed);
        NeoForge.EVENT_BUS.addListener(WardEvents::explosion);
        NeoForge.EVENT_BUS.addListener(WardEvents::teleport);
        NeoForge.EVENT_BUS.addListener(WardEvents::died);
        NeoForge.EVENT_BUS.addListener(WardEvents::respawned);
        NeoForge.EVENT_BUS.addListener(WardEvents::loggedIn);
    }

    /** True when a survival player may not change the block at {@code pos} because a standing ward guards it. */
    public static boolean locked(Level level, BlockPos pos, BlockState state, Player p) {
        return !p.isCreative() && !state.is(LabRegistry.MUTABLE) && WardCoreBlockEntity.guarding(level, pos) != null;
    }

    private static void broken(BreakBlockEvent e) {
        if (e.getLevel() instanceof Level level && locked(level, e.getPos(), e.getState(), e.getPlayer())) {
            e.setCanceled(true);
            e.getPlayer().sendOverlayMessage(Component.translatable("message.%s.ward.shell".formatted(MODID)));
        }
    }

    private static void placed(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof Player p && e.getLevel() instanceof Level level && locked(level, e.getPos(), e.getPlacedBlock(), p)) {
            e.setCanceled(true);
            p.sendOverlayMessage(Component.translatable("message.%s.ward.shell".formatted(MODID)));
        }
    }

    private static void explosion(ExplosionEvent.Detonate e) {
        e.getAffectedBlocks().removeIf(pos -> WardCoreBlockEntity.guarding(e.getLevel(), pos) != null);
    }

    private static void teleport(EntityTeleportEvent e) {
        if (e instanceof EntityTeleportEvent.TeleportCommand || e instanceof EntityTeleportEvent.SpreadPlayersCommand
                || !(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) {
            return;
        }
        BlockPos to = BlockPos.containing(e.getTarget());
        if (WardCoreBlockEntity.isSealed(p) || WardCoreBlockEntity.guarding(p.level(), to) != null) {
            e.setCanceled(true);
            p.sendOverlayMessage(Component.translatable("message.%s.ward.teleport".formatted(MODID)));
        }
    }

    private static void died(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            WardCoreBlockEntity w = WardCoreBlockEntity.of(p);
            if (w != null && w.getLevel() instanceof ServerLevel level) {
                w.leave(level, p.getUUID());
            }
        }
    }

    private static void respawned(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.hasData(LabRegistry.WARD_LINK.get())) {
            toExit(p);
        }
    }

    private static void loggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.hasData(LabRegistry.WARD_LINK.get())) {
            WardCoreBlockEntity w = WardCoreBlockEntity.of(p);
            if (w == null || !w.holds(p.getUUID())) {
                toExit(p);
            }
        }
    }

    /** Out at the ward's exit (or just unlinked if its core is gone), free of it. */
    private static void toExit(ServerPlayer p) {
        WardCoreBlockEntity w = WardCoreBlockEntity.of(p);
        p.removeData(LabRegistry.WARD_LINK.get());
        if (w != null && w.getLevel() instanceof ServerLevel level) {
            w.leave(level, p.getUUID());
            Vec3 at = w.exit();
            p.teleport(new TeleportTransition(level, at, Vec3.ZERO, p.getYRot(), p.getXRot(), TeleportTransition.DO_NOTHING));
            p.sendSystemMessage(Component.translatable("message.%s.ward.expelled".formatted(MODID)));
        }
    }
}
