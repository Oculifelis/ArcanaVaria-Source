package com.arcanavaria.thurible.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.thurible.ThuribleItem;
import com.arcanavaria.thurible.ThuribleRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** The cloud draws itself with particles; the Thurible swings like a censer in first person while use is held. */
@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class ThuribleClient {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ThuribleRegistry.CLOUD.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
                                                   float equip, float swing) {
                if (!player.isUsingItem() || player.getUseItem() != stack) {
                    return false;
                }
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                float t = player.getTicksUsingItem() + partialTick;
                // one there-and-back every two clouds, easing in over the first half second
                float phase = (float) Math.sin(t * Math.PI / ThuribleItem.SWING);
                float ease = Mth.clamp(t / 10.0F, 0, 1);
                pose.translate(side * 0.56F, -0.52F, -0.72F);
                pose.translate(0, 0.25F, 0);                  // pivot at the hand: the censer hangs below it
                pose.mulPose(Axis.ZP.rotationDegrees(phase * 32.0F * ease * side));
                pose.mulPose(Axis.XP.rotationDegrees(-12.0F * ease + (float) Math.cos(t * Math.PI / ThuribleItem.SWING) * 8.0F * ease));
                pose.translate(0, -0.25F, 0);
                return true;
            }
        }, ThuribleRegistry.THURIBLE.get());
    }

    private ThuribleClient() {
    }
}
