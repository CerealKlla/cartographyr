package com.github.cerealklla.cartographyr.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * Persistent top-right "Location: <name>" overlay (design doc Section 5.8/demo notification),
 * replacing the original chat-message notification 2026-09-24 per user request -- see
 * decisions.md. Renders nothing until the first {@code LocationPayload} arrives, then always
 * shows the most recently received name (never blanks again on its own).
 */
public final class LocationOverlay implements GuiLayer {

    private static final int MARGIN = 6;
    private static final int COLOR = 0xFFFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        String name = ClientLocationState.get();
        if (name == null || name.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        String text = "Location: " + name;
        int x = guiGraphics.guiWidth() - font.width(text) - MARGIN;
        int y = MARGIN;
        guiGraphics.text(font, text, x, y, COLOR);
    }
}
