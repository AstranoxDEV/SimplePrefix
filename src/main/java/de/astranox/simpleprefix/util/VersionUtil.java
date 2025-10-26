package de.astranox.simpleprefix.util;

import org.bukkit.Bukkit;

public final class VersionUtil {
    private static final String BUKKIT_VERSION = Bukkit.getBukkitVersion();
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;

    private static final int LEGACY_LIMIT = 16;
    private static final int MODERN_LIMIT = 256;

    static {
        String base = BUKKIT_VERSION.split("-")[0];
        String[] parts = base.split("\\.");
        MAJOR_VERSION = parseIntSafe(parts, 0, 1);
        MINOR_VERSION = parseIntSafe(parts, 1, 8);
    }

    private VersionUtil() {}

    private static int parseIntSafe(String[] parts, int idx, int fallback) {
        try {
            if (idx < parts.length) {
                return Integer.parseInt(parts[idx]);
            }
        } catch (NumberFormatException ignored) {}
        return fallback;
    }

    public static boolean isLegacyVersion() {
        if (MAJOR_VERSION != 1) return MAJOR_VERSION < 1;
        return MINOR_VERSION <= 12;
    }

    public static int getPrefixLimit() {
        if (isLegacyVersion()) return LEGACY_LIMIT;
        return MODERN_LIMIT;
    }

    public static int getSuffixLimit() {
        if (isLegacyVersion()) return LEGACY_LIMIT;
        return MODERN_LIMIT;
    }

    public static int getPlayerListNameLimit() {
        if (isLegacyVersion()) return LEGACY_LIMIT;
        return MODERN_LIMIT;
    }

    public static String getVersionString() {
        return MAJOR_VERSION + "." + MINOR_VERSION;
    }
}
