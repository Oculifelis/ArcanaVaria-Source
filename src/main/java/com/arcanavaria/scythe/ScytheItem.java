package com.arcanavaria.scythe;

import com.arcanavaria.AVDataMaps;
import com.arcanavaria.AVRegistry;
import com.arcanavaria.ArcanaVaria;
import com.fundamentum.scythe.Scythes;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jspecify.annotations.Nullable;

/**
 * A scythe after Scythes Vanilla (Drift007_): slow heavy swings (1.2 a second) with one block more reach and a built-in
 * sweep whose strength is the item's sweeping damage ratio, fast on leaves, crops and grass, Eclipse's bonus damage
 * against the undead and Reaper's Step's dash. Void scythes mend themselves while carried and weaken what they hit, like
 * Thaumaturge's void tools.
 */
public class ScytheItem extends Item {
    /** Scythes Vanilla's speed for every normal scythe: 4 - 2.8 = 1.2 attacks a second. */
    private static final float ATTACK_SPEED = -2.8f;
    /** Leaves, crops, flowers and grass cut at least this fast, whatever the metal. */
    private static final float PLANT_SPEED = 10f;
    // built-in values; data/<ns>/data_maps/enchantment/enchantment_values.json can change each
    private static final double ECLIPSE_DAMAGE_PER_LEVEL = 2;
    private static final double DASH_POWER = 1.25;
    private static final double DASH_UPWARD = 0.3;
    private static final double DASH_RANGE = 3.0;
    private static final double DASH_RADIUS = 0.9;
    private static final double[] DASH_DAMAGE = {0, 4, 6, 8};
    private static final double DASH_COOLDOWN = 30;
    private static final int REPAIR_INTERVAL = 20;

    private final boolean voidMetal;

    public ScytheItem(Item.Properties properties, boolean voidMetal) {
        super(properties);
        this.voidMetal = voidMetal;
    }

    /**
     * The metal's durability, mining speed on {@link AVRegistry#SCYTHE_MINEABLE}, enchantability and repair item, then
     * the scythe's attributes in place of the tool ones: damage on top of the metal's bonus, 1.2 attack speed, a sweep
     * ratio and one block more entity reach.
     */
    public static Item.Properties scythe(Item.Properties p, ToolMaterial material, String name, float damage, float sweep) {
        material.applyToolProperties(p, AVRegistry.SCYTHE_MINEABLE, damage, ATTACK_SPEED, 0f);
        return p.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        damage + material.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.SWEEPING_DAMAGE_RATIO, new AttributeModifier(ArcanaVaria.id(name + "_sweep"),
                        sweep, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ArcanaVaria.id(name + "_reach"),
                        1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build());
    }

