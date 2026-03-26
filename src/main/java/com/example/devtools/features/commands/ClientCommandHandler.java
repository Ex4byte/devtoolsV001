package com.example.devtools.features.commands;

import com.example.devtools.core.FeatureRegistry;
import com.example.devtools.core.IDevFeature;
import com.example.devtools.features.camera.FlyFeature;
import com.example.devtools.features.esp.ESPRenderer;
import com.example.devtools.features.packets.PacketLogger;
import com.example.devtools.features.profiler.TickProfiler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client-Commands via Chat-Präfix ".".
 * ClientChatEvent wird cancelled – kein Packet ans Server.
 *
 * Befehle:
 *   .help
 *   .esp [on|off|players|mobs|items|animals|xray]
 *   .profiler [on|off|reset]
 *   .packets [on|off|filter <pattern>|clear]
 *   .flyspeed <0.01-1.0>
 *   .pos
 *   .near [radius]
 *   .feature <id> [on|off]
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientCommandHandler implements IDevFeature {

    public static final ClientCommandHandler INSTANCE = new ClientCommandHandler();
    private static final String PREFIX = ".";
    private boolean enabled = true;

    private static final Map<String, Consumer<String[]>> COMMANDS = new HashMap<>();

    static {
        COMMANDS.put("help",     ClientCommandHandler::cmdHelp);
        COMMANDS.put("esp",      ClientCommandHandler::cmdEsp);
        COMMANDS.put("profiler", ClientCommandHandler::cmdProfiler);
        COMMANDS.put("packets",  ClientCommandHandler::cmdPackets);
        COMMANDS.put("flyspeed", ClientCommandHandler::cmdFlySpeed);
        COMMANDS.put("pos",      ClientCommandHandler::cmdPos);
        COMMANDS.put("near",     ClientCommandHandler::cmdNear);
        COMMANDS.put("feature",  ClientCommandHandler::cmdFeature);
    }

    private ClientCommandHandler() {}

    @Override public String getId()          { return "client_commands"; }
    @Override public String getDisplayName() { return "Client Commands (.)"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String msg = event.getMessage().trim();
        if (!msg.startsWith(PREFIX)) return;

        event.setCanceled(true);

        String[] parts = msg.substring(PREFIX.length()).split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;

        String cmd = parts[0].toLowerCase();
        Consumer<String[]> handler = COMMANDS.get(cmd);

        if (handler != null) {
            handler.accept(parts);
        } else {
            reply("§cUnbekannter Command: §f" + cmd + " §7– .help für Liste");
        }
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    private static void cmdHelp(String[] args) {
        reply("§6=== DevTools Commands ===");
        reply("§e.esp §7[on|off|players|mobs|items|animals|xray]");
        reply("§e.profiler §7[on|off|reset]");
        reply("§e.packets §7[on|off|filter <pattern>|clear]");
        reply("§e.flyspeed §7<0.01-1.0>");
        reply("§e.pos §7– aktuelle Position");
        reply("§e.near §7[radius=16] – Entities auflisten");
        reply("§e.feature §7<id> [on|off] – Feature togglen");
    }

    private static void cmdEsp(String[] args) {
        ESPRenderer esp = ESPRenderer.INSTANCE;
        if (args.length < 2) {
            reply("§7ESP: " + status(esp.isEnabled())
                + " xray=" + status(esp.xrayMode)
                + " players=" + status(esp.showPlayers)
                + " mobs=" + status(esp.showMobs));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "on"      -> { esp.setEnabled(true);   reply("§aESP aktiviert"); }
            case "off"     -> { esp.setEnabled(false);  reply("§cESP deaktiviert"); }
            case "players" -> { esp.showPlayers = !esp.showPlayers; reply("Players: " + status(esp.showPlayers)); }
            case "mobs"    -> { esp.showMobs    = !esp.showMobs;    reply("Mobs: "    + status(esp.showMobs));    }
            case "items"   -> { esp.showItems   = !esp.showItems;   reply("Items: "   + status(esp.showItems));   }
            case "animals" -> { esp.showAnimals = !esp.showAnimals; reply("Animals: " + status(esp.showAnimals)); }
            case "xray"    -> { esp.xrayMode    = !esp.xrayMode;    reply("XRay: "    + status(esp.xrayMode));    }
            default        -> reply("§cUnbekannte Option: " + args[1]);
        }
    }

    private static void cmdProfiler(String[] args) {
        TickProfiler profiler = TickProfiler.INSTANCE;
        if (args.length < 2) {
            FeatureRegistry.toggle("tick_profiler");
            reply("Profiler: " + status(profiler.isEnabled()));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "on"    -> { profiler.setEnabled(true);  reply("§aProfiler gestartet"); }
            case "off"   -> { profiler.setEnabled(false); reply("§cProfiler gestoppt");  }
            case "reset" -> { profiler.reset();           reply("§eProfiler zurückgesetzt"); }
            default      -> reply("§cOption: on|off|reset");
        }
    }

    private static void cmdPackets(String[] args) {
        PacketLogger logger = PacketLogger.INSTANCE;
        if (args.length < 2) {
            reply("Packet Logger: " + status(logger.isEnabled())
                + " | Buffer: " + logger.getBufferSize() + " Pakete");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "on"     -> { logger.setEnabled(true);  reply("§aPacket Logger aktiv"); }
            case "off"    -> { logger.setEnabled(false); reply("§cPacket Logger deaktiviert"); }
            case "clear"  -> { logger.clearBuffer();     reply("§eBuffer geleert"); }
            case "filter" -> {
                if (args.length < 3) { reply("§cUsage: .packets filter <pattern>"); return; }
                logger.setFilter(args[2]);
                reply("§eFilter gesetzt: §f" + args[2]);
            }
            default -> reply("§cOption: on|off|clear|filter <pattern>");
        }
    }

    private static void cmdFlySpeed(String[] args) {
        if (args.length < 2) { reply("§cUsage: .flyspeed <0.01-1.0>"); return; }
        try {
            float speed = Float.parseFloat(args[1]);
            FlyFeature.INSTANCE.setSpeed(speed);
            reply("§aFly-Speed gesetzt: §f" + String.format("%.2f", FlyFeature.INSTANCE.getSpeed()));
        } catch (NumberFormatException e) {
            reply("§cKein gültiger Wert: " + args[1]);
        }
    }

    private static void cmdPos(String[] args) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        reply(String.format("§ePos: §fX=%.2f Y=%.2f Z=%.2f  §7Dim: %s",
            player.getX(), player.getY(), player.getZ(),
            player.level().dimension().location()));
    }

    private static void cmdNear(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        double radius = 16.0;
        if (args.length >= 2) {
            try { radius = Double.parseDouble(args[1]); } catch (NumberFormatException ignored) {}
        }
        double finalRadius = radius;
        var entities = mc.level.getEntities(mc.player,
            mc.player.getBoundingBox().inflate(radius),
            e -> e.distanceTo(mc.player) <= finalRadius);

        reply("§eEntities in " + (int)radius + " Blöcken (" + entities.size() + "):");
        entities.stream()
            .sorted((a, b) -> Double.compare(a.distanceTo(mc.player), b.distanceTo(mc.player)))
            .limit(10)
            .forEach(e -> reply(String.format("  §f%s §7(%.1fm) @%.0f,%.0f,%.0f",
                e.getType().toShortString(), e.distanceTo(mc.player),
                e.getX(), e.getY(), e.getZ())));
        if (entities.size() > 10) reply("  §7... und " + (entities.size() - 10) + " weitere");
    }

    private static void cmdFeature(String[] args) {
        if (args.length < 2) {
            reply("§eRegistrierte Features:");
            FeatureRegistry.getAll().forEach(f ->
                reply("  §f" + f.getId() + " §7– " + status(f.isEnabled())));
            return;
        }
        String id = args[1].toLowerCase();
        if (args.length >= 3) {
            IDevFeature f = FeatureRegistry.get(id);
            if (f == null) { reply("§cFeature nicht gefunden: " + id); return; }
            f.setEnabled(args[2].equalsIgnoreCase("on"));
            reply(id + ": " + status(f.isEnabled()));
        } else {
            boolean on = FeatureRegistry.toggle(id);
            reply(id + ": " + status(on));
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────────
    public static void reply(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.player.sendSystemMessage(Component.literal("§8[§6DT§8] §r" + msg));
    }

    private static String status(boolean b) {
        return b ? "§aON§r" : "§cOFF§r";
    }
}
