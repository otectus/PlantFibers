package com.otectus.plantfiber.config;

import net.minecraft.util.Mth;

/**
 * The armor-only snapshot of gameplay config that item code is allowed to read.
 *
 * <p>{@link PlantFiberConfig} is a COMMON spec, so its values are unavailable during registration and
 * may differ between a client's own file and the server it joins. This holder starts at the validated
 * default, is refreshed from the spec on config load/reload, and can be overridden by the armor
 * snapshot a server sends on login. Nothing here is persisted: the remote view is cleared on
 * disconnect so single-player never sees a stale remote value.
 */
public final class EffectivePlantFiberConfig {
    /** Validated default for {@code grassArmorDurabilityMultiplier}; also the config's declared default. */
    public static final double DEFAULT_ARMOR_DURABILITY_MULTIPLIER = 0.5d;
    public static final double MIN_ARMOR_DURABILITY_MULTIPLIER = 0.1d;
    public static final double MAX_ARMOR_DURABILITY_MULTIPLIER = 1.0d;

    /** This installation's own value, from the loaded spec or the validated default. */
    private static volatile double localArmorDurabilityMultiplier = DEFAULT_ARMOR_DURABILITY_MULTIPLIER;
    /** The joined server's value while connected, or {@code null} when there is no remote view. */
    private static volatile Double remoteArmorDurabilityMultiplier;

    private EffectivePlantFiberConfig() {
    }

    /** The multiplier armor should actually use: the server's view when connected, else the local one. */
    public static double armorDurabilityMultiplier() {
        Double remote = remoteArmorDurabilityMultiplier;
        return remote != null ? remote : localArmorDurabilityMultiplier;
    }

    /** This installation's own multiplier, independent of any remote view. */
    public static double localArmorDurabilityMultiplier() {
        return localArmorDurabilityMultiplier;
    }

    /** Refresh the local snapshot; never reads an unloaded spec. */
    public static void refreshLocalFromSpec() {
        if (PlantFiberConfig.SPEC.isLoaded()) {
            localArmorDurabilityMultiplier = clamp(PlantFiberConfig.COMMON.grassArmorDurabilityMultiplier.get());
        }
    }

    public static void setRemoteArmorDurabilityMultiplier(double multiplier) {
        remoteArmorDurabilityMultiplier = clamp(multiplier);
    }

    public static void clearRemoteArmorDurabilityMultiplier() {
        remoteArmorDurabilityMultiplier = null;
    }

    public static double clamp(double multiplier) {
        if (!Double.isFinite(multiplier)) {
            return DEFAULT_ARMOR_DURABILITY_MULTIPLIER;
        }
        return Mth.clamp(multiplier, MIN_ARMOR_DURABILITY_MULTIPLIER, MAX_ARMOR_DURABILITY_MULTIPLIER);
    }
}
