package com.example.devtools.features.camera;

import com.example.devtools.core.IDevFeature;
import com.example.devtools.util.AbilitiesAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Creative Fly für Survival – rein clientseitig.
 * flyingSpeed via AbilitiesAccessor (Reflection) – robust gegen Obfuskierung.
 * ⚠ SERVER: allow-flight=true in server.properties nötig.
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FlyFeature implements IDevFeature {

    public static final FlyFeature INSTANCE = new FlyFeature();

    private float   speed   = 0.1f;
    private boolean enabled = false;

    private boolean savedMayfly = false;
    private boolean savedFlying = false;
    private float   savedSpeed  = 0.05f;

    private FlyFeature() {}

    @Override public String getId()          { return "fly"; }
    @Override public String getDisplayName() { return "Creative Fly"; }
    @Override public boolean isEnabled()     { return enabled; }

    @Override
    public void setEnabled(boolean e) {
        if (e == this.enabled) return;
        this.enabled = e;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (e) {
            savedMayfly = player.getAbilities().mayfly;
            savedFlying = player.getAbilities().flying;
            savedSpeed  = AbilitiesAccessor.getFlyingSpeed(player);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            AbilitiesAccessor.setFlyingSpeed(player, Mth.clamp(speed, 0.01f, 1.0f));
            player.onUpdateAbilities();
        } else {
            player.getAbilities().mayfly = savedMayfly;
            player.getAbilities().flying = savedFlying;
            AbilitiesAccessor.setFlyingSpeed(player, savedSpeed);
            player.onUpdateAbilities();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!INSTANCE.enabled) return;
        if (event.phase != TickEvent.Phase.START) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        AbilitiesAccessor.setFlyingSpeed(player, Mth.clamp(INSTANCE.speed, 0.01f, 1.0f));
        player.fallDistance = 0f;
    }

    public void setSpeed(float s) {
        this.speed = Mth.clamp(s, 0.01f, 1.0f);
        LocalPlayer player = Minecraft.getInstance().player;
        if (enabled && player != null)
            AbilitiesAccessor.setFlyingSpeed(player, this.speed);
    }

    public float getSpeed() { return speed; }
}
