package com.arcanavaria.lens;

import static com.arcanavaria.ArcanaVaria.MODID;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A Blueprint Scroll: blank until the Lens copies a region into it; then the Lens can paste it anywhere. */
public class BlueprintScrollItem extends Item {
    public BlueprintScrollItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        BlueprintData d = stack.get(LensRegistry.BLUEPRINT.get());
        if (d == null) {
            return super.getName(stack);
        }
        return d.name().isEmpty() ? Component.translatable("item.%s.blueprint_scroll.written".formatted(MODID))
                : Component.translatable(d.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        BlueprintData d = stack.get(LensRegistry.BLUEPRINT.get());
        if (d == null) {
            out.accept(Component.translatable("tooltip.%s.blueprint_scroll.blank".formatted(MODID)).withStyle(ChatFormatting.GRAY));
        } else {
            BlockPos s = d.size();
            out.accept(Component.translatable("tooltip.%s.blueprint_scroll.written".formatted(MODID), d.blocks().size(), s.getX(), s.getY(),
                    s.getZ()).withStyle(ChatFormatting.GRAY));
        }
    }
}
