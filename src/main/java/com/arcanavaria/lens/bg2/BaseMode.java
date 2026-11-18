/* SPDX-License-Identifier: MIT
 * Copyright (c) 2023 Direwolf20-MC
 * Adapted from Building Gadgets 2 v26.1.2.3 BaseMode.java and GadgetUtils.getSquareArea.
 * Full notice in LICENSE-third-party.md.
 */
package com.arcanavaria.lens.bg2;

import com.arcanavaria.lens.LensSettings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public abstract class BaseMode {
    protected final boolean isExchanging;
    protected LensSettings settings = LensSettings.DEFAULT;
    private BlockState target;

    protected BaseMode(boolean exchanging) {
        isExchanging = exchanging;
    }

    public final ArrayList<StatePos> collect(Direction hitSide, Player player, BlockPos start, BlockState state,
            LensSettings settings) {
        this.settings = settings;
        target = player.level().getBlockState(start);
        return player.mayBuild() ? collectWorld(hitSide, player, start, state) : new ArrayList<>();
    }

    public abstract ArrayList<StatePos> collectWorld(Direction hitSide, Player player, BlockPos start, BlockState state);

    protected boolean isPosValid(Level level, Player player, BlockPos pos, BlockState state) {
        if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos) || !state.canSurvive(level, pos)) {
            return false;
        }
        BlockState before = level.getBlockState(pos);
        if (!isExchanging) {
            return before.canBeReplaced();
        }
        return !before.isAir() && !before.hasBlockEntity() && before.getDestroySpeed(level, pos) >= 0
                && before != state && (settings.fuzzy() || before == target);
    }

    protected ArrayList<StatePos> removeUnConnected(Level level, Player player, BlockPos startAt,
            ArrayList<StatePos> coordinates, Direction hitSide) {
        Map<BlockPos, StatePos> positions = new HashMap<>();
        coordinates.forEach(entry -> positions.put(entry.pos(), entry));
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> pending = new ArrayDeque<>();
        pending.add(startAt);
        ArrayList<StatePos> connected = new ArrayList<>();
        while (!pending.isEmpty()) {
            BlockPos at = pending.remove();
            if (!positions.containsKey(at) || !visited.add(at)) {
                continue;
            }
            connected.add(positions.get(at));
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() != hitSide.getAxis()) {
                    pending.add(at.relative(direction));
                }
            }
        }
        return connected;
    }

    protected static AABB squareArea(BlockPos pos, Direction face, int range) {
        return switch (face.getAxis()) {
            case Y -> new AABB(pos.getX() - range, pos.getY(), pos.getZ() - range,
                    pos.getX() + range, pos.getY(), pos.getZ() + range);
            case Z -> new AABB(pos.getX() - range, pos.getY() - range, pos.getZ(),
                    pos.getX() + range, pos.getY() + range, pos.getZ());
            case X -> new AABB(pos.getX(), pos.getY() - range, pos.getZ() - range,
                    pos.getX(), pos.getY() + range, pos.getZ() + range);
        };
    }
}
