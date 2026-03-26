package com.example.devtools.mixin;

import com.example.devtools.features.movement.NoClipFeature;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinNoClip {

    @Inject(
        method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devtools$bypassCollision(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (NoClipFeature.INSTANCE.isEnabled()) {
            cir.setReturnValue(movement);
        }
    }
}
