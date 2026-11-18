package com.arcanavaria.thurible;

import static com.arcanavaria.ArcanaVaria.MODID;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** A Thurible upgrade blueprint from the Forsaken Laboratories: use it with a Thurible in the other hand to fit it. */
public class ThuribleBlueprintItem extends Item {
    public enum Kind {
        CHAIN("chain"), CLOUD("cloud"), ORBIT("orbit");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private final Kind kind;

    public ThuribleBlueprintItem(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack thurible = player.getItemInHand(other);
        if (!(thurible.getItem() instanceof ThuribleItem)) {
            player.sendOverlayMessage(Component.translatable("message.%s.blueprint.hold_thurible".formatted(MODID)));
            return InteractionResult.FAIL;
        }
        ThuribleUpgrades up = ThuribleItem.upgrades(thurible);
        if (up.has(kind)) {
            player.sendOverlayMessage(Component.translatable("message.%s.blueprint.already".formatted(MODID)));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            thurible.set(ThuribleRegistry.UPGRADES.get(), up.with(kind));
            player.getItemInHand(hand).consume(1, player);
            level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.sendOverlayMessage(Component.translatable("message.%s.blueprint.fitted".formatted(MODID),
                    Component.translatable("tooltip.%s.thurible.upgrade.%s".formatted(MODID, kind.id()))));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.%s.blueprint.%s".formatted(MODID, kind.id())).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.%s.blueprint.howto".formatted(MODID)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
