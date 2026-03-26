package com.example.devtools.features.movement;

import com.example.devtools.core.IDevFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SpeedHackFeature implements IDevFeature {

    public static final SpeedHackFeature INSTANCE = new SpeedHackFeature();

    private static final UUID   SPEED_UUID      = UUID.fromString("a4c37f1b-2e3d-4f5a-8c9b-0d1e2f3a4b5c");
    private static final String SPEED_ATTR_NAME = "devtools_speed";

    private boolean enabled    = false;
    public  float   multiplier = 2.0f;
    public  float   serverSafeLimit = 0.5f;

    private SpeedHackFeature() {}

    @Override public String getId()          { return "speedhack"; }
    @Override public String getDisplayName() { return "Speed Hack"; }
    @Override public boolean isEnabled()     { return enabled; }

    @Override
    public void setEnabled(boolean e) {
        if (e == this.enabled) return;
        this.enabled = e;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(SPEED_UUID);
        if (e) {
            attr.addTransientModifier(new AttributeModifier(
                SPEED_UUID, SPEED_ATTR_NAME,
                multiplier - 1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    public void setMultiplier(float m) {
        this.multiplier = Math.max(1.0f, Math.min(m, 20.0f));
        if (enabled) { setEnabled(false); setEnabled(true); }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!INSTANCE.enabled) return;
        if (event.phase != TickEvent.Phase.START) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Modifier nach Respawn refreshen
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && attr.getModifier(SPEED_UUID) == null) {
            attr.addTransientModifier(new AttributeModifier(
                SPEED_UUID, SPEED_ATTR_NAME,
                INSTANCE.multiplier - 1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }

        Vec3 delta = player.getDeltaMovement();
        double hLen = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (hLen > 0.001) {
            double newH  = Math.min(hLen * INSTANCE.multiplier, INSTANCE.serverSafeLimit);
            double ratio = newH / hLen;
            player.setDeltaMovement(delta.x * ratio, delta.y, delta.z * ratio);
        }
        if (player.isOnGround()) player.fallDistance = 0f;
    }
}
