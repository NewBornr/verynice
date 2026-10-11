package com.example.myclient.module.modules;

import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;

public class AutoDigDown extends Module {
    public final NumberSetting targetY = add(new NumberSetting("Target Y", "Stops digging at this Y level", -50, -64, 320, 1));

    public AutoDigDown() {
        super("Auto Dig Down", "Automatically digs down to a set Y level", Category.DONUT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) { setEnabled(false); return; }
        if (Mc.screen() != null) return;

        if (mc.player.getY() <= targetY.get() + 0.5) {
            Mc.say("\u00a76[Auto Dig Down] \u00a7fReached Y " + targetY.intValue());
            setEnabled(false);
            return;
        }

        BlockPos feet = mc.player.blockPosition();
        // Safety: never dig toward lava
        for (int i = 1; i <= 3; i++) {
            if (mc.level.getFluidState(feet.below(i)).is(FluidTags.LAVA)) {
                Mc.say("\u00a76[Auto Dig Down] \u00a7fLava below, stopped");
                setEnabled(false);
                return;
            }
        }

        mc.player.setXRot(90f);
        BlockPos below = feet.below();
        if (!mc.level.getBlockState(below).isAir() && mc.level.getFluidState(below).isEmpty()) {
            mc.gameMode.continueDestroyBlock(below, Direction.UP);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
