package com.example.myclient.module.modules;

import com.example.myclient.MyClient;
import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.setting.BoolSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import net.minecraft.world.entity.player.Player;

public class AutoRtp extends Module {
    public final NumberSetting interval = add(new NumberSetting("Interval", "Seconds between /rtp commands", 10, 3, 120, 1));
    public final BoolSetting stopOnPlayer = add(new BoolSetting("Stop On Player", "Stop when another player is in render distance", true));
    public final BoolSetting stopOnBase = add(new BoolSetting("Stop On Base", "Stop when Base Finder or Base ESP flags something", true));

    private int timer;
    private int sinceRtp;

    public AutoRtp() {
        super("Auto Rtp", "Automatically searches for players or bases using RTP", Category.DONUT);
    }

    @Override
    protected void onEnable() { timer = interval.intValue() * 20; sinceRtp = 1000; }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) { setEnabled(false); return; }
        sinceRtp++;

        if (stopOnPlayer.get()) {
            for (Player p : mc.level.players()) {
                if (p == mc.player || p.isSpectator()) continue;
                Mc.say("\u00a76[Auto Rtp] \u00a7fStopped: player " + p.getName().getString() + " nearby");
                setEnabled(false);
                return;
            }
        }

        if (stopOnBase.get() && sinceRtp > 100 && baseFound()) {
            Mc.say("\u00a76[Auto Rtp] \u00a7fStopped: base detected");
            setEnabled(false);
            return;
        }

        if (++timer >= interval.intValue() * 20 && Mc.screen() == null) {
            mc.player.connection.sendCommand("rtp");
            timer = 0;
            sinceRtp = 0;
        }
    }

    private boolean baseFound() {
        BaseFinder bf = MyClient.modules.get(BaseFinder.class);
        BaseEsp be = MyClient.modules.get(BaseEsp.class);
        return (bf != null && bf.isEnabled() && bf.hasFlagged()) || (be != null && be.isEnabled() && be.hasGroups());
    }
}
