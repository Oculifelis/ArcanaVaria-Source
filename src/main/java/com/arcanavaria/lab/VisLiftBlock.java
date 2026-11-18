package com.arcanavaria.lab;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A vis-current lift: a column of rising motes up to 16 blocks high (it stops at the first solid block) that carries
 * whoever steps in up; sneak to sink slowly. With needs_signal set (by the structure) it runs only while powered,
 * so a lock can switch it on.
 */
public class VisLiftBlock extends BaseEntityBlock {
    public static final BooleanProperty NEEDS_SIGNAL = BooleanProperty.create("needs_signal");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<VisLiftBlock> CODEC = simpleCodec(VisLiftBlock::new);
    public static final int HEIGHT = 16;
    private static final DustParticleOptions MOTE = new DustParticleOptions(0x8FD8FF, 0.9F);

    public VisLiftBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(NEEDS_SIGNAL, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NEEDS_SIGNAL, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    public static boolean active(BlockState state) {
        return !state.getValue(NEEDS_SIGNAL) || state.getValue(POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tick(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LabRegistry.VIS_LIFT_BE.get(), VisLiftBlock::tick);
    }

    private static void tick(Level level, BlockPos pos, BlockState state, Tick be) {
        if (!active(state)) {
            return;
        }
        int top = 1;
        while (top < HEIGHT && !level.getBlockState(pos.above(top)).isCollisionShapeFullBlock(level, pos.above(top))) {
            top++;
        }
        AABB column = new AABB(pos.getX() + 0.1, pos.getY() + 1, pos.getZ() + 0.1, pos.getX() + 0.9, pos.getY() + top, pos.getZ() + 0.9);
        for (Entity e : level.getEntitiesOfClass(Entity.class, column, x -> x instanceof LivingEntity || x instanceof net.minecraft.world.entity.item.ItemEntity)) {
            // players move themselves: the client lifts its own player, the server everything else
            if (e instanceof Player ? !e.isLocalInstanceAuthoritative() : level.isClientSide()) {
                continue;
            }
            Vec3 v = e.getDeltaMovement();
            double vy = e.isShiftKeyDown() ? Math.max(v.y, -0.12) : Math.min(v.y + 0.11, 0.42);
            e.setDeltaMovement(v.x * 0.9, vy, v.z * 0.9);
            e.resetFallDistance();
        }
        if (level.isClientSide()) {
            var r = level.getRandom();
            for (int i = 0; i < 2; i++) {
                level.addParticle(MOTE, pos.getX() + 0.15 + r.nextFloat() * 0.7, pos.getY() + 1 + r.nextFloat() * top * 0.3,
                        pos.getZ() + 0.15 + r.nextFloat() * 0.7, 0, 0.25, 0);
            }
        }
    }

    /** Only here so the lift ticks; it keeps nothing. */
    public static class Tick extends BlockEntity {
        public Tick(BlockPos pos, BlockState state) {
            super(LabRegistry.VIS_LIFT_BE.get(), pos, state);
        }
    }
}
