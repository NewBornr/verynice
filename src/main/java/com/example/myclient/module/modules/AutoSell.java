package com.example.myclient.module.modules;

import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.setting.BoolSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public class AutoSell extends Module {
    public final NumberSetting interval = add(new NumberSetting("Interval", "Seconds between sell runs", 30, 5, 600, 5));
    public final NumberSetting clickDelay = add(new NumberSetting("Click Delay", "Ticks between item clicks", 3, 1, 20, 1));
    public final BoolSetting skipHotbar = add(new BoolSetting("Skip Hotbar", "Never sell items in your hotbar", true));

    private enum State { IDLE, WAIT_GUI, FILL, CLOSING }

    private State state = State.IDLE;
    private int timer, wait, clicks;

    public AutoSell() {
        super("Auto Sell", "Automatically sells items using /sell", Category.DONUT);
    }

    @Override
    protected void onEnable() { state = State.IDLE; timer = interval.intValue() * 20; }

    @Override
    protected void onDisable() { state = State.IDLE; }

    private boolean sellIndex(int i) {
        return (i >= 9 && i <= 35) || (!skipHotbar.get() && i >= 0 && i <= 8);
    }

    private boolean hasSellable() {
        for (int i = 0; i <= 35; i++) {
            if (sellIndex(i) && !mc.player.getInventory().getItem(i).isEmpty()) return true;
        }
        return false;
    }

    private Slot nextSellable() {
        AbstractContainerMenu menu = mc.player.containerMenu;
        for (Slot s : menu.slots) {
            if (s.container == mc.player.getInventory() && s.hasItem() && sellIndex(s.getContainerSlot())) return s;
        }
        return null;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) return;

        switch (state) {
            case IDLE -> {
                if (++timer < interval.intValue() * 20) return;
                if (Mc.screen() != null) return;
                if (!hasSellable()) { timer = 0; return; }
                mc.player.connection.sendCommand("sell");
                state = State.WAIT_GUI;
                wait = 0;
            }
            case WAIT_GUI -> {
                if (Mc.screen() instanceof ContainerScreen) {
                    state = State.FILL; wait = 0; clicks = 0;
                } else if (++wait > 60) {
                    state = State.IDLE; timer = 0; // no menu opened, try again next interval
                }
            }
            case FILL -> {
                if (!(Mc.screen() instanceof ContainerScreen)) { state = State.IDLE; timer = 0; return; }
                if (++wait < clickDelay.intValue()) return;
                wait = 0;

                Slot next = nextSellable();
                if (next == null || clicks >= 36) { state = State.CLOSING; wait = 0; return; }
                AbstractContainerMenu menu = mc.player.containerMenu;
                mc.gameMode.handleContainerInput(menu.containerId, next.index, 0, ContainerInput.QUICK_MOVE, mc.player);
                clicks++;
            }
            case CLOSING -> {
                if (++wait >= 5) {
                    mc.player.closeContainer();
                    state = State.IDLE; timer = 0;
                }
            }
        }
    }
}
