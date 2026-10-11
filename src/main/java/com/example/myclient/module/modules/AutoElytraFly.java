package com.example.myclient.module.modules;

import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.setting.ModeSetting;
import com.example.myclient.setting.NumberSetting;
import com.example.myclient.util.Mc;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Needs an elytra already equipped and firework rockets in your hotbar. */
public class AutoElytraFly extends Module {
    public final NumberSetting altitude = add(new NumberSetting("Altitude", "Y level to cruise at", 150, -64, 320, 1));
    public final ModeSetting heading = add(new ModeSetting("Heading", "Direction to fly", "Current", "Current", "North", "South", "East", "West"));
    public final NumberSetting minSpeed = add(new NumberSetting("Min Speed", "Use a firework when slower than this (blocks/s)", 30, 10, 60, 1));
    public final NumberSetting boostInterval = add(new NumberSetting("Boost Interval", "Minimum ticks between fireworks", 40, 10, 200, 5));

    private float yawLock;
    private int boostTimer;
    private boolean warnedNoFireworks;

    public AutoElytraFly() {
        super("Auto Elytra Fly", "Automatically flies your elytra to search for bases", Category.DONUT);
    }

    @Override
    protected void onEnable() {
        warnedNoFireworks = false;
        boostTimer = 0;
        if (mc.player == null) return;
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            say("Equip an elytra first");
            setEnabled(false);
            return;
        }
        yawLock = switch (heading.get()) {
            case "North" -> 180f;
            case "South" -> 0f;
            case "East" -> -90f;
            case "West" -> 90f;
            default -> mc.player.getYRot();
        };
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) { setEnabled(false); return; }

        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA)) { say("Elytra no longer equipped"); setEnabled(false); return; }
        if (chest.getMaxDamage() - chest.getDamageValue() < 15) { say("Elytra almost broken, stopping"); setEnabled(false); return; }

        // Take off
        if (!mc.player.isFallFlying()) {
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            } else if (mc.player.getDeltaMovement().y < 0) {
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,
                        ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
            return;
        }

        // Hold heading and altitude
        mc.player.setYRot(yawLock);
        double dy = altitude.get() - mc.player.getY();
        float desired = (float) Mth.clamp(-dy * 0.6, -45.0, 25.0);
        mc.player.setXRot(Mth.lerp(0.15f, mc.player.getXRot(), desired));

        // Firework boost when slow
        boostTimer++;
        Vec3 v = mc.player.getDeltaMovement();
        double speedPerSecond = Math.hypot(v.x, v.z) * 20.0;
        if (boostTimer >= boostInterval.intValue() && speedPerSecond < minSpeed.get()) {
            if (useFirework()) boostTimer = 0;
        }
    }

    private boolean useFirework() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) { slot = i; break; }
        }
        if (slot < 0) {
            if (!warnedNoFireworks) { say("No fireworks in your hotbar"); warnedNoFireworks = true; }
            return false;
        }
        int previous = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(previous);
        return true;
    }

    private void say(String msg) { Mc.say("\u00a76[Auto Elytra Fly] \u00a7f" + msg); }
}
