package com.example.devtools.mixin;

import com.example.devtools.features.camera.FreecamFeature;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Überschreibt Camera-Position am Ende von setup().
 * Rotation (Yaw/Pitch) bleibt unberührt – folgt dem Spieler-Look.
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(
        method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
        at = @At("TAIL")
    )
    private void devtools$applyFreecamPosition(
            BlockGetter level, Entity entity,
            boolean detached, boolean mirrorMode, float partialTick,
            CallbackInfo ci) {
        if (FreecamFeature.INSTANCE.isEnabled()) {
            Vec3 pos = FreecamFeature.INSTANCE.camPos;
            setPosition(pos.x, pos.y, pos.z);
        }
    }
}
