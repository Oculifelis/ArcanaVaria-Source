package com.arcanavaria.lab;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A timed path stone. The clock runs in four one-second steps; a stone is solid for the two steps from its offset
 * (0-3, set by the structure) and a ghost you fall through for the other two, so a row with rising offsets is a wave
 * you have to run with. A redstone signal holds it solid.
 */
public class PhaseBlock extends BaseEntityBlock {
    public static final BooleanProperty SOLID = BooleanProperty.create("solid");
    public static final IntegerProperty OFFSET = IntegerProperty.create("offset", 0, 3);
    public static final MapCodec<PhaseBlock> CODEC = simpleCodec(PhaseBlock::new);
    public static final int STEP = 20;

    public PhaseBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(SOLID, true).setValue(OFFSET, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOLID, OFFSET);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(SOLID) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    public static boolean solidAt(long time, int offset) {
        return Math.floorMod(time / STEP - offset, 4) < 2;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tick(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, LabRegistry.PHASE_BLOCK_BE.get(), PhaseBlock::tick);
    }

    private static void tick(Level level, BlockPos pos, BlockState state, Tick be) {
        if (level.getGameTime() % STEP != 0) {
            return;
        }
        boolean solid = level.hasNeighborSignal(pos) || solidAt(level.getGameTime(), state.getValue(OFFSET));
        if (solid != state.getValue(SOLID)) {
            level.setBlock(pos, state.setValue(SOLID, solid), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, solid ? SoundEvents.AMETHYST_BLOCK_PLACE : SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.5F,
                    solid ? 1.4F : 0.8F);
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                var c = pos.getCenter();
                server.sendParticles(solid ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 6, 0.35, 0.35, 0.35, 0.01);
            }
        }
    }

    /** Only here so the stone keeps time; it keeps nothing. */
    public static class Tick extends BlockEntity {
        public Tick(BlockPos pos, BlockState state) {
            super(LabRegistry.PHASE_BLOCK_BE.get(), pos, state);
        }
    }
}
