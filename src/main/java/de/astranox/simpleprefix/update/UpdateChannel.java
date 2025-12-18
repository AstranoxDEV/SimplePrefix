package de.astranox.simpleprefix.update;

public enum UpdateChannel {
    DEV,
    SNAPSHOT,
    STABLE;

    public static UpdateChannel fromString(String channel) {
        if (channel == null) return STABLE;

        try {
            return valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STABLE;
        }
    }

    public boolean acceptsVersion(String version) {
        if (version == null || version.isEmpty()) return false;

        String lower = version.toLowerCase();
        boolean isDev = lower.contains("-dev") || lower.contains("-alpha");
        boolean isSnapshot = lower.contains("-snapshot") || lower.contains("-beta") || lower.contains("-rc");

        switch (this) {
            case DEV:
                return true;
            case SNAPSHOT:
                return !isDev;
            case STABLE:
                return !isDev && !isSnapshot;
            default:
                return false;
        }
    }

    public String getDisplayName() {
        switch (this) {
            case DEV: return "Development";
            case SNAPSHOT: return "Snapshot";
            case STABLE: return "Stable";
            default: return "Unknown";
        }
    }
}