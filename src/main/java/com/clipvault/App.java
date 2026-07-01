package com.clipvault;

import com.clipvault.clipboard.ClipboardMonitor;
import com.clipvault.hotkey.HotkeyManager;
import com.clipvault.model.AppSettings;
import com.clipvault.storage.HistoryManager;
import com.clipvault.storage.SettingsManager;
import com.clipvault.ui.MainPanel;
import com.clipvault.ui.SettingsWindow;
import com.clipvault.ui.TrayManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class App extends Application {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static App instance;

    private SettingsManager settingsManager;
    private HistoryManager historyManager;
    private ClipboardMonitor clipboardMonitor;
    private HotkeyManager hotkeyManager;
    private TrayManager trayManager;
    private MainPanel mainPanel;
    private SettingsWindow settingsWindow;
    private Stage mainStage;
    private boolean isPanelVisible = false;

    @Override
    public void init() throws Exception {
        super.init();
        instance = this;
        logger.info("ClipVault starting...");

        System.setProperty("prism.lcdtext", "true");
        System.setProperty("prism.textlcdfont", "true");
        System.setProperty("prism.allowhidpi", "true");

        settingsManager = new SettingsManager();
        AppSettings settings = settingsManager.getSettings();
        historyManager = new HistoryManager(settings.getSavePath());

        if (settings.isFirstRun()) {
            logger.info("First run detected");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;

        logger.info("Initializing clipboard monitor...");
        clipboardMonitor = new ClipboardMonitor(historyManager);
        clipboardMonitor.setOnChangeCallback(() -> {
            if (mainPanel != null) {
                Platform.runLater(() -> mainPanel.markContentDirty());
            }
        });
        clipboardMonitor.start();

        logger.info("Initializing hotkey manager with hotkey: " + settingsManager.getSettings().getHotkey());
        hotkeyManager = new HotkeyManager(settingsManager.getSettings().getHotkey());
        hotkeyManager.setOnHotkeyPressed(() -> {
            logger.fine("Hotkey pressed callback triggered");
            togglePanel();
        });
        hotkeyManager.setOnRegistrationFailed(() -> {
            logger.severe("Hotkey registration failed callback triggered");
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hotkey Registration Failed");
                alert.setHeaderText("Failed to register hotkey: " + settingsManager.getSettings().getHotkey());
                alert.setContentText("The hotkey may be already in use by another application. Please change the hotkey in settings.");
                alert.showAndWait();
            });
        });
        logger.info("Calling hotkeyManager.register()...");
        hotkeyManager.register();

        logger.info("Initializing main panel...");
        mainPanel = new MainPanel(historyManager, this);
        mainPanel.applyTheme(settingsManager.getSettings().isDarkTheme());
        mainPanel.setOnHideCallback(() -> {
            isPanelVisible = false;
            logger.fine("Panel hidden callback triggered, isPanelVisible set to false");
        });
        settingsWindow = new SettingsWindow(settingsManager, this);

        logger.info("Initializing tray manager...");
        trayManager = new TrayManager(this);

        Platform.setImplicitExit(false);

        logger.info("ClipVault started successfully");
    }

    @Override
    public void stop() throws Exception {
        logger.info("ClipVault shutting down...");

        AppSettings settings = settingsManager.getSettings();
        if (settings.isClearHistoryOnExit()) {
            historyManager.clearAll();
            logger.info("History cleared on exit");
        }

        if (clipboardMonitor != null) {
            clipboardMonitor.stop();
        }

        if (hotkeyManager != null) {
            hotkeyManager.unregister();
        }

        trayManager.destroy();

        if (historyManager != null) {
            historyManager.shutdown();
        }

        logger.info("ClipVault shutdown complete");
        super.stop();
    }

    public void togglePanel() {
        Platform.runLater(() -> {
            if (isPanelVisible) {
                hidePanel();
            } else {
                showPanel();
            }
        });
    }

    public void showPanel() {
        if (mainPanel == null) return;
        mainPanel.show();
        isPanelVisible = true;
        logger.fine("Panel shown");
    }

    public void hidePanel() {
        if (mainPanel == null) return;
        mainPanel.hide();
        isPanelVisible = false;
        logger.fine("Panel hidden");
    }

    public void showSettings() {
        if (settingsWindow == null) return;
        settingsWindow.show();
    }

    public void refreshHotkey(String newHotkey) {
        if (hotkeyManager != null) {
            hotkeyManager.updateHotkey(newHotkey);
        }
    }

    public void applyTheme(boolean darkTheme) {
        if (mainPanel != null) {
            mainPanel.applyTheme(darkTheme);
        }
    }

    public void updateSavePath(String newPath) {
        if (historyManager != null) {
            historyManager.updateSavePath(newPath);
        }
    }

    public static App getInstance() {
        return instance;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public ClipboardMonitor getClipboardMonitor() {
        return clipboardMonitor;
    }

    public boolean isPanelVisible() {
        return isPanelVisible;
    }

    public void exit() {
        Platform.exit();
    }
}
