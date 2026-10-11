package com.example.myclient;

import com.example.myclient.config.Config;
import com.example.myclient.gui.ClickGuiScreen;
import com.example.myclient.module.ModuleManager;
import com.example.myclient.render.Render3D;
import com.example.myclient.util.Mc;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class MyClient implements ClientModInitializer {
    public static ModuleManager modules;
    private static boolean guiKeyHeld;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        modules.init();
        Config.load();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            modules.tick(client);

            // Right Shift opens the menu
            boolean down = GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (down && !guiKeyHeld && Mc.screen() == null && client.player != null) {
                Mc.setScreen(new ClickGuiScreen());
            }
            guiKeyHeld = down;
        });

        // ESP boxes: collect during extraction, draw after translucent terrain
        LevelExtractionEvents.END_EXTRACTION.register(Render3D::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(Render3D::draw);

        // HUD panels
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("myclient", "hud"),
                (graphics, deltaTracker) -> modules.renderHud(graphics));
    }
}
