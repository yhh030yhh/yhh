package com.clipvault.storage;

import com.clipvault.model.AppSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SettingsManagerTest {

    @TempDir
    Path tempDir;

    private SettingsManager settingsManager;
    private Path tempSettingsFile;

    @BeforeEach
    void setUp() throws IOException {
        tempSettingsFile = tempDir.resolve("settings.json");
        System.setProperty("APPDATA", tempDir.toString());
        settingsManager = new SettingsManager();
    }

    @AfterEach
    void tearDown() {
        if (tempSettingsFile != null && Files.exists(tempSettingsFile)) {
            try {
                Files.delete(tempSettingsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void testDefaultSettings() {
        AppSettings settings = settingsManager.getSettings();

        assertNotNull(settings);
        assertNotNull(settings.getHotkey());
        assertFalse(settings.isAutoStart());
        assertTrue(settings.isDarkTheme());
    }

    @Test
    void testUpdateSettings() {
        AppSettings newSettings = new AppSettings();
        newSettings.setHotkey("CTRL+ALT+X");
        newSettings.setAutoStart(true);
        newSettings.setDarkTheme(false);

        settingsManager.updateSettings(newSettings);

        AppSettings loaded = settingsManager.getSettings();
        assertEquals("CTRL+ALT+X", loaded.getHotkey());
        assertTrue(loaded.isAutoStart());
        assertFalse(loaded.isDarkTheme());
    }

    @Test
    void testSettingsPersistence() {
        AppSettings settings = new AppSettings();
        settings.setHotkey("CTRL+SHIFT+A");
        settings.setMaxHistoryCount(100);

        settingsManager.updateSettings(settings);

        SettingsManager newManager = new SettingsManager();
        AppSettings loaded = newManager.getSettings();

        assertEquals(100, loaded.getMaxHistoryCount());
    }

    @Test
    void testSetHotkey() {
        settingsManager.getSettings().setHotkey("CTRL+SHIFT+B");
        settingsManager.save();

        SettingsManager newManager = new SettingsManager();
        assertEquals("CTRL+SHIFT+B", newManager.getSettings().getHotkey());
    }

    @Test
    void testSetAutoStart() {
        settingsManager.getSettings().setAutoStart(true);
        settingsManager.save();

        SettingsManager newManager = new SettingsManager();
        assertTrue(newManager.getSettings().isAutoStart());
    }

    @Test
    void testSetDarkTheme() {
        settingsManager.getSettings().setDarkTheme(false);
        settingsManager.save();

        SettingsManager newManager = new SettingsManager();
        assertFalse(newManager.getSettings().isDarkTheme());
    }

    @Test
    void testClearHistoryOnExit() {
        settingsManager.getSettings().setClearHistoryOnExit(true);
        settingsManager.save();

        SettingsManager newManager = new SettingsManager();
        assertTrue(newManager.getSettings().isClearHistoryOnExit());
    }

    @Test
    void testMaxHistoryCount() {
        settingsManager.getSettings().setMaxHistoryCount(500);
        settingsManager.save();

        SettingsManager newManager = new SettingsManager();
        assertEquals(500, newManager.getSettings().getMaxHistoryCount());
    }

    @Test
    void testFirstRunFlag() {
        settingsManager.getSettings().setFirstRun(false);
        settingsManager.save();

        SettingsManager newManager = new SettingsManager();
        assertFalse(newManager.getSettings().isFirstRun());
    }

    @Test
    void testSavePath() {
        settingsManager.getSettings().setSavePath("D:\\CustomPath");
        settingsManager.save();

        SettingsManager newManager = new SettingsManager();
        assertEquals("D:\\CustomPath", newManager.getSettings().getSavePath());
    }

    @Test
    void testAllSettingsModified() {
        AppSettings settings = new AppSettings();
        settings.setHotkey("CTRL+ALT+V");
        settings.setAutoStart(true);
        settings.setDarkTheme(false);
        settings.setClearHistoryOnExit(true);
        settings.setMaxHistoryCount(200);
        settings.setFirstRun(false);
        settings.setSavePath("E:\\Data");

        settingsManager.updateSettings(settings);

        SettingsManager newManager = new SettingsManager();
        AppSettings loaded = newManager.getSettings();

        assertEquals("CTRL+ALT+V", loaded.getHotkey());
        assertTrue(loaded.isAutoStart());
        assertFalse(loaded.isDarkTheme());
        assertTrue(loaded.isClearHistoryOnExit());
        assertEquals(200, loaded.getMaxHistoryCount());
        assertFalse(loaded.isFirstRun());
        assertEquals("E:\\Data", loaded.getSavePath());
    }
}
