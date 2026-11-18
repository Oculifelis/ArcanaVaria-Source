package com.arcanavaria.lab;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The light-beam puzzle: an emitter throws a beam level along its face up to 32 blocks; standing mirrors turn it a
 * quarter (right-click one to flip it between / and \); it stops at anything solid. A receptor it reaches lights and
 * gives a redstone signal. The beam is worked out the same way on both sides, so the client draws it with no packets.
 */
public final class BeamBlocks {
    public static final int RANGE = 32;
    private static final DustParticleOptions BEAM = new DustParticleOptions(0xFFE9A8, 0.7F);

    private BeamBlocks() {
    }

    /** The points the beam passes, from the emitter's face to where it ends; reports the receptor it hit. */
    public static List<Vec3> trace(Level level, BlockPos from, Direction dir, @Nullable BlockPos[] hit) {
        List<Vec3> path = new ArrayList<>();
        path.add(from.getCenter());
        BlockPos at = from;
        for (int i = 0; i < RANGE; i++) {
            at = at.relative(dir);
            BlockState s = level.getBlockState(at);
            if (s.getBlock() instanceof Mirror) {
                path.add(at.getCenter());
                dir = Mirror.turn(dir, s.getValue(Mirror.FLIPPED));
                continue;
            }
            if (s.getBlock() instanceof Receptor) {
                path.add(at.getCenter());
                if (hit != null) {
                    hit[0] = at;
                }
                return path;
            }
            if (s.canOcclude() || !s.getCollisionShape(level, at).isEmpty()) {
                path.add(at.getCenter().relative(dir.getOpposite(), 0.5));
                return path;
            }
        }
        path.add(at.getCenter());
        return path;
    }

    // ---- emitter ----------------------------------------------------------------------------------------------------

    public static class Emitter extends BaseEntityBlock {
        public static final MapCodec<Emitter> CODEC = simpleCodec(Emitter::new);

        public Emitter(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStateProperties.HORIZONTAL_FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new EmitterTick(pos, state);
        }

        @Override
        public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return createTickerHelper(type, LabRegistry.BEAM_EMITTER_BE.get(), Emitter::tick);
        }

        private static void tick(Level level, BlockPos pos, BlockState state, EmitterTick be) {
            if (level.getGameTime() % 4 != 0) {
                return;
            }
            Direction dir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos[] hit = new BlockPos[1];
            List<Vec3> path = trace(level, pos, dir, hit);
            if (!level.isClientSide()) {
                if (hit[0] != null && level.getBlockEntity(hit[0]) instanceof ReceptorTick r) {
                    r.hit(level);
                }
                return;
            }
            var rnd = level.getRandom();
            for (int i = 1; i < path.size(); i++) {
                Vec3 a = path.get(i - 1), b = path.get(i);
                int n = (int) Math.ceil(a.distanceTo(b) * 2);
                for (int k = 0; k < n; k++) {
                    Vec3 p = a.lerp(b, (k + rnd.nextFloat()) / n);
                    level.addParticle(BEAM, p.x, p.y, p.z, 0, 0, 0);
                }
            }
        }
    }

    /** Only here so the emitter ticks; it keeps nothing. */
    public static class EmitterTick extends BlockEntity {
        public EmitterTick(BlockPos pos, BlockState state) {
            super(LabRegistry.BEAM_EMITTER_BE.get(), pos, state);
        }
    }

    // ---- mirror -----------------------------------------------------------------------------------------------------

    public static class Mirror extends Block {
        /** false: a / mirror (from the south-west corner to the north-east); true: a \ mirror. */
        public static final BooleanProperty FLIPPED = BooleanProperty.create("flipped");
        public static final MapCodec<Mirror> CODEC = simpleCodec(Mirror::new);
        private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

        public Mirror(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(FLIPPED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FLIPPED);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        static Direction turn(Direction d, boolean flipped) {
            if (d.getAxis().isVertical()) {
                return d;
            }
            // '/': east <-> north, west <-> south; '\': east <-> south, west <-> north
            return switch (d) {
                case EAST -> flipped ? Direction.SOUTH : Direction.NORTH;
                case NORTH -> flipped ? Direction.WEST : Direction.EAST;
                case WEST -> flipped ? Direction.NORTH : Direction.SOUTH;
                default -> flipped ? Direction.EAST : Direction.WEST;
            };
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.cycle(FLIPPED), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5F, 1.6F);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // ---- receptor ---------------------------------------------------------------------------------------------------

    public static class Receptor extends BaseEntityBlock {
        public static final BooleanProperty LIT = BlockStateProperties.LIT;
        public static final MapCodec<Receptor> CODEC = simpleCodec(Receptor::new);

        public Receptor(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(LIT, false));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        @Override
        protected boolean isSignalSource(BlockState state) {
            return true;
        }

        @Override
        protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return state.getValue(LIT) ? 15 : 0;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ReceptorTick(pos, state);
        }

        @Override
        public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide() ? null : createTickerHelper(type, LabRegistry.BEAM_RECEPTOR_BE.get(), Receptor::tick);
        }

        private static void tick(Level level, BlockPos pos, BlockState state, ReceptorTick be) {
            boolean lit = level.getGameTime() - be.lastHit <= 10;
            if (lit != state.getValue(LIT)) {
                level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
                if (lit && level instanceof ServerLevel server) {
                    server.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.5F);
                }
            }
        }
    }

    /** Remembers when a beam last reached it. */
    public static class ReceptorTick extends BlockEntity {
        long lastHit = -100;

        public ReceptorTick(BlockPos pos, BlockState state) {
            super(LabRegistry.BEAM_RECEPTOR_BE.get(), pos, state);
        }

        void hit(Level level) {
            lastHit = level.getGameTime();
        }
    }
}
