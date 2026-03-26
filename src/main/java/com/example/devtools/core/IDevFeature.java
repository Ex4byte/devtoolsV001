package com.example.devtools.core;

/**
 * Jedes DevTool implementiert dieses Interface.
 * Instanzen werden per FeatureRegistry beim Forge EventBus registriert.
 */
public interface IDevFeature {
    /** Eindeutige ID, z.B. "esp_renderer" */
    String getId();
    /** Anzeigename für Dev Panel GUI */
    String getDisplayName();

    boolean isEnabled();
    void setEnabled(boolean enabled);

    default void onEnable() {}
    default void onDisable() {}
}
