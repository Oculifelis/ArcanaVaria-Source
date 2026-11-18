package com.arcanavaria.lens;

import com.arcanavaria.ArcanaVaria;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Dev-server regression checks for the Lens's build, paste and undo entry points. */
public final class LensUndoSelfTest {
    private static final BlockPos AT = new BlockPos(8, 200, 8);

    private LensUndoSelfTest() {}

    public static void run(ServerLevel level) {
        FakePlayer player = player(level, new GameProfile(UUID.randomUUID(), "LensUndoTest"));
        level.getChunkAt(AT);
        steps(level, player);
        emptyAttempts(level, player);
        limit(level, player);
        isolation(level, player);
        survival(level, player);
        ArcanaVaria.LOG.info("LENSUNDO COMPLETE");
    }

    private static FakePlayer player(ServerLevel level, GameProfile profile) {
        FakePlayer player = new FakePlayer(level, profile);
        player.setGameMode(GameType.CREATIVE);
        player.snapTo(8, 200, 8, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LensRegistry.ARCHITECTS_LENS.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS, 64));
        return player;
    }

    private static void build(ServerLevel level, ServerPlayer player, BlockPos at) {
        player.getMainHandItem().set(LensRegistry.STATE.get(),
                new LensState(LensMode.FILL, Optional.empty(), Optional.of(at), Optional.of(at)));
        player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
    }

    private static void undo(ServerLevel level, ServerPlayer player) {
        player.setShiftKeyDown(true);
        player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
    }

    private static void check(boolean ok, String name) {
        if (!ok) {
            throw new IllegalStateException("LENSUNDO FAIL " + name);
        }
        ArcanaVaria.LOG.info("LENSUNDO PASS {}", name);
    }

    private static void steps(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < 3; i++) {
            level.setBlockAndUpdate(AT.above(i), Blocks.AIR.defaultBlockState());
            build(level, player, AT.above(i));
        }
        for (int i = 2; i >= 0; i--) {
            undo(level, player);
            check(level.isEmptyBlock(AT.above(i))
                    && (i == 0 || level.getBlockState(AT.above(i - 1)).is(Blocks.STONE_BRICKS)), "undo step " + (3 - i));
        }
        check(LensBuilder.undo(level, player) == 0, "exhausted history is empty");

        build(level, player, AT);
        build(level, player, AT.above());
        undo(level, player);
        LensBuilder.place(level, player, AT.above(2), Blocks.STONE_BRICKS.defaultBlockState(), false);
        undo(level, player);
        check(level.getBlockState(AT).is(Blocks.STONE_BRICKS) && level.isEmptyBlock(AT.above(2)),
                "direct placement after undo starts a new operation");
        undo(level, player);

        build(level, player, AT);
        level.setBlockAndUpdate(AT, Blocks.DIRT.defaultBlockState());
        check(LensBuilder.undo(level, player) == 0 && level.getBlockState(AT).is(Blocks.DIRT),
                "undo preserves a block changed after building");
        level.setBlockAndUpdate(AT, Blocks.AIR.defaultBlockState());
    }

    private static void emptyAttempts(ServerLevel level, ServerPlayer player) {
        build(level, player, AT);
        build(level, player, AT); // Already the requested state: no changes.
        build(level, player, AT);
        undo(level, player);
        check(level.isEmptyBlock(AT), "empty attempts preserve the previous operation");

        build(level, player, AT);
        player.setGameMode(GameType.SURVIVAL);
        build(level, player, AT.above()); // No wand: no blocks can be placed.
        undo(level, player);
        check(level.isEmptyBlock(AT) && level.isEmptyBlock(AT.above()), "missing vis preserves the previous operation");
        player.setGameMode(GameType.CREATIVE);
    }

    private static void limit(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i <= LensBuilder.MAX_UNDO; i++) {
            level.setBlockAndUpdate(AT.above(i), Blocks.AIR.defaultBlockState());
            build(level, player, AT.above(i));
        }
        build(level, player, AT.above(LensBuilder.MAX_UNDO)); // A no-op at the limit must not evict another step.
        int undone = 0;
        for (int i = LensBuilder.MAX_UNDO; i > 0; i--) {
            undone += LensBuilder.undo(level, player);
            check(level.isEmptyBlock(AT.above(i)), "bounded history step " + i);
        }
        check(undone == LensBuilder.MAX_UNDO && level.getBlockState(AT).is(Blocks.STONE_BRICKS)
                && LensBuilder.undo(level, player) == 0, "history keeps exactly the latest 20 successful operations");
        level.setBlockAndUpdate(AT, Blocks.AIR.defaultBlockState());
    }

    private static void isolation(ServerLevel level, ServerPlayer player) {
        build(level, player, AT);
        FakePlayer other = player(level, new GameProfile(UUID.randomUUID(), "OtherLensTest"));
        check(LensBuilder.undo(level, other) == 0 && level.getBlockState(AT).is(Blocks.STONE_BRICKS),
                "players have separate histories");

        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            throw new IllegalStateException("LENSUNDO FAIL Nether unavailable");
        }
        nether.getChunkAt(AT);
        nether.setBlockAndUpdate(AT, Blocks.AIR.defaultBlockState());
        FakePlayer traveler = player(nether, player.getGameProfile());
        check(LensBuilder.undo(nether, traveler) == 0, "overworld history cannot undo in the Nether");
        build(nether, traveler, AT);
        undo(level, player);
        check(level.isEmptyBlock(AT) && nether.getBlockState(AT).is(Blocks.STONE_BRICKS),
                "overworld undo leaves Nether builds alone");
        undo(nether, traveler);
        check(nether.isEmptyBlock(AT), "Nether retains its own undo history");
    }

    private static void survival(ServerLevel level, ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS, 1));
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("thaumaturge:wand")));
        player.getInventory().setItem(8, WandAccess.withVis(wand, TTAspects.ORDO, 100));
        player.getMainHandItem().set(LensRegistry.STATE.get(), new LensState(LensMode.FILL, Optional.empty(),
                Optional.of(AT), Optional.of(AT.above())));
        player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
        check(level.getBlockState(AT).is(Blocks.STONE_BRICKS) && level.isEmptyBlock(AT.above())
                && player.getOffhandItem().isEmpty()
                && WandAccess.getVis(player.getInventory().getItem(8), TTAspects.ORDO) == 100 - LensBuilder.COST,
                "partial survival build spends one item and the existing vis cost");
        build(level, player, AT.above()); // Wand has vis, but there are no building items left.
        undo(level, player);
        check(level.isEmptyBlock(AT) && player.getInventory().countItem(Items.STONE_BRICKS) == 1,
                "missing items keep history and undo refunds the spent block exactly once");
        undo(level, player);
        check(player.getInventory().countItem(Items.STONE_BRICKS) == 1, "repeated undo cannot refund twice");

        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS));
        build(level, player, AT);
        ItemStack scroll = new ItemStack(LensRegistry.BLUEPRINT_SCROLL.get());
        scroll.set(LensRegistry.BLUEPRINT.get(), new BlueprintData("", List.of(
                new BlueprintData.Entry(BlockPos.ZERO, Blocks.STONE_BRICKS.defaultBlockState()),
                new BlueprintData.Entry(BlockPos.ZERO.above(), Blocks.STONE_BRICKS.defaultBlockState()))));
        player.setItemInHand(InteractionHand.OFF_HAND, scroll);
        player.getMainHandItem().set(LensRegistry.STATE.get(), new LensState(LensMode.PASTE, Optional.empty(),
                Optional.of(AT.offset(1, 0, 0)), Optional.empty()));
        player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
        check(level.getBlockState(AT).is(Blocks.STONE_BRICKS)
                && level.getBlockState(AT.offset(1, 0, 0)).is(Blocks.STONE_BRICKS)
                && level.getBlockState(AT.offset(1, 1, 0)).is(Blocks.STONE_BRICKS), "build and paste both place their blocks");
        undo(level, player);
        check(level.isEmptyBlock(AT.offset(1, 0, 0)) && level.isEmptyBlock(AT.offset(1, 1, 0))
                && level.getBlockState(AT).is(Blocks.STONE_BRICKS), "paste is one undo operation");
        undo(level, player);
        check(level.isEmptyBlock(AT), "build before paste remains undoable");
    }
}
