package com.example.myclient.module.modules;

import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.setting.BoolSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;

public class FarmFpsOptimizer extends Module {
    public final BoolSetting xpOrbs = add(new BoolSetting("XP Orbs", "Hides experience orbs", true));
    public final BoolSetting fallingBlocks = add(new BoolSetting("Falling Blocks", "Hides falling sand, gravel and concrete powder", true));
    public final BoolSetting tnt = add(new BoolSetting("TNT", "Hides activated TNT", true));
    public final BoolSetting explosions = add(new BoolSetting("Explosions", "Hides explosion particles", true));

    public FarmFpsOptimizer() {
        super("Farm FPS Optimizer", "Reduces lag around farms", Category.DONUT);
    }

    public boolean hides(Entity e) {
        return (xpOrbs.get() && e instanceof ExperienceOrb)
                || (fallingBlocks.get() && e instanceof FallingBlockEntity)
                || (tnt.get() && e instanceof PrimedTnt);
    }
}
