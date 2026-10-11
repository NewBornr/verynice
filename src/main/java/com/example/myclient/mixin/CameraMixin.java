package com.example.myclient.mixin;

import com.example.myclient.MyClient;
import com.example.myclient.module.modules.Freecam;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void myclient$setup(CallbackInfo ci) {
        Freecam fc = MyClient.modules == null ? null : MyClient.modules.get(Freecam.class);
        if (fc != null && fc.isEnabled()) {
            fc.updateCamera();
            setPosition(fc.x, fc.y, fc.z);
            setRotation(fc.yaw, fc.pitch);
        }
    }

    // Makes your own player model render while in Freecam
    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true, require = 0)
    private void myclient$detached(CallbackInfoReturnable<Boolean> cir) {
        Freecam fc = MyClient.modules == null ? null : MyClient.modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && fc.showPlayer.get()) cir.setReturnValue(true);
    }
}
