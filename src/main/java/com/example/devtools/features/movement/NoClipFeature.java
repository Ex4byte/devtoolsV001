package com.example.devtools.features.movement;

import com.example.devtools.core.IDevFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NoClipFeature implements IDevFeature {

    public static final NoClipFeature INSTANCE = new NoClipFeature();

    private boolean enabled = false;
    public float speedLimit = 0.4f;

    private NoClipFeature() {}

    @Override public String getId()          { return "noclip"; }
    @Override public String getDisplayName() { return "NoClip"; }
    @Override public boolean isEnabled()     { return enabled; }

    @Override
    public void setEnabled(boolean e) {
        if (e == this.enabled) return;
        this.enabled = e;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !e) {
            player.noPhysics = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!INSTANCE.enabled) return;
        if (event.phase != TickEvent.Phase.START) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        player.noPhysics = true;
        player.fallDistance = 0f;
        player.resetFallDistance();

        Vec3 delta = player.getDeltaMovement();
        double len = delta.length();
        if (len > INSTANCE.speedLimit) {
            player.setDeltaMovement(delta.normalize().scale(INSTANCE.speedLimit));
        }
    }
}
