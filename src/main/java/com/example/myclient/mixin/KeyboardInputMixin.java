package com.example.myclient.mixin;

import com.example.myclient.MyClient;
import com.example.myclient.module.modules.Freecam;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends ClientInput {
    // Stops your real player from walking around while the camera is detached
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void myclient$tick(CallbackInfo ci) {
        Freecam fc = MyClient.modules == null ? null : MyClient.modules.get(Freecam.class);
        if (fc != null && fc.isEnabled()) {
            this.keyPresses = Input.EMPTY;
            this.moveVector = Vec2.ZERO;
        }
    }
}
