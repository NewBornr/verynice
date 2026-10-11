package com.example.myclient.module.modules;

import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.render.Draw2D;
import com.example.myclient.render.Render3D;
import com.example.myclient.setting.BoolSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseEsp extends Module {
    public final NumberSetting minBlocks = add(new NumberSetting("Min Blocks",
            "Minimum storage/machinery score to flag a group. Chests count as 3, hoppers as 4", 24, 1, 200, 1));
    public final NumberSetting clusterRadius = add(new NumberSetting("Cluster Radius",
            "Blocks closer together than this are grouped (blocks)", 24, 4, 64, 1));
    public final NumberSetting range = add(new NumberSetting("Range",
            "Maximum distance to detect groups (blocks)", 308, 32, 512, 4));
    public final NumberSetting maxY = add(new NumberSetting("Max Y",
            "Only count blocks at or below this Y level", 0, -64, 320, 1));
    public final NumberSetting maxPanels = add(new NumberSetting("Max Panels",
            "HUD panels for the nearest groups (0 hides them)", 8, 0, 16, 1));
    public final BoolSetting box = add(new BoolSetting("Box", "Outline each detected group", true));
    public final BoolSetting notify = add(new BoolSetting("Notify", "Chat message and sound once per new group", true));

    private static class Group {
        AABB box; int score; Vec3 center; double dist; long key;
    }

    private final List<Group> groups = new ArrayList<>();
    private final Set<Long> announced = new HashSet<>();
    private int timer;

    public BaseEsp() {
        super("Base ESP", "Highlights storage and machinery clusters", Category.DONUT);
    }

    @Override
    protected void onEnable() { groups.clear(); announced.clear(); timer = 0; }

    @Override
    protected void onDisable() { groups.clear(); }

    public boolean hasGroups() { return !groups.isEmpty(); }

    private static int scoreOf(BlockEntity be) {
        if (be instanceof HopperBlockEntity) return 4;
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity
                || be instanceof ShulkerBoxBlockEntity || be instanceof DispenserBlockEntity) return 3;
        if (be instanceof AbstractFurnaceBlockEntity) return 2;
        return 0;
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) return;
        if (timer++ % 20 != 0) return;

        double rng = range.get();
        double rngSq = rng * rng;
        int chunkR = (int) (rng / 16) + 1;
        ChunkPos pc = mc.player.chunkPosition();
        Vec3 pp = mc.player.position();

        // 1. collect scored block entities
        List<int[]> pts = new ArrayList<>();
        outer:
        for (int dx = -chunkR; dx <= chunkR; dx++) {
            for (int dz = -chunkR; dz <= chunkR; dz++) {
                LevelChunk chunk = Mc.chunk(pc.x() + dx, pc.z() + dz);
                if (chunk == null) continue;
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    int s = scoreOf(be);
                    if (s <= 0) continue;
                    BlockPos p = be.getBlockPos();
                    if (p.getY() > maxY.intValue()) continue;
                    double ddx = p.getX() + 0.5 - pp.x, ddy = p.getY() + 0.5 - pp.y, ddz = p.getZ() + 0.5 - pp.z;
                    if (ddx * ddx + ddy * ddy + ddz * ddz > rngSq) continue;
                    pts.add(new int[]{p.getX(), p.getY(), p.getZ(), s});
                    if (pts.size() >= 6000) break outer;
                }
            }
        }

        // 2. cluster with union-find over a spatial hash
        int n = pts.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        int r = clusterRadius.intValue();
        Map<Long, List<Integer>> cells = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int[] p = pts.get(i);
            cells.computeIfAbsent(cellKey(Math.floorDiv(p[0], r), Math.floorDiv(p[1], r), Math.floorDiv(p[2], r)),
                    k -> new ArrayList<>()).add(i);
        }
        for (int i = 0; i < n; i++) {
            int[] p = pts.get(i);
            int cx = Math.floorDiv(p[0], r), cy = Math.floorDiv(p[1], r), cz = Math.floorDiv(p[2], r);
            for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++) for (int oz = -1; oz <= 1; oz++) {
                List<Integer> bucket = cells.get(cellKey(cx + ox, cy + oy, cz + oz));
                if (bucket == null) continue;
                for (int j : bucket) {
                    if (j <= i) continue;
                    int[] q = pts.get(j);
                    if (Math.abs(p[0] - q[0]) <= r && Math.abs(p[1] - q[1]) <= r && Math.abs(p[2] - q[2]) <= r) {
                        union(parent, i, j);
                    }
                }
            }
        }

        // 3. build groups
        Map<Integer, int[]> acc = new HashMap<>(); // root -> {score, minX, minY, minZ, maxX, maxY, maxZ}
        for (int i = 0; i < n; i++) {
            int[] p = pts.get(i);
            int root = find(parent, i);
            int[] a = acc.get(root);
            if (a == null) {
                acc.put(root, new int[]{p[3], p[0], p[1], p[2], p[0], p[1], p[2]});
            } else {
                a[0] += p[3];
                a[1] = Math.min(a[1], p[0]); a[2] = Math.min(a[2], p[1]); a[3] = Math.min(a[3], p[2]);
                a[4] = Math.max(a[4], p[0]); a[5] = Math.max(a[5], p[1]); a[6] = Math.max(a[6], p[2]);
            }
        }

        List<Group> found = new ArrayList<>();
        for (int[] a : acc.values()) {
            if (a[0] < minBlocks.intValue()) continue;
            Group g = new Group();
            g.score = a[0];
            g.box = new AABB(a[1], a[2], a[3], a[4] + 1, a[5] + 1, a[6] + 1).inflate(0.1);
            g.center = g.box.getCenter();
            g.dist = g.center.distanceTo(pp);
            g.key = (((long) (a[1] >> 4)) << 32) ^ ((a[3] >> 4) & 0xFFFFFFFFL);
            found.add(g);
        }
        found.sort(Comparator.comparingDouble(g -> g.dist));

        for (Group g : found) {
            if (announced.add(g.key) && notify.get()) {
                Mc.say("\u00a76[BaseESP] \u00a7fGroup (score " + g.score + ") near "
                        + (int) g.center.x + ", " + (int) g.center.y + ", " + (int) g.center.z
                        + " - " + (int) g.dist + "m away");
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }

        groups.clear();
        groups.addAll(found);
    }

    private static long cellKey(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) { parent[i] = parent[parent[i]]; i = parent[i]; }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a), rb = find(parent, b);
        if (ra != rb) parent[ra] = rb;
    }

    @Override
    public void onRender3D(Render3D.Collector out) {
        if (!box.get()) return;
        for (Group g : groups) out.box(g.box, 0xFFFF8A3D, true);
    }

    @Override
    public void onRenderHud(GuiGraphicsExtractor g) {
        int count = Math.min(maxPanels.intValue(), groups.size());
        int w = 170, h = 26;
        int x = g.guiWidth() - w - 8, y = 8;
        for (int i = 0; i < count; i++) {
            Group grp = groups.get(i);
            Draw2D.box(g, x, y, w, h, 5, 0xE00D0D21, 0xFF23234A);
            g.text(mc.font, "Base group  " + (int) grp.dist + "m", x + 7, y + 4, 0xFFFFFFFF, false);
            g.text(mc.font, "Score " + grp.score + "   Y " + (int) grp.center.y, x + 7, y + 14, 0xFF8585A8, false);
            y += h + 4;
        }
    }
}
