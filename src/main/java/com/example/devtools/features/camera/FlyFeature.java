package com.example.devtools.features.camera;

import com.example.devtools.core.IDevFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FlyFeature implements IDevFeature {

    public static final FlyFeature INSTANCE = new FlyFeature();

    private boolean enabled        = false;
    private float   speed          = 0.1f;
    public  boolean serverSafeMode = true;

    private static final int GROUND_PACKET_INTERVAL = 20;
    private int groundPacketTimer = 0;

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
        if (!e) {
            player.noPhysics = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        FlyFeature fly = INSTANCE;
        if (!fly.enabled) return;
        if (event.phase != TickEvent.Phase.START) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Minecraft mc = Minecraft.getInstance();

        player.noPhysics = true;
        player.fallDistance = 0f;

        float actualSpeed = Mth.clamp(fly.speed, 0.01f, 1.0f);
        if (mc.options.keySprint.isDown()) actualSpeed *= 4.0f;

        float yawRad   = (float) Math.toRadians(player.getYRot());
        float pitchRad = (float) Math.toRadians(player.getXRot());

        Vec3 fwd = new Vec3(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
             Math.cos(yawRad)  * Math.cos(pitchRad)
        );
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));

        float fw   = (mc.options.keyUp.isDown()    ? 1 : 0) - (mc.options.keyDown.isDown()  ? 1 : 0);
        float str  = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown()  ? 1 : 0);
        float vert = (mc.options.keyJump.isDown()  ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);

        Vec3 movement = fwd.scale(fw * actualSpeed)
            .add(right.scale(str * actualSpeed))
            .add(0, vert * actualSpeed, 0);

        player.setDeltaMovement(movement.lengthSqr() > 0 ? movement : Vec3.ZERO);

        // Gefälschte onGround=true Pakete verhindern den Flying-Kick auf Vanilla/Paper
        if (fly.serverSafeMode) {
            fly.groundPacketTimer++;
            if (fly.groundPacketTimer >= GROUND_PACKET_INTERVAL) {
                fly.groundPacketTimer = 0;
                if (player.connection != null) {
                    player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                        player.getX(), player.getY(), player.getZ(),
                        player.getYRot(), player.getXRot(),
                        true
                    ));
                }
            }
        }
    }

    public void setSpeed(float s) { this.speed = Mth.clamp(s, 0.01f, 1.0f); }
    public float getSpeed()       { return speed; }
}
