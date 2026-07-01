package com.clipvault.ui;

import com.clipvault.App;
import com.clipvault.model.AppSettings;
import com.clipvault.storage.SettingsManager;
import com.clipvault.win32.Win32.*;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.logging.Logger;

public class SettingsWindow {
    private static final Logger logger = Logger.getLogger(SettingsWindow.class.getName());
    private static final double SHADOW_PADDING = 24;
    private static final double WINDOW_WIDTH = 500;
    private static final double WINDOW_HEIGHT = 520;

    private static final int WM_NCLBUTTONDOWN = 0x00A1;
    private static final int HTCAPTION = 2;
    private static final NativeLibrary user32Lib = NativeLibrary.getInstance("user32");

    private final SettingsManager settingsManager;
    private final App app;
    private final Stage stage;
    private final TextField hotkeyField;
    private boolean darkTheme = true;
    private StackPane root;
    private HWND nativeHwnd = null;

    public SettingsWindow(SettingsManager settingsManager, App app) {
        this.settingsManager = settingsManager;
        this.app = app;
        this.stage = new Stage();
        this.hotkeyField = new TextField();
        darkTheme = settingsManager.getSettings().isDarkTheme();
        setupStage();
    }

    private void setupStage() {
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("设置 - ClipVault");
        stage.setWidth(WINDOW_WIDTH + SHADOW_PADDING * 2);
        stage.setHeight(WINDOW_HEIGHT + SHADOW_PADDING * 2);
        stage.setResizable(false);

        root = new StackPane();
        root.getStyleClass().add("settings-root");
        root.setPadding(new Insets(SHADOW_PADDING));
        root.setOpacity(0.0);

        StackPane container = new StackPane();
        container.getStyleClass().add("settings-container");
        container.getChildren().add(createContent());

        root.getChildren().add(container);

        Scene scene = new Scene(root, WINDOW_WIDTH + SHADOW_PADDING * 2, WINDOW_HEIGHT + SHADOW_PADDING * 2);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        applyTheme(darkTheme);
    }

