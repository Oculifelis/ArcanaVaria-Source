package com.arcanavaria.lab.boss;

import com.arcanavaria.lab.LabRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The altar a lab's guardian rises from (GuardianAltarBlockEntity); awake, it smoulders with soul fire. */
public class GuardianAltarBlock extends BaseEntityBlock {
    public static final BooleanProperty AWAKE = BooleanProperty.create("awake");
    public static final MapCodec<GuardianAltarBlock> CODEC = simpleCodec(GuardianAltarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 10, 14),
            Block.box(1, 10, 1, 15, 13, 15));

    public GuardianAltarBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AWAKE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AWAKE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuardianAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, LabRegistry.GUARDIAN_ALTAR_BE.get(), GuardianAltarBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(AWAKE)) {
            if (r.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, 0.02, 0);
            }
            return;
        }
        for (int i = 0; i < 3; i++) {
            double a = r.nextFloat() * Math.PI * 2;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5 + Math.cos(a) * 0.45, pos.getY() + 0.85,
                    pos.getZ() + 0.5 + Math.sin(a) * 0.45, -Math.cos(a) * 0.02, 0.06, -Math.sin(a) * 0.02);
        }
    }
}
