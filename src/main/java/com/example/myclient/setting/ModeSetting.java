package com.example.myclient.setting;

import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;
    private int index;

    public ModeSetting(String name, String description, String defaultMode, String... modes) {
        super(name, description, defaultMode);
        this.modes = List.of(modes);
        this.index = Math.max(0, this.modes.indexOf(defaultMode));
        this.value = this.modes.get(index);
    }

    @Override
    public void set(String v) {
        int i = modes.indexOf(v);
        if (i >= 0) { index = i; value = v; }
    }

    public void cycle(int direction) {
        index = Math.floorMod(index + direction, modes.size());
        value = modes.get(index);
    }
}
