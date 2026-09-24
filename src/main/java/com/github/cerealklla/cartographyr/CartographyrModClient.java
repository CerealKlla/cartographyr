package com.github.cerealklla.cartographyr;

import com.github.cerealklla.cartographyr.client.LocationOverlay;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = CartographyrMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CartographyrMod.MODID, value = Dist.CLIENT)
public class CartographyrModClient {
    public CartographyrModClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CartographyrMod.LOGGER.info("Cartographyr client setup");
    }

    // LocationPayload's registration (including its handler) lives in CartographyrMod, not here --
    // RegisterPayloadHandlersEvent shares one global network registry per modid, so registering the
    // same payload type from both this class and the common one throws "already registered"
    // (confirmed via a real client boot crash). See CartographyrMod.registerPayloads for why
    // registering the handler from common code is still safe.
    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(CartographyrMod.MODID, "location_overlay"), new LocationOverlay());
    }
}
