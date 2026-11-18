package com.arcanavaria.lattice;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A lattice device: intake, outlet, junction, splitter prism or synchronizer, modelled (not a full block); its hitbox is
 * the model's boxes (LatticeShapes, written by tools/lattice_models.py). Intakes and outlets face the block they
 * are put against; the others face away from the player who places them (prisms only level). An empty hand sets an
 * intake's pulse or a synchronizer's delay.
 */
public class LatticeDeviceBlock extends BaseEntityBlock implements LatticePart {
    private final LatticeKind kind;
    private final MapCodec<LatticeDeviceBlock> codec;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public LatticeDeviceBlock(LatticeKind kind, Properties props) {
        super(props);
        this.kind = kind;
        this.codec = simpleCodec(p -> new LatticeDeviceBlock(kind, p));
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.NORTH));
        for (Direction d : Direction.values()) {
            shapes.put(d, shape(d));
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public LatticeKind kind() {
        return kind;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.get(state.getValue(BlockStateProperties.FACING));
    }

    /** The model's boxes (LatticeShapes, facing north) turned the way the blockstate turns the model. */
    private VoxelShape shape(Direction facing) {
        VoxelShape out = Shapes.empty();
        for (double[] b : LatticeShapes.boxes(kind)) {
            double[] p = turn(facing, b[0], b[1], b[2]), q = turn(facing, b[3], b[4], b[5]);
            out = Shapes.or(out, Block.box(Math.min(p[0], q[0]), Math.min(p[1], q[1]), Math.min(p[2], q[2]),
                    Math.max(p[0], q[0]), Math.max(p[1], q[1]), Math.max(p[2], q[2])));
        }
        return out.optimize();
    }

    private static double[] turn(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case NORTH -> new double[] {x, y, z};
            case EAST -> new double[] {16 - z, y, x};
            case SOUTH -> new double[] {16 - x, y, 16 - z};
            case WEST -> new double[] {z, y, 16 - x};
            case DOWN -> new double[] {x, z, 16 - y};
            case UP -> new double[] {x, 16 - z, y};
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = switch (kind) {
            case INTAKE, OUTLET -> ctx.getClickedFace().getOpposite();
            case PRISM -> ctx.getHorizontalDirection();
            default -> ctx.getNearestLookingDirection();
        };
        return defaultBlockState().setValue(BlockStateProperties.FACING, facing);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LatticeBlockEntity be) || kind != LatticeKind.INTAKE && kind != LatticeKind.SYNCHRONIZER) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (kind == LatticeKind.INTAKE) {
                int period = be.cyclePeriod();
                player.sendOverlayMessage(period == 0 ? Component.translatable("message.arcanavaria.intake_pulse")
                        : Component.translatable("message.arcanavaria.intake_period", period));
            } else {
                player.sendOverlayMessage(Component.translatable("message.arcanavaria.sync_delay", be.cycleDelay()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LatticeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ticker(level, type);
    }

    static <T extends BlockEntity> @Nullable BlockEntityTicker<T> ticker(Level level, BlockEntityType<T> type) {
        return createTickerHelper(type, LatticeRegistry.LATTICE_BE.get(),
                level.isClientSide() ? LatticeBlockEntity::clientTick : LatticeBlockEntity::serverTick);
    }
}
