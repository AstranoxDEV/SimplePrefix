package de.astranox.simpleprefix.update;

import de.astranox.simpleprefix.SimplePrefix;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateDownloader {

    private final SimplePrefix plugin;

    public UpdateDownloader(SimplePrefix plugin) {
        this.plugin = plugin;
    }

    public File downloadFile(String urlString, File targetFile) throws IOException {
        plugin.getLogger().info("Downloading from: " + urlString);

        HttpURLConnection con = (HttpURLConnection) new URL(urlString).openConnection();
        con.setRequestProperty("User-Agent", "SimplePrefix/" + plugin.getDescription().getVersion());
        con.setConnectTimeout(10000);
        con.setReadTimeout(30000);

        int status = con.getResponseCode();
        if (status != 200) {
            throw new IOException("HTTP " + status + " while downloading from " + urlString);
        }

        File tempFile = new File(targetFile.getAbsolutePath() + ".tmp");

        try (InputStream in = con.getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            plugin.getLogger().info("Downloaded " + (totalBytes / 1024) + " KB");
        }

        if (targetFile.exists()) {
            targetFile.delete();
        }

        if (!tempFile.renameTo(targetFile)) {
            throw new IOException("Failed to rename temporary file to " + targetFile.getName());
        }

        return targetFile;
    }

    public File getPluginJarFile() {
        try {
            File jar = new File(plugin.getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            return jar.isFile() ? jar : null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to locate plugin JAR", e);
            return null;
        }
    }
}
