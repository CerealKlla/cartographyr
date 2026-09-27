package com.github.cerealklla.cartographyr;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.geo.Layer;
import com.github.cerealklla.cartographyr.geo.ProtectionLevel;
import com.github.cerealklla.cartographyr.settlement.SettlementListener;

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

        // Cartographyr's own natural regions and settlements used to share one "Location" layer;
        // split 2026-09-26 (see decisions.md) into separate Region/Settlement layers so destroying
        // a settlement never requires touching the natural region underneath it. Lyfe's location
        // overlay still renders both on the same HUD row (Settlement preferred over Region), just
        // no longer via same-layer resolution -- see Layer.REGION_ID/SETTLEMENT_ID's own Javadoc.
        Cartography.registerLayer(new Layer(Layer.REGION_ID, "Region", 0));
        Cartography.registerLayer(new Layer(Layer.SETTLEMENT_ID, "Settlement", 1));

        // Protection level (design doc addendum, 2026-09-26) -- register the three built-ins so
        // they show up in Cartography.getRegisteredProtectionLevels, and set each new layer's own
        // accurate default -- the Region/Settlement split above means this can now be a clean
        // per-layer default with no explicit per-creation override needed (the workaround
        // ProtectionDefaults' Javadoc used to describe, back when both shared one layer).
        Cartography.registerProtectionLevel(ProtectionLevel.UNPROTECTED);
        Cartography.registerProtectionLevel(ProtectionLevel.NO_VOXEL_CHANGE_FULL_HEIGHT);
        Cartography.registerProtectionLevel(ProtectionLevel.NO_VOXEL_CHANGE_ALONG_SURFACE_AND_UP);
        Cartography.setDefaultProtectionLevel(Layer.REGION_ID, ProtectionLevel.UNPROTECTED);
        Cartography.setDefaultProtectionLevel(Layer.SETTLEMENT_ID, ProtectionLevel.NO_VOXEL_CHANGE_ALONG_SURFACE_AND_UP);

        // Game-bus listener (not the mod bus above) — this is what actually triggers
        // CartographySavedData.TYPE's registration at real server boot, proving the wiring
        // doesn't crash outside of unit tests.
        NeoForge.EVENT_BUS.register(this);

        // Villages are world-gen-driven, not player-driven, so Cartographyr detects them itself
        // rather than waiting for an external caller (see settlement package + decisions.md,
        // 2026-09-24).
        NeoForge.EVENT_BUS.addListener(SettlementListener::onChunkLoad);
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
