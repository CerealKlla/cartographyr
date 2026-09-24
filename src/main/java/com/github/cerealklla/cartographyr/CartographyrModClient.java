package com.github.cerealklla.cartographyr;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

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
}
