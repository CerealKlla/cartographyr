package com.github.cerealklla.cartographyr.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class LayerRegistryTest {

    @Test
    void registeredLayerIsRetrievableById() {
        Identifier id = Identifier.fromNamespaceAndPath("layerregistrytest", "one");
        Layer layer = new Layer(id, "Test Layer", 3);

        LayerRegistry.register(layer);

        assertEquals(Optional.of(layer), LayerRegistry.get(id));
        assertTrue(LayerRegistry.all().contains(layer));
    }

    @Test
    void unregisteredIdReturnsEmpty() {
        Identifier id = Identifier.fromNamespaceAndPath("layerregistrytest", "never-registered");
        assertEquals(Optional.empty(), LayerRegistry.get(id));
    }

    @Test
    void reRegisteringSameIdOverwrites() {
        Identifier id = Identifier.fromNamespaceAndPath("layerregistrytest", "overwrite");
        LayerRegistry.register(new Layer(id, "First", 1));
        LayerRegistry.register(new Layer(id, "Second", 2));

        assertEquals(Optional.of(new Layer(id, "Second", 2)), LayerRegistry.get(id));
    }
}
