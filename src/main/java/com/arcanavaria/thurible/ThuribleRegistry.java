package com.arcanavaria.thurible;

import static com.arcanavaria.ArcanaVaria.MODID;
import static com.arcanavaria.ArcanaVaria.id;

import com.arcanavaria.AVRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Thurible (0.5.0): item, blueprints, cloud entity, its two data components and the cloud-effect datapack registry. */
public final class ThuribleRegistry {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);

    /** data/&lt;ns&gt;/arcanavaria/thurible_effect/*.json */
    public static final ResourceKey<Registry<CloudEffect>> EFFECT_KEY = ResourceKey.createRegistryKey(id("thurible_effect"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ThuribleContents>> CONTENTS = COMPONENTS.registerComponentType(
            "thurible_contents", b -> b.persistent(ThuribleContents.CODEC).networkSynchronized(ThuribleContents.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ThuribleUpgrades>> UPGRADES = COMPONENTS.registerComponentType(
            "thurible_upgrades", b -> b.persistent(ThuribleUpgrades.CODEC).networkSynchronized(ThuribleUpgrades.STREAM_CODEC));

    public static final DeferredHolder<EntityType<?>, EntityType<ThuribleCloud>> CLOUD = ENTITIES.registerEntityType(
            "thurible_cloud", ThuribleCloud::new, MobCategory.MISC,
            b -> b.sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(Integer.MAX_VALUE).noSave().noSummon());

    public static final DeferredItem<ThuribleItem> THURIBLE = AVRegistry.tab(AVRegistry.ITEMS.registerItem("thurible", ThuribleItem::new,
            p -> ThuribleItem.properties(p).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ThuribleBlueprintItem> CHAIN_BLUEPRINT = blueprint(ThuribleBlueprintItem.Kind.CHAIN);
    public static final DeferredItem<ThuribleBlueprintItem> CLOUD_BLUEPRINT = blueprint(ThuribleBlueprintItem.Kind.CLOUD);
    public static final DeferredItem<ThuribleBlueprintItem> ORBIT_BLUEPRINT = blueprint(ThuribleBlueprintItem.Kind.ORBIT);

    private static DeferredItem<ThuribleBlueprintItem> blueprint(ThuribleBlueprintItem.Kind kind) {
        return AVRegistry.tab(AVRegistry.ITEMS.registerItem("thurible_blueprint_" + kind.id(), p -> new ThuribleBlueprintItem(kind, p),
                p -> p.stacksTo(16).rarity(Rarity.RARE)));
    }

    /** Called from AVRegistry before ITEMS registers, so loading this class adds the items above in time. */
    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(ThuribleRegistry::registries);
    }

    private static void registries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(EFFECT_KEY, CloudEffect.CODEC, CloudEffect.CODEC);
    }

    private ThuribleRegistry() {
    }
}
