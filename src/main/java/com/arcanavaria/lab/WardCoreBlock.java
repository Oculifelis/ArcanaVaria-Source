package com.arcanavaria.lab;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/** The ward core a sealed tower is built round (WardCoreBlockEntity has the rules). */
public class WardCoreBlock extends BaseEntityBlock {
    public static final EnumProperty<WardState> WARD = EnumProperty.create("ward", WardState.class);
    public static final MapCodec<WardCoreBlock> CODEC = simpleCodec(WardCoreBlock::new);

    public WardCoreBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(WARD, WardState.DORMANT));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WARD);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WardCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LabRegistry.WARD_CORE_BE.get(),
                level.isClientSide() ? WardCoreBlockEntity::clientTick : WardCoreBlockEntity::serverTick);
    }
}
