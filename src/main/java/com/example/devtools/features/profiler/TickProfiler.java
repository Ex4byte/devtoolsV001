package com.example.devtools.features.profiler;

import com.example.devtools.core.IDevFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tick & Render Profiler.
 *
 * 1.20.1 API-Änderungen:
 *  - RenderGuiOverlayEvent hat kein getPoseStack() mehr → getGuiGraphics()
 *  - Font.drawShadow(PoseStack,...) existiert nicht → GuiGraphics.drawString(..., true)
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TickProfiler implements IDevFeature {

    public static final TickProfiler INSTANCE = new TickProfiler();

    private static final int WINDOW_SIZE = 20;

    private final Deque<Long> tickTimes   = new ArrayDeque<>(WINDOW_SIZE);
    private final Deque<Long> renderTimes = new ArrayDeque<>(WINDOW_SIZE);

    private long tickStart   = 0L;
    private long lastFrameNs = 0L;

    private volatile double avgTickMs    = 0.0;
    private volatile double avgRenderMs  = 0.0;
    private volatile double estimatedTps = 20.0;

    private boolean enabled = false;

    private TickProfiler() {}

    @Override public String getId()          { return "tick_profiler"; }
    @Override public String getDisplayName() { return "Tick/Render Profiler"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; if (!e) reset(); }

    public void reset() {
        tickTimes.clear();
        renderTimes.clear();
        avgTickMs    = 0.0;
        avgRenderMs  = 0.0;
        estimatedTps = 20.0;
    }

    @SubscribeEvent
    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        if (!INSTANCE.enabled || event.phase != TickEvent.Phase.START) return;
        INSTANCE.tickStart = System.nanoTime();
    }

    @SubscribeEvent
    public static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        if (!INSTANCE.enabled || event.phase != TickEvent.Phase.END) return;
        long elapsed = System.nanoTime() - INSTANCE.tickStart;
        INSTANCE.addSample(INSTANCE.tickTimes, elapsed);
        INSTANCE.avgTickMs    = INSTANCE.calcAverage(INSTANCE.tickTimes) / 1_000_000.0;
        INSTANCE.estimatedTps = Math.min(20.0, 1000.0 / Math.max(INSTANCE.avgTickMs, 1.0));
    }

    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (!INSTANCE.enabled) return;

        long now = System.nanoTime();
        if (INSTANCE.lastFrameNs != 0L) {
            INSTANCE.addSample(INSTANCE.renderTimes, now - INSTANCE.lastFrameNs);
            INSTANCE.avgRenderMs = INSTANCE.calcAverage(INSTANCE.renderTimes) / 1_000_000.0;
        }
        INSTANCE.lastFrameNs = now;

        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;

        // 1.20.1: getGuiGraphics() statt getPoseStack()
        INSTANCE.renderHud(event.getGuiGraphics());
    }

    private void renderHud(GuiGraphics g) {
        Minecraft mc   = Minecraft.getInstance();
        Font      font = mc.font;
        int x = 5, y = 5;
        int lh = font.lineHeight + 2;

        String tpsColor = estimatedTps >= 19 ? "§a" : estimatedTps >= 15 ? "§e" : "§c";
        String[] lines = {
            "§8[§6Profiler§8]",
            "§7FPS:   §f" + mc.getFps(),
            "§7TPS~:  " + tpsColor + String.format("%.1f", estimatedTps),
            "§7Tick:  §f" + String.format("%.2f ms", avgTickMs),
            "§7Frame: §f" + String.format("%.2f ms", avgRenderMs),
        };

        g.fill(x - 2, y - 2, x + 114, y + lines.length * lh + 4, 0x88000000);
        for (String line : lines) {
            // drawString(font, text, x, y, color, dropShadow)
            g.drawString(font, line, x, y, 0xFFFFFF, true);
            y += lh;
        }
    }

    private void addSample(Deque<Long> buf, long value) {
        if (buf.size() >= WINDOW_SIZE) buf.pollFirst();
        buf.addLast(value);
    }

    private double calcAverage(Deque<Long> buf) {
        if (buf.isEmpty()) return 0.0;
        return buf.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    public double getAvgTickMs()    { return avgTickMs; }
    public double getAvgRenderMs()  { return avgRenderMs; }
    public double getEstimatedTps() { return estimatedTps; }
}
