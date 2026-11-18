package com.arcanavaria.client;

import com.arcanavaria.lens.ArchitectsLensItem;
import com.arcanavaria.lens.BlueprintData;
import com.arcanavaria.lens.LensMode;
import com.arcanavaria.lens.LensRegistry;
import com.arcanavaria.lens.LensState;
import com.arcanavaria.lens.LensUndoSelfTest;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * The Lens stage of the client test, driven on the server: fill a box and undo it, fill it again, place one block by
 * hand under a radial-4 anchor (the copies come from the real placing event), copy the box into a scroll and paste it
 * beside, then leave a fill selection unbuilt so the client shows its ghost for the shot.
 */
final class LensAutotest {
    private static final int Y = -60;

    private LensAutotest() {
    }

    static void run(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        LensUndoSelfTest.run(level);
        com.arcanavaria.lens.LensGadgetSelfTest.run(level);
        var server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=!minecraft:player]");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "fill -8 -60 2 8 -50 20 minecraft:air");
        p.getInventory().clearContent();
        // the full run is in survival by now (the Warden stage), so the Lens draws real Ordo vis from a charged wand
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                "item replace entity @a hotbar.8 with thaumaturge:wand[thaumaturge:wand_parts={cap:\"thaumaturge:thaumium\",rod:\"thaumaturge:greatwood\"}]");
        ItemStack wand = p.getInventory().getItem(8);
        p.getInventory().setItem(8, WandAccess.withVis(wand, TTAspects.ORDO, 10000));
        log.info("AVTEST lens wand={} ordo={} creative={}", wand.getItem(), WandAccess.getVis(p.getInventory().getItem(8), TTAspects.ORDO), p.isCreative());
        ItemStack lens = new ItemStack(LensRegistry.ARCHITECTS_LENS.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, lens);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS, 64));

        // fill, undo, fill again
        BlockPos a = new BlockPos(-5, Y, 6), b = new BlockPos(-3, Y + 2, 8);
        set(lens, LensMode.FILL, a, b);
        lens.getItem().use(level, p, InteractionHand.MAIN_HAND);
        int filled = count(level, a, b);
        p.setShiftKeyDown(true);
        lens.getItem().use(level, p, InteractionHand.MAIN_HAND);
        p.setShiftKeyDown(false);
        int afterUndo = count(level, a, b);
        lens.getItem().use(level, p, InteractionHand.MAIN_HAND);
        log.info("AVTEST lens fill={} afterUndo={} refill={} (want 27, 0, 27)", filled, afterUndo, count(level, a, b));

        // radial 4 about an anchor: place one block by hand with the Lens in the other hand
        BlockPos anchor = new BlockPos(3, Y, 10);
        ItemStack lensOff = lens.copy();
        lensOff.set(LensRegistry.STATE.get(), new LensState(LensMode.RADIAL_4, Optional.of(anchor), Optional.empty(), Optional.empty()));
        p.setItemInHand(InteractionHand.OFF_HAND, lensOff);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_STAIRS, 64));
        BlockPos below = new BlockPos(5, Y - 1, 10);
        p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false)));
        int stairs = 0;
        for (BlockPos q : new BlockPos[] {new BlockPos(5, Y, 10), new BlockPos(3, Y, 12), new BlockPos(1, Y, 10), new BlockPos(3, Y, 8)}) {
            stairs += level.getBlockState(q).is(Blocks.OAK_STAIRS) ? 1 : 0;
        }
        log.info("AVTEST lens radial4 stairs={} (want 4)", stairs);

        // copy the filled box into a scroll, paste it beside
        p.setItemInHand(InteractionHand.MAIN_HAND, lens);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(LensRegistry.BLUEPRINT_SCROLL.get()));
        set(lens, LensMode.COPY, a, b);
        lens.getItem().use(level, p, InteractionHand.MAIN_HAND);
        ItemStack written = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).has(LensRegistry.BLUEPRINT.get())) {
                written = p.getInventory().getItem(i).copy();
            }
        }
        BlueprintData data = written.get(LensRegistry.BLUEPRINT.get());
        log.info("AVTEST lens copied {} blocks into {}", data == null ? -1 : data.blocks().size(), written.getHoverName().getString());
        p.setItemInHand(InteractionHand.OFF_HAND, written);
        p.getInventory().add(new ItemStack(Items.STONE_BRICKS, 64));      // a survival paste spends the blocks it lays
        BlockPos at = new BlockPos(-5, Y, 12);
        lens.set(LensRegistry.STATE.get(), new LensState(LensMode.PASTE, Optional.empty(), Optional.of(at), Optional.empty()));
        lens.getItem().use(level, p, InteractionHand.MAIN_HAND);
        log.info("AVTEST lens pasted={} (want 27)", count(level, at, at.offset(2, 2, 2)));

        // a selection left for the ghost: a wall, shown in the shot but not built
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS, 64));
        set(lens, LensMode.WALL, new BlockPos(0, Y, 15), new BlockPos(4, Y + 3, 17));
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a -0.5 -57 1.5 10 30");
        log.info("AVTEST lens mode now {}", ArchitectsLensItem.state(lens).mode().getSerializedName());
    }

    private static void set(ItemStack lens, LensMode mode, BlockPos a, BlockPos b) {
        lens.set(LensRegistry.STATE.get(), new LensState(mode, Optional.empty(), Optional.of(a), Optional.of(b)));
    }

    private static int count(ServerLevel level, BlockPos a, BlockPos b) {
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(a, b)) {
            n += level.getBlockState(q).is(Blocks.STONE_BRICKS) ? 1 : 0;
        }
        return n;
    }
}
