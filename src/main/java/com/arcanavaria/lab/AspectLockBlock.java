package com.arcanavaria.lab;

import static com.arcanavaria.ArcanaVaria.MODID;

import com.arcanavaria.thurible.ThuribleContents;
import com.arcanavaria.thurible.ThuribleEffects;
import com.arcanavaria.thurible.ThuribleItem;
import com.arcanavaria.thurible.ThuribleRegistry;
import com.leclowndu93150.thaumaturge.api.aspect.AspectComponents;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * An aspect lock, set in a lab wall: its keyhole glows the colour of the aspect it wants and motes of it drift in.
 * Right-click it empty-handed to read what it wants; hold a phial or a Thurible of that aspect and use it on the lock to
 * pour; a lattice outlet or an essentia tube can feed it too.
 */
public class AspectLockBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final MapCodec<AspectLockBlock> CODEC = simpleCodec(AspectLockBlock::new);

    public AspectLockBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AspectLockBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AspectLockBlockEntity lock)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        boolean phial = stack.is(TTItems.PHIAL.get()) && stack.has(TTDataComponents.ASPECTS.get());
        boolean thurible = stack.is(ThuribleRegistry.THURIBLE.get());
        if (!phial && !thurible) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server) {
            int took = phial ? fromPhial(player, hand, stack, lock) : fromThurible(stack, lock, server);
            if (took > 0) {
                server.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.9F, 0.8F);
            }
            report(player, lock, server);
        }
        return InteractionResult.SUCCESS;
    }

    private static int fromPhial(Player player, InteractionHand hand, ItemStack stack, AspectLockBlockEntity lock) {
        AspectList list = stack.get(TTDataComponents.ASPECTS.get());
        if (list == null || list.isEmpty()) {
            return 0;
        }
        AspectInstance a = list.entries().getFirst();
        Optional<ResourceKey<IAspect>> key = a.aspect().unwrapKey();
        if (key.isEmpty() || !key.get().equals(lock.aspect()) || lock.open()) {
            return 0;
        }
        int took = lock.pour(key.get(), a.amount());
        if (!player.isCreative()) {
            ItemStack empty = new ItemStack(TTItems.PHIAL.get());
            if (stack.getCount() == 1) {
                player.setItemInHand(hand, empty);
            } else {
                stack.shrink(1);
                if (!player.getInventory().add(empty)) {
                    player.drop(empty, false);
                }
            }
        }
        return took;
    }

    private static int fromThurible(ItemStack stack, AspectLockBlockEntity lock, ServerLevel level) {
        ThuribleContents c = ThuribleItem.contents(stack);
        if (c.isEmpty() || !c.aspect().get().equals(lock.aspect())) {
            return 0;
        }
        int took = lock.pour(lock.aspect(), c.amount());
        int left = c.amount() - took;
        ThuribleItem.setContents(stack, left > 0 ? ThuribleContents.of(lock.aspect(), left) : ThuribleContents.EMPTY, level.registryAccess());
        return took;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof AspectLockBlockEntity lock && !level.isClientSide()) {
            report(player, lock, level);
        }
        return InteractionResult.SUCCESS;
    }

    private static void report(Player player, AspectLockBlockEntity lock, Level level) {
        Component name = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(lock.aspect())
                .map(AspectComponents::name).orElse(Component.literal(lock.aspect().identifier().getPath()));
        player.sendOverlayMessage(lock.open() ? Component.translatable("message.%s.aspect_lock.open".formatted(MODID))
                : Component.translatable("message.%s.aspect_lock.wants".formatted(MODID), lock.need() - lock.have(), name));
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(LIT) ? 15 : 0;
    }

    /** Motes of the wanted aspect drift into the keyhole; an open lock breathes them out instead. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!(level.getBlockEntity(pos) instanceof AspectLockBlockEntity lock)) {
            return;
        }
        Direction f = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Vec3 hole = pos.getCenter().add(f.getStepX() * 0.55, 0, f.getStepZ() * 0.55);
        DustParticleOptions dust = new DustParticleOptions(ThuribleEffects.colour(level.registryAccess(), lock.aspect()), 0.8F);
        for (int i = 0; i < 2; i++) {
            Vec3 off = new Vec3(r.nextGaussian() * 0.4 + f.getStepX() * 0.6, r.nextGaussian() * 0.4, r.nextGaussian() * 0.4 + f.getStepZ() * 0.6);
            if (lock.open()) {
                level.addParticle(dust, hole.x, hole.y, hole.z, off.x * 0.05, off.y * 0.05 + 0.02, off.z * 0.05);
            } else {
                level.addParticle(dust, hole.x + off.x, hole.y + off.y, hole.z + off.z, -off.x * 0.06, -off.y * 0.06, -off.z * 0.06);
            }
        }
    }
}
