package com.example.myclient.module.modules;

import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.setting.BoolSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Freecam extends Module {
    public double x, y, z;
    public float yaw, pitch;

    public final NumberSetting speed = add(new NumberSetting("Speed", "Camera speed in blocks per second", 12, 1, 60, 1));
    public final NumberSetting sprintBoost = add(new NumberSetting("Sprint Boost", "Speed multiplier while holding sprint", 3, 1, 10, 0.5));
    public final BoolSetting showPlayer = add(new BoolSetting("Show Player", "Render your player model while detached", true));

    private long lastNanos;

    public Freecam() {
        super("Freecam", "Detach the camera from your player", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) { setEnabled(false); return; }
        Vec3 p = mc.player.getEyePosition();
        x = p.x; y = p.y; z = p.z;
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
        lastNanos = System.nanoTime();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) setEnabled(false);
    }

    /** Called once per rendered frame from CameraMixin, so movement is smooth. */
    public void updateCamera() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
        lastNanos = now;
        if (Mc.screen() != null) return;

        double forward = axis(mc.options.keyUp.isDown(), mc.options.keyDown.isDown());
        double strafe  = axis(mc.options.keyRight.isDown(), mc.options.keyLeft.isDown());
        double vertical = axis(mc.options.keyJump.isDown(), mc.options.keyShift.isDown());

        double step = speed.get() * (mc.options.keySprint.isDown() ? sprintBoost.get() : 1.0) * dt;
        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad), cos = Math.cos(rad);

        x += (-sin * forward - cos * strafe) * step;
        z += ( cos * forward - sin * strafe) * step;
        y += vertical * step;
    }

    public void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1 : 0) - (negative ? 1 : 0);
    }
}
