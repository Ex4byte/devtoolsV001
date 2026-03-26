package com.example.devtools.features.packets;

import com.example.devtools.core.IDevFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Packet Logger.
 * 1.20.1: getGuiGraphics() statt getPoseStack(), GuiGraphics.drawString() statt Font.drawShadow()
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PacketLogger implements IDevFeature {

    public static final PacketLogger INSTANCE = new PacketLogger();

    private static final int MAX_ENTRIES = 100;
    private static final int HUD_LINES   = 12;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ConcurrentLinkedDeque<PacketEntry> buffer = new ConcurrentLinkedDeque<>();

    private boolean enabled = false;
    private String  filter  = "";

    private PacketLogger() {}

    @Override public String getId()          { return "packet_logger"; }
    @Override public String getDisplayName() { return "Packet Logger"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    public void logIncoming(Packet<?> packet) {
        if (!enabled) return;
        log("IN ", packet.getClass().getSimpleName());
    }

    public void logOutgoing(Packet<?> packet) {
        if (!enabled) return;
        log("OUT", packet.getClass().getSimpleName());
    }

    private void log(String direction, String name) {
        if (!filter.isEmpty() && !name.toLowerCase().contains(filter.toLowerCase())) return;
        buffer.addLast(new PacketEntry(direction, name, LocalTime.now().format(TIME_FMT)));
        while (buffer.size() > MAX_ENTRIES) buffer.pollFirst();
    }

    public void clearBuffer()       { buffer.clear(); }
    public void setFilter(String f) { this.filter = f == null ? "" : f; }
    public String getFilter()       { return filter; }
    public int getBufferSize()      { return buffer.size(); }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!INSTANCE.enabled) return;
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;

        Minecraft mc = Minecraft.getInstance();
        // 1.20.1: getGuiGraphics() statt getPoseStack()
        GuiGraphics g    = event.getGuiGraphics();
        Font        font = mc.font;

        int screenH = mc.getWindow().getGuiScaledHeight();
        int x       = 5;
        int lh      = font.lineHeight + 1;
        int startY  = screenH - 30 - (HUD_LINES * lh);

        String header = "§8[§6Packets§8]"
            + (INSTANCE.filter.isEmpty() ? "" : " §7filter:§f" + INSTANCE.filter)
            + " §7(" + INSTANCE.buffer.size() + ")";

        // GuiGraphics.drawString(font, text, x, y, color, shadow)
        g.drawString(font, header, x, startY - lh - 2, 0xFFFFFF, true);

        PacketEntry[] entries = INSTANCE.buffer.toArray(new PacketEntry[0]);
        int from = Math.max(0, entries.length - HUD_LINES);
        int y    = startY;

        for (int i = from; i < entries.length; i++) {
            PacketEntry e = entries[i];
            String col = e.direction().startsWith("IN") ? "§a" : "§c";
            g.drawString(font,
                "§7" + e.time() + " " + col + e.direction() + "§f " + e.name(),
                x, y, 0xFFFFFF, true);
            y += lh;
        }
    }

    public record PacketEntry(String direction, String name, String time) {}
}
