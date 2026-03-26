package com.example.devtools;

import com.example.devtools.core.FeatureRegistry;
import com.example.devtools.features.hud.HudOverlay;
import com.example.devtools.keybind.KeybindManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DevToolsMod.MODID)
public class DevToolsMod {

    public static final String MODID  = "devtools";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public DevToolsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // MOD-Bus Listener
        modBus.addListener(KeybindManager::onRegisterKeybinds);
        modBus.addListener(HudOverlay::onRegisterOverlays);
        modBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // enqueueWork: thread-safe nach parallelem Laden
        event.enqueueWork(FeatureRegistry::init);
    }
}
