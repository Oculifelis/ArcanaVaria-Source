package com.arcanavaria.client;

import com.arcanavaria.lens.LensMode;
import com.arcanavaria.lens.LensRegistry;
import com.arcanavaria.lens.LensSettings;
import com.arcanavaria.lens.LensState;
import com.arcanavaria.lens.client.LensClient;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * -Pautotest=lensghost: the Architect's Lens block preview in each colour. A surface build aimed at the floor, the same
 * surface as an exchange, then a destruction box aimed at a stone wall; one shot each.
 */
final class LensGhostAutotest {
    private static final int START = 100, PER = 60;

    private LensGhostAutotest() {
    }

    static boolean step(Minecraft mc, int t, Logger log, BiConsumer<Minecraft, Consumer<ServerPlayer>> onServer, BiConsumer<Minecraft, String> shot) {
        if (t == 60) {
            onServer.accept(mc, LensGhostAutotest::build);
            mc.options.hideGui = true;
        }
        if (t < START) {
            return false;
        }
        int look = (t - START) / PER, at = (t - START) % PER;
        if (look >= 3) {
            return true;
        }
        String[] names = {"build", "exchange", "destroy"};
        if (at == 0) {
            onServer.accept(mc, p -> hold(p, look));
        } else if (at == 40) {
            log.info("AVTEST lensghost {} preview={} exchange={} destroy={} hit={}", names[look], LensClient.preview().size(),
                    LensClient.exchangePreview(), LensClient.destructionPreview(), mc.hitResult == null ? null : mc.hitResult.getType());
            shot.accept(mc, "lensghost_" + names[look]);
        }
        return false;
    }

    private static void build(ServerPlayer p) {
        for (String c : new String[] {"difficulty peaceful", "time set noon", "weather clear", "gamemode creative @a",
            "kill @e[type=!minecraft:player]", "fill -8 -60 -8 8 -50 8 minecraft:air", "fill -3 -61 -1 3 -61 5 minecraft:dirt",
            "fill -3 -60 6 3 -56 6 minecraft:stone", "clear @a"}) {
            run(p, c);
        }
    }

    private static void hold(ServerPlayer p, int look) {
        ItemStack lens = new ItemStack(LensRegistry.ARCHITECTS_LENS.get());
        LensMode mode = look == 2 ? LensMode.DESTROY : LensMode.SURFACE;
        lens.set(LensRegistry.STATE.get(), new LensState(mode, Optional.empty(), Optional.empty(), Optional.empty()));
        LensSettings d = LensSettings.DEFAULT;
        lens.set(LensRegistry.SETTINGS.get(), new LensSettings(2, look == 1, d.fuzzy(), d.connected(), look != 1, d.turns(), d.mirror(),
                d.width(), d.height(), d.depth()));
        p.setItemInHand(InteractionHand.MAIN_HAND, lens);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS, 64));
        run(p, look == 2 ? "tp @a 0.5 -58.4 1.5 facing 0.5 -58 6.5" : "tp @a 0.5 -58.6 -3.5 facing 0.5 -61 2.5");
    }

    private static void run(ServerPlayer p, String c) {
        MinecraftServer server = p.level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
    }
}
