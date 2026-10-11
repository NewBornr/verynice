package com.example.myclient.gui;

import com.example.myclient.render.Draw2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Radial quick-command menu. Edit ACTIONS to change what the wheel does. */
public class DonutWheelScreen extends Screen {
    // {label, command without the slash}
    private static final String[][] ACTIONS = {
            {"RTP", "rtp"},
            {"Spawn", "spawn"},
            {"Home", "home"},
            {"Sell", "sell"},
            {"Shop", "shop"},
            {"Auction", "ah"}
    };

    private static final int BTN_W = 70, BTN_H = 24, RADIUS = 78;

    public DonutWheelScreen() { super(Component.literal("Donut Wheel")); }

    @Override
    public boolean isPauseScreen() { return false; }

    private int btnX(int i) {
        double a = Math.toRadians(i * (360.0 / ACTIONS.length) - 90);
        return (int) (width / 2.0 + Math.cos(a) * RADIUS) - BTN_W / 2;
    }

    private int btnY(int i) {
        double a = Math.toRadians(i * (360.0 / ACTIONS.length) - 90);
        return (int) (height / 2.0 + Math.sin(a) * RADIUS) - BTN_H / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta); // blurred background

        int cx = width / 2, cy = height / 2;
        Draw2D.box(g, cx - 26, cy - 26, 52, 52, 26, 0xF00D0D21, 0xFF3A2E8C);
        String title = "Donut";
        g.text(font, title, cx - font.width(title) / 2, cy - 4, 0xFFB9A6FF, false);

        for (int i = 0; i < ACTIONS.length; i++) {
            int x = btnX(i), y = btnY(i);
            boolean hover = mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H;
            Draw2D.box(g, x, y, BTN_W, BTN_H, 8, hover ? 0xFF1E1948 : 0xF00D0D21, hover ? 0xFF7C5CFF : 0xFF23234A);
            String label = ACTIONS[i][0];
            g.text(font, label, x + (BTN_W - font.width(label)) / 2, y + 8,
                    hover ? 0xFFFFFFFF : 0xFFB0B0CC, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        for (int i = 0; i < ACTIONS.length; i++) {
            int x = btnX(i), y = btnY(i);
            if (mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.connection.sendCommand(ACTIONS[i][1]);
                }
                onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
