package com.example.myclient.module.modules;

import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.render.Draw2D;
import com.example.myclient.render.Render3D;
import com.example.myclient.setting.BoolSetting;
import com.example.myclient.setting.ModeSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Scores every loaded chunk for signs of player activity and marks the ones above a threshold. */
public class BaseFinder extends Module {
    public final BoolSetting storageSignal = add(new BoolSetting("Storage", "Count chests, barrels and shulker boxes", true));
    public final BoolSetting machinerySignal = add(new BoolSetting("Machinery", "Count hoppers, dispensers/droppers and furnaces", true));
    public final BoolSetting lightSignal = add(new BoolSetting("Lights", "Count torches, lanterns and other light sources", true));
    public final BoolSetting playerBlockSignal = add(new BoolSetting("Player Blocks", "Count planks, cobblestone, bricks, glass, ladders and doors", true));
    public final NumberSetting maxY = add(new NumberSetting("Max Y", "Only count signs at or below this Y level", 0, -64, 320, 1));
    public final NumberSetting minScore = add(new NumberSetting("Min Score", "Minimum score needed to mark a chunk", 10, 1, 100, 1));
    public final NumberSetting range = add(new NumberSetting("Range", "Scan distance in chunks", 8, 1, 16, 1));
    public final ModeSetting alerts = add(new ModeSetting("Alerts", "Off, chat message, or chat message with sound", "Chat + Sound", "Off", "Chat", "Chat + Sound"));
    public final NumberSetting alertCooldown = add(new NumberSetting("Alert Cooldown", "Delay before alerting for the same chunk again (seconds)", 30, 1, 300, 1));
    public final NumberSetting maxPanels = add(new NumberSetting("Max Panels", "HUD entries to show (0 hides the list)", 5, 0, 12, 1));
    public final BoolSetting render = add(new BoolSetting("Render", "Draw a marker over each flagged chunk", true));
    public final BoolSetting exportSnapshot = add(new BoolSetting("Export Snapshot",
            "Saves current detections to myclient-basefinder-snapshot.json, then switches off", false));

    private static class Info {
        int cx, cz, storage, machinery, lights, player;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        double raw, dist;
        int pct;
        String kind = "Unknown";

        void track(int y) { minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
        double centerX() { return cx * 16 + 8; }
        double centerZ() { return cz * 16 + 8; }
    }

    private final Map<Long, Info> cache = new HashMap<>();
    private final Map<Long, Integer> lastScan = new HashMap<>();
    private final Map<Long, Integer> lastAlert = new HashMap<>();
    private final List<Info> flagged = new ArrayList<>();
    private int tick;

    public BaseFinder() {
        super("Base Finder", "Marks chunks with signs of player activity", Category.DONUT);
    }

    public boolean hasFlagged() { return !flagged.isEmpty(); }

    @Override
    protected void onEnable() { cache.clear(); lastScan.clear(); lastAlert.clear(); flagged.clear(); tick = 0; }

    @Override
    protected void onDisable() { flagged.clear(); }

