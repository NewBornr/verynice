package com.example.myclient.config;

import com.example.myclient.MyClient;
import com.example.myclient.module.BindMode;
import com.example.myclient.module.Module;
import com.example.myclient.setting.BoolSetting;
import com.example.myclient.setting.ModeSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Saves keybinds and settings to .minecraft/config/myclient.json. Modules are never auto-enabled on load. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("myclient.json");
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : MyClient.modules.all()) {
                JsonObject o = new JsonObject();
                o.addProperty("key", m.getKey());
                o.addProperty("bindMode", m.bindMode.name());
                JsonObject settings = new JsonObject();
                for (Setting<?> s : m.getSettings()) {
                    if (s instanceof BoolSetting b) settings.addProperty(s.name, b.get());
                    else if (s instanceof NumberSetting n) settings.addProperty(s.name, n.get());
                    else if (s instanceof ModeSetting mo) settings.addProperty(s.name, mo.get());
                }
                o.add("settings", settings);
                root.add(m.name, o);
            }
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        try {
            if (!Files.exists(file())) return;
            JsonObject root = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            for (Module m : MyClient.modules.all()) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("key")) m.setKey(o.get("key").getAsInt());
                if (o.has("bindMode")) {
                    try { m.bindMode = BindMode.valueOf(o.get("bindMode").getAsString()); } catch (Exception ignored) {}
                }
                if (!o.has("settings")) continue;
                JsonObject settings = o.getAsJsonObject("settings");
                for (Setting<?> s : m.getSettings()) {
                    if (!settings.has(s.name)) continue;
                    JsonElement el = settings.get(s.name);
                    if (s instanceof BoolSetting b) b.set(el.getAsBoolean());
                    else if (s instanceof NumberSetting n) n.set(el.getAsDouble());
                    else if (s instanceof ModeSetting mo) mo.set(el.getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
