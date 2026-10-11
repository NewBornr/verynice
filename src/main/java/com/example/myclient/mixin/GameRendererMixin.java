package com.example.myclient.mixin;

import com.example.myclient.render.Render3D;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // Frees the ESP vertex buffer when the renderer shuts down
    @Inject(method = "close", at = @At("RETURN"), require = 0)
    private void myclient$close(CallbackInfo ci) {
        Render3D.close();
    }
}
