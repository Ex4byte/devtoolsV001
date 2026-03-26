package com.example.devtools.features.camera;

import com.example.devtools.core.IDevFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Freecam – rein clientseitig.
 *
 * Bewegung:  WASD (horizontal), Space (auf), Shift (ab), Sprint (4× Speed)
 * Rotation:  Folgt Spieler-Yaw/Pitch (Maus-Input normal).
 * Freeze:    Spieler-Position wird END jedes Ticks zurückgesetzt.
 *
 * MixinCamera überschreibt Camera.setup() @TAIL und setzt camPos.
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FreecamFeature implements IDevFeature {

    public static final FreecamFeature INSTANCE = new FreecamFeature();

    private boolean enabled = false;

    /** Kameraposition (world space) – wird von MixinCamera gelesen. */
    public volatile Vec3 camPos = Vec3.ZERO;

    public float speedMultiplier = 1.0f;

    private static final float BASE  = 0.25f;
    private static final float BOOST = 4.0f;

    private Vec3 frozenPos = null;

    private FreecamFeature() {}

    @Override public String getId()          { return "freecam"; }
    @Override public String getDisplayName() { return "Freecam"; }
    @Override public boolean isEnabled()     { return enabled; }

    @Override
    public void setEnabled(boolean e) {
        if (e == this.enabled) return;
        this.enabled = e;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (e) {
            camPos    = mc.player.getEyePosition(1.0f);
            frozenPos = mc.player.position();
        } else {
            frozenPos = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        FreecamFeature fc = INSTANCE;
        if (!fc.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // START: Kamera bewegen
        if (event.phase == TickEvent.Phase.START) {
            float speed = BASE * fc.speedMultiplier;
            Options o   = mc.options;
            if (o.keySprint.isDown()) speed *= BOOST;

            float yawRad   = (float) Math.toRadians(mc.player.getYRot());
            float pitchRad = (float) Math.toRadians(mc.player.getXRot());

            Vec3 fwd = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                 Math.cos(yawRad)  * Math.cos(pitchRad)
            );
            Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));

            float fw   = key(o.keyUp)    - key(o.keyDown);
            float str  = key(o.keyRight) - key(o.keyLeft);
            float vert = key(o.keyJump)  - key(o.keyShift);

            fc.camPos = fc.camPos
                .add(fwd.scale(fw    * speed))
                .add(right.scale(str * speed))
                .add(0, vert * speed, 0);
        }

        // END: Spieler einfrieren
        if (event.phase == TickEvent.Phase.END && fc.frozenPos != null) {
            mc.player.setPos(fc.frozenPos.x, fc.frozenPos.y, fc.frozenPos.z);
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.fallDistance = 0f;
        }
    }

    private static float key(net.minecraft.client.KeyMapping k) {
        return k.isDown() ? 1f : 0f;
    }
}
