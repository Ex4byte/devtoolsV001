package com.example.devtools.util;

import com.example.devtools.DevToolsMod;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

/**
 * Reflection-Accessor für Abilities.flyingSpeed.
 *
 * Grund: In manchen Forge-Setups ist flyingSpeed trotz offizieller
 * Mappings nach Obfuskierung nicht direkt zugreifbar.
 * Reflection ist hier der robusteste Cross-Version-Ansatz.
 */
public final class AbilitiesAccessor {

    private static Field flyingSpeedField = null;

    static {
        // Suche das Feld über alle deklarierten Felder (Name kann je nach
        // Obfuskierungsstand variieren → suche per Typ float + Position)
        for (Field f : Abilities.class.getDeclaredFields()) {
            if (f.getType() == float.class && f.getName().contains("flying") ||
                f.getType() == float.class && f.getName().equals("f_20125_")) {
                f.setAccessible(true);
                flyingSpeedField = f;
                break;
            }
        }
        // Fallback: erstes float-Feld in Abilities (ist flyingSpeed)
        if (flyingSpeedField == null) {
            for (Field f : Abilities.class.getDeclaredFields()) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    flyingSpeedField = f;
                    DevToolsMod.LOGGER.warn("[DevTools] flyingSpeed via Fallback-Feld: {}", f.getName());
                    break;
                }
            }
        }
        if (flyingSpeedField != null) {
            DevToolsMod.LOGGER.debug("[DevTools] AbilitiesAccessor: flyingSpeed → {}", flyingSpeedField.getName());
        } else {
            DevToolsMod.LOGGER.error("[DevTools] AbilitiesAccessor: flyingSpeed-Feld nicht gefunden!");
        }
    }

    private AbilitiesAccessor() {}

    public static float getFlyingSpeed(Player player) {
        if (flyingSpeedField == null) return 0.05f;
        try {
            return flyingSpeedField.getFloat(player.getAbilities());
        } catch (IllegalAccessException e) {
            DevToolsMod.LOGGER.error("[DevTools] getFlyingSpeed failed", e);
            return 0.05f;
        }
    }

    public static void setFlyingSpeed(Player player, float speed) {
        if (flyingSpeedField == null) return;
        try {
            flyingSpeedField.setFloat(player.getAbilities(), speed);
        } catch (IllegalAccessException e) {
            DevToolsMod.LOGGER.error("[DevTools] setFlyingSpeed failed", e);
        }
    }
}
