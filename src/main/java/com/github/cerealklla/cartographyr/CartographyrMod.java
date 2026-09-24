package com.github.cerealklla.cartographyr;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.github.cerealklla.cartographyr.api.Cartography;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here must match the modId entry in META-INF/neoforge.mods.toml (sourced from mod_id in gradle.properties)
@Mod(CartographyrMod.MODID)
public class CartographyrMod {
    public static final String MODID = "cartographyr";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CartographyrMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // Game-bus listener (not the mod bus above) — this is what actually triggers
        // CartographySavedData.TYPE's registration at real server boot, proving the wiring
        // doesn't crash outside of unit tests.
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Cartographyr common setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        int count = Cartography.getEntitiesAt(event.getServer(), Level.OVERWORLD, 0, 0).size();
        LOGGER.info("Cartographyr geographic data loaded, {} entities at overworld origin", count);
    }
}
