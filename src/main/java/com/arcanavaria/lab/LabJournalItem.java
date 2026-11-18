package com.arcanavaria.lab;

import static com.arcanavaria.ArcanaVaria.MODID;
import static com.arcanavaria.ArcanaVaria.id;

import com.leclowndu93150.thaumaturge.api.capability.IPlayerKnowledge;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * A dead thaumaturge's journal, found in the Forsaken Laboratories. Reading it (use) teaches the research it holds,
 * once; the book stays. Loot tables set which research and title (the arcanavaria:journal component).
 */
public class LabJournalItem extends Item {
    public record Journal(Identifier research, String title) {
        public static final Journal DEFAULT = new Journal(id("forsaken_laboratories"), "journal.%s.forsaken_laboratories".formatted(MODID));
        public static final Codec<Journal> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("research").forGetter(Journal::research),
                Codec.STRING.fieldOf("title").forGetter(Journal::title)).apply(i, Journal::new));
        public static final StreamCodec<ByteBuf, Journal> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Journal::research, ByteBufCodecs.STRING_UTF8, Journal::title, Journal::new);
    }

    public LabJournalItem(Properties props) {
        super(props);
    }

    public static Journal journal(ItemStack stack) {
        return stack.getOrDefault(LabRegistry.JOURNAL.get(), Journal.DEFAULT);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(journal(stack).title());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            Journal j = journal(player.getItemInHand(hand));
            IPlayerKnowledge k = KnowledgeAccess.of(sp);
            if (k.isResearchComplete(j.research())) {
                sp.sendOverlayMessage(Component.translatable("message.%s.journal.known".formatted(MODID)));
            } else {
                k.addResearch(j.research());
                k.sync(sp);
                sp.sendSystemMessage(Component.translatable("message.%s.journal.learned".formatted(MODID),
                        Component.translatable("research.%s.%s.title".formatted(j.research().getNamespace(), j.research().getPath())))
                        .withStyle(ChatFormatting.DARK_PURPLE));
                level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
                level.playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.%s.journal".formatted(MODID)).withStyle(ChatFormatting.GRAY));
    }
}
