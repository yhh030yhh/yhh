package com.clipvault;

import javafx.application.Application;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Launcher {
    public static void main(String[] args) {
        // Set file encoding before anything else
        System.setProperty("file.encoding", "UTF-8");

        // Extract and configure AWT font configuration for CJK support
        configureAwtFonts();

        Application.launch(App.class, args);
    }

    /**
     * Extracts a custom fontconfig.properties that includes Simplified Chinese
     * font mappings (chinese-ms936) and sets the sun.awt.fontconfig system
     * property so AWT can properly render CJK text.
     */
    private static void configureAwtFonts() {
        try {
            // Determine the target directory for fontconfig
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isBlank()) {
                appData = System.getProperty("user.home");
            }
            Path configDir = Paths.get(appData, "ClipVault", "fonts");
            Files.createDirectories(configDir);

            Path fontConfigFile = configDir.resolve("fontconfig.properties");

            // Always copy the latest fontconfig from the classpath
            try (InputStream is = Launcher.class.getClassLoader()
                    .getResourceAsStream("fontconfig/fontconfig.properties")) {
                if (is != null) {
                    Files.copy(is, fontConfigFile, StandardCopyOption.REPLACE_EXISTING);
                    System.setProperty("sun.awt.fontconfig", fontConfigFile.toString());
                    System.out.println("[ClipVault] Fontconfig extracted to: " + fontConfigFile);
                } else {
                    System.err.println("[ClipVault] WARNING: fontconfig.properties not found in classpath");
                }
            }
        } catch (Exception e) {
            System.err.println("[ClipVault] WARNING: Failed to configure AWT fonts: " + e.getMessage());
        }

        // Additional AWT font properties for better CJK rendering
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
    }
}
