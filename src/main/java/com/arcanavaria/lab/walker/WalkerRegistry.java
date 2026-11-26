package com.arcanavaria.lab.walker;

import static com.arcanavaria.ArcanaVaria.MODID;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Crucible Walker's entity type and attributes. */
public final class WalkerRegistry {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);

    /**
     * The model is 5 blocks tall and 5 across the legs at scale 1; the box covers the body and cage. Lab archetypes
     * raise it at scale 0.8 (2.7 wide, 3.5 tall), which fits the 4x4x4 room the altar asks for.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<CrucibleWalker>> CRUCIBLE_WALKER = ENTITIES.registerEntityType(
        "crucible_walker", CrucibleWalker::new, MobCategory.MONSTER,
        b -> b.sized(3.4F, 4.4F).eyeHeight(3.2F).clientTrackingRange(10).fireImmune());

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(WalkerRegistry::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(CRUCIBLE_WALKER.get(), CrucibleWalker.createAttributes().build());
    }

    private WalkerRegistry() {
    }
}
