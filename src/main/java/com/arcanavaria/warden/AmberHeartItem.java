package com.arcanavaria.warden;

import com.arcanavaria.ArcanaVaria;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The Warden's living amber heart. Using it gives +2 hearts of max health for good, once per player: the flag is a
 * player attachment copied through death, and the modifier is re-applied on login and respawn.
 */
@EventBusSubscriber(modid = ArcanaVaria.MODID)
public class AmberHeartItem extends Item {
    private static final AttributeModifier BONUS = new AttributeModifier(ArcanaVaria.id("warden_amber_heart"), 4.0,
        AttributeModifier.Operation.ADD_VALUE);

    public AmberHeartItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.getData(WardenRegistry.AMBER_HEART_USED)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.arcanavaria.warden_amber_heart.used").withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer sp) {
            sp.setData(WardenRegistry.AMBER_HEART_USED, true);
            apply(sp);
            sp.heal(4.0F);
            level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, sp.getSoundSource(), 1.0F, 0.7F);
            player.getItemInHand(hand).consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    static void apply(Player player) {
        AttributeInstance max = player.getAttribute(Attributes.MAX_HEALTH);
        if (max != null && player.getData(WardenRegistry.AMBER_HEART_USED)) {
            max.addOrUpdateTransientModifier(BONUS);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply(event.getEntity());
    }
}
