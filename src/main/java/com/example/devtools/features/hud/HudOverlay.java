package com.example.devtools.features.hud;

import com.example.devtools.core.IDevFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Vollständiges HUD Overlay.
 * Registrierung: MOD-Bus via RegisterGuiOverlaysEvent.
 * registerAboveAll nimmt in Forge 1.20.1 einen String als ID.
 */
public final class HudOverlay implements IDevFeature {

    public static final HudOverlay INSTANCE = new HudOverlay();

    private boolean enabled = true;

    public boolean showFps      = true;
    public boolean showPing     = true;
    public boolean showMemory   = true;
    public boolean showCoords   = true;
    public boolean showFacing   = true;
    public boolean showChunk    = false;
    public boolean showVelocity = true;
    public boolean showLight    = true;
    public boolean showBiome    = false;
    public boolean showTime     = false;

    private HudOverlay() {}

    @Override public String getId()          { return "hud_overlay"; }
    @Override public String getDisplayName() { return "HUD Overlay"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    public static final IGuiOverlay HUD_OVERLAY = (gui, g, partialTick, sw, sh) -> {
        if (!INSTANCE.enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return;

        Player   player = mc.player;
        Level    level  = mc.level;
        BlockPos pos    = player.blockPosition();

        List<String> lines = new ArrayList<>();

        if (INSTANCE.showFps) {
            int fps = mc.getFps();
            lines.add("FPS " + grade(fps, 60, 30) + fps + "§r");
        }
        if (INSTANCE.showPing) {
            int ms = getPing(mc, player);
            lines.add("Ping " + grade(150 - ms, 100, 50) + ms + "ms§r");
        }
        if (INSTANCE.showMemory) {
            Runtime rt  = Runtime.getRuntime();
            long used   = (rt.totalMemory() - rt.freeMemory()) >> 20;
            long max    = rt.maxMemory() >> 20;
            float ratio = (float) used / max;
            lines.add(String.format("MEM %s%dMB§7/%dMB§r",
                grade(1f - ratio, 0.30f, 0.15f), used, max));
        }

        if (INSTANCE.showCoords || INSTANCE.showFacing || INSTANCE.showChunk)
            lines.add("");

        if (INSTANCE.showCoords)
            lines.add(String.format("§fXYZ §7%.2f §e/ §7%.2f §e/ §7%.2f",
                player.getX(), player.getY(), player.getZ()));

        if (INSTANCE.showFacing)
            lines.add("§fFacing §7" + facing(player.getYRot())
                + String.format(" §8(%.1f°)", player.getYRot()));

        if (INSTANCE.showChunk) {
            int cx = SectionPos.blockToSectionCoord(pos.getX());
            int cz = SectionPos.blockToSectionCoord(pos.getZ());
            lines.add(String.format("§fChunk §7%d, %d  §8in: %d, %d",
                cx, cz, pos.getX() & 15, pos.getZ() & 15));
        }

        if (INSTANCE.showVelocity) {
            Vec3 v = player.getDeltaMovement();
            double h = Math.sqrt(v.x * v.x + v.z * v.z);
            lines.add(String.format("§fVel §7H:%.3f  V:%.3f", h, v.y));
        }

        if (INSTANCE.showLight) {
            int sky   = level.getBrightness(LightLayer.SKY,   pos);
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            lines.add("§fLight §7B:" + lightGrade(block) + block
                + "§7  S:§e" + sky + "§7  (§f" + Math.max(sky, block) + "§7)§r");
        }
        if (INSTANCE.showBiome) {
            String name = level.getBiome(pos).unwrapKey()
                .map(k -> capitalize(k.location().getPath().replace('_', ' ')))
                .orElse("?");
            lines.add("§fBiome §7" + name);
        }
        if (INSTANCE.showTime) {
            long t = level.getDayTime() % 24000L;
            long h = (t / 1000 + 6) % 24;
            long m = (t % 1000) * 60 / 1000;
            lines.add(String.format("§fTime §7%02d:%02d §8(tick %d)", h, m, t));
        }

        int x = 5, y = 5;
        int lh  = mc.font.lineHeight + 2;
        int bgW = lines.stream().mapToInt(l -> mc.font.width(l)).max().orElse(80) + 6;
        int bgH = lines.size() * lh + 4;

        g.fill(x - 2, y - 2, x + bgW, y + bgH, 0x99000000);
        for (String line : lines) {
            if (!line.isEmpty()) g.drawString(mc.font, line, x, y, 0xFFFFFF);
            y += lh;
        }
    };

    private static int getPing(Minecraft mc, Player player) {
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) return 0;
        PlayerInfo info = conn.getPlayerInfo(player.getUUID());
        return info != null ? info.getLatency() : 0;
    }

    private static String grade(float value, float green, float yellow) {
        if (value >= green)  return "§a";
        if (value >= yellow) return "§e";
        return "§c";
    }
    private static String grade(int v, int g, int y) { return grade((float)v, g, y); }

    private static String lightGrade(int l) {
        if (l <= 7)  return "§c";
        if (l <= 11) return "§e";
        return "§a";
    }

    private static String facing(float yaw) {
        float y = ((yaw % 360) + 360) % 360;
        if (y < 22.5f  || y >= 337.5f) return "South §8(+Z)";
        if (y < 67.5f)                  return "SW";
        if (y < 112.5f)                 return "West §8(-X)";
        if (y < 157.5f)                 return "NW";
        if (y < 202.5f)                 return "North §8(-Z)";
        if (y < 247.5f)                 return "NE";
        if (y < 292.5f)                 return "East §8(+X)";
        return "SE";
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Forge 1.20.1: registerAboveAll nimmt String, nicht ResourceLocation. */
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hud", HUD_OVERLAY);
    }
}