    // ---------------------------------------------------------------- block classes
    private static boolean isLight(BlockState s) {
        return s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH)
                || s.is(Blocks.SOUL_WALL_TORCH) || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN)
                || s.is(Blocks.GLOWSTONE) || s.is(Blocks.SEA_LANTERN) || s.is(Blocks.SHROOMLIGHT)
                || s.is(Blocks.REDSTONE_LAMP);
    }

    private static boolean isPlayerBlock(BlockState s) {
        return s.is(Blocks.COBBLESTONE) || s.is(Blocks.STONE_BRICKS) || s.is(Blocks.BRICKS)
                || s.is(Blocks.GLASS) || s.is(Blocks.LADDER) || s.is(Blocks.SCAFFOLDING)
                || s.is(BlockTags.PLANKS) || s.is(BlockTags.DOORS);
    }

    // ---------------------------------------------------------------- scanning
    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) return;
        tick++;

        int r = range.intValue();
        ChunkPos pc = mc.player.chunkPosition();
        int budget = 3;

        for (int dx = -r; dx <= r && budget > 0; dx++) {
            for (int dz = -r; dz <= r && budget > 0; dz++) {
                int cx = pc.x() + dx, cz = pc.z() + dz;
                long key = Mc.chunkKey(cx, cz);
                Integer last = lastScan.get(key);
                if (last != null && tick - last < 200) continue;
                scanChunk(cx, cz, key);
                lastScan.put(key, tick);
                budget--;
            }
        }

        if (tick % 20 == 0) refresh();

        if (tick % 40 == 0) {
            Iterator<Long> it = cache.keySet().iterator();
            while (it.hasNext()) {
                long k = it.next();
                if (Math.abs(Mc.chunkKeyX(k) - pc.x()) > r + 2 || Math.abs(Mc.chunkKeyZ(k) - pc.z()) > r + 2) {
                    it.remove();
                    lastScan.remove(k);
                }
            }
        }

        if (exportSnapshot.get()) {
            export();
            exportSnapshot.set(false);
        }
    }

    private void scanChunk(int cx, int cz, long key) {
        LevelChunk chunk = Mc.chunk(cx, cz);
        if (chunk == null) { cache.remove(key); return; }

        Info info = new Info();
        info.cx = cx; info.cz = cz;
        int yLimit = maxY.intValue();

        for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
            BlockPos p = be.getBlockPos();
            if (p.getY() > yLimit) continue;
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity) {
                info.storage++;
            } else if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity
                    || be instanceof AbstractFurnaceBlockEntity) {
                info.machinery++;
            } else {
                continue;
            }
            info.track(p.getY());
        }

        Predicate<BlockState> wanted = s -> isLight(s) || isPlayerBlock(s);
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir()) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            if (baseY > yLimit || !sec.maybeHas(wanted)) continue;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    int wy = baseY + y;
                    if (wy > yLimit) continue;
                    for (int z = 0; z < 16; z++) {
                        BlockState s = sec.getBlockState(x, y, z);
                        if (isLight(s)) { info.lights++; info.track(wy); }
                        else if (isPlayerBlock(s)) { info.player++; }
                    }
                }
            }
        }
        cache.put(key, info);
    }

    private void refresh() {
        flagged.clear();
        Vec3 pp = mc.player.position();
        double threshold = minScore.get();

        for (Info i : cache.values()) {
            double raw = 0;
            if (storageSignal.get()) raw += i.storage * 3.0;
            if (machinerySignal.get()) raw += i.machinery * 4.0;
            if (lightSignal.get()) raw += i.lights;
            if (playerBlockSignal.get()) raw += i.player * 0.25;
            i.raw = raw;
            if (raw < threshold) continue;

            i.pct = (int) Math.min(99, raw / (threshold * 2.0) * 100.0);
            double st = i.storage * 3.0, ma = i.machinery * 4.0;
            if (st > 0 && st >= raw * 0.4 && st >= ma) i.kind = "Stash";
            else if (ma > 0 && ma >= raw * 0.4) i.kind = "Redstone";
            else i.kind = "Unknown";
            i.dist = Math.hypot(i.centerX() - pp.x, i.centerZ() - pp.z);
            flagged.add(i);
        }
        flagged.sort(Comparator.comparingDouble(i -> i.dist));

        String mode = alerts.get();
        if (mode.equals("Off")) return;
        int cooldownTicks = alertCooldown.intValue() * 20;
        for (Info i : flagged) {
            long key = Mc.chunkKey(i.cx, i.cz);
            Integer last = lastAlert.get(key);
            if (last != null && tick - last < cooldownTicks) continue;
            lastAlert.put(key, tick);
            Mc.say("\u00a76[BaseFinder] \u00a7f" + i.kind + " " + i.pct + "% at "
                    + (int) i.centerX() + ", " + (int) i.centerZ() + " (Y " + yRange(i) + ") - "
                    + (int) i.dist + "m " + direction(i.centerX() - pp.x, i.centerZ() - pp.z));
            if (mode.equals("Chat + Sound")) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    private static String yRange(Info i) {
        if (i.minY == Integer.MAX_VALUE) return "?";
        return i.minY == i.maxY ? String.valueOf(i.minY) : i.minY + ".." + i.maxY;
    }

    private static String direction(double dx, double dz) {
        double ang = Math.toDegrees(Math.atan2(dx, -dz));
        if (ang < 0) ang += 360;
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return names[(int) Math.round(ang / 45.0) % 8];
    }

    private void export() {
        try {
            JsonArray arr = new JsonArray();
            for (Info i : flagged) {
                JsonObject o = new JsonObject();
                o.addProperty("chunkX", i.cx);
                o.addProperty("chunkZ", i.cz);
                o.addProperty("kind", i.kind);
                o.addProperty("percent", i.pct);
                o.addProperty("storage", i.storage);
                o.addProperty("machinery", i.machinery);
                o.addProperty("lights", i.lights);
                o.addProperty("playerBlocks", i.player);
                o.addProperty("y", yRange(i));
                arr.add(o);
            }
            JsonObject root = new JsonObject();
            root.add("detections", arr);
            Files.writeString(mc.gameDirectory.toPath().resolve("myclient-basefinder-snapshot.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Mc.say("\u00a76[BaseFinder] \u00a7fSnapshot saved (" + arr.size() + " chunks)");
        } catch (Exception e) {
            Mc.say("\u00a76[BaseFinder] \u00a7cCould not save snapshot");
        }
    }

    // ---------------------------------------------------------------- drawing
    @Override
    public void onRender3D(Render3D.Collector out) {
        if (!render.get()) return;
        for (Info i : flagged) {
            double y0 = i.minY == Integer.MAX_VALUE ? maxY.get() : i.minY;
            AABB b = new AABB(i.cx * 16, y0, i.cz * 16, i.cx * 16 + 16, y0 + 1, i.cz * 16 + 16);
            int color = switch (i.kind) {
                case "Stash" -> 0xFFFFC857;
                case "Redstone" -> 0xFFFF5A5A;
                default -> 0xFF6C8CFF;
            };
            out.box(b, color, true);
        }
    }

    @Override
    public void onRenderHud(GuiGraphicsExtractor g) {
        int shown = Math.min(maxPanels.intValue(), flagged.size());
        if (maxPanels.intValue() == 0 || mc.player == null) return;

        Vec3 pp = mc.player.position();
        int x = 8, y = 8, w = 170;
        int h = 22 + shown * 30 + (flagged.size() > shown ? 14 : 0) + 4;
        Draw2D.box(g, x, y, w, h, 6, 0xE00D0D21, 0xFF23234A);
        g.text(mc.font, "BaseFinder", x + 8, y + 7, 0xFF7C9CFF, false);
        String n = flagged.size() + " nearby";
        g.text(mc.font, n, x + w - 8 - mc.font.width(n), y + 7, 0xFF8585A8, false);

        int ey = y + 22;
        for (int k = 0; k < shown; k++) {
            Info i = flagged.get(k);
            int barColor = i.pct >= 95 ? 0xFF4D7CFF : 0xFF4DDB6A;
            g.text(mc.font, i.kind, x + 8, ey, 0xFFFFFFFF, false);
            String pct = i.pct + "%";
            g.text(mc.font, pct, x + w - 8 - mc.font.width(pct), ey, barColor, false);
            String info = direction(i.centerX() - pp.x, i.centerZ() - pp.z) + " " + (int) i.dist + "m / Y " + yRange(i);
            g.text(mc.font, info, x + 8, ey + 10, 0xFF8585A8, false);
            g.fill(x + 8, ey + 21, x + w - 8, ey + 23, 0xFF1C1C3C);
            g.fill(x + 8, ey + 21, x + 8 + (int) ((w - 16) * i.pct / 100.0), ey + 23, barColor);
            ey += 30;
        }
        if (flagged.size() > shown) {
            g.text(mc.font, "+" + (flagged.size() - shown) + " more nearby", x + 8, ey, 0xFF8585A8, false);
        }
    }
}
