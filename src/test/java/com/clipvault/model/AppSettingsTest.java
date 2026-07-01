package com.clipvault.model;

import com.clipvault.storage.JsonUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AppSettingsTest {

    @Test
    void testDefaultValues() {
        AppSettings settings = new AppSettings();
        assertEquals("CTRL+SHIFT+V", settings.getHotkey());
        assertFalse(settings.isAutoStart());
        assertTrue(settings.isDarkTheme());
        assertFalse(settings.isClearHistoryOnExit());
        assertEquals(0, settings.getMaxHistoryCount());
        assertTrue(settings.isFirstRun());
        assertEquals("", settings.getSavePath());
    }

    @Test
    void testSettersAndGetters() {
        AppSettings settings = new AppSettings();

        settings.setHotkey("CTRL+ALT+V");
        assertEquals("CTRL+ALT+V", settings.getHotkey());

        settings.setAutoStart(true);
        assertTrue(settings.isAutoStart());

        settings.setDarkTheme(false);
        assertFalse(settings.isDarkTheme());

        settings.setClearHistoryOnExit(true);
        assertTrue(settings.isClearHistoryOnExit());

        settings.setMaxHistoryCount(100);
        assertEquals(100, settings.getMaxHistoryCount());

        settings.setFirstRun(false);
        assertFalse(settings.isFirstRun());

        settings.setSavePath("D:\\Test");
        assertEquals("D:\\Test", settings.getSavePath());
    }

    @Test
    void testJsonSerialization() throws Exception {
        AppSettings settings = new AppSettings();
        settings.setHotkey("CTRL+SHIFT+V");
        settings.setAutoStart(true);
        settings.setDarkTheme(false);

        String json = JsonUtil.toJson(settings);
        assertNotNull(json);
        assertTrue(json.contains("hotkey"));
        assertTrue(json.contains("CTRL+SHIFT+V"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testJsonDeserialization() throws Exception {
        String json = """
            {
              "hotkey": "CTRL+ALT+X",
              "autoStart": true,
              "darkTheme": false,
              "clearHistoryOnExit": true,
              "maxHistoryCount": 50,
              "firstRun": false,
              "savePath": "C:\\\\TestPath"
            }
            """;

        Map<String, Object> map = JsonUtil.parse(json);
        AppSettings settings = new AppSettings();
        JsonUtil.fillSettings(settings, map);

        assertEquals("CTRL+ALT+X", settings.getHotkey());
        assertTrue(settings.isAutoStart());
        assertFalse(settings.isDarkTheme());
        assertTrue(settings.isClearHistoryOnExit());
        assertEquals(50, settings.getMaxHistoryCount());
        assertFalse(settings.isFirstRun());
        assertEquals("C:\\TestPath", settings.getSavePath());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPartialJsonDeserialization() throws Exception {
        String json = """
            {
              "hotkey": "CTRL+T",
              "autoStart": false
            }
            """;

        Map<String, Object> map = JsonUtil.parse(json);
        AppSettings settings = new AppSettings();
        JsonUtil.fillSettings(settings, map);

        assertEquals("CTRL+T", settings.getHotkey());
        assertFalse(settings.isAutoStart());
        assertTrue(settings.isDarkTheme());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFileRoundTrip() throws Exception {
        Path tempFile = Files.createTempFile("settings", ".json");

        AppSettings original = new AppSettings();
        original.setHotkey("CTRL+SHIFT+A");
        original.setAutoStart(true);
        original.setMaxHistoryCount(200);

        String json = JsonUtil.toJson(original);
        Files.write(tempFile, json.getBytes(StandardCharsets.UTF_8));

        String loadedJson = new String(Files.readAllBytes(tempFile), StandardCharsets.UTF_8);
        Map<String, Object> map = JsonUtil.parse(loadedJson);
        AppSettings loaded = new AppSettings();
        JsonUtil.fillSettings(loaded, map);

        assertEquals(original.getHotkey(), loaded.getHotkey());
        assertEquals(original.isAutoStart(), loaded.isAutoStart());
        assertEquals(original.getMaxHistoryCount(), loaded.getMaxHistoryCount());

        Files.deleteIfExists(tempFile);
    }
}
