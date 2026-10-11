package com.example.myclient.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Draw2D {
    private Draw2D() {}

    /** Filled rounded rectangle (scanline approximation of the corners). */
    public static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        g.fill(x, y + r, x + w, y + h - r, color);
    }

    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int fill, int border) {
        rounded(g, x, y, w, h, r, border);
        rounded(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }
}
