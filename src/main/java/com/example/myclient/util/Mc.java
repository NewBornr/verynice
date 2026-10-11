package com.example.myclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Small helpers so version-specific calls live in one place. */
public final class Mc {
    private Mc() {}

    /** Currently open screen, or null. (26.2: Minecraft.screen moved to Minecraft.gui.screen()) */
    public static Screen screen() { return Minecraft.getInstance().gui.screen(); }

    /** Opens a screen. (26.2: setScreen was renamed setScreenAndShow) */
    public static void setScreen(Screen screen) { Minecraft.getInstance().setScreenAndShow(screen); }

    public static void say(String message) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal(message));
    }

    /** Packs chunk coordinates into one long (own helpers: ChunkPos packing methods were renamed). */
    public static long chunkKey(int cx, int cz) { return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL); }
    public static int chunkKeyX(long key) { return (int) (key >> 32); }
    public static int chunkKeyZ(long key) { return (int) key; }

    /** Loaded client chunk or null. */
    public static LevelChunk chunk(int cx, int cz) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        ChunkAccess c = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
        return c instanceof LevelChunk lc ? lc : null;
    }
}
