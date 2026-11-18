package com.arcanavaria;

import com.arcanavaria.scythe.ScytheEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Arcana Varia: assorted Thaumaturge things that belong to no port. First in: the Thaumium, Void and Elemental
 * Scythes and their four enchantments, after Scythes Vanilla by Drift007_ (MIT / CC0, see licenses/).
 */
@Mod(ArcanaVaria.MODID)
public final class ArcanaVaria {
    public static final String MODID = "arcanavaria";
    public static final Logger LOG = LogUtils.getLogger();

    public ArcanaVaria(IEventBus modBus) {
        AVRegistry.register(modBus);
        AVDataMaps.register(modBus);
        com.arcanavaria.worldgen.AVWorldgen.register(modBus);
        if (AVRegistry.FUNDAMENTUM) {
            com.arcanavaria.whip.AVWhips.init();
        }
        ScytheEvents.register();
        SelfTest.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
