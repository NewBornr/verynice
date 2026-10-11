package com.example.myclient.gui;

import com.example.myclient.MyClient;
import com.example.myclient.module.BindMode;
import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.render.Draw2D;
import com.example.myclient.setting.BoolSetting;
import com.example.myclient.setting.ModeSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.setting.Setting;
import com.example.myclient.util.Mc;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    // ---- palette (dark navy + purple accent) ----
    private static final int WINDOW_BG   = 0xF00D0D21;
    private static final int SIDEBAR_BG  = 0xFF0A0A19;
    private static final int CARD_BG     = 0xFF0F0F28;
    private static final int ROW_BG      = 0xFF13132F;
    private static final int BORDER      = 0xFF23234A;
    private static final int ACCENT      = 0xFF7C5CFF;
    private static final int ACCENT_SOFT = 0xFF1E1948;
    private static final int ACCENT_EDGE = 0xFF3A2E8C;
    private static final int TEXT        = 0xFFFFFFFF;
    private static final int MUTED       = 0xFF8585A8;
    private static final int OFF         = 0xFF2A2A4F;
    private static final int DIVIDER     = 0xFF1C1C3C;
    private static final int INPUT_BG    = 0xFF0D0D24;

    private static final int ROW_H = 38, ROW_GAP = 6;
    private static final int HEADER_H = 38, SETTING_H = 28, BIND_H = 28;
    private static final String[] GENERAL = {"Settings", "Theme", "Configs", "Socials", "Keybinds"};

    private interface Click { void on(double mx, double my, int button); }
    private record Hit(int x, int y, int w, int h, boolean clipped, Click click) {}

    private final List<Hit> hits = new ArrayList<>();
    private Category selected = Category.DONUT;
    private String general = null;
    private Module open = null;          // module whose settings page is showing
    private String search = "";
    private boolean searchFocused;
    private double scroll, maxScroll;
    private Module listening;
    private NumberSetting dragging;
    private int dragX, dragW;
    private int mx, my;
    private int vx, vy, vw, vh;          // scrolling viewport

    public ClickGuiScreen() { super(Component.literal("MyClient")); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void removed() { com.example.myclient.config.Config.save(); }

    // =====================================================================
    // Rendering
    // =====================================================================
    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        mx = mouseX; my = mouseY;
        super.extractRenderState(ctx, mouseX, mouseY, delta); // draws the blurred background
        hits.clear();

        int w = Math.min(width - 30, 580);
        int h = Math.min(height - 30, 330);
        int x0 = (width - w) / 2, y0 = (height - h) / 2;
        box(ctx, x0, y0, w, h, 8, WINDOW_BG, BORDER);

        int sideX = x0 + 6, sideY = y0 + 6, sideW = 124, sideH = h - 12;
        drawSidebar(ctx, sideX, sideY, sideW, sideH);

        int cx = sideX + sideW + 12;
        drawContent(ctx, cx, y0 + 12, x0 + w - 12 - cx, h - 24);
    }

    private void drawSidebar(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        rounded(ctx, x, y, w, h, 6, SIDEBAR_BG);

        Component logo = Component.literal("My").withStyle(ChatFormatting.BOLD)
                .append(Component.literal("Client").setStyle(Style.EMPTY.withItalic(true).withColor(0xB9A6FF)));
        text(ctx, logo, x + 12, y + 12, TEXT, 1.2f);
        text(ctx, "ALPHA RELEASE 0.2.0", x + 12, y + 27, MUTED, 0.6f);

        text(ctx, "MODULES", x + 12, y + 44, MUTED, 0.65f);
        int iy = y + 56;
        for (Category c : Category.values()) {
            Category cat = c;
            int count = MyClient.modules.byCategory(c).size();
            sideItem(ctx, x + 6, iy, w - 12, c.label, String.valueOf(count),
                    general == null && c == selected,
                    (a, b, btn) -> { selected = cat; general = null; open = null; scroll = 0; });
            iy += 20;
        }

        iy += 6;
        text(ctx, "GENERAL", x + 12, iy, MUTED, 0.65f);
        iy += 12;
        for (String g : GENERAL) {
            sideItem(ctx, x + 6, iy, w - 12, g, null, g.equals(general),
                    (a, b, btn) -> { general = g; open = null; scroll = 0; });
            iy += 20;
        }
    }

    private void sideItem(GuiGraphicsExtractor ctx, int x, int y, int w, String label, String right, boolean active, Click click) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 18;
        if (active) box(ctx, x, y, w, 18, 5, ACCENT_SOFT, ACCENT_EDGE);
        else if (hover) rounded(ctx, x, y, w, 18, 5, 0x18FFFFFF);

        rounded(ctx, x + 7, y + 7, 4, 4, 2, active ? ACCENT : MUTED);
        text(ctx, label, x + 17, y + 5, active ? TEXT : MUTED, 1f);
        if (right != null) {
            float s = 0.7f;
            int tw = (int) (font.width(right) * s);
            text(ctx, right, x + w - 6 - tw, y + 6, MUTED, s);
        }
        hits.add(new Hit(x, y, w, 18, false, click));
    }

    private List<Module> visibleModules() {
        if (general != null) return List.of();
        if (!search.isEmpty()) {
            String q = search.toLowerCase();
            return MyClient.modules.all().stream().filter(m -> m.name.toLowerCase().contains(q)).toList();
        }
        return MyClient.modules.byCategory(selected);
    }

    private void drawContent(GuiGraphicsExtractor ctx, int cx, int cy, int cw, int ch) {
        List<Module> mods = visibleModules();
        int enabled = (int) mods.stream().filter(Module::isEnabled).count();

        // ---- header ----
        String word = general != null ? general : (search.isEmpty() ? selected.label : "Search");
        Component title = Component.literal(word).setStyle(Style.EMPTY.withItalic(true).withColor(0xB9A6FF))
                .append(Component.literal(general != null ? "" : " Modules").withStyle(ChatFormatting.BOLD));
        text(ctx, title, cx + 4, cy + 2, TEXT, 1.5f);
        String sub = general != null ? "Not built yet" : mods.size() + " modules  |  " + enabled + " enabled";
        text(ctx, sub, cx + 4, cy + 22, MUTED, 0.8f);

        // ---- search box ----
        int sx = cx + cw - 150, sy = cy + 2, sw = 150;
        box(ctx, sx, sy, sw, 20, 5, INPUT_BG, searchFocused ? ACCENT : BORDER);
        String shown = search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        if (search.isEmpty() && !searchFocused) text(ctx, "Search modules", sx + 8, sy + 6, MUTED, 1f);
        else text(ctx, shown, sx + 8, sy + 6, TEXT, 1f);
        hits.add(new Hit(sx, sy, sw, 20, false, (a, b, btn) -> { searchFocused = true; open = null; }));

        // ---- back chip (settings page only) ----
        int cardY = cy + 42;
        if (open != null && general == null) {
            int bx = cx + 4, by = cy + 40, bw = 56, bh = 16;
            boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
            box(ctx, bx, by, bw, bh, 8, hover ? ACCENT_SOFT : INPUT_BG, BORDER);
            text(ctx, "<  Back", bx + 11, by + 4, TEXT, 1f);
            hits.add(new Hit(bx, by, bw, bh, false, (a, b, btn) -> { open = null; scroll = 0; }));
            cardY = cy + 62;
        }
        int cardH = ch - (cardY - cy);

        // ---- card ----
        box(ctx, cx, cardY, cw, cardH, 6, CARD_BG, BORDER);
        vx = cx + 8; vy = cardY + 6; vw = cw - 16; vh = cardH - 12;

        if (general != null || mods.isEmpty()) {
            String msg = general != null ? ("Configs".equals(general) ? "Keybinds and settings save automatically when you close this menu" : "Coming soon") : "No modules here yet";
            int tw = font.width(msg);
            text(ctx, msg, cx + (cw - tw) / 2, cardY + cardH / 2 - 4, MUTED, 1f);
            return;
        }

        int total;
        if (open != null) total = HEADER_H + 1 + BIND_H + open.getSettings().size() * SETTING_H;
        else total = mods.size() * (ROW_H + ROW_GAP);
        maxScroll = Math.max(0, total - vh);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        ctx.enableScissor(cx + 2, cardY + 2, cx + cw - 2, cardY + cardH - 2);
        int y = vy - (int) scroll;

        if (open != null) {
            drawDetailHeader(ctx, open, y);
            y += HEADER_H;
            ctx.fill(vx + 2, y, vx + vw - 2, y + 1, DIVIDER);
            y += 1;
            drawBind(ctx, open, y);
            y += BIND_H;
            for (Setting<?> s : open.getSettings()) {
                if (visible(y, SETTING_H)) drawSetting(ctx, s, y);
                y += SETTING_H;
            }
        } else {
            for (Module m : mods) {
                if (visible(y, ROW_H)) drawRow(ctx, m, y);
                y += ROW_H + ROW_GAP;
            }
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int thumbH = Math.max(16, (int) (vh * (vh / (double) total)));
            int thumbY = vy + (int) ((vh - thumbH) * (scroll / maxScroll));
            rounded(ctx, cx + cw - 5, thumbY, 2, thumbH, 1, 0x667C5CFF);
        }
    }

    private boolean visible(int y, int h) { return y + h > vy && y < vy + vh; }

    // ---- list view: one card per module ----
    private void drawRow(GuiGraphicsExtractor ctx, Module m, int y) {
        boolean hover = mx >= vx && mx < vx + vw && my >= y && my < y + ROW_H && my >= vy && my < vy + vh;
        box(ctx, vx, y, vw, ROW_H, 6, hover ? 0xFF181838 : ROW_BG, BORDER);
        if (m.isEnabled()) rounded(ctx, vx, y + 6, 2, ROW_H - 12, 1, ACCENT);

        Component name = Component.literal(m.name).withStyle(ChatFormatting.BOLD);
        text(ctx, name, vx + 14, y + 9, TEXT, 1f);
        text(ctx, m.description, vx + 14, y + 22, MUTED, 0.75f);

        if (m.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
            String k = keyName(m.getKey());
            int kw = (int) (font.width(k) * 0.7f) + 8;
            int kx = vx + 14 + font.width(name) + 6;
            box(ctx, kx, y + 8, kw, 11, 3, INPUT_BG, BORDER);
            text(ctx, k, kx + 4, y + 10, MUTED, 0.7f);
        }

        text(ctx, ">", vx + vw - 52, y + 15, MUTED, 1f);
        toggle(ctx, vx + vw - 8 - 28, y + 12, m.isEnabled());

        hits.add(new Hit(vx, y, vw, ROW_H, true, (a, b, btn) -> { open = m; scroll = 0; }));
        hits.add(new Hit(vx + vw - 44, y + 6, 40, ROW_H - 12, true, (a, b, btn) -> m.toggle()));
    }

    // ---- settings page ----
    private void drawDetailHeader(GuiGraphicsExtractor ctx, Module m, int y) {
        text(ctx, Component.literal(m.name).withStyle(ChatFormatting.BOLD), vx + 8, y + 8, TEXT, 1.15f);
        text(ctx, m.description, vx + 8, y + 23, MUTED, 0.8f);
        toggle(ctx, vx + vw - 8 - 28, y + 11, m.isEnabled());
        hits.add(new Hit(vx + vw - 44, y + 4, 40, HEADER_H - 8, true, (a, b, btn) -> m.toggle()));
    }

    private void drawBind(GuiGraphicsExtractor ctx, Module m, int y) {
        text(ctx, Component.literal("Bind").withStyle(ChatFormatting.BOLD), vx + 14, y + 5, TEXT, 1f);
        text(ctx, "Keybind for " + m.name, vx + 14, y + 16, MUTED, 0.75f);

        int keyW = 64, keyX = vx + vw - 8 - keyW;
        boolean lis = listening == m;
        box(ctx, keyX, y + 6, keyW, 16, 4, lis ? ACCENT_SOFT : INPUT_BG, lis ? ACCENT : BORDER);
        String label = lis ? "Press key..." : (m.getKey() == GLFW.GLFW_KEY_UNKNOWN ? "None" : keyName(m.getKey()));
        text(ctx, label, keyX + (keyW - font.width(label)) / 2, y + 10, TEXT, 1f);
        hits.add(new Hit(keyX, y + 6, keyW, 16, true, (a, b, btn) -> {
            if (btn == 1) m.setKey(GLFW.GLFW_KEY_UNKNOWN); else listening = m;
        }));

        int togW = 44, togX = keyX - 6 - togW;
        int holdW = 34, holdX = togX - holdW;
        segment(ctx, togX, y + 6, togW, "Toggle", m.bindMode == BindMode.TOGGLE);
        segment(ctx, holdX, y + 6, holdW, "Hold", m.bindMode == BindMode.HOLD);
        hits.add(new Hit(togX, y + 6, togW, 16, true, (a, b, btn) -> m.bindMode = BindMode.TOGGLE));
        hits.add(new Hit(holdX, y + 6, holdW, 16, true, (a, b, btn) -> m.bindMode = BindMode.HOLD));
    }

    private void segment(GuiGraphicsExtractor ctx, int x, int y, int w, String label, boolean active) {
        if (active) rounded(ctx, x, y, w, 16, 4, ACCENT_SOFT);
        text(ctx, label, x + (w - font.width(label)) / 2, y + 4, active ? TEXT : MUTED, 1f);
    }

    private void drawSetting(GuiGraphicsExtractor ctx, Setting<?> s, int y) {
        boolean hover = mx >= vx && mx < vx + vw && my >= y && my < y + SETTING_H && my >= vy && my < vy + vh;
        if (hover) rounded(ctx, vx, y, vw, SETTING_H, 5, 0x0AFFFFFF);

        text(ctx, Component.literal(s.name).withStyle(ChatFormatting.BOLD), vx + 14, y + 5, TEXT, 1f);
        text(ctx, s.description, vx + 14, y + 16, MUTED, 0.75f);

        if (s instanceof BoolSetting b) {
            toggle(ctx, vx + vw - 8 - 28, y + 7, b.get());
            hits.add(new Hit(vx, y, vw, SETTING_H, true, (a, c, btn) -> b.toggle()));

        } else if (s instanceof NumberSetting n) {
            int boxW = 40, boxX = vx + vw - 8 - boxW;
            box(ctx, boxX, y + 6, boxW, 16, 4, INPUT_BG, BORDER);
            String val = n.display();
            text(ctx, val, boxX + (boxW - font.width(val)) / 2, y + 10, TEXT, 1f);

            int trackW = 100, trackX = boxX - 14 - trackW, trackY = y + SETTING_H / 2 - 1;
            rounded(ctx, trackX, trackY, trackW, 3, 1, OFF);
            int fillW = Math.max(3, (int) (trackW * n.fraction()));
            rounded(ctx, trackX, trackY, fillW, 3, 1, ACCENT);
            rounded(ctx, trackX + (int) (trackW * n.fraction()) - 4, trackY - 3, 8, 8, 4, 0xFFFFFFFF);

            hits.add(new Hit(trackX - 4, y + 4, trackW + 8, 20, true, (a, c, btn) -> {
                if (btn != 0) return;
                dragging = n; dragX = trackX; dragW = trackW;
                n.setFromFraction((a - trackX) / trackW);
            }));

        } else if (s instanceof ModeSetting mo) {
            int boxW = 84, boxX = vx + vw - 8 - boxW;
            box(ctx, boxX, y + 6, boxW, 16, 4, INPUT_BG, BORDER);
            text(ctx, mo.get(), boxX + 7, y + 10, TEXT, 1f);
            text(ctx, "v", boxX + boxW - 12, y + 10, MUTED, 1f);
            hits.add(new Hit(boxX, y + 6, boxW, 16, true, (a, c, btn) -> mo.cycle(btn == 1 ? -1 : 1)));
        }
    }

    // =====================================================================
    // Drawing helpers
    // =====================================================================
    private void toggle(GuiGraphicsExtractor ctx, int x, int y, boolean on) {
        rounded(ctx, x, y, 28, 14, 7, on ? ACCENT : OFF);
        rounded(ctx, on ? x + 16 : x + 2, y + 2, 10, 10, 5, on ? 0xFFFFFFFF : 0xFFA8A8C4);
    }

    private static void rounded(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int color) {
        Draw2D.rounded(ctx, x, y, w, h, r, color);
    }

    private static void box(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int fill, int border) {
        Draw2D.box(ctx, x, y, w, h, r, fill, border);
    }

    private void text(GuiGraphicsExtractor ctx, String s, int x, int y, int color, float scale) {
        text(ctx, Component.literal(s), x, y, color, scale);
    }

    private void text(GuiGraphicsExtractor ctx, Component t, int x, int y, int color, float scale) {
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(scale, scale);
        ctx.text(font, t, 0, 0, color, false);
        pose.popMatrix();
    }

    private static String keyName(int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }

    // =====================================================================
    // Input
    // =====================================================================
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (listening != null) { listening = null; return true; }
        searchFocused = false;

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mouseX < h.x() || mouseX >= h.x() + h.w() || mouseY < h.y() || mouseY >= h.y() + h.h()) continue;
            if (h.clipped() && (mouseY < vy || mouseY >= vy + vh)) continue;
            h.click().on(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            dragging.setFromFraction((event.x() - dragX) / dragW);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Mth.clamp(scroll - vertical * 20, 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (listening != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) listening.setKey(GLFW.GLFW_KEY_UNKNOWN);
            else if (keyCode != GLFW.GLFW_KEY_ESCAPE) listening.setKey(keyCode);
            listening = null;
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                scroll = 0;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && open != null) { open = null; scroll = 0; return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (searchFocused && cp >= 32 && search.length() < 24) {
            search += new String(Character.toChars(cp));
            scroll = 0;
            return true;
        }
        return super.charTyped(event);
    }
}
