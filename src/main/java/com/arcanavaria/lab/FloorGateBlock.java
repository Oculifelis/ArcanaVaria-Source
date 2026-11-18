package com.arcanavaria.lab;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A warded hatch over a tower stair (Worldforge's floor/gate marker). It stays shut while the floor below it is not
 * cleared: a lock there (aspect lock, rune lock, beam receptor) not yet lit, or a guardian spawner still standing.
 * A parkour floor has neither, so its hatch stands open. Locks go dark again when the ward resets, and the hatch shuts
 * with them.
 */
public class FloorGateBlock extends BaseEntityBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final MapCodec<FloorGateBlock> CODEC = simpleCodec(FloorGateBlock::new);
    private static final int STEP = 20;
    /** The floor below: the tower's inside from the hatch (it sits near a corner), down one storey (10 to 13 high). */
    private static final int REACH = 13, DEPTH = 13;

    public FloorGateBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    /** Whether the floor under the hatch at {@code pos} is cleared. */
    public static boolean cleared(Level level, BlockPos pos) {
        AABB below = new AABB(pos.getX() - REACH, pos.getY() - DEPTH, pos.getZ() - REACH,
                pos.getX() + REACH + 1, pos.getY(), pos.getZ() + REACH + 1);
        for (BlockEntity be : LabScan.in(level, below, BlockEntity.class)) {
            if (be instanceof SpawnerBlockEntity) {
                return false;
            }
            BlockState s = be.getBlockState();
            if ((s.is(LabRegistry.ASPECT_LOCK.get()) || s.is(LabRegistry.RUNE_LOCK.get()) || s.is(LabRegistry.BEAM_RECEPTOR.get()))
                    && !s.getValue(BlockStateProperties.LIT)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tick(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, LabRegistry.FLOOR_GATE_BE.get(), FloorGateBlock::tick);
    }

    private static void tick(Level level, BlockPos pos, BlockState state, Tick be) {
        if (level.getGameTime() % STEP != Math.floorMod(pos.asLong(), STEP)
                || !level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32)) {
            return;
        }
        boolean open = cleared(level, pos);
        if (open != state.getValue(OPEN)) {
            level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
            level.playSound(null, pos, open ? SoundEvents.AMETHYST_BLOCK_BREAK : SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.7F,
                    open ? 0.7F : 1.3F);
            if (level instanceof ServerLevel server) {
                var c = pos.getCenter();
                server.sendParticles(open ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD, c.x, c.y, c.z, 8, 0.4, 0.3, 0.4, 0.02);
            }
        }
    }

    /** Only here so the hatch keeps watch; it keeps nothing. */
    public static class Tick extends BlockEntity {
        public Tick(BlockPos pos, BlockState state) {
            super(LabRegistry.FLOOR_GATE_BE.get(), pos, state);
        }
    }
}
