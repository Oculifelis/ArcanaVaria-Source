package com.arcanavaria.client;

import com.arcanavaria.AVRegistry;
import com.arcanavaria.ArcanaVaria;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.ClientHooks;
import org.lwjgl.glfw.GLFW;

/** The input and tooltip checks that require a client, plus recipe and tag verification for the KubeJS smoke script. */
final class ScytheClientAutotest {
    private ScytheClientAutotest() {}

    static void check(Minecraft mc) {
        var player = mc.player;
        long window = mc.getWindow().handle();
        require(GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == 0
                && mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).get() == 0.0,
                "client stayed unfocused and muted");
        var stack = new ItemStack(AVRegistry.VOID_SCYTHE.get());
        var tooltip = stack.getTooltipLines(Item.TooltipContext.of(mc.level, player), player, TooltipFlag.NORMAL)
                .stream().map(Component::getString).toList();
        require(tooltip.contains("Sweet Spot") && tooltip.contains("Reap Pull (use)") && tooltip.contains("Harvest Souls")
                && tooltip.stream().filter(line -> line.contains("Attack Damage")).count() == 1,
                "Void tooltip lists all moves and one attack damage line");

        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        require(ClientHooks.onClickInput(GLFW.GLFW_MOUSE_BUTTON_RIGHT, mc.options.keyUse, InteractionHand.MAIN_HAND).isCanceled(),
                "ordinary Void use belongs to Reap Pull");
        stack.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(AVRegistry.REAPERS_STEP), 1);
        require(!ClientHooks.onClickInput(GLFW.GLFW_MOUSE_BUTTON_RIGHT, mc.options.keyUse, InteractionHand.MAIN_HAND).isCanceled(),
                "enchanted Void use reaches vanilla Reaper's Step");
    }

    static void checkKubeJs(ServerPlayer player) {
        if (!ModList.get().isLoaded("kubejs")) {
            ArcanaVaria.LOG.info("AVTEST KUBEJS skipped");
            return;
        }
        var recipes = player.level().getServer().getRecipeManager();
        boolean added = recipes.byKey(ResourceKey.create(Registries.RECIPE, ArcanaVaria.id("scythe_qa"))).isPresent();
        boolean removed = recipes.byKey(ResourceKey.create(Registries.RECIPE, ArcanaVaria.id("thaumium_scythe"))).isEmpty();
        boolean tagged = new ItemStack(AVRegistry.EXANIMATORS_EDGE.get())
                .is(TagKey.create(Registries.ITEM, ArcanaVaria.id("scythe_qa")));
        require(added && removed && tagged, "KUBEJS added and removed AV recipes and tagged the Edge");
    }

    private static void require(boolean ok, String description) {
        if (!ok) {
            throw new IllegalStateException(description);
        }
        ArcanaVaria.LOG.info("AVTEST PASS {}", description);
    }
}
