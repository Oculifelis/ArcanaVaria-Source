package com.arcanavaria.lens.client;

import com.arcanavaria.ArcanaVaria;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class LensKeys {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ArcanaVaria.id("lens"));
    public static final KeyMapping MENU = new KeyMapping("key.arcanavaria.lens_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private LensKeys() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(MENU);
    }
}
