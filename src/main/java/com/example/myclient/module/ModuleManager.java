package com.example.myclient.module;

import com.example.myclient.module.modules.AncientDebrisFinder;
import com.example.myclient.module.modules.AutoBaseHunt;
import com.example.myclient.module.modules.AutoDigDown;
import com.example.myclient.module.modules.AutoElytraFly;
import com.example.myclient.module.modules.AutoRtp;
import com.example.myclient.module.modules.AutoSell;
import com.example.myclient.module.modules.BaseEsp;
import com.example.myclient.module.modules.BaseFinder;
import com.example.myclient.module.modules.BlockEsp;
import com.example.myclient.module.modules.DonutWheel;
import com.example.myclient.module.modules.FarmFpsOptimizer;
import com.example.myclient.module.modules.Freecam;
import com.example.myclient.render.Render3D;
import com.example.myclient.util.Mc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Set<Module> held = new HashSet<>();

    public void init() {
        Freecam freecam = new Freecam();
        freecam.setKey(GLFW.GLFW_KEY_V);
        add(freecam);

        add(new BlockEsp());

        DonutWheel wheel = new DonutWheel();
        wheel.setKey(GLFW.GLFW_KEY_G);
        add(wheel);

        BaseFinder baseFinder = new BaseFinder();
        baseFinder.setKey(GLFW.GLFW_KEY_B);
        add(baseFinder);

        add(new BaseEsp());
        add(new AncientDebrisFinder());
        add(new AutoSell());
        add(new AutoRtp());
        add(new AutoDigDown());
        add(new AutoElytraFly());
        add(new AutoBaseHunt());
        add(new FarmFpsOptimizer());
    }

    public void add(Module module) { modules.add(module); }
    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category c) {
        return modules.stream().filter(m -> m.category == c).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) {
            if (type.isInstance(m)) return (T) m;
        }
        return null;
    }

    public void tick(Minecraft client) {
        // Keybinds only work with no screen open (so typing in chat doesn't toggle things)
        if (Mc.screen() == null) {
            long window = GLFW.glfwGetCurrentContext();
            for (Module m : modules) {
                int key = m.getKey();
                if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
                boolean down = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
                boolean was = held.contains(m);
                if (down && !was) {
                    held.add(m);
                    if (m.bindMode == BindMode.HOLD) m.setEnabled(true); else m.toggle();
                } else if (!down && was) {
                    held.remove(m);
                    if (m.bindMode == BindMode.HOLD) m.setEnabled(false);
                }
            }
        }

        for (Module m : modules) {
            if (m.isEnabled()) m.onTick();
        }
    }

    public void collect3D(Render3D.Collector out) {
        for (Module m : modules) {
            if (m.isEnabled()) m.onRender3D(out);
        }
    }

    public void renderHud(GuiGraphicsExtractor g) {
        for (Module m : modules) {
            if (m.isEnabled()) m.onRenderHud(g);
        }
    }
}
