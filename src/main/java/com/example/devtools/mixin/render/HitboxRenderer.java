package com.example.devtools.mixin.render;

import com.example.devtools.core.IDevFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Erweiterter Hitbox-Renderer.
 *
 * Pro Entity:
 *  - Farbige AABB (nach Entity-Typ)
 *  - Cyan-Kreuz auf Eye-Height
 *  - Orange Linie in Blickrichtung (LivingEntity)
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HitboxRenderer implements IDevFeature {

    public static final HitboxRenderer INSTANCE = new HitboxRenderer();

    public boolean showPlayers  = true;
    public boolean showMobs     = true;
    public boolean showAnimals  = true;
    public boolean showItems    = false;
    public boolean showOther    = false;
    public boolean showEyeLine  = true;
    public boolean showLookVec  = true;
    public float   maxDist      = 48f;

    private boolean enabled = false;

    private HitboxRenderer() {}

    @Override public String getId()          { return "hitbox_renderer"; }
    @Override public String getDisplayName() { return "Hitbox Renderer"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!INSTANCE.enabled) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        float pt  = event.getPartialTick();
        PoseStack ps  = event.getPoseStack();
        Vec3      cam = event.getCamera().getPosition();

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.getCameraEntity()) continue;
            if (entity.distanceTo(mc.player) > INSTANCE.maxDist) continue;

            float[] col = INSTANCE.colorOf(entity);
            if (col == null) continue;

            double ex = Mth.lerp(pt, entity.xOld, entity.getX()) - cam.x;
            double ey = Mth.lerp(pt, entity.yOld, entity.getY()) - cam.y;
            double ez = Mth.lerp(pt, entity.zOld, entity.getZ()) - cam.z;

            // Bounding Box
            AABB box = entity.getBoundingBox().move(-cam.x, -cam.y, -cam.z);
            ps.pushPose();
            LevelRenderer.renderLineBox(ps, vc, box, col[0], col[1], col[2], 1.0f);
            ps.popPose();

            double eyeRelY = ey + entity.getEyeHeight();

            // Eye-Level Kreuz
            if (INSTANCE.showEyeLine) {
                float w = entity.getBbWidth() * 0.5f + 0.02f;
                drawLine(ps, vc, ex - w, eyeRelY, ez,     ex + w, eyeRelY, ez,     0f, 1f, 1f);
                drawLine(ps, vc, ex,     eyeRelY, ez - w, ex,     eyeRelY, ez + w, 0f, 1f, 1f);
            }

            // Blickvektor
            if (INSTANCE.showLookVec && entity instanceof LivingEntity le) {
                Vec3 look = le.getLookAngle();
                drawLine(ps, vc,
                    ex, eyeRelY, ez,
                    ex + look.x, eyeRelY + look.y, ez + look.z,
                    1f, 0.5f, 0f);
            }
        }

        buf.endBatch(RenderType.lines());
    }

    private float[] colorOf(Entity e) {
        if (e instanceof Player     && showPlayers) return new float[]{0.25f, 0.55f, 1.00f};
        if (e instanceof Monster    && showMobs)    return new float[]{1.00f, 0.20f, 0.20f};
        if (e instanceof Animal     && showAnimals) return new float[]{0.20f, 1.00f, 0.30f};
        if (e instanceof ItemEntity && showItems)   return new float[]{1.00f, 1.00f, 0.20f};
        if (showOther)                              return new float[]{0.75f, 0.75f, 0.75f};
        return null;
    }

    private static void drawLine(PoseStack ps, VertexConsumer vc,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            float r, float g, float b) {
        Matrix4f mat = ps.last().pose();
        Matrix3f nm  = ps.last().normal();

        float dx = (float)(x1 - x0), dy = (float)(y1 - y0), dz = (float)(z1 - z0);
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-6f) return;

        vc.vertex(mat,(float)x0,(float)y0,(float)z0).color(r,g,b,1f).normal(nm,dx/len,dy/len,dz/len).endVertex();
        vc.vertex(mat,(float)x1,(float)y1,(float)z1).color(r,g,b,1f).normal(nm,dx/len,dy/len,dz/len).endVertex();
    }
}
