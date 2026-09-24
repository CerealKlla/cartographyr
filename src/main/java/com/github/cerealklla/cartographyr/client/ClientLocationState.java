package com.github.cerealklla.cartographyr.client;

import org.jspecify.annotations.Nullable;

/**
 * Client-side holder for the current player's location name, updated whenever a
 * {@code LocationPayload} arrives (design doc Section 5.8/demo notification). No client-only
 * imports, so it's harmless if classloaded on a dedicated server -- it just never gets written to
 * there, since the server never receives this payload.
 */
public final class ClientLocationState {

    @Nullable
    private static volatile String currentName;

    private ClientLocationState() {
    }

    public static void set(String name) {
        currentName = name;
    }

    @Nullable
    public static String get() {
        return currentName;
    }
}
