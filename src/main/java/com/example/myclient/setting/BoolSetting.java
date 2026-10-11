package com.example.myclient.setting;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean value) {
        super(name, description, value);
    }

    public void toggle() { value = !value; }
}
