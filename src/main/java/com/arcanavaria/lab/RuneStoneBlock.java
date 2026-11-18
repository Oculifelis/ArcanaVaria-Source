package com.arcanavaria.lab;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A rune stone: one of eight glyphs (the structure picks it). Pressed, it lights for a moment and tells every rune lock
 * within 12 blocks; the lock decides if it was the right rune next. A creative player sneaking changes the glyph.
 */
public class RuneStoneBlock extends Block {
    public static final IntegerProperty RUNE = IntegerProperty.create("rune", 0, 7);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final MapCodec<RuneStoneBlock> CODEC = simpleCodec(RuneStoneBlock::new);
    public static final int REACH = 12;

    public RuneStoneBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(RUNE, 0).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUNE, LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isCreative() && player.isShiftKeyDown()) {
            server.setBlock(pos, state.cycle(RUNE), Block.UPDATE_ALL);
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(LIT)) {
            return InteractionResult.SUCCESS;
        }
        server.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, 24);
        server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F + state.getValue(RUNE) * 0.12F);
        for (RuneLockBlockEntity lock : LabScan.near(server, pos, REACH, RuneLockBlockEntity.class)) {
            lock.press(server, state.getValue(RUNE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (state.getValue(LIT) || r.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + r.nextFloat(), pos.getY() + 1.1, pos.getZ() + r.nextFloat(), 0, 0.3, 0);
        }
    }
}
