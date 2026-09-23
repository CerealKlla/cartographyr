package com.github.cerealklla.cartographyr;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// The value here must match the modId entry in META-INF/neoforge.mods.toml (sourced from mod_id in gradle.properties)
@Mod(CartographyrMod.MODID)
public class CartographyrMod {
    public static final String MODID = "cartographyr";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CartographyrMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Cartographyr common setup");
    }
}
