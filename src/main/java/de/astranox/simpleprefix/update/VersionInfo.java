package de.astranox.simpleprefix.update;

public class VersionInfo {
    private final String version;
    private final String downloadUrl;
    private final String updaterUrl;

    public VersionInfo(String version, String downloadUrl, String updaterUrl) {
        this.version = version;
        this.downloadUrl = downloadUrl;
        this.updaterUrl = updaterUrl;
    }

    public String getVersion() {
        return version;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getUpdaterUrl() {
        return updaterUrl;
    }
}
