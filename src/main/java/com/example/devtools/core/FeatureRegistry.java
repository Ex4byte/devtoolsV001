package com.example.devtools.core;

import com.example.devtools.DevToolsMod;
import com.example.devtools.features.camera.FlyFeature;
import com.example.devtools.features.camera.FreecamFeature;
import com.example.devtools.features.commands.ClientCommandHandler;
import com.example.devtools.features.esp.ESPRenderer;
import com.example.devtools.features.hud.HudOverlay;
import com.example.devtools.features.movement.BlockReachFeature;
import com.example.devtools.features.movement.NoClipFeature;
import com.example.devtools.features.movement.SpeedHackFeature;
import com.example.devtools.features.packets.InventoryViewer;
import com.example.devtools.features.packets.PacketLogger;
import com.example.devtools.features.profiler.TickProfiler;
import com.example.devtools.features.render.ChunkBorderRenderer;
import com.example.devtools.features.render.HitboxRenderer;
import com.example.devtools.features.render.TrajectoryPredictor;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FeatureRegistry {

    private static final Map<String, IDevFeature> FEATURES = new LinkedHashMap<>();

    private FeatureRegistry() {}

    public static void init() {
        register(HudOverlay.INSTANCE);
        register(ESPRenderer.INSTANCE);
        register(HitboxRenderer.INSTANCE);
        register(ChunkBorderRenderer.INSTANCE);
        register(TrajectoryPredictor.INSTANCE);
        register(FreecamFeature.INSTANCE);
        register(FlyFeature.INSTANCE);
        register(NoClipFeature.INSTANCE);
        register(SpeedHackFeature.INSTANCE);
        register(BlockReachFeature.INSTANCE);
        register(TickProfiler.INSTANCE);
        register(PacketLogger.INSTANCE);
        register(InventoryViewer.INSTANCE);
        register(ClientCommandHandler.INSTANCE);
        DevToolsMod.LOGGER.info("[DevTools] {} features registered.", FEATURES.size());
    }

    public static void register(IDevFeature feature) {
        FEATURES.put(feature.getId(), feature);
        MinecraftForge.EVENT_BUS.register(feature);
        DevToolsMod.LOGGER.debug("[DevTools] Registered feature: {}", feature.getId());
    }

    public static IDevFeature get(String id)       { return FEATURES.get(id); }
    public static Collection<IDevFeature> getAll() { return FEATURES.values(); }
    public static boolean isEnabled(String id)     { IDevFeature f = FEATURES.get(id); return f != null && f.isEnabled(); }

    public static boolean toggle(String id) {
        IDevFeature f = FEATURES.get(id);
        if (f == null) return false;
        boolean next = !f.isEnabled();
        f.setEnabled(next);
        return next;
    }
}
