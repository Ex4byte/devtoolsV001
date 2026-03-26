package com.example.devtools.features.movement;

import com.example.devtools.core.IDevFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlockReachFeature implements IDevFeature {

    public static final BlockReachFeature INSTANCE = new BlockReachFeature();

    private static final UUID REACH_UUID  = UUID.fromString("b5d48e2c-3f4e-5a6b-9d0c-1e2f3a4b5c6d");
    private static final UUID ATTACK_UUID = UUID.fromString("c6e59f3d-4050-6b7c-ae1d-2f3a4b5c6d7e");

    private boolean enabled     = false;
    public  float   extraReach  = 3.5f;
    public  float   extraAttack = 3.0f;

    private BlockReachFeature() {}

    @Override public String getId()          { return "block_reach"; }
    @Override public String getDisplayName() { return "Block Reach"; }
    @Override public boolean isEnabled()     { return enabled; }

    @Override
    public void setEnabled(boolean e) {
        if (e == this.enabled) return;
        this.enabled = e;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) applyModifiers(player, e);
    }

    private void applyModifiers(LocalPlayer player, boolean apply) {
        AttributeInstance reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reach != null) {
            reach.removeModifier(REACH_UUID);
            if (apply) reach.addTransientModifier(new AttributeModifier(
                REACH_UUID, "devtools_reach", extraReach, AttributeModifier.Operation.ADDITION));
        }
        AttributeInstance attack = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attack != null) {
            attack.removeModifier(ATTACK_UUID);
            if (apply) attack.addTransientModifier(new AttributeModifier(
                ATTACK_UUID, "devtools_attack", extraAttack, AttributeModifier.Operation.ADDITION));
        }
    }

    public void setExtraReach(float r)  { extraReach  = Math.max(0, Math.min(r, 50f)); if (enabled) { setEnabled(false); setEnabled(true); } }
    public void setExtraAttack(float a) { extraAttack = Math.max(0, Math.min(a, 50f)); if (enabled) { setEnabled(false); setEnabled(true); } }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!INSTANCE.enabled) return;
        if (event.phase != TickEvent.Phase.START) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        AttributeInstance reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reach != null && reach.getModifier(REACH_UUID) == null) {
            INSTANCE.applyModifiers(player, true);
        }
    }
}
