package com.arcanavaria.lens.client;

import com.arcanavaria.lens.ArchitectsLensItem;
import com.arcanavaria.lens.LensMode;
import com.arcanavaria.lens.LensRegistry;
import com.arcanavaria.lens.LensSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class LensSettingsScreen extends Screen {
    private LensMode mode;
    private int range, turns, widthBlocks, heightBlocks, depth;
    private boolean exchange, fuzzy, connected, onTop, mirror;
    private int materialPage;

    public LensSettingsScreen(ItemStack lens) {
        super(Component.translatable("screen.arcanavaria.lens.settings"));
        mode = ArchitectsLensItem.state(lens).mode();
        LensSettings settings = lens.getOrDefault(LensRegistry.SETTINGS.get(), LensSettings.DEFAULT);
        range = settings.range();
        turns = settings.turns();
        widthBlocks = settings.width();
        heightBlocks = settings.height();
        depth = settings.depth();
        exchange = settings.exchange();
        fuzzy = settings.fuzzy();
        connected = settings.connected();
        onTop = settings.onTop();
        mirror = settings.mirror();
    }

    @Override
    protected void init() {
        int x = width / 2 - 142;
        int y = Math.max(24, height / 2 - 76);
        option(x, y, "mode", Component.translatable("lens.arcanavaria.mode." + mode.getSerializedName()), () -> mode = mode.step(1));
        option(x + 144, y, "range", range, () -> range = next(range));
        option(x, y + 22, "exchange", exchange, () -> exchange = !exchange);
        option(x + 144, y + 22, "fuzzy", fuzzy, () -> fuzzy = !fuzzy);
        option(x, y + 44, "connected", connected, () -> connected = !connected);
        option(x + 144, y + 44, "on_top", onTop, () -> onTop = !onTop);
        option(x, y + 66, "rotation", turns * 90, () -> turns = (turns + 1) % 4);
        option(x + 144, y + 66, "mirror", mirror, () -> mirror = !mirror);
        option(x, y + 88, "width", widthBlocks, () -> widthBlocks = next(widthBlocks));
        option(x + 144, y + 88, "height", heightBlocks, () -> heightBlocks = next(heightBlocks));
        option(x, y + 110, "depth", depth, () -> depth = next(depth));
        ItemStack scroll = getMinecraft().player.getOffhandItem();
        if (scroll.get(LensRegistry.BLUEPRINT.get()) == null) {
            scroll = getMinecraft().player.getMainHandItem();
        }
        var blueprint = scroll.get(LensRegistry.BLUEPRINT.get());
        int pages = blueprint == null ? 1 : Math.max(1, (blueprint.materials().size() + 31) / 32);
        materialPage %= pages;
        addRenderableWidget(Button.builder(Component.translatable("screen.arcanavaria.lens.materials", materialPage + 1, pages), button -> {
            ClientPacketDistributor.sendToServer(new LensRegistry.ActionPayload(materialPage + 1));
            materialPage = (materialPage + 1) % pages;
            button.setMessage(Component.translatable("screen.arcanavaria.lens.materials", materialPage + 1, pages));
        }).bounds(x + 144, y + 110, 140, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 70, y + 138, 140, 20).build());
    }

    private static int next(int value) {
        return value >= 15 ? 1 : Math.min(15, value + 2);
    }

    private void option(int x, int y, String key, Object value, Runnable change) {
        Component display = value instanceof Boolean toggle
                ? Component.translatable(toggle ? "options.on" : "options.off")
                : value instanceof Component component ? component : Component.literal(value.toString());
        addRenderableWidget(Button.builder(Component.translatable("screen.arcanavaria.lens." + key, display), button -> {
            change.run();
            rebuildWidgets();
        }).bounds(x, y, 140, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0131A25);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFDEA0);
        super.extractRenderState(graphics, x, y, partialTick);
    }

    @Override
    public void onClose() {
        ClientPacketDistributor.sendToServer(new LensRegistry.SettingsPayload(mode,
                new LensSettings(range, exchange, fuzzy, connected, onTop, turns, mirror, widthBlocks, heightBlocks, depth)));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
