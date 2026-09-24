package com.github.cerealklla.cartographyr;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.client.ClientLocationState;
import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.natural.NaturalRegionDiscovery;
import com.github.cerealklla.cartographyr.network.LocationPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// The value here must match the modId entry in META-INF/neoforge.mods.toml (sourced from mod_id in gradle.properties)
@Mod(CartographyrMod.MODID)
public class CartographyrMod {
    public static final String MODID = "cartographyr";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Five times a second; NeoForge has no built-in throttled tick event, so this is a manual
    // modulo guard inside the every-tick listener. The 2-consecutive-checks debounce below means
    // confirmation takes at least two of these intervals, so this needs to be short enough that
    // the combined delay still feels immediate -- tuned down from 10 (2/sec, ~1s to confirm) after
    // that felt slightly laggy in practice; still cheap even at this rate (a plain getEntitiesAt
    // lookup, or a single getBiome() call for untracked territory -- the expensive flood-fill only
    // runs once per newly-discovered area, not on every check).
    private static final int NOTIFY_CHECK_INTERVAL_TICKS = 4;

    // Session-only (in-memory, never persisted, resets on rejoin/restart) -- deliberately NOT the
    // real Player Knowledge system (design doc Section 5.9), which is still out of scope. This is
    // a demo/test trigger to make Natural Geography visible in-game; a real version of this would
    // likely belong in a different mod in the suite, consuming Cartography's public API the same
    // way this does.
    private final Map<UUID, EntityId> lastNotifiedRegion = new HashMap<>();

    // Debounce: a candidate must be seen on two consecutive checks before it's announced, so
    // briefly clipping a jagged real-world biome border (e.g. desert/badlands, which vanilla
    // generates with sharp, non-smooth edges) doesn't repeatedly re-fire the notification as the
    // player oscillates across it. Confirmed necessary via a real playtest -- see decisions.md.
    private final Map<UUID, EntityId> pendingRegion = new HashMap<>();

    public CartographyrMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        // Game-bus listener (not the mod bus above) — this is what actually triggers
        // CartographySavedData.TYPE's registration at real server boot, proving the wiring
        // doesn't crash outside of unit tests.
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Cartographyr common setup");
    }

    // Single registration point, including the handler — RegisterPayloadHandlersEvent shares one
    // global network registry per modid, so also registering from CartographyrModClient (confirmed
    // via a real client boot crash, "already registered") isn't an option. The handler lambda
    // touches client-only state (ClientLocationState), but that's safe here: it's only ever
    // *invoked* when this payload is received, which never happens on a dedicated server (servers
    // only send it) -- referencing it is fine even though this class also loads on the server.
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(LocationPayload.TYPE, LocationPayload.STREAM_CODEC,
                (payload, context) -> ClientLocationState.set(payload.name()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        int count = Cartography.getEntitiesAt(event.getServer(), Level.OVERWORLD, 0, 0).size();
        LOGGER.info("Cartographyr geographic data loaded, {} entities at overworld origin", count);
    }

    /**
     * Proactively syncs the current location on (re)join. Without this, a player reconnecting
     * without having moved would see a blank overlay indefinitely: {@link #notifyIfChanged} only
     * sends an update when the detected region actually *changes*, but the client's overlay state
     * is fresh/blank on every new connection regardless of whether the server-side
     * {@link #lastNotifiedRegion} entry for them is unchanged from a previous session.
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        int x = player.getBlockX();
        int z = player.getBlockZ();

        Set<GeographicEntity> here = Cartography.getEntitiesAt(level, x, z);
        GeographicEntity entity = here.isEmpty()
                ? NaturalRegionDiscovery.discover(level, player.blockPosition()).orElse(null)
                : here.iterator().next();
        if (entity == null) {
            return;
        }

        UUID playerId = player.getUUID();
        pendingRegion.remove(playerId);
        lastNotifiedRegion.put(playerId, entity.id());
        sendLocation(player, entity);
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
        if (!here.isEmpty()) {
            notifyIfChanged(player, here.iterator().next());
            return;
        }

        NaturalRegionDiscovery.discover(level, player.blockPosition())
                .ifPresent(entity -> notifyIfChanged(player, entity));
    }

    private void notifyIfChanged(ServerPlayer player, GeographicEntity entity) {
        UUID playerId = player.getUUID();
        EntityId lastNotified = lastNotifiedRegion.get(playerId);
        if (entity.id().equals(lastNotified)) {
            pendingRegion.remove(playerId);
            return;
        }

        EntityId pending = pendingRegion.get(playerId);
        if (!entity.id().equals(pending)) {
            // First sighting of this candidate -- wait for a second consecutive match before
            // announcing it, rather than firing immediately.
            pendingRegion.put(playerId, entity.id());
            return;
        }

        // Confirmed: this candidate was also seen on the previous check.
        pendingRegion.remove(playerId);
        lastNotifiedRegion.put(playerId, entity.id());
        sendLocation(player, entity);
    }

    private void sendLocation(ServerPlayer player, GeographicEntity entity) {
        String name = entity.name().orElse("an unnamed place");
        LOGGER.info("Player {} entered {} ('{}')", player.getName().getString(), entity.id(), name);
        PacketDistributor.sendToPlayer(player, new LocationPayload(name));
    }
}
