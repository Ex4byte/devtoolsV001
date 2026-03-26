package com.example.devtools.features.esp;

import com.example.devtools.core.IDevFeature;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
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

import java.util.ArrayList;
import java.util.List;

/**
 * ESP-Renderer: farbige Wireframe-Boxen durch Blöcke (XRay-Mode).
 * entitiesForRendering() gibt Iterable zurück → manuell in List kopieren.
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ESPRenderer implements IDevFeature {

    public static final ESPRenderer INSTANCE = new ESPRenderer();

    public boolean showPlayers = true;
    public boolean showMobs    = true;
    public boolean showItems   = false;
    public boolean showAnimals = false;
    public boolean xrayMode    = true;
    public float   maxDistance = 64.0f;

    private boolean enabled = false;

    private ESPRenderer() {}

    @Override public String getId()          { return "esp_renderer"; }
    @Override public String getDisplayName() { return "ESP / Entity Visualizer"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!INSTANCE.enabled) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack ps  = event.getPoseStack();
        Vec3      cam = event.getCamera().getPosition();

        // entitiesForRendering() → Iterable, kein Stream → manuell filtern
        List<Entity> toRender = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e != mc.player && e.distanceTo(mc.player) <= INSTANCE.maxDistance) {
                toRender.add(e);
            }
        }

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());

        if (INSTANCE.xrayMode) RenderSystem.disableDepthTest();

        for (Entity entity : toRender) {
            ESPColor color = INSTANCE.getColorFor(entity);
            if (color == null) continue;
            AABB box = entity.getBoundingBox().move(-cam.x, -cam.y, -cam.z);
            ps.pushPose();
            LevelRenderer.renderLineBox(ps, vc, box, color.r, color.g, color.b, 1.0f);
            ps.popPose();
        }

        buf.endLastBatch();
        if (INSTANCE.xrayMode) RenderSystem.enableDepthTest();
    }

    private ESPColor getColorFor(Entity entity) {
        if (entity instanceof Player    && showPlayers) return ESPColor.PLAYER;
        if (entity instanceof Monster   && showMobs)    return ESPColor.MOB;
        if (entity instanceof ItemEntity && showItems)  return ESPColor.ITEM;
        if (entity instanceof Animal    && showAnimals) return ESPColor.ANIMAL;
        return null;
    }

    public enum ESPColor {
        PLAYER(0.2f, 0.6f, 1.0f),
        MOB   (1.0f, 0.2f, 0.2f),
        ITEM  (1.0f, 1.0f, 0.2f),
        ANIMAL(0.2f, 1.0f, 0.4f);

        public final float r, g, b;
        ESPColor(float r, float g, float b) { this.r = r; this.g = g; this.b = b; }
    }
}
