package com.arcanavaria.thurible;

import static com.arcanavaria.ArcanaVaria.MODID;

import com.leclowndu93150.thaumaturge.api.aspect.AspectComponents;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaStorage;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * The Thurible: a brass censer on a chain that leaves clouds of essentia where it is swung (docs/PLAN-0.4-0.7.md, 0.5.0).
 * No GUI: right-click a jar to fill it (sneak to pour it back), or hold a filled phial in the other hand and use it; hold
 * use to swing, one cloud every {@link #SWING} ticks. What a cloud does is data (CloudEffect). The fill shows through the
 * glass in the aspect's colour: CUSTOM_MODEL_DATA carries the level and colour to the item model (created by
 * tools/thurible_models.py), so the look needs no client code.
 */
public class ThuribleItem extends Item {
    public static final int CAPACITY = 64;
    public static final int SWING = 12;
    private static final int ORBIT_EVERY = 30;

    public ThuribleItem(Properties props) {
        super(props);
    }

    public static Properties properties(Properties p) {
        return p.stacksTo(1)
                .component(ThuribleRegistry.CONTENTS.get(), ThuribleContents.EMPTY)
                .component(ThuribleRegistry.UPGRADES.get(), ThuribleUpgrades.NONE)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 3.0, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build());
    }

    public static ThuribleContents contents(ItemStack stack) {
        return stack.getOrDefault(ThuribleRegistry.CONTENTS.get(), ThuribleContents.EMPTY);
    }

    public static ThuribleUpgrades upgrades(ItemStack stack) {
        return stack.getOrDefault(ThuribleRegistry.UPGRADES.get(), ThuribleUpgrades.NONE);
    }

    /** Sets what the thurible holds and the fill level and colour its model reads. */
    public static void setContents(ItemStack stack, ThuribleContents c, HolderLookup.Provider access) {
        stack.set(ThuribleRegistry.CONTENTS.get(), c);
        if (c.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) c.amount() / CAPACITY), List.of(),
                    List.of(), List.of(ThuribleEffects.colour(access, c.aspect().get()))));
        }
    }

    // ---- filling ------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        IEssentiaStorage storage = level.getCapability(EssentiaCapabilities.STORAGE, pos, ctx.getClickedFace());
        Player player = ctx.getPlayer();
        if (storage == null || player == null) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = ctx.getItemInHand();
        ThuribleContents c = contents(stack);
        if (player.isSecondaryUseActive()) {
            if (c.isEmpty()) {
                return InteractionResult.PASS;
            }
            int moved = pour(storage, server, c);
            if (moved > 0) {
                setContents(stack, ThuribleContents.of(c.aspect().get(), c.amount() - moved), server.registryAccess());
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.1F);
            }
            report(player, contents(stack));
            return InteractionResult.SUCCESS;
        }
        for (AspectInstance a : storage.contents().entries()) {
            Optional<ResourceKey<IAspect>> key = a.aspect().unwrapKey();
            if (key.isEmpty() || !c.accepts(key.get())) {
                continue;
            }
            int want = CAPACITY - c.amount();
            if (want <= 0) {
                break;
            }
            int got;
            try (Transaction tx = Transaction.openRoot()) {
                got = storage.extract(a.aspect(), want, tx);
                tx.commit();
            }
            if (got > 0) {
                setContents(stack, ThuribleContents.of(key.get(), c.amount() + got), server.registryAccess());
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8F, 0.9F);
                report(player, contents(stack));
                return InteractionResult.SUCCESS;
            }
        }
        player.sendOverlayMessage(c.isEmpty() ? Component.translatable("message.%s.thurible.nothing".formatted(MODID))
                : Component.translatable("message.%s.thurible.other".formatted(MODID), name(server.registryAccess(), c.aspect().get())));
        return InteractionResult.SUCCESS;
    }

    private static int pour(IEssentiaStorage storage, ServerLevel level, ThuribleContents c) {
        Optional<Holder.Reference<IAspect>> h = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(c.aspect().get());
        if (h.isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int moved = storage.insert(h.get(), c.amount(), tx);
            tx.commit();
            return moved;
        }
    }

    /** Pours a filled phial from the other hand in, leaving the empty phial there. */
    private static boolean pourPhial(Player player, InteractionHand hand, ItemStack stack, ServerLevel level) {
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack phial = player.getItemInHand(other);
        AspectList list = phial.is(TTItems.PHIAL.get()) ? phial.get(TTDataComponents.ASPECTS.get()) : null;
        if (list == null || list.isEmpty()) {
            return false;
        }
        AspectInstance a = list.entries().getFirst();
        ThuribleContents c = contents(stack);
        Optional<ResourceKey<IAspect>> key = a.aspect().unwrapKey();
        if (key.isEmpty() || !c.accepts(key.get()) || c.amount() + a.amount() > CAPACITY) {
            player.sendOverlayMessage(Component.translatable("message.%s.thurible.phial_full".formatted(MODID)));
            return true;
        }
        setContents(stack, ThuribleContents.of(key.get(), c.amount() + a.amount()), level.registryAccess());
        ItemStack empty = new ItemStack(TTItems.PHIAL.get());
        if (phial.getCount() == 1) {
            player.setItemInHand(other, empty);
        } else {
            phial.shrink(1);
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.0F);
        report(player, contents(stack));
        return true;
    }

    private static void report(Player player, ThuribleContents c) {
        if (c.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.%s.thurible.empty".formatted(MODID)));
        } else {
            player.sendOverlayMessage(Component.translatable("message.%s.thurible.holds".formatted(MODID),
                    name(player.level().registryAccess(), c.aspect().get()), c.amount(), CAPACITY));
        }
    }

    private static Component name(HolderLookup.Provider access, ResourceKey<IAspect> key) {
        return access.lookup(IAspect.REGISTRY_KEY).flatMap(r -> r.get(key)).<Component>map(AspectComponents::name)
                .orElse(Component.literal(key.identifier().getPath()));
    }

    // ---- swinging -----------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ThuribleUpgrades up = upgrades(stack);
        if (player.isSecondaryUseActive() && up.orbit()) {
            if (level instanceof ServerLevel) {
                ThuribleUpgrades next = up.toggleOrbit();
                stack.set(ThuribleRegistry.UPGRADES.get(), next);
                player.sendOverlayMessage(Component.translatable("message.%s.thurible.orbit_%s".formatted(MODID, next.orbiting() ? "on" : "off")));
            }
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server && pourPhial(player, hand, stack, server)) {
            return InteractionResult.SUCCESS;
        }
        if (contents(stack).isEmpty()) {
            if (level instanceof ServerLevel) {
                report(player, ThuribleContents.EMPTY);
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;      // ThuribleClient swings it
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int used = getUseDuration(stack, user) - remaining;
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (used % SWING == 2) {
            level.playSound(null, user.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.6F, 0.7F + user.getRandom().nextFloat() * 0.2F);
        }
        if (used % SWING != SWING / 2) {
            return;
        }
        ThuribleUpgrades up = upgrades(stack);
        // the end of the arc: in front, a little to the side the censer is swinging towards, at waist height
        float side = (used / SWING) % 2 == 0 ? 0.8F : -0.8F;
        Vec3 look = user.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = user.position().add(look.scale(up.reach(stack.getItem()))).add(new Vec3(-look.z, 0, look.x).scale(side)).add(0, 0.6, 0);
        if (!emit(server, stack, user, at)) {
            user.releaseUsingItem();
        }
    }

    /** Spends one essentia on a cloud at {@code at}; false when there was none. */
    private static boolean emit(ServerLevel level, ItemStack stack, @Nullable LivingEntity user, Vec3 at) {
        ThuribleContents c = contents(stack);
        if (c.isEmpty()) {
            return false;
        }
        ThuribleCloud.puff(level, at.x, at.y, at.z, c.aspect().get(), upgrades(stack).radius(stack.getItem()), user);
        setContents(stack, ThuribleContents.of(c.aspect().get(), c.amount() - 1), level.registryAccess());
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.35F, 1.4F);
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel level) {
            emit(level, stack, attacker, target.position().add(0, target.getBbHeight() * 0.4, 0));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if ((slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) || !(owner instanceof LivingEntity user)
                || !upgrades(stack).orbiting() || owner.tickCount % ORBIT_EVERY != 0) {
            return;
        }
        double a = owner.tickCount / (double) ORBIT_EVERY * 2.4;
        emit(level, stack, user, owner.position().add(Math.cos(a) * 2.5, 0.8, Math.sin(a) * 2.5));
    }

    // ---- what it shows ------------------------------------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Mth.clamp(contents(stack).amount() / (float) CAPACITY, 0, 1));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        CustomModelData d = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return d == null || d.colors().isEmpty() ? 0xC8B8E8 : d.colors().getFirst();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        ThuribleContents c = contents(stack);
        HolderLookup.Provider access = ctx.registries();
        if (c.isEmpty() || access == null) {
            out.accept(Component.translatable("tooltip.%s.thurible.empty".formatted(MODID)).withStyle(ChatFormatting.GRAY));
        } else {
            int rgb = ThuribleEffects.colour(access, c.aspect().get());
            out.accept(Component.translatable("tooltip.%s.thurible.holds".formatted(MODID), name(access, c.aspect().get()), c.amount(), CAPACITY)
                    .withColor(rgb));
            for (ThuribleEffects.Resolved r : ThuribleEffects.resolve(access, c.aspect().get())) {
                MutableComponent line = Component.literal(" ").append(r.effect().descriptionKey().<Component>map(Component::translatable)
                        .orElse(name(access, r.effect().aspect())));
                if (r.boost() > 0) {
                    line.append(Component.translatable("tooltip.%s.thurible.stronger".formatted(MODID), "%.1f".formatted(r.scale())));
                }
                out.accept(line.withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        ThuribleUpgrades up = upgrades(stack);
        for (ThuribleBlueprintItem.Kind k : ThuribleBlueprintItem.Kind.values()) {
            if (up.has(k)) {
                out.accept(Component.translatable("tooltip.%s.thurible.upgrade.%s".formatted(MODID, k.id())).withStyle(ChatFormatting.GOLD));
            }
        }
        if (up.orbiting()) {
            out.accept(Component.translatable("tooltip.%s.thurible.orbiting".formatted(MODID)).withStyle(ChatFormatting.GOLD));
        }
        out.accept(Component.translatable("tooltip.%s.thurible.howto".formatted(MODID)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
