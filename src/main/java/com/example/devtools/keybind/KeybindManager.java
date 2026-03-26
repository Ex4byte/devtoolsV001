package com.example.devtools.keybind;

import com.example.devtools.core.FeatureRegistry;
import com.example.devtools.gui.DevPanelScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Alle Keybindings zentral verwaltet.
 * Registrierung: MOD-Bus (RegisterKeyMappingsEvent).
 * Polling:       FORGE-Bus (ClientTickEvent).
 */
@Mod.EventBusSubscriber(modid = "devtools", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KeybindManager {

    // ── Keybinds ──────────────────────────────────────────────────────────────
    public static final KeyMapping KEY_DEV_PANEL = new KeyMapping(
            "key.devtools.dev_panel",       GLFW.GLFW_KEY_F8, "key.categories.devtools");
    public static final KeyMapping KEY_TOGGLE_ESP = new KeyMapping(
            "key.devtools.toggle_esp",      GLFW.GLFW_KEY_F7, "key.categories.devtools");
    public static final KeyMapping KEY_TOGGLE_PROFILER = new KeyMapping(
            "key.devtools.toggle_profiler", GLFW.GLFW_KEY_F6, "key.categories.devtools");
    public static final KeyMapping KEY_TOGGLE_PACKETS = new KeyMapping(
            "key.devtools.toggle_packets",  GLFW.GLFW_KEY_F5, "key.categories.devtools");
    public static final KeyMapping KEY_TOGGLE_FLY = new KeyMapping(
            "key.devtools.toggle_fly",      GLFW.GLFW_KEY_F4, "key.categories.devtools");
    public static final KeyMapping KEY_TOGGLE_FREECAM = new KeyMapping(
            "key.devtools.toggle_freecam",  GLFW.GLFW_KEY_F3, "key.categories.devtools");
    public static final KeyMapping KEY_TOGGLE_HITBOX = new KeyMapping(
            "key.devtools.toggle_hitbox",   GLFW.GLFW_KEY_H,  "key.categories.devtools");
    public static final KeyMapping KEY_TOGGLE_CHUNKS = new KeyMapping(
            "key.devtools.toggle_chunks",   GLFW.GLFW_KEY_G,  "key.categories.devtools");

    // ── MOD-Bus: Registrierung ────────────────────────────────────────────────
    public static void onRegisterKeybinds(RegisterKeyMappingsEvent event) {
        event.register(KEY_DEV_PANEL);
        event.register(KEY_TOGGLE_ESP);
        event.register(KEY_TOGGLE_PROFILER);
        event.register(KEY_TOGGLE_PACKETS);
        event.register(KEY_TOGGLE_FLY);
        event.register(KEY_TOGGLE_FREECAM);
        event.register(KEY_TOGGLE_HITBOX);
        event.register(KEY_TOGGLE_CHUNKS);
    }

    // ── FORGE-Bus: Polling ────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (KEY_DEV_PANEL.consumeClick())
            mc.setScreen(new DevPanelScreen());

        if (KEY_TOGGLE_ESP.consumeClick())
            msg(mc, "ESP", FeatureRegistry.toggle("esp_renderer"));

        if (KEY_TOGGLE_PROFILER.consumeClick())
            msg(mc, "Profiler", FeatureRegistry.toggle("tick_profiler"));

        if (KEY_TOGGLE_PACKETS.consumeClick())
            msg(mc, "PacketLog", FeatureRegistry.toggle("packet_logger"));

        if (KEY_TOGGLE_FLY.consumeClick())
            msg(mc, "Fly", FeatureRegistry.toggle("fly"));

        if (KEY_TOGGLE_FREECAM.consumeClick())
            msg(mc, "Freecam", FeatureRegistry.toggle("freecam"));

        if (KEY_TOGGLE_HITBOX.consumeClick())
            FeatureRegistry.toggle("hitbox_renderer");

        if (KEY_TOGGLE_CHUNKS.consumeClick())
            FeatureRegistry.toggle("chunk_borders");
    }

    private static void msg(Minecraft mc, String name, boolean on) {
        mc.player.sendSystemMessage(Component.literal(
            "§8[§6DT§8] §r" + name + ": " + (on ? "§aON" : "§cOFF")));
    }
}
