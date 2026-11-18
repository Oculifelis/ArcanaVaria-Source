/* SPDX-License-Identifier: MIT
 * Copyright (c) 2023 Direwolf20-MC
 * Bound-handler lookup and transactional extraction adapted from Building Gadgets 2 v26.1.2.3 BuildingUtils.
 * Full notice in LICENSE-third-party.md.
 */
package com.arcanavaria.lens;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class LensInventory {
    private LensInventory() {}

    public static int quantity(BlockState state) {
        return state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE ? 2 : 1;
    }

    public static List<ItemStack> take(Player player, Item item, int amount) {
        List<ItemStack> payment = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            ItemStack stack = take(player, item);
            if (stack.isEmpty()) {
                give(player, payment);
                return List.of();
            }
            payment.add(stack);
        }
        return payment;
    }

    public static ResourceHandler<ItemResource> bound(Player player) {
        ItemStack lens = LensRegistry.held(player);
        LensSupply supply = lens == null ? null : lens.get(LensRegistry.SUPPLY.get());
        var level = player.level();
        if (supply == null || supply.location().dimension() != level.dimension()
                || !level.isLoaded(supply.location().pos())
                || supply.location().pos().distSqr(player.blockPosition()) > 64 * 64
                || !level.mayInteract(player, supply.location().pos())
                || com.arcanavaria.lab.WardEvents.locked(level, supply.location().pos(),
                        level.getBlockState(supply.location().pos()), player)) {
            return null;
        }
        return level.getCapability(Capabilities.Item.BLOCK, supply.location().pos(), supply.side());
    }

    public static ItemStack take(Player player, Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                return stack.split(1);
            }
        }
        ResourceHandler<ItemResource> handler = bound(player);
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            if (!resource.isEmpty() && resource.is(item)) {
                try (Transaction tx = Transaction.openRoot()) {
                    if (handler.extract(slot, resource, 1, tx) == 1) {
                        tx.commit();
                        return resource.toStack(1);
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static int count(Player player, Item item) {
        int count = player.getInventory().countItem(item);
        ResourceHandler<ItemResource> handler = bound(player);
        if (handler != null) {
            for (int slot = 0; slot < handler.size(); slot++) {
                if (handler.getResource(slot).is(item)) {
                    long available = Math.clamp(handler.getAmountAsLong(slot), 0L, Integer.MAX_VALUE);
                    count = (int) Math.min(Integer.MAX_VALUE, count + available);
                }
            }
        }
        return count;
    }

    public static void give(Player player, ItemStack stack) {
        ItemStack remainder = stack.copy();
        if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
            player.drop(remainder, false);
        }
    }

    public static void give(Player player, List<ItemStack> stacks) {
        stacks.forEach(stack -> give(player, stack));
    }
}
