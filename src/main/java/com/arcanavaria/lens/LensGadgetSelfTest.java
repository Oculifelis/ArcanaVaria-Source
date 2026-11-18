package com.arcanavaria.lens;

import com.arcanavaria.ArcanaVaria;
import com.google.gson.JsonParser;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Exercises the adapted BG2 collectors and the Lens's normal block-use entry point. */
public final class LensGadgetSelfTest {
    private static final BlockPos START = new BlockPos(40, 210, 40);

    private LensGadgetSelfTest() {}

    public static void run(ServerLevel level) {
        for (int x = 1; x <= 4; x++) {
            for (int z = 1; z <= 4; z++) {
                level.getChunk(x, z);
            }
        }
        shapes(level);
        templates();
        exchange(level);
        destruction(level);
        supply(level);
        protectionAndCosts(level);
        codecs();
        ArcanaVaria.LOG.info("LENSGADGET COMPLETE");
    }

    private static void check(boolean ok, String description) {
        if (!ok) {
            throw new IllegalStateException("LENSGADGET FAIL " + description);
        }
        ArcanaVaria.LOG.info("LENSGADGET PASS {}", description);
    }

    private static FakePlayer player(ServerLevel level, boolean creative) {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "LensGadgetTest"));
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        player.snapTo(40.5, 211, 43.5, 180, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LensRegistry.ARCHITECTS_LENS.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS));
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("thaumaturge:wand")));
        wand = WandAccess.withVis(wand, TTAspects.ORDO, 10000);
        player.getInventory().setItem(8, WandAccess.withVis(wand, TTAspects.PERDITIO, 10000));
        return player;
    }

    private static void click(ServerPlayer player, BlockPos at, Direction face) {
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), face, at, false)));
    }

    private static void mode(ServerPlayer player, LensMode mode, LensSettings settings) {
        ArchitectsLensItem.setMode(player.getMainHandItem(), mode);
        player.getMainHandItem().set(LensRegistry.SETTINGS.get(), settings);
    }

    private static void floor(ServerLevel level) {
        for (BlockPos at : BlockPos.betweenClosed(START.offset(-10, 0, -10), START.offset(10, 8, 10))) {
            level.setBlockAndUpdate(at, at.getY() == START.getY() ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }

    private static void shapes(ServerLevel level) {
        FakePlayer player = player(level, true);
        for (LensMode mode : LensMode.values()) {
            if (!mode.gadgetShape()) {
                continue;
            }
            floor(level);
            mode(player, mode, LensSettings.DEFAULT);
            var positions = LensShapes.collect(player, mode, LensSettings.DEFAULT, START, Direction.UP, Blocks.STONE_BRICKS.defaultBlockState());
            check(!positions.isEmpty() && positions.size() <= LensGeometry.MAX, mode + " has bounded build positions");
            click(player, START, Direction.UP);
            check(positions.stream().allMatch(entry -> level.getBlockState(entry.pos()).is(Blocks.STONE_BRICKS)),
                    mode + " click matches its preview positions");
            check(LensBuilder.undo(level, player) == positions.size()
                    && positions.stream().allMatch(entry -> level.isEmptyBlock(entry.pos())), mode + " undo restores the area");
        }
        floor(level);
        mode(player, LensMode.SURFACE, LensSettings.DEFAULT);
        var surface = LensShapes.collect(player, LensMode.SURFACE, LensSettings.DEFAULT, START, Direction.UP, Blocks.STONE_BRICKS.defaultBlockState());
        check(surface.size() == 25 && surface.stream().allMatch(entry -> entry.pos().getY() == START.getY() + 1),
                "BG2 surface is a supported five-by-five plane on the clicked face");
        mode(player, LensMode.COLUMN, new LensSettings(15, false, false, false, true, 0, false, 3, 3, 3));
        check(LensShapes.collect(player, LensMode.COLUMN, LensBuilder.settings(player), START, Direction.UP,
                Blocks.STONE_BRICKS.defaultBlockState()).size() == 15, "column honors the configured range");
    }

    private static void templates() {
        var stair = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        BlueprintData data = new BlueprintData("old scroll", List.of(new BlueprintData.Entry(BlockPos.ZERO, stair),
                new BlueprintData.Entry(new BlockPos(2, 0, 1), Blocks.STONE_BRICKS.defaultBlockState())));
        BlueprintData turned = data.transformed(Rotation.CLOCKWISE_90, Mirror.NONE);
        check(turned.blocks().get(0).at().equals(new BlockPos(1, 0, 0))
                && turned.blocks().get(1).at().equals(new BlockPos(0, 0, 2))
                && turned.blocks().get(0).state().getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
                "BG2 rotation turns template coordinates and facing together");
        BlueprintData fourTurns = data;
        for (int i = 0; i < 4; i++) {
            fourTurns = fourTurns.transformed(Rotation.CLOCKWISE_90, Mirror.NONE);
        }
        check(fourTurns.equals(data), "four quarter turns restore an existing scroll");
        check(data.transformed(Rotation.NONE, Mirror.FRONT_BACK).transformed(Rotation.NONE, Mirror.FRONT_BACK).equals(data),
                "two mirrors restore an existing scroll");
        check(data.materials().get(Items.OAK_STAIRS) == 1 && data.materials().get(Items.STONE_BRICKS) == 1,
                "template material list counts each block item");
    }

    private static void exchange(ServerLevel level) {
        floor(level);
        FakePlayer player = player(level, false);
        LensSettings settings = new LensSettings(1, true, false, true, true, 0, false, 1, 1, 1);
        mode(player, LensMode.SURFACE, settings);
        click(player, START, Direction.UP);
        check(level.getBlockState(START).is(Blocks.STONE_BRICKS)
                && WandAccess.getVis(player.getInventory().getItem(8), TTAspects.ORDO) == 9995
                && WandAccess.getVis(player.getInventory().getItem(8), TTAspects.PERDITIO) == 9995,
                "exchange charges both Ordo and Perditio");
        check(player.getInventory().countItem(Items.OAK_PLANKS) == 1, "exchange returns the replaced block");
        check(LensBuilder.undo(level, player) == 1 && level.getBlockState(START).is(Blocks.OAK_PLANKS)
                && player.getInventory().countItem(Items.OAK_PLANKS) == 0
                && player.getInventory().countItem(Items.STONE_BRICKS) == 1, "exchange undo balances both item types");

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS));
        click(player, START, Direction.UP);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(Items.OAK_PLANKS)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        check(LensBuilder.undo(level, player) == 0 && level.getBlockState(START).is(Blocks.STONE_BRICKS),
                "exchange undo waits when the returned original block was spent");
        player.getInventory().add(new ItemStack(Items.OAK_PLANKS));
        check(LensBuilder.undo(level, player) == 1 && level.getBlockState(START).is(Blocks.OAK_PLANKS),
                "exchange undo can retry after the original material is returned");

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS));
        level.setBlockAndUpdate(START, Blocks.BEDROCK.defaultBlockState());
        click(player, START, Direction.UP);
        check(level.getBlockState(START).is(Blocks.BEDROCK), "exchange preserves unbreakable blocks");
        level.setBlockAndUpdate(START, Blocks.OAK_PLANKS.defaultBlockState());
        player.setGameMode(GameType.CREATIVE);
        level.setBlockAndUpdate(START.east(), Blocks.DIRT.defaultBlockState());
        mode(player, LensMode.SURFACE, new LensSettings(3, true, false, false, true, 0, false, 1, 1, 1));
        click(player, START, Direction.UP);
        check(level.getBlockState(START.east()).is(Blocks.DIRT), "non-fuzzy exchange keeps different block states");
        LensBuilder.undo(level, player);
        mode(player, LensMode.SURFACE, new LensSettings(3, true, true, false, true, 0, false, 1, 1, 1));
        click(player, START, Direction.UP);
        check(level.getBlockState(START.east()).is(Blocks.STONE_BRICKS), "fuzzy exchange includes different block states");
        LensBuilder.undo(level, player);
    }

    private static void destruction(ServerLevel level) {
        floor(level);
        FakePlayer player = player(level, false);
        mode(player, LensMode.DESTROY, new LensSettings(1, false, false, false, true, 0, false, 1, 1, 2));
        click(player, START, Direction.NORTH);
        check(level.isEmptyBlock(START) && level.isEmptyBlock(START.south())
                && WandAccess.getVis(player.getInventory().getItem(8), TTAspects.PERDITIO) == 9990
                && WandAccess.getVis(player.getInventory().getItem(8), TTAspects.ORDO) == 10000,
                "destruction honors facing and depth and charges only Perditio");
        check(player.getInventory().countItem(Items.OAK_PLANKS) == 0 && LensBuilder.undo(level, player) == 2
                && level.getBlockState(START).is(Blocks.OAK_PLANKS) && level.getBlockState(START.south()).is(Blocks.OAK_PLANKS),
                "destruction voids items and undo restores both recorded states");
        level.setBlockAndUpdate(START, Blocks.CHEST.defaultBlockState());
        click(player, START, Direction.NORTH);
        check(level.getBlockState(START).is(Blocks.CHEST), "destruction preserves block entities");
    }

    private static void supply(ServerLevel level) {
        floor(level);
        BlockPos chestPos = START.offset(0, 1, 5);
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.STONE_BRICKS, 10));
        FakePlayer player = player(level, false);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getMainHandItem().set(LensRegistry.SELECTED.get(), Blocks.STONE_BRICKS.defaultBlockState());
        mode(player, LensMode.BIND, LensSettings.DEFAULT);
        click(player, chestPos, Direction.UP);
        check(player.getMainHandItem().has(LensRegistry.SUPPLY.get()) && LensInventory.bound(player) != null,
                "bind mode links the real chest capability");
        mode(player, LensMode.COLUMN, new LensSettings(3, false, false, false, true, 0, false, 1, 1, 1));
        click(player, START, Direction.UP);
        check(chest.getItem(0).getCount() == 7 && level.getBlockState(START.above(3)).is(Blocks.STONE_BRICKS),
                "building draws three items transactionally from the linked chest");
        check(LensBuilder.undo(level, player) == 3 && chest.getItem(0).getCount()
                + player.getInventory().countItem(Items.STONE_BRICKS) == 10, "linked-supply undo conserves total items");
        player.getMainHandItem().set(LensRegistry.SUPPLY.get(), new LensSupply(GlobalPos.of(Level.NETHER, chestPos), Direction.UP));
        check(LensInventory.bound(player) == null, "a cross-dimension supply link cannot extract items");
        player.getMainHandItem().set(LensRegistry.SUPPLY.get(), new LensSupply(GlobalPos.of(level.dimension(), chestPos.offset(1000, 0, 0)), Direction.UP));
        check(LensInventory.bound(player) == null, "distant supply links do not load chunks");
        level.setBlockAndUpdate(chestPos, Blocks.AIR.defaultBlockState());
    }

    private static void codecs() {
        LensState old = LensState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"mode\":\"fill\",\"a\":[1,2,3],\"b\":[4,5,6]}"))
                .getOrThrow();
        check(old.mode() == LensMode.FILL && old.ready(), "old saved Lens selections still decode");
        LensSettings defaults = LensSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        check(defaults.equals(LensSettings.DEFAULT), "old Lenses receive default gadget settings");
        check(LensSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"range\":999}")).error().isPresent(),
                "oversized settings fail packet and item decoding");
    }

    private static void protectionAndCosts(ServerLevel level) {
        floor(level);
        FakePlayer player = player(level, false);
        mode(player, LensMode.COLUMN, new LensSettings(11, false, false, false, true, 0, false, 1, 1, 1));
        click(player, START, Direction.UP);
        check(level.getBlockState(START.above()).is(Blocks.STONE_BRICKS) && level.isEmptyBlock(START.above(2))
                && WandAccess.getVis(player.getInventory().getItem(8), TTAspects.ORDO) == 9985,
                "range-scaled payment charges only the one successful placement");
        check(LensBuilder.undo(level, player) == 1, "a partial gadget build remains one undo operation");

        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LensRegistry.ARCHITECTS_LENS.get()));
        player.setGameMode(GameType.CREATIVE);
        var waterlogged = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        check(!com.arcanavaria.lens.bg2.BlockStates.cleanBlockState(waterlogged).getValue(BlockStateProperties.WATERLOGGED),
                "BG2 state cleaning removes free water from copied blocks");
        player.setGameMode(GameType.SURVIVAL);
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("thaumaturge:wand")));
        player.getInventory().setItem(8, WandAccess.withVis(wand, TTAspects.ORDO, 10000));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_SLAB, 2));
        var doubleSlab = Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
        LensBuilder.begin(player);
        LensBuilder.place(level, player, START.above(), doubleSlab, false);
        check(level.getBlockState(START.above()) == doubleSlab && player.getInventory().countItem(Items.STONE_SLAB) == 0,
                "a double slab consumes two slab items");
        check(LensBuilder.undo(level, player) == 1 && player.getInventory().countItem(Items.STONE_SLAB) == 2,
                "double slab undo returns exactly two slab items");
        check(new BlueprintData("slabs", List.of(new BlueprintData.Entry(BlockPos.ZERO, doubleSlab)))
                .materials().get(Items.STONE_SLAB) == 2, "the material list counts both halves of a double slab");

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS));
        mode(player, LensMode.COLUMN, new LensSettings(1, false, false, false, true, 0, false, 1, 1, 1));
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> veto = event -> {
            if (event.getPos().equals(START.above())) {
                event.setCanceled(true);
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent.class, veto);
        int vis = WandAccess.getVis(player.getInventory().getItem(8), TTAspects.ORDO);
        int items = player.getInventory().countItem(Items.STONE_BRICKS);
        try {
            click(player, START, Direction.UP);
            check(level.isEmptyBlock(START.above()) && player.getInventory().countItem(Items.STONE_BRICKS) == items
                    && WandAccess.getVis(player.getInventory().getItem(8), TTAspects.ORDO) == vis,
                    "a protection event cancels placement without item or vis costs");
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(veto);
        }
    }
}
