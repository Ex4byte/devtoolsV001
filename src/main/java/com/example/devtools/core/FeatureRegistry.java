package com.example.devtools.core;

import com.example.devtools.DevToolsMod;
import com.example.devtools.features.camera.FlyFeature;
import com.example.devtools.features.camera.FreecamFeature;
import com.example.devtools.features.commands.ClientCommandHandler;
import com.example.devtools.features.esp.ESPRenderer;
import com.example.devtools.features.hud.HudOverlay;
import com.example.devtools.features.packets.PacketLogger;
import com.example.devtools.features.profiler.TickProfiler;
import com.example.devtools.features.render.ChunkBorderRenderer;
import com.example.devtools.features.render.HitboxRenderer;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale Feature-Verwaltung.
 * Registriert alle Features am Forge EventBus.
 * Einzige Stelle für Feature-Lookup und Toggle.
 */
public final class FeatureRegistry {

    private static final Map<String, IDevFeature> FEATURES = new LinkedHashMap<>();

    private FeatureRegistry() {}

    /** Aufgerufen aus FMLClientSetupEvent (via DevToolsMod). */
    public static void init() {
        register(HudOverlay.INSTANCE);
        register(ESPRenderer.INSTANCE);
        register(HitboxRenderer.INSTANCE);
        register(ChunkBorderRenderer.INSTANCE);
        register(FreecamFeature.INSTANCE);
        register(FlyFeature.INSTANCE);
        register(TickProfiler.INSTANCE);
        register(PacketLogger.INSTANCE);
        register(ClientCommandHandler.INSTANCE);
        DevToolsMod.LOGGER.info("[DevTools] {} features registered.", FEATURES.size());
    }

    public static void register(IDevFeature feature) {
        FEATURES.put(feature.getId(), feature);
        MinecraftForge.EVENT_BUS.register(feature);
        DevToolsMod.LOGGER.debug("[DevTools] Registered feature: {}", feature.getId());
    }

    public static IDevFeature get(String id) {
        return FEATURES.get(id);
    }

    public static Collection<IDevFeature> getAll() {
        return FEATURES.values();
    }

    public static boolean toggle(String id) {
        IDevFeature f = FEATURES.get(id);
        if (f == null) return false;
        boolean next = !f.isEnabled();
        f.setEnabled(next);
        return next;
    }

    public static boolean isEnabled(String id) {
        IDevFeature f = FEATURES.get(id);
        return f != null && f.isEnabled();
    }
}
