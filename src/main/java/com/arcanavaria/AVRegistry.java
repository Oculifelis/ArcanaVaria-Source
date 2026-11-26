package com.arcanavaria;

import static com.arcanavaria.ArcanaVaria.MODID;
import static com.arcanavaria.ArcanaVaria.id;

import com.arcanavaria.lab.LabRegistry;
import com.arcanavaria.lattice.LatticeRegistry;
import com.arcanavaria.lens.LensRegistry;
import com.arcanavaria.scythe.GrimHarvestEffect;
import com.arcanavaria.scythe.ScytheItem;
import com.arcanavaria.thurible.ThuribleRegistry;
import com.arcanavaria.warden.AmberHeartItem;
import com.arcanavaria.warden.HeartseedItem;
import com.arcanavaria.warden.WardenRegistry;
import com.leclowndu93150.thaumaturge.content.equipment.GearWarp;
import com.leclowndu93150.thaumaturge.content.equipment.TTMaterials;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    /** Leaves, crops, grass and other plants a scythe cuts with its tool speed. */
    public static final TagKey<Block> SCYTHE_MINEABLE = TagKey.create(Registries.BLOCK, id("mineable/scythe"));

    // Scythes Vanilla's enchantments; the enchantments themselves are data (data/arcanavaria/enchantment).
    public static final ResourceKey<Enchantment> SOUL_DEVOURER = enchantment("soul_devourer");
    public static final ResourceKey<Enchantment> GRIM_HARVEST = enchantment("grim_harvest");
    public static final ResourceKey<Enchantment> ECLIPSE = enchantment("eclipse");
    public static final ResourceKey<Enchantment> REAPERS_STEP = enchantment("reapers_step");

    public static final DeferredHolder<MobEffect, GrimHarvestEffect> GRIM_HARVEST_EFFECT = MOB_EFFECTS.register("grim_harvest", GrimHarvestEffect::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> SCYTHE_SWEEP = sound("scythe.sweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCYTHE_DASH = sound("scythe.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECLIPSE_KILL = sound("enchant.eclipse.kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_DEVOURER_HEAL = sound("enchant.soul_devourer.heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRIM_HARVEST_PROC = sound("enchant.grim_harvest.proc");

    // Scythes Vanilla's stats per tier (iron 5.5 / 0.35, diamond 6.5 / 0.5, netherite 7.5 / 0.75; all 1.2 speed,
    // +1 reach), set on Thaumaturge's metals: damage bonus on top of the metal's, then the sweep ratio.
    // Thaumium 3.5 + 2.5 = 6.0 / 0.4, void 4.0 + 3.0 = 7.0 / 0.55, elemental 4.5 + 3.0 = 7.5 / 0.75.
    public static final DeferredItem<ScytheItem> THAUMIUM_SCYTHE = tab(ITEMS.registerItem("thaumium_scythe",
            p -> new ScytheItem(p, false), p -> ScytheItem.scythe(p, TTMaterials.TOOL_THAUMIUM, "thaumium_scythe", 3.5f, 0.4f)
                    .rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ScytheItem> VOID_SCYTHE = tab(ITEMS.registerItem("void_scythe",
            p -> new ScytheItem(p, true), p -> GearWarp.with(ScytheItem.scythe(p, TTMaterials.TOOL_VOID, "void_scythe", 4.0f, 0.55f), 1)
                    .rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ScytheItem> ELEMENTAL_SCYTHE = tab(ITEMS.registerItem("elemental_scythe",
            p -> new ScytheItem(p, false), p -> ScytheItem.scythe(p, TTMaterials.TOOL_ELEMENTAL, "elemental_scythe", 4.5f, 0.75f)
                    .rarity(Rarity.RARE)));
    // The Exanimator's blade: 1.5 + 3.0 elemental metal + 1.0 player damage, with the widest sweep.
    public static final DeferredItem<ScytheItem> EXANIMATORS_EDGE = tab(ITEMS.registerItem("exanimators_edge",
            p -> new ScytheItem(p, false), p -> ScytheItem.scythe(p, TTMaterials.TOOL_ELEMENTAL, "exanimators_edge", 1.5f, 0.9f)
                    .rarity(Rarity.EPIC).fireResistant()));

    // The Greatwood Warden (docs/WARDEN.md): the Heartseed summons it, its Amber Heart is the reward.
    public static final DeferredItem<HeartseedItem> HEARTSEED = tab(ITEMS.registerItem("heartseed", HeartseedItem::new,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<AmberHeartItem> AMBER_HEART = tab(ITEMS.registerItem("warden_amber_heart", AmberHeartItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE)));

    // Cord whips on Fundamentum's whip system (whip/AVWhips): registered only when Fundamentum is installed, so these
    // stay null without it and every whip recipe and research entry carries a mod_loaded condition.
    public static final boolean FUNDAMENTUM = ModList.get().isLoaded("fundamentum");
    public static final DeferredItem<Item> THAUMIUM_WHIP = whip("thaumium_whip", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> VOID_WHIP = whip("void_whip", p -> GearWarp.with(p, 1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ELEMENTAL_WHIP = whip("elemental_whip", p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<Item> AMBERBARK_LASH = whip("amberbark_lash", p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.arcanavaria"))
            .icon(() -> new ItemStack(THAUMIUM_SCYTHE.get()))
            .displayItems((params, out) -> TAB_ITEMS.forEach(i -> out.accept(i.get())))
            .build());

    private AVRegistry() {
    }

    private static DeferredItem<Item> whip(String name, UnaryOperator<Item.Properties> props) {
        return FUNDAMENTUM ? tab(ITEMS.registerSimpleItem(name, p -> props.apply(p.stacksTo(1)))) : null;
    }

    public static <I extends Item> DeferredItem<I> tab(DeferredItem<I> item) {
        TAB_ITEMS.add(item);
        return item;
    }

    private static ResourceKey<Enchantment> enchantment(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, id(name));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }

    static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
        SOUNDS.register(bus);
        LatticeRegistry.register(bus);
        ThuribleRegistry.register(bus);
        LensRegistry.register(bus);
        com.arcanavaria.fx.SwingFxPacket.register(bus);
        LabRegistry.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        WardenRegistry.register(bus);
        com.arcanavaria.exanimator.ExanimatorRegistry.register(bus);
        com.arcanavaria.lab.walker.WalkerRegistry.register(bus);
    }
}
