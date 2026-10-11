package com.example.myclient.mixin;

import com.example.myclient.MyClient;
import com.example.myclient.module.modules.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    // Redirect mouse look to the freecam instead of the player
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
    private void myclient$turn(double yaw, double pitch, CallbackInfo ci) {
        if ((Object) this != Minecraft.getInstance().player) return;
        Freecam fc = MyClient.modules == null ? null : MyClient.modules.get(Freecam.class);
        if (fc != null && fc.isEnabled()) {
            fc.look(yaw, pitch);
            ci.cancel();
        }
    }
}
