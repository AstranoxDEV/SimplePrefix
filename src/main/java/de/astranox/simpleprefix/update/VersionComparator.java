package de.astranox.simpleprefix.update;

public class VersionComparator {

    private final UpdateChannel channel;

    public VersionComparator(UpdateChannel channel) {
        this.channel = channel;
    }

    public boolean shouldUpdate(String current, String latest) {
        if (current == null || latest == null) return false;
        if (current.equals(latest)) return false;

        if (!channel.acceptsVersion(latest)) {
            return false;
        }

        ParsedVersion currentParsed = parseVersion(current);
        ParsedVersion latestParsed = parseVersion(latest);

        int baseCompare = compareVersionNumbers(currentParsed.baseVersion, latestParsed.baseVersion);

        if (baseCompare < 0) {
            return true;
        }

        if (baseCompare > 0) {
            return false;
        }

        if (currentParsed.isPreRelease && !latestParsed.isPreRelease) {
            return true;
        }

        return false;
    }

    private ParsedVersion parseVersion(String version) {
        if (version == null || version.isEmpty()) {
            return new ParsedVersion("0.0.0", false);
        }

        String lower = version.toLowerCase();
        boolean isPreRelease = lower.contains("-dev") ||
                lower.contains("-snapshot") ||
                lower.contains("-alpha") ||
                lower.contains("-beta") ||
                lower.contains("-rc");

        String baseVersion = version;
        int dashIndex = version.indexOf('-');
        if (dashIndex > 0) {
            baseVersion = version.substring(0, dashIndex);
        }

        return new ParsedVersion(baseVersion, isPreRelease);
    }

    private int compareVersionNumbers(String v1, String v2) {
        try {
            String[] parts1 = v1.replaceAll("[^0-9.]", "").split("\\.");
            String[] parts2 = v2.replaceAll("[^0-9.]", "").split("\\.");

            int maxLen = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < maxLen; i++) {
                int num1 = i < parts1.length && !parts1[i].isEmpty() ? Integer.parseInt(parts1[i]) : 0;
                int num2 = i < parts2.length && !parts2[i].isEmpty() ? Integer.parseInt(parts2[i]) : 0;

                if (num1 < num2) return -1;
                if (num1 > num2) return 1;
            }
            return 0;
        } catch (NumberFormatException e) {
            return v1.compareTo(v2);
        }
    }

    private static class ParsedVersion {
        final String baseVersion;
        final boolean isPreRelease;

        ParsedVersion(String baseVersion, boolean isPreRelease) {
            this.baseVersion = baseVersion;
            this.isPreRelease = isPreRelease;
        }
    }
}