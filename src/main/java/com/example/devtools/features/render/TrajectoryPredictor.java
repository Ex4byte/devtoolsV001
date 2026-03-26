package com.example.devtools.features.render;

import com.example.devtools.core.IDevFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TrajectoryPredictor implements IDevFeature {

    public static final TrajectoryPredictor INSTANCE = new TrajectoryPredictor();

    public enum ProjectileType {
        ARROW    (0.99f, 0.05f, 3.0f),
        THROWABLE(0.99f, 0.03f, 1.5f),
        TRIDENT  (0.99f, 0.05f, 2.5f);

        final float drag, gravity, baseSpeed;
        ProjectileType(float drag, float gravity, float baseSpeed) {
            this.drag = drag; this.gravity = gravity; this.baseSpeed = baseSpeed;
        }
    }

    private static final int MAX_TICKS = 120;

    private boolean       enabled    = false;
    public  ProjectileType type       = ProjectileType.ARROW;
    public  float          charge     = 1.0f;
    public  boolean        showImpact = true;

    private TrajectoryPredictor() {}

    @Override public String getId()          { return "trajectory"; }
    @Override public String getDisplayName() { return "Trajectory Predictor"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!INSTANCE.enabled) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        float  pt     = event.getPartialTick();
        Vec3   cam    = event.getCamera().getPosition();
        Level  level  = mc.level;

        Vec3 startPos = player.getEyePosition(pt);
        Vec3 look     = player.getLookAngle();
        float speed   = INSTANCE.type.baseSpeed * Math.max(0.1f, INSTANCE.charge);
        Vec3 velocity = look.scale(speed);

        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());

        Vec3 prev = startPos, impact = null;

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            velocity = new Vec3(
                velocity.x * INSTANCE.type.drag,
                velocity.y * INSTANCE.type.drag - INSTANCE.type.gravity,
                velocity.z * INSTANCE.type.drag
            );
            Vec3 next = prev.add(velocity);

            HitResult hit = level.clip(new ClipContext(
                prev, next,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            ));

            float progress = (float) tick / MAX_TICKS;
            float r = Math.min(1f, progress * 2f);
            float g = Math.min(1f, (1f - progress) * 2f);

            if (hit.getType() == HitResult.Type.BLOCK) {
                impact = hit.getLocation();
                drawLine(ps, vc, cam, prev, impact, r, g, 0f, 0.9f);
                break;
            }
            drawLine(ps, vc, cam, prev, next, r, g, 0f, 0.8f);
            prev = next;
        }

        if (INSTANCE.showImpact && impact != null) drawImpactCross(ps, vc, cam, impact);

        buf.endBatch(RenderType.lines());
    }

    private static void drawLine(PoseStack ps, VertexConsumer vc, Vec3 cam,
            Vec3 from, Vec3 to, float r, float g, float b, float a) {
        Matrix4f mat = ps.last().pose();
        Matrix3f nm  = ps.last().normal();
        float x0 = (float)(from.x-cam.x), y0 = (float)(from.y-cam.y), z0 = (float)(from.z-cam.z);
        float x1 = (float)(to.x  -cam.x), y1 = (float)(to.y  -cam.y), z1 = (float)(to.z  -cam.z);
        float dx = x1-x0, dy = y1-y0, dz = z1-z0;
        float len = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (len < 1e-6f) return;
        dx/=len; dy/=len; dz/=len;
        vc.vertex(mat,x0,y0,z0).color(r,g,b,a).normal(nm,dx,dy,dz).endVertex();
        vc.vertex(mat,x1,y1,z1).color(r,g,b,a).normal(nm,dx,dy,dz).endVertex();
    }

    private static void drawImpactCross(PoseStack ps, VertexConsumer vc, Vec3 cam, Vec3 pos) {
        float s = 0.15f;
        float x = (float)(pos.x-cam.x), y = (float)(pos.y-cam.y), z = (float)(pos.z-cam.z);
        Matrix4f mat = ps.last().pose();
        Matrix3f nm  = ps.last().normal();
        vc.vertex(mat,x-s,y,z).color(1f,0f,0f,1f).normal(nm,1,0,0).endVertex();
        vc.vertex(mat,x+s,y,z).color(1f,0f,0f,1f).normal(nm,1,0,0).endVertex();
        vc.vertex(mat,x,y-s,z).color(1f,0f,0f,1f).normal(nm,0,1,0).endVertex();
        vc.vertex(mat,x,y+s,z).color(1f,0f,0f,1f).normal(nm,0,1,0).endVertex();
        vc.vertex(mat,x,y,z-s).color(1f,0f,0f,1f).normal(nm,0,0,1).endVertex();
        vc.vertex(mat,x,y,z+s).color(1f,0f,0f,1f).normal(nm,0,0,1).endVertex();
    }
}
