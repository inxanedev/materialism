package com.materialism.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class UiDraw {
    private UiDraw() {
    }

    public static void roundedRect(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + height, color);
    }

    public static void panel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int radius
    ) {
        roundedRect(graphics, x + 2, y + 4, width, height, radius, UiTheme.SHADOW);
        roundedRect(graphics, x, y, width, height, radius, UiTheme.BORDER_DARK);
        roundedRect(graphics, x + 1, y + 1, width - 2, height - 2, Math.max(1, radius - 1), UiTheme.BORDER);
        roundedRect(graphics, x + 2, y + 2, width - 4, height - 4, Math.max(1, radius - 2), UiTheme.PANEL);
        graphics.fill(x + 14, y + 3, x + width - 14, y + 4, UiTheme.HIGHLIGHT);
        graphics.fill(x + 5, y + 5, x + 7, y + 7, UiTheme.ACCENT);
        graphics.fill(x + width - 7, y + 5, x + width - 5, y + 7, UiTheme.ACCENT);
        graphics.fill(x + 5, y + height - 7, x + 7, y + height - 5, UiTheme.ACCENT);
        graphics.fill(x + width - 7, y + height - 7, x + width - 5, y + height - 5, UiTheme.ACCENT);
    }

    public static void card(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int radius
    ) {
        roundedRect(graphics, x, y, width, height, radius, UiTheme.BORDER_DARK);
        roundedRect(graphics, x + 1, y + 1, width - 2, height - 2, Math.max(1, radius - 1), UiTheme.BORDER_SOFT);
        roundedRect(graphics, x + 2, y + 2, width - 4, height - 4, Math.max(1, radius - 2), UiTheme.PANEL_RAISED);
        graphics.fill(x + 8, y + 3, x + width - 8, y + 4, UiTheme.HIGHLIGHT);
    }

    public static void field(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean focused
    ) {
        int border = focused ? UiTheme.ACCENT : UiTheme.BORDER_SOFT;
        roundedRect(graphics, x, y, width, height, 6, UiTheme.BORDER_DARK);
        roundedRect(graphics, x + 1, y + 1, width - 2, height - 2, 5, border);
        roundedRect(graphics, x + 2, y + 2, width - 4, height - 4, 4, UiTheme.FIELD);
    }

    public static void divider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, UiTheme.BORDER_SOFT);
        graphics.fill(x + width / 2 - 2, y - 1, x + width / 2 + 2, y + 2, UiTheme.ACCENT);
    }

    public static void label(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int x,
            int y
    ) {
        graphics.text(font, text, x, y, UiTheme.DIM, false);
    }

    public static void rightText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int right,
            int y,
            int color
    ) {
        graphics.text(font, text, right - font.width(text), y, color, false);
    }

    public static void centeredText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int centerX,
            int y,
            int color
    ) {
        graphics.centeredText(font, text, centerX, y, color);
    }
}
