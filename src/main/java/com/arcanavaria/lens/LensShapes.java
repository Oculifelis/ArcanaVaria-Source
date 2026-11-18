/* SPDX-License-Identifier: MIT
 * Copyright (c) 2023 Direwolf20-MC
 * Destruction-area geometry adapted from Building Gadgets 2 v26.1.2.3 GadgetUtils.getDestructionArea.
 * Full notice in LICENSE-third-party.md.
 */
package com.arcanavaria.lens;

import com.arcanavaria.lens.bg2.BaseMode;
import com.arcanavaria.lens.bg2.Grid;
import com.arcanavaria.lens.bg2.HorizontalRow;
import com.arcanavaria.lens.bg2.HorizontalWall;
import com.arcanavaria.lens.bg2.Stairs;
import com.arcanavaria.lens.bg2.StatePos;
import com.arcanavaria.lens.bg2.Surface;
import com.arcanavaria.lens.bg2.VerticalColumn;
import com.arcanavaria.lens.bg2.VerticalWall;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** The same shape collectors feed the server operation and the client preview. */
public final class LensShapes {
    private LensShapes() {}

    public static List<StatePos> collect(Player player, LensMode mode, LensSettings settings,
            BlockPos start, Direction face, BlockState state) {
        BaseMode collector = switch (mode) {
            case SURFACE -> new Surface(settings.exchange());
            case COLUMN -> new VerticalColumn(settings.exchange());
            case ROW -> new HorizontalRow(settings.exchange());
            case GRID -> new Grid(settings.exchange());
            case STAIRS -> new Stairs();
            case VERTICAL_WALL -> new VerticalWall();
            case HORIZONTAL_WALL -> new HorizontalWall();
            default -> throw new IllegalArgumentException("Not a building shape: " + mode);
        };
        return collector.collect(face, player, start, state, settings).stream()
                .limit(LensGeometry.MAX).map(entry -> new StatePos(entry.state(), start.offset(entry.pos()))).toList();
    }

    public static List<BlockPos> destruction(Player player, BlockPos start, Direction face, LensSettings settings) {
        boolean vertical = face.getAxis().isVertical();
        Direction up = vertical ? player.getDirection() : Direction.UP;
        Direction right = vertical ? up.getClockWise() : face.getCounterClockWise();
        BlockPos first = start.relative(right.getOpposite(), settings.width() / 2)
                .relative(up, settings.height() / 2);
        BlockPos second = start.relative(right, (settings.width() - 1) / 2)
                .relative(up.getOpposite(), (settings.height() - 1) / 2)
                .relative(face.getOpposite(), settings.depth() - 1);
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(first, second)) {
            if (positions.size() == LensGeometry.MAX) {
                break;
            }
            if (LensBuilder.canChange(player.level(), player, at) && !player.level().getBlockState(at).isAir()) {
                positions.add(at.immutable());
            }
        }
        return positions;
    }
}
