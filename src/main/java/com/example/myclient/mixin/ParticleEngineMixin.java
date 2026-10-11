package com.example.myclient.mixin;

import com.example.myclient.MyClient;
import com.example.myclient.module.modules.FarmFpsOptimizer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void myclient$createParticle(ParticleOptions options, double x, double y, double z,
                                         double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        FarmFpsOptimizer f = MyClient.modules == null ? null : MyClient.modules.get(FarmFpsOptimizer.class);
        if (f == null || !f.isEnabled() || !f.explosions.get()) return;
        if (options.getType() == ParticleTypes.EXPLOSION || options.getType() == ParticleTypes.EXPLOSION_EMITTER) {
            cir.setReturnValue(null);
        }
    }
}
