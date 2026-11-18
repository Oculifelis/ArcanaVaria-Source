package com.arcanavaria.lens.client;

import com.arcanavaria.lens.ArchitectsLensItem;
import com.arcanavaria.lens.LensMode;
import com.arcanavaria.lens.LensRegistry;
import com.arcanavaria.lens.LensSettings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Hold the menu key, point at an action and release. Settings use the native button controls. */
public final class LensWheelScreen extends Screen {
    private final LensMode[] modes;
    private final LensSettings settings;
    private int selected = -1;
    private int age;

    public LensWheelScreen(ItemStack lens) {
        super(Component.translatable("screen.arcanavaria.lens"));
        LensMode current = ArchitectsLensItem.state(lens).mode();
        modes = new LensMode[] {current.gadgetShape() ? current : LensMode.SURFACE, LensMode.COPY, LensMode.PASTE,
                LensMode.DESTROY, LensMode.MIRROR_X, LensMode.RADIAL_4, LensMode.REPLACE, LensMode.BIND};
        settings = lens.getOrDefault(LensRegistry.SETTINGS.get(), LensSettings.DEFAULT);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.arcanavaria.lens.settings"), button -> {
            ItemStack lens = LensRegistry.held(getMinecraft().player);
            if (lens != null) {
                getMinecraft().setScreen(new LensSettingsScreen(lens));
            }
        }).bounds(width / 2 - 45, height - 26, 90, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x90131A25);
        int centerX = width / 2;
        int centerY = height / 2 - 5;
        int radius = Math.min(70, (height - 60) / 2);
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);
        selected = distance > 18 && distance < radius + 35
                ? Math.floorMod((int) Math.round((Math.atan2(dy, dx) + Math.PI / 2) * 4 / Math.PI), 8) : -1;
        for (int index = 0; index < modes.length; index++) {
            double angle = index * Math.PI / 4 - Math.PI / 2;
            int x = centerX + (int) (Math.cos(angle) * radius);
            int y = centerY + (int) (Math.sin(angle) * radius);
            Component label = Component.translatable("lens.arcanavaria.wheel." + index);
            int halfWidth = font.width(label) / 2 + 5;
            graphics.fill(x - halfWidth, y - 9, x + halfWidth, y + 9, index == selected ? 0xFF775D30 : 0xEE242F40);
            graphics.centeredText(font, label, x, y - 4, index == selected ? 0xFFFFDEA0 : 0xFFE6EAF1);
        }
        graphics.centeredText(font, title, centerX, 8, 0xFFFFDEA0);
        if (selected >= 0) {
            graphics.centeredText(font, Component.translatable("lens.arcanavaria.mode." + modes[selected].getSerializedName()),
                    centerX, centerY - 4, 0xFFFFDEA0);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        if (++age > 1 && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), LensKeys.MENU.getKey().getValue())) {
            if (selected >= 0) {
                ClientPacketDistributor.sendToServer(new LensRegistry.SettingsPayload(modes[selected], settings));
            }
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
