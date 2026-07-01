package com.clipvault.storage;

import com.clipvault.model.AppSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsManager {
    private static final Logger logger = Logger.getLogger(SettingsManager.class.getName());
    private static final String SETTINGS_FILE = "settings.json";

    private final Path settingsPath;
    private AppSettings settings;

    public SettingsManager() {
        this.settingsPath = getSettingsPath();
        ensureDirectoryExists();
        load();
    }

    private Path getSettingsPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, "ClipVault", SETTINGS_FILE);
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(settingsPath.getParent());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create settings directory", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void load() {
        if (Files.exists(settingsPath)) {
            try {
                String json = new String(Files.readAllBytes(settingsPath), StandardCharsets.UTF_8);
                Map<String, Object> map = JsonUtil.parse(json);
                settings = new AppSettings();
                JsonUtil.fillSettings(settings, map);
                logger.info("Settings loaded from " + settingsPath);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load settings, using defaults", e);
                settings = new AppSettings();
            }
        } else {
            settings = new AppSettings();
            save();
            logger.info("Created default settings");
        }
    }

    public void save() {
        try {
            String json = JsonUtil.toJson(settings);
            Files.write(settingsPath, json.getBytes(StandardCharsets.UTF_8));
            logger.fine("Settings saved to " + settingsPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save settings", e);
        }
    }

    public AppSettings getSettings() {
        return settings;
    }

    public void updateSettings(AppSettings newSettings) {
        this.settings = newSettings;
        save();
    }
}
