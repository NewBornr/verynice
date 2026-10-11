package com.example.myclient.mixin;

import com.example.myclient.MyClient;
import com.example.myclient.module.modules.FarmFpsOptimizer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void myclient$shouldRender(Entity entity, Frustum frustum, double x, double y, double z,
                                       CallbackInfoReturnable<Boolean> cir) {
        FarmFpsOptimizer f = MyClient.modules == null ? null : MyClient.modules.get(FarmFpsOptimizer.class);
        if (f != null && f.isEnabled() && f.hides(entity)) cir.setReturnValue(false);
    }
}
