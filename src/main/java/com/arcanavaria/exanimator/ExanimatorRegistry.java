package com.arcanavaria.exanimator;

import static com.arcanavaria.ArcanaVaria.MODID;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Exanimator's entity type and attributes. */
public final class ExanimatorRegistry {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);

    /** 2.4 wide, 5.6 tall: the head clears two players stacked; the raised scythe and halo reach past 6.5 blocks. */
    public static final DeferredHolder<EntityType<?>, EntityType<Exanimator>> EXANIMATOR = ENTITIES.registerEntityType(
        "exanimator", Exanimator::new, MobCategory.MONSTER,
        b -> b.sized(2.4F, 5.6F).eyeHeight(4.9F).clientTrackingRange(12).fireImmune());

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ExanimatorRegistry::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(EXANIMATOR.get(), Exanimator.createAttributes().build());
    }

    private ExanimatorRegistry() {
    }
}
