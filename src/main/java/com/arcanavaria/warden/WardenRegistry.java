package com.arcanavaria.warden;

import static com.arcanavaria.ArcanaVaria.MODID;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The Greatwood Warden's entities and the player flag its Amber Heart sets (its items are in AVRegistry). */
public final class WardenRegistry {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /** 2.6 wide, 5.0 tall by default (boss_moves hitbox_* overrides it): mantle height; the antlers reach higher still. */
    public static final DeferredHolder<EntityType<?>, EntityType<GreatwoodWarden>> GREATWOOD_WARDEN = ENTITIES.registerEntityType(
        "greatwood_warden", GreatwoodWarden::new, MobCategory.MONSTER,
        b -> b.sized(2.6F, 5.0F).eyeHeight(4.6F).clientTrackingRange(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<RootSpike>> ROOT_SPIKE = ENTITIES.registerEntityType(
        "root_spike", RootSpike::new, MobCategory.MISC, b -> b.sized(0.8F, 1.6F).clientTrackingRange(8).updateInterval(Integer.MAX_VALUE));
    public static final DeferredHolder<EntityType<?>, EntityType<WardenSplinter>> SPLINTER = ENTITIES.registerEntityType(
        "warden_splinter", WardenSplinter::new, MobCategory.MISC, b -> b.sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(10));

    /** Set once a player has eaten a Warden's Amber Heart; copied through death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> AMBER_HEART_USED = ATTACHMENTS.register("amber_heart_used",
        () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("used")).copyOnDeath().build());

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener(WardenRegistry::attributes);
    }

    private static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(GREATWOOD_WARDEN.get(), GreatwoodWarden.createAttributes().build());
    }

    private WardenRegistry() {
    }
}
