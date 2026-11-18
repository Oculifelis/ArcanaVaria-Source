package com.arcanavaria.lens;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places blocks for the Lens: each one takes the matching item from its builder's inventory (none in creative) and
 * {@link #COST} centivis of Ordo from a wand they carry. Recent operations, with what stood there before and what was
 * spent, are kept per player and dimension until the world closes.
 */
public final class LensBuilder {
    /** Ordo centivis per placed block (a hundredth of a vis each): 5 means 20 blocks a vis. */
    public static final int COST = 5;
    public static final int MAX_UNDO = 20;

    private record Placed(BlockPos at, BlockState before, BlockState after, List<ItemStack> spent, ItemStack recovered) {
    }

    // World instances keep integrated-server sessions separate. Weak keys let a closed world release its history.
    private static final Map<ServerLevel, Map<UUID, Deque<List<Placed>>>> HISTORY = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> PROTECTION_EVENT = new ThreadLocal<>();

    public enum Result { DONE, NO_VIS, NO_ITEMS }

    private LensBuilder() {
    }

    /** Starts a new operation. An empty or failed attempt keeps the preceding undo steps. */
    public static void begin(Player p) {
        if (p.level() instanceof ServerLevel level) {
            Deque<List<Placed>> history = history(level, p);
            if (history.isEmpty() || !history.getFirst().isEmpty()) {
                history.addFirst(new ArrayList<>());
            }
        }
    }

    private static Deque<List<Placed>> history(ServerLevel level, Player p) {
        return HISTORY.computeIfAbsent(level, k -> new HashMap<>())
                .computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
    }

    /** Places {@code state} at {@code at} if the spot can take it; joins the current operation. */
    public static Result place(ServerLevel level, ServerPlayer p, BlockPos at, BlockState state, boolean replace) {
        state = com.arcanavaria.lens.bg2.BlockStates.cleanBlockState(state);
        if (!canChange(level, p, at) || state.hasBlockEntity() || !state.canSurvive(level, at)) {
            return Result.DONE;
        }
        BlockState before = level.getBlockState(at);
        if (before == state || (!replace && !before.canBeReplaced())) {
            return Result.DONE;
        }
        boolean exchanging = replace && !before.canBeReplaced();
        if (exchanging && before.getBlock().asItem() == Items.AIR) {
            return Result.DONE;
        }
        if (exchanging && NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, at, before, p)).isCanceled()) {
            return Result.DONE;
        }
        if (placementCanceled(level, p, at)) {
            return Result.DONE;
        }
        int cost = settings(p).visCost();
        List<ItemStack> spent = List.of();
        ItemStack recovered = ItemStack.EMPTY;
        if (!p.isCreative()) {
            if (wand(p, TTAspects.ORDO, cost) == null || (exchanging && wand(p, TTAspects.PERDITIO, cost) == null)) {
                return Result.NO_VIS;
            }
            spent = LensInventory.take(p, state.getBlock().asItem(), LensInventory.quantity(state));
            if (spent.isEmpty()) {
                return Result.NO_ITEMS;
            }
        }
        if (!level.setBlock(at, state, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS)) {
            LensInventory.give(p, spent);
            return Result.DONE;
        }
        if (!p.isCreative()) {
            spendVis(p, TTAspects.ORDO, cost);
            if (exchanging) {
                spendVis(p, TTAspects.PERDITIO, cost);
                recovered = new ItemStack(before.getBlock().asItem(), LensInventory.quantity(before));
                LensInventory.give(p, recovered);
            }
        }
        record(level, p, new Placed(at.immutable(), before, state, spent, recovered));
        return Result.DONE;
    }

    public static LensSettings settings(Player player) {
        ItemStack lens = LensRegistry.held(player);
        return lens == null ? LensSettings.DEFAULT : lens.getOrDefault(LensRegistry.SETTINGS.get(), LensSettings.DEFAULT);
    }

    public static boolean checkingPlacement() {
        return Boolean.TRUE.equals(PROTECTION_EVENT.get());
    }

    private static boolean placementCanceled(ServerLevel level, ServerPlayer player, BlockPos at) {
        boolean nested = checkingPlacement();
        PROTECTION_EVENT.set(true);
        try {
            return EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, at), Direction.UP);
        } finally {
            if (nested) {
                PROTECTION_EVENT.set(true);
            } else {
                PROTECTION_EVENT.remove();
            }
        }
    }

    public static boolean canChange(Level level, Player player, BlockPos at) {
        if (!player.mayBuild() || !level.isLoaded(at) || level.isOutsideBuildHeight(at)
                || !level.getWorldBorder().isWithinBounds(at) || !level.mayInteract(player, at)) {
            return false;
        }
        BlockState before = level.getBlockState(at);
        return !before.hasBlockEntity() && before.getDestroySpeed(level, at) >= 0
                && !com.arcanavaria.lab.WardEvents.locked(level, at, before, player);
    }

    /** BG2 destruction semantics: removed blocks are voided, and undo restores the recorded states. */
    public static Result destroy(ServerLevel level, ServerPlayer player, BlockPos at) {
        if (!canChange(level, player, at) || level.getBlockState(at).isAir()) {
            return Result.DONE;
        }
        BlockState before = level.getBlockState(at);
        if (NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, at, before, player)).isCanceled()) {
            return Result.DONE;
        }
        int cost = settings(player).visCost();
        if (!player.isCreative() && wand(player, TTAspects.PERDITIO, cost) == null) {
            return Result.NO_VIS;
        }
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if (level.setBlock(at, air, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS)) {
            if (!player.isCreative()) {
                spendVis(player, TTAspects.PERDITIO, cost);
            }
            record(level, player, new Placed(at.immutable(), before, air, List.of(), ItemStack.EMPTY));
        }
        return Result.DONE;
    }

    private static void record(ServerLevel level, Player p, Placed change) {
        Deque<List<Placed>> history = history(level, p);
        if (history.isEmpty()) {
            history.addFirst(new ArrayList<>());
        }
        history.getFirst().add(change);
        while (history.size() > MAX_UNDO) {
            history.removeLast();
        }
    }

    /** Puts back what the last operation changed, where it still stands as placed, and returns the spent items. */
    public static int undo(ServerLevel level, ServerPlayer p) {
        Deque<List<Placed>> history = history(level, p);
        if (!history.isEmpty() && history.getFirst().isEmpty()) {
            history.removeFirst();
        }
        List<Placed> op = history.peekFirst();
        if (op == null) {
            return 0;
        }
        // Adapted from BG2 BaseGadget.canUndo: never load chunks or consume history when any target is unloaded.
        // Copyright (c) 2023 Direwolf20-MC, MIT. Full notice in LICENSE-third-party.md.
        if (op.stream().anyMatch(change -> !level.isLoaded(change.at()))) {
            return 0;
        }
        int count = 0;
        for (int i = op.size() - 1; i >= 0; i--) {
            Placed change = op.get(i);
            if (level.getBlockState(change.at()) != change.after()) {
                op.remove(i);
                continue;
            }
            if (!canChange(level, p, change.at())) {
                continue;
            }
            List<ItemStack> reclaimed = change.recovered().isEmpty() ? List.of()
                    : LensInventory.take(p, change.recovered().getItem(), change.recovered().getCount());
            if (!change.recovered().isEmpty() && reclaimed.isEmpty()) {
                continue;
            }
            if (!level.setBlock(change.at(), change.before(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS)) {
                LensInventory.give(p, reclaimed);
                continue;
            }
            LensInventory.give(p, change.spent());
            op.remove(i);
            count++;
        }
        if (op.isEmpty()) {
            history.removeFirst();
        }
        history.addFirst(new ArrayList<>());
        return count;
    }

    private static ItemStack wand(Player p, ResourceKey<IAspect> aspect, int cost) {
        var inventory = p.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && WandAccess.getVis(stack, aspect) >= cost) {
                return stack;
            }
        }
        return null;
    }

    private static void spendVis(Player p, ResourceKey<IAspect> aspect, int cost) {
        ItemStack wand = wand(p, aspect, cost);
        if (wand == null) {
            throw new IllegalStateException("Lens payment changed during placement");
        }
        ItemStack after = WandAccess.withVis(wand, aspect, WandAccess.getVis(wand, aspect) - cost);
        var inventory = p.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == wand) {
                inventory.setItem(slot, after);
                return;
            }
        }
    }
}
