package com.example.myclient.module;

import com.example.myclient.render.Render3D;
import com.example.myclient.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    public BindMode bindMode = BindMode.TOGGLE;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = GLFW.GLFW_KEY_UNKNOWN;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() { return settings; }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (enabled) onEnable(); else onDisable();
    }

    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    /** Add boxes to draw through walls. Called during the extraction phase while enabled. */
    public void onRender3D(Render3D.Collector out) {}
    /** 2D HUD drawing. Only called while enabled. */
    public void onRenderHud(GuiGraphicsExtractor g) {}
}