    public static boolean isScythe(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ScytheItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, out, flag);
        if (AVRegistry.FUNDAMENTUM) {
            MoveTooltip.append(stack, out);
        }
    }

    private static final class MoveTooltip {
        private static final String KEY = "tooltip.arcanavaria.scythe.";

        private static void append(ItemStack stack, Consumer<Component> out) {
            var spec = Scythes.spec(stack);
            if (spec == null) {
                return;
            }
            for (String move : spec.moves()) {
                var line = Component.translatable(KEY + move);
                if (move.equals(spec.moveOn("use"))) {
                    line = Component.translatable(KEY + "use", line);
                } else if (move.equals(spec.moveOn("sneak_use"))) {
                    line = Component.translatable(KEY + "sneak_use", line);
                }
                out.accept(line.withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** Scythes sweep like swords; the sweep's strength is the item's sweeping damage ratio. */
    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility ability) {
        return ability == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, ability);
    }

    /** The sweep is built in, so Sweeping Edge has nothing to add (as in Scythes Vanilla). */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return !enchantment.is(Enchantments.SWEEPING_EDGE) && super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.CROPS) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.REPLACEABLE)) {
            return Math.max(speed, PLANT_SPEED);
        }
        return speed;
    }

    /** Swords lose one durability a hit; so do scythes. */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        if (voidMetal && target.level() instanceof ServerLevel level
                && (!(target instanceof Player) || level.getGameRules().get(GameRules.PVP))) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80), attacker);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (voidMetal && stack.isDamaged() && owner.tickCount % REPAIR_INTERVAL == 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    /** Eclipse: +2 damage a level (enchantment_values damage_per_level) against the undead at night, in total darkness, and always in the Nether and the End. */
    @Override
    public float getAttackDamageBonus(Entity target, float baseDamage, DamageSource source) {
        float bonus = super.getAttackDamageBonus(target, baseDamage, source);
        if (target instanceof LivingEntity living && living.isInvertedHealAndHarm() && isEclipseCondition(target)) {
            ItemStack weapon = source.getWeaponItem();
            int eclipse = ScytheEvents.level(target.level(), weapon, AVRegistry.ECLIPSE);
            bonus += eclipse * (float) AVDataMaps.enchant(target.level(), AVRegistry.ECLIPSE, "damage_per_level", ECLIPSE_DAMAGE_PER_LEVEL);
        }
        return bonus;
    }

    public static boolean isEclipseCondition(Entity entity) {
        Level level = entity.level();
        if (level.dimension() == Level.NETHER || level.dimension() == Level.END) {
            return true;
        }
        return level.isDarkOutside() || level.getRawBrightness(entity.blockPosition(), 0) == 0;
    }

    /**
     * Reaper's Step: use to dash forward, hurting everything in the path (4 / 6 / 8 damage), for one durability and a
     * 1.5 s cooldown. Never when the scythe would break, and with a shield in the off hand only while sneaking, so the
     * shield still blocks.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        int step = reapersStepLevel(player, stack, player.isShiftKeyDown());
        if (step < 1) {
            return InteractionResult.PASS;
        }
        if (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 1) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            dash(serverLevel, player, step);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            player.getCooldowns().addCooldown(stack, (int) AVDataMaps.enchant(level, AVRegistry.REAPERS_STEP, "cooldown", DASH_COOLDOWN));
        }
        return InteractionResult.SUCCESS;
    }

    /** The inputs reserved for the enchantment, shared with Fundamentum's client and server use handlers. */
    public static boolean claimsReapersStep(Player player, boolean sneak) {
        return reapersStepLevel(player, player.getMainHandItem(), sneak) > 0;
    }

    private static int reapersStepLevel(Player player, ItemStack stack, boolean sneak) {
        if (!isScythe(stack) || player.isPassenger() || player.isFallFlying() || player.isInWater()
                || (!sneak && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS))) {
            return 0;
        }
        return Math.min(ScytheEvents.level(player.level(), stack, AVRegistry.REAPERS_STEP), 3);
    }

    private static void dash(ServerLevel level, Player player, int step) {
        Vec3 look = player.getLookAngle();
        double power = AVDataMaps.enchant(level, AVRegistry.REAPERS_STEP, "power", DASH_POWER);
        player.setDeltaMovement(new Vec3(look.x * power, AVDataMaps.enchant(level, AVRegistry.REAPERS_STEP, "upward", DASH_UPWARD), look.z * power));
        player.hurtMarked = true;
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.GUST, pos.x, pos.y + 0.5, pos.z, 10, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.SMALL_GUST, pos.x, pos.y + 0.3, pos.z, 6, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, AVRegistry.SCYTHE_DASH.get(), SoundSource.PLAYERS, 1f, 1f);
        AABB path = player.getBoundingBox().expandTowards(look.scale(AVDataMaps.enchant(level, AVRegistry.REAPERS_STEP, "range", DASH_RANGE)))
                .inflate(AVDataMaps.enchant(level, AVRegistry.REAPERS_STEP, "radius", DASH_RADIUS));
        float damage = (float) AVDataMaps.enchant(level, AVRegistry.REAPERS_STEP, "damage", step, DASH_DAMAGE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, path, e -> e != player && e.isAlive())) {
            target.hurtServer(level, player.damageSources().playerAttack(player), damage);
        }
    }
}
