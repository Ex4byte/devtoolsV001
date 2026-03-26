package com.example.devtools.mixin.render;

import com.example.devtools.core.IDevFeature;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Chunk Border Renderer.
 * Fix: blockX/blockZ → getBlockX()/getBlockZ() (public Methoden in Entity)
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ChunkBorderRenderer implements IDevFeature {

    public static final ChunkBorderRenderer INSTANCE = new ChunkBorderRenderer();

    public int     radius  = 1;
    public boolean xray    = true;
    public boolean yBounds = true;

    private boolean enabled = false;

    private ChunkBorderRenderer() {}

    @Override public String getId()          { return "chunk_borders"; }
    @Override public String getDisplayName() { return "Chunk Borders"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!INSTANCE.enabled) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack ps  = event.getPoseStack();
        Vec3      cam = event.getCamera().getPosition();

        // Fix: getBlockX() / getBlockZ() statt blockX / blockZ (private Felder)
        int pcx = SectionPos.blockToSectionCoord(mc.player.getBlockX());
        int pcz = SectionPos.blockToSectionCoord(mc.player.getBlockZ());

        int worldMinY = mc.level.getMinBuildHeight();
        int worldMaxY = mc.level.getMaxBuildHeight();
        double clipMin = INSTANCE.yBounds ? worldMinY : Math.max(worldMinY, cam.y - 80);
        double clipMax = INSTANCE.yBounds ? worldMaxY : Math.min(worldMaxY, cam.y + 80);

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());

        if (INSTANCE.xray) RenderSystem.disableDepthTest();

        int r = INSTANCE.radius;
        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                boolean cur  = (cx == pcx && cz == pcz);
                float   dist = Math.max(Math.abs(cx - pcx), Math.abs(cz - pcz));
                float   alpha = cur ? 1.0f : Math.max(0.15f, 1.0f - dist * 0.25f);
                float   red   = cur ? 1.0f : 0.55f;
                float   green = cur ? 0.9f : 0.55f;
                float   blue  = cur ? 0.0f : 0.55f;

                double x0 = (cx * 16.0) - cam.x;
                double z0 = (cz * 16.0) - cam.z;
                AABB box = new AABB(x0, clipMin - cam.y, z0, x0 + 16, clipMax - cam.y, z0 + 16);
                LevelRenderer.renderLineBox(ps, vc, box, red, green, blue, alpha);
            }
        }

        buf.endBatch(RenderType.lines());
        if (INSTANCE.xray) RenderSystem.enableDepthTest();
    }
}
