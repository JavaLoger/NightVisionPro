package org.loger.signalr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginHashCalculator {
    private static final int BUFFER_SIZE = 8192;
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String calculateHash(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            return "";
        }
        try {
            JarFile jar = new JarFile(jarFile);
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                List<JarEntry> entries = new ArrayList<>();
                Enumeration<JarEntry> en = jar.entries();
                while (en.hasMoreElements()) {
                    entries.add(en.nextElement());
                }
                entries.sort(Comparator.comparing((v0) -> {
                    return v0.getName();
                }));
                byte[] buffer = new byte[8192];
                for (JarEntry entry : entries) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        digest.update(entry.getName().getBytes(StandardCharsets.UTF_8));
                        InputStream is = jar.getInputStream(entry);
                        while (true) {
                            try {
                                int bytesRead = is.read(buffer);
                                if (bytesRead == -1) {
                                    break;
                                }
                                digest.update(buffer, 0, bytesRead);
                            } finally {
                            }
                        }
                        if (is != null) {
                            is.close();
                        }
                    }
                }
                String strBytesToHex = bytesToHex(digest.digest());
                jar.close();
                return strBytesToHex;
            } catch (Throwable th) {
                try {
                    jar.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static File getPluginJarFile(JavaPlugin plugin) {
        if (plugin == null) {
            return null;
        }
        try {
            Class<?> pluginClass = plugin.getClass();
            URL location = pluginClass.getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                File file = new File(location.toURI());
                if (file.exists()) {
                    if (file.getName().endsWith(".jar")) {
                        return file;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[SignalR] Failed to locate plugin JAR file", (Throwable) e);
        }
        return null;
    }

    public static String calculatePluginHash(JavaPlugin plugin) {
        File jarFile = getPluginJarFile(plugin);
        if (jarFile == null) {
            if (plugin != null) {
                plugin.getLogger().warning("[SignalR] Could not locate plugin JAR file for hash calculation");
                return "";
            }
            return "";
        }
        String hash = calculateHash(jarFile);
        if (hash.isEmpty() && plugin != null) {
            plugin.getLogger().warning("[SignalR] Failed to calculate plugin JAR hash");
        }
        return hash;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 255;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[(i * 2) + 1] = HEX_CHARS[v & 15];
        }
        return new String(hexChars);
    }
}
