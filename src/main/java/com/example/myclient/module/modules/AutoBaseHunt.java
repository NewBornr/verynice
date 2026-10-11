package com.example.myclient.module.modules;

import com.example.myclient.MyClient;
import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import net.minecraft.world.phys.Vec3;

/** Loop: /rtp -> dig down -> wait and scan -> repeat, until Base Finder or Base ESP flags something. */
public class AutoBaseHunt extends Module {
    public final NumberSetting digToY = add(new NumberSetting("Dig To Y", "Y level to dig down to after each RTP", -40, -64, 320, 1));
    public final NumberSetting scanTime = add(new NumberSetting("Scan Time", "Seconds to wait at the bottom before the next RTP", 15, 5, 120, 1));

    private enum State { RTP, WAIT_TP, DIG, SCAN }

    private State state = State.RTP;
    private Vec3 startPos = Vec3.ZERO;
    private int wait, sinceRtp;

    public AutoBaseHunt() {
        super("Auto Base Hunt", "Automatically RTPs, digs down and scans for bases", Category.DONUT);
    }

    @Override
    protected void onEnable() {
        state = State.RTP; wait = 0; sinceRtp = 1000;
        BaseFinder bf = MyClient.modules.get(BaseFinder.class);
        BaseEsp be = MyClient.modules.get(BaseEsp.class);
        if (bf != null) bf.setEnabled(true);
        if (be != null) be.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        AutoDigDown dig = MyClient.modules.get(AutoDigDown.class);
        if (dig != null) dig.setEnabled(false);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) { setEnabled(false); return; }
        sinceRtp++;

        if (sinceRtp > 100 && baseFound()) {
            Mc.say("\u00a76[Auto Base Hunt] \u00a7fBase detected, hunt stopped");
            setEnabled(false);
            return;
        }

        AutoDigDown dig = MyClient.modules.get(AutoDigDown.class);
        switch (state) {
            case RTP -> {
                if (Mc.screen() != null) return;
                startPos = mc.player.position();
                mc.player.connection.sendCommand("rtp");
                sinceRtp = 0; wait = 0;
                state = State.WAIT_TP;
            }
            case WAIT_TP -> {
                if (mc.player.position().distanceTo(startPos) > 64) {
                    if (dig != null) {
                        dig.targetY.set(digToY.get());
                        dig.setEnabled(true);
                    }
                    wait = 0;
                    state = State.DIG;
                } else if (++wait > 400) {
                    state = State.RTP; // teleport never happened, try again
                }
            }
            case DIG -> {
                if (dig == null || !dig.isEnabled()) { wait = 0; state = State.SCAN; }
            }
            case SCAN -> {
                if (++wait >= scanTime.intValue() * 20) state = State.RTP;
            }
        }
    }

    private boolean baseFound() {
        BaseFinder bf = MyClient.modules.get(BaseFinder.class);
        BaseEsp be = MyClient.modules.get(BaseEsp.class);
        return (bf != null && bf.isEnabled() && bf.hasFlagged()) || (be != null && be.isEnabled() && be.hasGroups());
    }
}
