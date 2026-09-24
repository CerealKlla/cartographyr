package com.github.cerealklla.cartographyr;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.natural.NaturalRegionDiscovery;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// The value here must match the modId entry in META-INF/neoforge.mods.toml (sourced from mod_id in gradle.properties)
@Mod(CartographyrMod.MODID)
public class CartographyrMod {
    public static final String MODID = "cartographyr";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Once per second is plenty for a position check; NeoForge has no built-in throttled tick
    // event, so this is a manual modulo guard inside the every-tick listener.
    private static final int NOTIFY_CHECK_INTERVAL_TICKS = 20;

    // Session-only (in-memory, never persisted, resets on rejoin/restart) -- deliberately NOT the
    // real Player Knowledge system (design doc Section 5.9), which is still out of scope. This is
    // a demo/test trigger to make Natural Geography visible in-game; a real version of this would
    // likely belong in a different mod in the suite, consuming Cartography's public API the same
    // way this does.
    private final Map<UUID, EntityId> lastNotifiedRegion = new HashMap<>();

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

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        // instanceof ServerPlayer alone is sufficient to restrict this to the server side: the
        // client-side tick uses LocalPlayer, never ServerPlayer, so no separate isClientSide()
        // check is needed.
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % NOTIFY_CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        int x = player.getBlockX();
        int z = player.getBlockZ();

        Set<GeographicEntity> here = Cartography.getEntitiesAt(level, x, z);
        LOGGER.info("Position check ({}, {}): {} entit(y/ies) here: {}", x, z, here.size(),
                here.stream().map(e -> e.id() + "/" + e.name().orElse("?")).toList());
        if (!here.isEmpty()) {
            notifyIfChanged(player, here.iterator().next());
            return;
        }

        NaturalRegionDiscovery.discover(level, player.blockPosition())
                .ifPresent(entity -> notifyIfChanged(player, entity));
    }

    private void notifyIfChanged(ServerPlayer player, GeographicEntity entity) {
        EntityId lastNotified = lastNotifiedRegion.get(player.getUUID());
        if (entity.id().equals(lastNotified)) {
            LOGGER.info("Suppressed repeat notification for {} (already last-notified)", entity.id());
            return;
        }
        lastNotifiedRegion.put(player.getUUID(), entity.id());

        String name = entity.name().orElse("an unnamed place");
        LOGGER.info("Sending notification: entered {} ('{}')", entity.id(), name);
        player.sendSystemMessage(Component.literal("You have entered " + name));
    }
}
