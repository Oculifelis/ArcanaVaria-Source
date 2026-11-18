package com.arcanavaria.compat;

import com.arcanavaria.ArcanaVaria;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

/**
 * JEI: information pages for the items nobody crafts (the Forsaken Laboratory blocks, journals, Thurible blueprints,
 * the Warden's heart). Every crafted item uses a vanilla or Thaumaturge recipe type, which those already show. Loaded
 * only with JEI.
 */
@JeiPlugin
public final class AVJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return ArcanaVaria.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        JeiKit.infoPages(registration, ArcanaVaria.MODID);
    }
}
