package com.example.myclient.module.modules;

import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.render.Render3D;
import com.example.myclient.setting.BoolSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class BlockEsp extends Module {
    public final BoolSetting ancientDebris = add(new BoolSetting("Ancient Debris", "Highlight ancient debris", true));
    public final BoolSetting diamond = add(new BoolSetting("Diamond Ore", "Highlight diamond ore", true));
    public final BoolSetting emerald = add(new BoolSetting("Emerald Ore", "Highlight emerald ore", false));
    public final BoolSetting gold = add(new BoolSetting("Gold Ore", "Highlight gold ore", false));
    public final BoolSetting spawners = add(new BoolSetting("Spawners", "Highlight mob spawners", false));
    public final NumberSetting range = add(new NumberSetting("Range", "Scan distance in chunks", 6, 1, 16, 1));
    public final BoolSetting fill = add(new BoolSetting("Fill", "Also draw a translucent fill inside each box", true));

    private record Found(BlockPos pos, int color) {}

    private final Map<Long, List<Found>> cache = new HashMap<>();
    private final Map<Long, Integer> lastScan = new HashMap<>();
    private int tick;

    public BlockEsp() {
        this("Block ESP", "Highlights selected blocks through walls", Category.VISUAL);
    }

    protected BlockEsp(String name, String description, Category category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() { cache.clear(); lastScan.clear(); tick = 0; }

    @Override
    protected void onDisable() { cache.clear(); lastScan.clear(); }

    protected int colorOf(BlockState s) {
        if (ancientDebris.get() && s.is(Blocks.ANCIENT_DEBRIS)) return 0xFFB5651D;
        if (diamond.get() && (s.is(Blocks.DIAMOND_ORE) || s.is(Blocks.DEEPSLATE_DIAMOND_ORE))) return 0xFF3DE3E3;
        if (emerald.get() && (s.is(Blocks.EMERALD_ORE) || s.is(Blocks.DEEPSLATE_EMERALD_ORE))) return 0xFF2ECC40;
        if (gold.get() && (s.is(Blocks.GOLD_ORE) || s.is(Blocks.DEEPSLATE_GOLD_ORE) || s.is(Blocks.NETHER_GOLD_ORE))) return 0xFFFFD700;
        if (spawners.get() && s.is(Blocks.SPAWNER)) return 0xFFAA33FF;
        return 0;
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) return;
        tick++;

        int r = range.intValue();
        ChunkPos pc = mc.player.chunkPosition();
        int budget = 4; // chunks scanned per tick, keeps the game smooth

        for (int dx = -r; dx <= r && budget > 0; dx++) {
            for (int dz = -r; dz <= r && budget > 0; dz++) {
                int cx = pc.x() + dx, cz = pc.z() + dz;
                long key = Mc.chunkKey(cx, cz);
                Integer last = lastScan.get(key);
                if (last != null && tick - last < 200) continue; // rescan every ~10s
                scanChunk(cx, cz, key);
                lastScan.put(key, tick);
                budget--;
            }
        }

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
    }

    private void scanChunk(int cx, int cz, long key) {
        LevelChunk chunk = Mc.chunk(cx, cz);
        if (chunk == null) { cache.remove(key); return; }

        List<Found> out = new ArrayList<>();
        Predicate<BlockState> wanted = s -> colorOf(s) != 0;
        LevelChunkSection[] sections = chunk.getSections();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(wanted)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int c = colorOf(sec.getBlockState(x, y, z));
                        if (c != 0) out.add(new Found(new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z), c));
                    }
                }
            }
        }
        cache.put(key, out);
    }

    @Override
    public void onRender3D(Render3D.Collector out) {
        if (mc.player == null) return;
        double maxDist = range.intValue() * 16.0 + 16;
        double maxSq = maxDist * maxDist;
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        boolean f = fill.get();
        for (List<Found> list : cache.values()) {
            for (Found found : list) {
                BlockPos p = found.pos();
                double dx = p.getX() + 0.5 - px, dy = p.getY() + 0.5 - py, dz = p.getZ() + 0.5 - pz;
                if (dx * dx + dy * dy + dz * dz > maxSq) continue;
                out.box(new AABB(p).inflate(0.002), found.color(), f);
            }
        }
    }
}
