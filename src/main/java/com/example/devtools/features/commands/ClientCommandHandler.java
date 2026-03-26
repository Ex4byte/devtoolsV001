package com.example.devtools.features.commands;

import com.example.devtools.core.FeatureRegistry;
import com.example.devtools.core.IDevFeature;
import com.example.devtools.features.camera.FlyFeature;
import com.example.devtools.features.esp.ESPRenderer;
import com.example.devtools.features.movement.BlockReachFeature;
import com.example.devtools.features.movement.NoClipFeature;
import com.example.devtools.features.packets.InventoryViewer;
import com.example.devtools.features.packets.PacketLogger;
import com.example.devtools.features.profiler.TickProfiler;
import com.example.devtools.features.render.TrajectoryPredictor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
        COMMANDS.put("fly",      ClientCommandHandler::cmdFly);
        COMMANDS.put("pos",      ClientCommandHandler::cmdPos);
        COMMANDS.put("near",     ClientCommandHandler::cmdNear);
        COMMANDS.put("feature",  ClientCommandHandler::cmdFeature);
        COMMANDS.put("noclip",   ClientCommandHandler::cmdNoclip);
        COMMANDS.put("reach",    ClientCommandHandler::cmdReach);
        COMMANDS.put("traj",     ClientCommandHandler::cmdTraj);
        COMMANDS.put("invsee",   ClientCommandHandler::cmdInvsee);
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
        Consumer<String[]> handler = COMMANDS.get(parts[0].toLowerCase());
        if (handler != null) handler.accept(parts);
        else reply("§cUnbekannter Command: §f" + parts[0] + " §7– .help");
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    private static void cmdHelp(String[] a) {
        reply("§6=== DevTools Commands ===");
        reply("§e.fly §7[on|off|safe <on|off>]  §e.flyspeed §7<val>");
        reply("§e.noclip §7[on|off|speed <val>]");
        reply("§e.speed §7[on|off|mult <val>]");
        reply("§e.reach §7[on|off|reach <val>|attack <val>]");
        reply("§e.traj §7[on|off|type arrow|throwable|trident|charge <0-1>]");
        reply("§e.invsee §7<Spielername>");
        reply("§e.esp §7[on|off|players|mobs|items|animals|xray]");
        reply("§e.profiler §7[on|off|reset]  §e.packets §7[on|off|filter <p>|clear]");
        reply("§e.pos  §e.near §7[radius]  §e.feature §7<id> [on|off]");
    }

    private static void cmdFly(String[] a) {
        FlyFeature fly = FlyFeature.INSTANCE;
        if (a.length < 2) { FeatureRegistry.toggle("fly"); reply("Fly: " + s(fly.isEnabled()) + " §7ServerSafe: " + s(fly.serverSafeMode)); return; }
        switch (a[1].toLowerCase()) {
            case "on"   -> { fly.setEnabled(true);  reply("§aFly an"); }
            case "off"  -> { fly.setEnabled(false); reply("§cFly aus"); }
            case "safe" -> {
                if (a.length < 3) { reply("§cUsage: .fly safe <on|off>"); return; }
                fly.serverSafeMode = a[2].equalsIgnoreCase("on");
                reply("Fly ServerSafe: " + s(fly.serverSafeMode));
            }
            default -> reply("§cOption: on|off|safe <on|off>");
        }
    }

    private static void cmdNoclip(String[] a) {
        NoClipFeature nc = NoClipFeature.INSTANCE;
        if (a.length < 2) { FeatureRegistry.toggle("noclip"); reply("NoClip: " + s(nc.isEnabled())); return; }
        switch (a[1].toLowerCase()) {
            case "on"    -> { nc.setEnabled(true);  reply("§aNoClip an"); }
            case "off"   -> { nc.setEnabled(false); reply("§cNoClip aus"); }
            case "speed" -> {
                if (a.length < 3) { reply("§cUsage: .noclip speed <val>"); return; }
                try { nc.speedLimit = Float.parseFloat(a[2]); reply("§eSpeed-Limit: §f" + nc.speedLimit); }
                catch (NumberFormatException e) { reply("§cUngültiger Wert"); }
            }
            default -> reply("§cOption: on|off|speed <val>");
        }
    }


    private static void cmdReach(String[] a) {
        BlockReachFeature br = BlockReachFeature.INSTANCE;
        if (a.length < 2) { reply("Reach: " + s(br.isEnabled()) + " §7+" + br.extraReach + " blocks, Attack: +" + br.extraAttack); return; }
        switch (a[1].toLowerCase()) {
            case "on"     -> { br.setEnabled(true);  reply("§aReach an"); }
            case "off"    -> { br.setEnabled(false); reply("§cReach aus"); }
            case "reach"  -> { if (a.length<3){reply("§cUsage: .reach reach <val>");return;} try{br.setExtraReach(Float.parseFloat(a[2]));reply("§eReach: §f+"+br.extraReach);}catch(NumberFormatException e){reply("§cUngültiger Wert");} }
            case "attack" -> { if (a.length<3){reply("§cUsage: .reach attack <val>");return;} try{br.setExtraAttack(Float.parseFloat(a[2]));reply("§eAttack: §f+"+br.extraAttack);}catch(NumberFormatException e){reply("§cUngültiger Wert");} }
            default -> reply("§cOption: on|off|reach <val>|attack <val>");
        }
    }

    private static void cmdTraj(String[] a) {
        TrajectoryPredictor tp = TrajectoryPredictor.INSTANCE;
        if (a.length < 2) { FeatureRegistry.toggle("trajectory"); reply("Traj: " + s(tp.isEnabled()) + " §7" + tp.type + " charge:" + tp.charge); return; }
        switch (a[1].toLowerCase()) {
            case "on"     -> { tp.setEnabled(true);  reply("§aTrajectory an"); }
            case "off"    -> { tp.setEnabled(false); reply("§cTrajectory aus"); }
            case "type"   -> {
                if (a.length<3){reply("§cTypes: arrow|throwable|trident");return;}
                try { tp.type = TrajectoryPredictor.ProjectileType.valueOf(a[2].toUpperCase()); reply("§eType: §f"+tp.type); }
                catch (IllegalArgumentException e) { reply("§cUnbekannter Typ: "+a[2]); }
            }
            case "charge" -> {
                if (a.length<3){reply("§cUsage: .traj charge <0.0-1.0>");return;}
                try { tp.charge = Math.max(0f, Math.min(1f, Float.parseFloat(a[2]))); reply("§eCharge: §f"+tp.charge); }
                catch (NumberFormatException e) { reply("§cUngültiger Wert"); }
            }
            default -> reply("§cOption: on|off|type <t>|charge <val>");
        }
    }

    private static void cmdInvsee(String[] a) {
        if (a.length < 2) { reply("§cUsage: .invsee <Spielername>"); return; }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof Player p && p.getName().getString().equalsIgnoreCase(a[1])) {
                reply("§e=== Equipment: §f" + p.getName().getString() + " §e===");
                reply(InventoryViewer.INSTANCE.getEquipmentSummary(p));
                return;
            }
        }
        reply("§cSpieler §f" + a[1] + " §cnicht in Sichtweite");
    }

    private static void cmdEsp(String[] a) {
        ESPRenderer esp = ESPRenderer.INSTANCE;
        if (a.length < 2) { reply("ESP: "+s(esp.isEnabled())+" xray="+s(esp.xrayMode)+" players="+s(esp.showPlayers)+" mobs="+s(esp.showMobs)); return; }
        switch (a[1].toLowerCase()) {
            case "on"      -> { esp.setEnabled(true);   reply("§aESP an"); }
            case "off"     -> { esp.setEnabled(false);  reply("§cESP aus"); }
            case "players" -> { esp.showPlayers = !esp.showPlayers; reply("Players: "+s(esp.showPlayers)); }
            case "mobs"    -> { esp.showMobs    = !esp.showMobs;    reply("Mobs: "+s(esp.showMobs)); }
            case "items"   -> { esp.showItems   = !esp.showItems;   reply("Items: "+s(esp.showItems)); }
            case "animals" -> { esp.showAnimals = !esp.showAnimals; reply("Animals: "+s(esp.showAnimals)); }
            case "xray"    -> { esp.xrayMode    = !esp.xrayMode;    reply("XRay: "+s(esp.xrayMode)); }
            default        -> reply("§cOption: on|off|players|mobs|items|animals|xray");
        }
    }

    private static void cmdProfiler(String[] a) {
        TickProfiler p = TickProfiler.INSTANCE;
        if (a.length < 2) { FeatureRegistry.toggle("tick_profiler"); reply("Profiler: "+s(p.isEnabled())); return; }
        switch (a[1].toLowerCase()) {
            case "on"    -> { p.setEnabled(true);  reply("§aProfiler an"); }
            case "off"   -> { p.setEnabled(false); reply("§cProfiler aus"); }
            case "reset" -> { p.reset();           reply("§eZurückgesetzt"); }
            default      -> reply("§cOption: on|off|reset");
        }
    }

    private static void cmdPackets(String[] a) {
        PacketLogger l = PacketLogger.INSTANCE;
        if (a.length < 2) { reply("Packets: "+s(l.isEnabled())+" buffer:"+l.getBufferSize()); return; }
        switch (a[1].toLowerCase()) {
            case "on"     -> { l.setEnabled(true);  reply("§aPacket Logger an"); }
            case "off"    -> { l.setEnabled(false); reply("§cPacket Logger aus"); }
            case "clear"  -> { l.clearBuffer();     reply("§eBuffer geleert"); }
            case "filter" -> { if (a.length<3){reply("§cUsage: .packets filter <p>");return;} l.setFilter(a[2]); reply("§eFilter: §f"+a[2]); }
            default -> reply("§cOption: on|off|clear|filter <p>");
        }
    }

    private static void cmdFlySpeed(String[] a) {
        if (a.length < 2) { reply("§cUsage: .flyspeed <0.01-1.0>"); return; }
        try { FlyFeature.INSTANCE.setSpeed(Float.parseFloat(a[1])); reply("§aSpeed: §f"+String.format("%.2f", FlyFeature.INSTANCE.getSpeed())); }
        catch (NumberFormatException e) { reply("§cUngültiger Wert"); }
    }

    private static void cmdPos(String[] a) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        reply(String.format("§eX=%.2f Y=%.2f Z=%.2f §7Dim: %s", p.getX(), p.getY(), p.getZ(), p.level().dimension().location()));
    }

    private static void cmdNear(String[] a) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        double radius = 16.0;
        if (a.length >= 2) { try { radius = Double.parseDouble(a[1]); } catch (NumberFormatException ignored) {} }
        double fr = radius;
        var entities = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(radius), e -> e.distanceTo(mc.player) <= fr);
        reply("§eEntities in "+(int)radius+"m ("+entities.size()+"):");
        entities.stream().sorted((x,y)->Double.compare(x.distanceTo(mc.player),y.distanceTo(mc.player))).limit(10)
            .forEach(e -> reply(String.format("  §f%s §7(%.1fm) @%.0f,%.0f,%.0f", e.getType().toShortString(), e.distanceTo(mc.player), e.getX(), e.getY(), e.getZ())));
        if (entities.size() > 10) reply("  §7... +"+( entities.size()-10)+" weitere");
    }

    private static void cmdFeature(String[] a) {
        if (a.length < 2) { reply("§eFeatures:"); FeatureRegistry.getAll().forEach(f -> reply("  §f"+f.getId()+" §7– "+s(f.isEnabled()))); return; }
        String id = a[1].toLowerCase();
        if (a.length >= 3) {
            IDevFeature f = FeatureRegistry.get(id);
            if (f == null) { reply("§cNicht gefunden: "+id); return; }
            f.setEnabled(a[2].equalsIgnoreCase("on")); reply(id+": "+s(f.isEnabled()));
        } else { reply(id+": "+s(FeatureRegistry.toggle(id))); }
    }

    public static void reply(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.player.sendSystemMessage(Component.literal("§8[§6DT§8] §r" + msg));
    }

    private static String s(boolean b) { return b ? "§aON§r" : "§cOFF§r"; }
}