    private VBox createContent() {
        VBox contentRoot = new VBox(0);

        // ===== Header bar (close button + drag region) =====
        HBox headerBar = new HBox();
        headerBar.getStyleClass().add("settings-header-bar");

        Region dragSpacer = new Region();
        HBox.setHgrow(dragSpacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("settings-close-btn");
        closeBtn.setOnAction(e -> hide());

        headerBar.getChildren().addAll(dragSpacer, closeBtn);

        // Win32 native window dragging (same as MainPanel)
        headerBar.setOnMousePressed(e -> {
            if (nativeHwnd == null) {
                nativeHwnd = User32.INSTANCE.GetForegroundWindow();
            }
            if (nativeHwnd != null) {
                try {
                    Function releaseCapture = user32Lib.getFunction("ReleaseCapture");
                    releaseCapture.invoke(Void.class, new Object[0]);
                    User32.INSTANCE.SendMessageW(
                            nativeHwnd, WM_NCLBUTTONDOWN,
                            new WPARAM(HTCAPTION), new LPARAM(0));
                    RECT rect = new RECT();
                    if (User32.INSTANCE.GetWindowRect(nativeHwnd, rect)) {
                        stage.setX(rect.left);
                        stage.setY(rect.top);
                    }
                } catch (Exception ex) {
                    logger.log(java.util.logging.Level.SEVERE, "Native drag via WM_NCLBUTTONDOWN failed", ex);
                }
            }
        });

        // ===== Card list with scroll =====
        VBox cardList = new VBox(10);
        cardList.getStyleClass().add("settings-card-list");

        cardList.getChildren().add(createHotkeyCard());
        cardList.getChildren().add(createThemeCard());
        cardList.getChildren().add(createPathCard());
        cardList.getChildren().add(createBehaviorCard());
        cardList.getChildren().add(createHistoryCard());

        ScrollPane scrollPane = new ScrollPane(cardList);
        scrollPane.getStyleClass().add("settings-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFocusTraversable(false);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        contentRoot.getChildren().addAll(headerBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return contentRoot;
    }

    // ===== Card builders =====

    private VBox createHotkeyCard() {
        VBox card = createCard("快捷键", "设置显示/隐藏剪贴板的快捷键");
        hotkeyField.setText(settingsManager.getSettings().getHotkey());
        hotkeyField.setEditable(false);
        hotkeyField.setPrefWidth(220);
        hotkeyField.getStyleClass().add("settings-text-field");
        hotkeyField.setOnMouseClicked(e -> hotkeyField.requestFocus());
        hotkeyField.setOnKeyPressed(e -> {
            String hotkey = formatHotkey(e);
            if (!hotkey.isEmpty()) {
                hotkeyField.setText(hotkey);
                if (!hotkey.equals(settingsManager.getSettings().getHotkey())) {
                    settingsManager.getSettings().setHotkey(hotkey);
                    settingsManager.save();
                    app.refreshHotkey(hotkey);
                }
            }
            e.consume();
        });
        card.getChildren().add(hotkeyField);
        return card;
    }

    private VBox createThemeCard() {
        VBox card = createCard("主题", "选择您喜欢的界面主题");
        ToggleGroup themeGroup = new ToggleGroup();
        ToggleButton darkBtn = new ToggleButton("深色主题");
        ToggleButton lightBtn = new ToggleButton("浅色主题");
        darkBtn.setToggleGroup(themeGroup);
        lightBtn.setToggleGroup(themeGroup);
        darkBtn.getStyleClass().add("settings-theme-button");
        lightBtn.getStyleClass().add("settings-theme-button");
        if (settingsManager.getSettings().isDarkTheme()) darkBtn.setSelected(true);

        darkBtn.selectedProperty().addListener((obs, was, is) -> {
            if (is) {
                settingsManager.getSettings().setDarkTheme(true);
                settingsManager.save();
                applyTheme(true);
                app.applyTheme(true);
            }
        });
        lightBtn.selectedProperty().addListener((obs, was, is) -> {
            if (is) {
                settingsManager.getSettings().setDarkTheme(false);
                settingsManager.save();
                applyTheme(false);
                app.applyTheme(false);
            }
        });

        HBox themeBox = new HBox(12);
        themeBox.getChildren().addAll(darkBtn, lightBtn);
        card.getChildren().add(themeBox);
        return card;
    }

    private VBox createPathCard() {
        VBox card = createCard("保存路径", "设置剪贴板内容的保存位置");
        TextField savePathField = new TextField(settingsManager.getSettings().getSavePath());
        savePathField.setPromptText("默认保存到 AppData");
        savePathField.getStyleClass().add("settings-text-field");

        Button browseButton = new Button("浏览");
        browseButton.getStyleClass().add("settings-button");
        browseButton.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("选择保存目录");
            java.io.File dir = dc.showDialog(stage);
            if (dir != null) savePathField.setText(dir.getAbsolutePath());
        });
        Button resetPathButton = new Button("重置");
        resetPathButton.getStyleClass().add("settings-button-secondary");
        resetPathButton.setOnAction(e -> {
            savePathField.setText("");
            settingsManager.getSettings().setSavePath("");
            settingsManager.save();
            app.updateSavePath("");
        });
        savePathField.textProperty().addListener((obs, old, val) -> {
            settingsManager.getSettings().setSavePath(val);
            settingsManager.save();
            app.updateSavePath(val);
        });

        HBox pathBox = new HBox(10);
        pathBox.getChildren().addAll(savePathField, browseButton, resetPathButton);
        card.getChildren().add(pathBox);
        return card;
    }

    private VBox createBehaviorCard() {
        VBox card = createCard("行为设置", "配置应用程序的自动化行为");

        CheckBox autoStartCheck = new CheckBox("开机时自动启动 ClipVault");
        autoStartCheck.getStyleClass().add("settings-checkbox");
        autoStartCheck.setSelected(settingsManager.getSettings().isAutoStart());
        autoStartCheck.selectedProperty().addListener((obs, was, is) -> {
            settingsManager.getSettings().setAutoStart(is);
            settingsManager.save();
            setAutoStart(is);
        });

        CheckBox clearCheck = new CheckBox("退出时自动清除所有历史记录");
        clearCheck.getStyleClass().add("settings-checkbox");
        clearCheck.setSelected(settingsManager.getSettings().isClearHistoryOnExit());
        clearCheck.selectedProperty().addListener((obs, was, is) -> {
            settingsManager.getSettings().setClearHistoryOnExit(is);
            settingsManager.save();
        });

        card.getChildren().addAll(autoStartCheck, clearCheck);
        return card;
    }

    private VBox createHistoryCard() {
        VBox card = createCard("历史记录", "限制保存的剪贴板历史条数（0 表示不限制）");
        Spinner<Integer> maxSpinner = new Spinner<>(0, 10000, settingsManager.getSettings().getMaxHistoryCount(), 10);
        maxSpinner.setEditable(true);
        maxSpinner.setPrefWidth(120);
        maxSpinner.getStyleClass().add("settings-spinner");
        maxSpinner.getValueFactory().valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                settingsManager.getSettings().setMaxHistoryCount(val);
                settingsManager.save();
            }
        });
        card.getChildren().add(maxSpinner);
        return card;
    }

    // ===== Helpers =====

    /** Create a card with title and description — returns the card, caller adds controls */
    private VBox createCard(String title, String description) {
        VBox card = new VBox(10);
        card.getStyleClass().add("settings-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-card-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("settings-card-desc");

        VBox header = new VBox(2);
        header.getChildren().addAll(titleLabel, descLabel);
        card.getChildren().add(header);
        return card;
    }

    // ===== Existing functionality =====

    private String formatHotkey(javafx.scene.input.KeyEvent e) {
        if (e.getCode().isModifierKey()) return "";
        String keyText = getKeyText(e.getCode());
        if (keyText.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        if (e.isAltDown()) sb.append("ALT+");
        if (e.isControlDown()) sb.append("CTRL+");
        if (e.isShiftDown()) sb.append("SHIFT+");
        if (e.isMetaDown()) sb.append("WIN+");
        sb.append(keyText);
        return sb.toString();
    }

    private String getKeyText(KeyCode code) {
        if (code.isLetterKey() || code.isDigitKey()) return code.getName().toUpperCase();
        switch (code) {
            case F1: return "F1"; case F2: return "F2"; case F3: return "F3";
            case F4: return "F4"; case F5: return "F5"; case F6: return "F6";
            case F7: return "F7"; case F8: return "F8"; case F9: return "F9";
            case F10: return "F10"; case F11: return "F11"; case F12: return "F12";
            case UP: return "UP"; case DOWN: return "DOWN";
            case LEFT: return "LEFT"; case RIGHT: return "RIGHT";
            case SPACE: return "SPACE"; case ESCAPE: return "ESC";
            default: return "";
        }
    }

    private void setAutoStart(boolean enabled) {
        try {
            String appPath = System.getProperty("user.dir") + "\\ClipVault.exe";
            String regKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
            ProcessBuilder pb = enabled
                    ? new ProcessBuilder("reg", "add", regKey, "/v", "ClipVault", "/d", appPath, "/f")
                    : new ProcessBuilder("reg", "delete", regKey, "/v", "ClipVault", "/f");
            pb.inheritIO();
            pb.start().waitFor();
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to set auto-start", e);
        }
    }

    private void applyTheme(boolean dark) {
        this.darkTheme = dark;
        if (stage.getScene() == null) return;
        String themeFile = dark ? "styles/dark-theme.css" : "styles/light-theme.css";
        stage.getScene().getStylesheets().clear();
        try {
            stage.getScene().getStylesheets().add(
                getClass().getClassLoader().getResource(themeFile).toExternalForm());
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to load theme: " + themeFile, e);
        }
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
        FadeTransition ft = new FadeTransition(Duration.millis(120), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    public void hide() {
        nativeHwnd = null;
        FadeTransition ft = new FadeTransition(Duration.millis(80), root);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> stage.hide());
        ft.play();
    }
}
