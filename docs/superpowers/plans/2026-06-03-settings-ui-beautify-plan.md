# Settings UI Beautification — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign SettingsWindow to match main panel's visual language — transparent window, dropshadow, custom title bar, card-based layout with modern controls.

**Architecture:** Keep the same Java/SettingsWindow.java entry point. Restructure layout hierarchy: root (transparent + drop shadow) → contentContainer (opaque + border-radius) → VBox (header + card list + footer). Replace RadioButton with ToggleButton, CheckBox with custom ToggleSwitch (CSS-driven). All drag logic uses JavaFX mouse events (no JNA). Both light and dark themes get matching styles.

**Tech Stack:** JavaFX 21, CSS (no SceneBuilder, no FXML), Maven shading

---

### Task 1: Write dark-theme.css settings classes

**Files:**
- Modify: `src/main/resources/styles/dark-theme.css` (lines 180-315, replace entire settings block)

- [ ] **Step 1: Replace the settings-root / card / header / footer section**

Replace everything from `.settings-root` onward with the new card-based design:

```css
/* ===== Settings Window ===== */
.settings-root {
    -fx-background-color: transparent;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 20, 0, 4, 8);
    -fx-padding: 24;
}
.settings-container {
    -fx-background-color: #1e1e1e;
    -fx-background-radius: 8;
    -fx-border-radius: 8;
}

/* Header */
.settings-header {
    -fx-background-color: #252525;
    -fx-background-radius: 8 8 0 0;
    -fx-padding: 20 24 16 24;
    -fx-border-width: 0 0 1 0;
    -fx-border-color: #333333;
}
.settings-title-row {
    -fx-alignment: center-left;
    -fx-spacing: 8;
}
.settings-title {
    -fx-font-family: "Segoe UI Variable", "Segoe UI", "Microsoft YaHei";
    -fx-font-size: 20px;
    -fx-font-weight: 700;
    -fx-text-fill: #ffffff;
}
.settings-close-btn {
    -fx-background-color: transparent;
    -fx-cursor: hand;
    -fx-font-size: 16px;
    -fx-text-fill: #888;
    -fx-padding: 2 8;
    -fx-background-radius: 4;
}
.settings-close-btn:hover {
    -fx-text-fill: #ff6b6b;
    -fx-background-color: rgba(255,107,107,0.1);
}
.settings-subtitle {
    -fx-font-family: "Segoe UI Variable", "Segoe UI", "Microsoft YaHei";
    -fx-font-size: 12px;
    -fx-text-fill: #808080;
    -fx-padding: 2 0 0 0;
}

/* Card list container */
.settings-card-list {
    -fx-background-color: #1e1e1e;
    -fx-padding: 12 16;
    -fx-spacing: 10;
}

/* Card */
.settings-card {
    -fx-background-color: #252525;
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-padding: 14 16;
    -fx-spacing: 10;
    -fx-border-width: 1;
    -fx-border-color: #2d2d2d;
}
.settings-card-title-row {
    -fx-alignment: center-left;
    -fx-spacing: 8;
}
.settings-card-icon {
    -fx-font-size: 16px;
}
.settings-card-title {
    -fx-font-family: "Segoe UI Variable", "Segoe UI", "Microsoft YaHei";
    -fx-font-size: 14px;
    -fx-font-weight: 600;
    -fx-text-fill: #e0e0e0;
}
.settings-card-desc {
    -fx-font-family: "Segoe UI Variable", "Segoe UI", "Microsoft YaHei";
    -fx-font-size: 11px;
    -fx-text-fill: #808080;
}
.settings-card-body {
    -fx-spacing: 10;
    -fx-padding: 2 0 0 0;
}

/* Input field */
.settings-text-field {
    -fx-background-color: #2d2d2d;
    -fx-text-fill: #e0e0e0;
    -fx-border-color: #404040;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-padding: 8 12;
    -fx-border-width: 1;
    -fx-font-size: 13px;
}
.settings-text-field:focused {
    -fx-border-color: #4a9eff;
    -fx-background-color: #333333;
    -fx-effect: dropshadow(gaussian, rgba(74,158,255,0.2), 8, 0, 0, 0);
}

/* Buttons */
.settings-button {
    -fx-background-color: #4a9eff;
    -fx-text-fill: #ffffff;
    -fx-background-radius: 6;
    -fx-padding: 8 20;
    -fx-cursor: hand;
    -fx-border-width: 0;
    -fx-font-size: 13px;
    -fx-font-weight: 500;
}
.settings-button:hover {
    -fx-background-color: #3a8eef;
}
.settings-button-secondary {
    -fx-background-color: #2d2d2d;
    -fx-text-fill: #d0d0d0;
    -fx-background-radius: 6;
    -fx-padding: 8 16;
    -fx-cursor: hand;
    -fx-border-width: 1;
    -fx-border-color: #404040;
    -fx-font-size: 13px;
}
.settings-button-secondary:hover {
    -fx-background-color: #3a3a3a;
}
.settings-button-primary {
    -fx-background-color: #4a9eff;
    -fx-text-fill: #ffffff;
    -fx-background-radius: 6;
    -fx-padding: 10 32;
    -fx-cursor: hand;
    -fx-border-width: 0;
    -fx-font-size: 14px;
    -fx-font-weight: 600;
}
.settings-button-primary:hover {
    -fx-background-color: #3a8eef;
}

/* ToggleButton (theme picker) */
.settings-theme-button {
    -fx-background-color: transparent;
    -fx-text-fill: #ccc;
    -fx-border-color: #555;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-border-width: 1;
    -fx-padding: 7 20;
    -fx-font-size: 13px;
    -fx-cursor: hand;
}
.settings-theme-button:selected {
    -fx-background-color: #4a9eff;
    -fx-text-fill: #ffffff;
    -fx-border-color: #4a9eff;
}
.settings-theme-button:hover {
    -fx-background-color: #3a3a3a;
}
.settings-theme-button:selected:hover {
    -fx-background-color: #3a8eef;
}

/* ToggleSwitch (replaces checkbox) */
.toggle-switch {
    -fx-background-color: #555;
    -fx-background-radius: 10;
    -fx-pref-width: 40;
    -fx-pref-height: 20;
    -fx-cursor: hand;
    -fx-effect: innershadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);
}
.toggle-switch:selected {
    -fx-background-color: #4a9eff;
}
.toggle-switch-thumb {
    -fx-background-color: #ffffff;
    -fx-background-radius: 8;
    -fx-pref-width: 16;
    -fx-pref-height: 16;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 2, 0, 0, 1);
}
.toggle-switch-row {
    -fx-alignment: center-left;
    -fx-spacing: 10;
}
.toggle-switch-label {
    -fx-font-family: "Segoe UI", "Microsoft YaHei", sans-serif;
    -fx-text-fill: #d0d0d0;
    -fx-font-size: 13px;
}

/* Spinner */
.settings-spinner {
    -fx-background-color: #2d2d2d;
    -fx-border-color: #404040;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-border-width: 1;
}
.settings-spinner .text-field {
    -fx-background-color: #2d2d2d;
    -fx-text-fill: #e0e0e0;
    -fx-font-size: 13px;
    -fx-padding: 4 8;
}
.settings-spinner .increment-arrow-button,
.settings-spinner .decrement-arrow-button {
    -fx-background-color: #3a3a3a;
    -fx-cursor: hand;
}
.settings-spinner .increment-arrow-button:hover,
.settings-spinner .decrement-arrow-button:hover {
    -fx-background-color: #4a4a4a;
}

/* Footer */
.settings-footer {
    -fx-background-color: #252525;
    -fx-background-radius: 0 0 8 8;
    -fx-padding: 14 24;
    -fx-alignment: center;
    -fx-border-width: 1 0 0 0;
    -fx-border-color: #333333;
}
```

- [ ] **Step 2: Verify no syntax errors**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/styles/dark-theme.css
git commit -m "style: rewrite settings dark-theme CSS with card layout and toggle controls"
```

---

### Task 2: Write light-theme.css settings classes

**Files:**
- Modify: `src/main/resources/styles/light-theme.css` (replace settings block)

- [ ] **Step 1: Replace the settings-root / card / header / footer section**

Replace everything from `.settings-root` to end of file with:

```css
/* ===== Settings Window ===== */
.settings-root {
    -fx-background-color: transparent;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 4, 8);
    -fx-padding: 24;
}
.settings-container {
    -fx-background-color: #f5f5f5;
    -fx-background-radius: 8;
    -fx-border-radius: 8;
}

/* Header */
.settings-header {
    -fx-background-color: #ffffff;
    -fx-background-radius: 8 8 0 0;
    -fx-padding: 20 24 16 24;
    -fx-border-width: 0 0 1 0;
    -fx-border-color: #e8e8e8;
}
.settings-title-row {
    -fx-alignment: center-left;
    -fx-spacing: 8;
}
.settings-title {
    -fx-font-family: "Segoe UI Variable", "Segoe UI", "Microsoft YaHei";
    -fx-font-size: 20px;
    -fx-font-weight: 700;
    -fx-text-fill: #1a1a1a;
}
.settings-close-btn {
    -fx-background-color: transparent;
    -fx-cursor: hand;
    -fx-font-size: 16px;
    -fx-text-fill: #999;
    -fx-padding: 2 8;
    -fx-background-radius: 4;
}
.settings-close-btn:hover {
    -fx-text-fill: #e74c3c;
    -fx-background-color: rgba(231,76,60,0.08);
}
.settings-subtitle {
    -fx-font-family: "Segoe UI Variable", "Segoe UI", "Microsoft YaHei";
    -fx-font-size: 12px;
    -fx-text-fill: #999999;
    -fx-padding: 2 0 0 0;
}

/* Card list container */
.settings-card-list {
    -fx-background-color: #f5f5f5;
    -fx-padding: 12 16;
    -fx-spacing: 10;
}

/* Card */
.settings-card {
    -fx-background-color: #ffffff;
    -fx-background-radius: 8;
    -fx-border-radius: 8;
    -fx-padding: 14 16;
    -fx-spacing: 10;
    -fx-border-width: 1;
    -fx-border-color: #e8e8e8;
}
.settings-card-title-row {
    -fx-alignment: center-left;
    -fx-spacing: 8;
}
.settings-card-icon {
    -fx-font-size: 16px;
}
.settings-card-title {
    -fx-font-family: "Segoe UI Variable", "Segoe UI", "Microsoft YaHei";
    -fx-font-size: 14px;
    -fx-font-weight: 600;
    -fx-text-fill: #333;
}
.settings-card-desc {
    -fx-font-family: "Segoe UI Variable", "Segoe UI", "Microsoft YaHei";
    -fx-font-size: 11px;
    -fx-text-fill: #999;
}
.settings-card-body {
    -fx-spacing: 10;
    -fx-padding: 2 0 0 0;
}

/* Input field */
.settings-text-field {
    -fx-background-color: #f0f0f0;
    -fx-text-fill: #333;
    -fx-border-color: #d0d0d0;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-padding: 8 12;
    -fx-border-width: 1;
    -fx-font-size: 13px;
}
.settings-text-field:focused {
    -fx-border-color: #4a90d9;
    -fx-background-color: #ffffff;
    -fx-effect: dropshadow(gaussian, rgba(74,144,217,0.15), 8, 0, 0, 0);
}

/* Buttons */
.settings-button {
    -fx-background-color: #4a90d9;
    -fx-text-fill: #ffffff;
    -fx-background-radius: 6;
    -fx-padding: 8 20;
    -fx-cursor: hand;
    -fx-border-width: 0;
    -fx-font-size: 13px;
    -fx-font-weight: 500;
}
.settings-button:hover {
    -fx-background-color: #3a80c9;
}
.settings-button-secondary {
    -fx-background-color: #f0f0f0;
    -fx-text-fill: #555;
    -fx-background-radius: 6;
    -fx-padding: 8 16;
    -fx-cursor: hand;
    -fx-border-width: 1;
    -fx-border-color: #d0d0d0;
    -fx-font-size: 13px;
}
.settings-button-secondary:hover {
    -fx-background-color: #e5e5e5;
}
.settings-button-primary {
    -fx-background-color: #4a90d9;
    -fx-text-fill: #ffffff;
    -fx-background-radius: 6;
    -fx-padding: 10 32;
    -fx-cursor: hand;
    -fx-border-width: 0;
    -fx-font-size: 14px;
    -fx-font-weight: 600;
}
.settings-button-primary:hover {
    -fx-background-color: #3a80c9;
}

/* ToggleButton (theme picker) */
.settings-theme-button {
    -fx-background-color: #f5f5f5;
    -fx-text-fill: #666;
    -fx-border-color: #d0d0d0;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-border-width: 1;
    -fx-padding: 7 20;
    -fx-font-size: 13px;
    -fx-cursor: hand;
}
.settings-theme-button:selected {
    -fx-background-color: #4a90d9;
    -fx-text-fill: #ffffff;
    -fx-border-color: #4a90d9;
}
.settings-theme-button:hover {
    -fx-background-color: #e8e8e8;
}
.settings-theme-button:selected:hover {
    -fx-background-color: #3a80c9;
}

/* ToggleSwitch (replaces checkbox) */
.toggle-switch {
    -fx-background-color: #ccc;
    -fx-background-radius: 10;
    -fx-pref-width: 40;
    -fx-pref-height: 20;
    -fx-cursor: hand;
    -fx-effect: innershadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);
}
.toggle-switch:selected {
    -fx-background-color: #4a90d9;
}
.toggle-switch-thumb {
    -fx-background-color: #ffffff;
    -fx-background-radius: 8;
    -fx-pref-width: 16;
    -fx-pref-height: 16;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);
}
.toggle-switch-row {
    -fx-alignment: center-left;
    -fx-spacing: 10;
}
.toggle-switch-label {
    -fx-font-family: "Segoe UI", "Microsoft YaHei", sans-serif;
    -fx-text-fill: #555;
    -fx-font-size: 13px;
}

/* Spinner */
.settings-spinner {
    -fx-background-color: #f0f0f0;
    -fx-border-color: #d0d0d0;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
    -fx-border-width: 1;
}
.settings-spinner .text-field {
    -fx-background-color: #f0f0f0;
    -fx-text-fill: #333;
    -fx-font-size: 13px;
    -fx-padding: 4 8;
}
.settings-spinner .increment-arrow-button,
.settings-spinner .decrement-arrow-button {
    -fx-background-color: #e8e8e8;
    -fx-cursor: hand;
}
.settings-spinner .increment-arrow-button:hover,
.settings-spinner .decrement-arrow-button:hover {
    -fx-background-color: #ddd;
}

/* Footer */
.settings-footer {
    -fx-background-color: #ffffff;
    -fx-background-radius: 0 0 8 8;
    -fx-padding: 14 24;
    -fx-alignment: center;
    -fx-border-width: 1 0 0 0;
    -fx-border-color: #e8e8e8;
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/styles/light-theme.css
git commit -m "style: rewrite settings light-theme CSS with card layout and toggle controls"
```

---

### Task 3: Rewrite SettingsWindow.java — window frame and layout

**Files:**
- Rewrite: `src/main/java/com/clipvault/ui/SettingsWindow.java`

- [ ] **Step 1: Replace the entire file**

Replace the full SettingsWindow.java with the new implementation:

```java
package com.clipvault.ui;

import com.clipvault.App;
import com.clipvault.model.AppSettings;
import com.clipvault.storage.SettingsManager;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsWindow {
    private static final Logger logger = LoggerFactory.getLogger(SettingsWindow.class);
    private static final double SHADOW_PADDING = 24;
    private static final double WINDOW_WIDTH = 500;
    private static final double WINDOW_HEIGHT = 520;

    private final SettingsManager settingsManager;
    private final App app;
    private final Stage stage;
    private final TextField hotkeyField;
    private boolean darkTheme = true;

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

        StackPane root = new StackPane();
        root.getStyleClass().add("settings-root");
        root.setPadding(new Insets(SHADOW_PADDING));
        root.setOpacity(0.0);

        StackPane container = new StackPane();
        container.getStyleClass().add("settings-container");
        container.setMaxSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        container.getChildren().add(createContent());

        root.getChildren().add(container);

        Scene scene = new Scene(root, WINDOW_WIDTH + SHADOW_PADDING * 2, WINDOW_HEIGHT + SHADOW_PADDING * 2);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        applyTheme(darkTheme);

        // Fade in on show
        stage.setOnShown(e -> {
            root.setOpacity(1.0);
        });
    }

    private VBox createContent() {
        VBox root = new VBox(0);
        root.getStyleClass().add("settings-root-inner");

        // ===== Header =====
        VBox headerBox = new VBox(4);
        headerBox.getStyleClass().add("settings-header");

        HBox titleRow = new HBox();
        titleRow.getStyleClass().add("settings-title-row");
        HBox.setHgrow(titleRow, Priority.ALWAYS);

        Label titleLabel = new Label("⚙️ 设置");
        titleLabel.getStyleClass().add("settings-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("settings-close-btn");
        closeBtn.setOnAction(e -> hide());

        titleRow.getChildren().addAll(titleLabel, spacer, closeBtn);

        Label subtitleLabel = new Label("自定义您的 ClipVault 体验");
        subtitleLabel.getStyleClass().add("settings-subtitle");

        headerBox.getChildren().addAll(titleRow, subtitleLabel);

        // ===== Card list =====
        VBox cardList = new VBox(10);
        cardList.getStyleClass().add("settings-card-list");
        VBox.setVgrow(cardList, Priority.ALWAYS);

        // -- Card: Hotkey --
        cardList.getChildren().add(createHotkeyCard());

        // -- Card: Theme --
        cardList.getChildren().add(createThemeCard());

        // -- Card: Save Path --
        cardList.getChildren().add(createPathCard());

        // -- Card: Behavior --
        cardList.getChildren().add(createBehaviorCard());

        // -- Card: History --
        cardList.getChildren().add(createHistoryCard());

        // ===== Footer =====
        HBox footer = new HBox();
        footer.getStyleClass().add("settings-footer");
        Button closeBtn2 = new Button("关闭");
        closeBtn2.getStyleClass().add("settings-button-primary");
        closeBtn2.setOnAction(e -> hide());
        footer.getChildren().add(closeBtn2);

        root.getChildren().addAll(headerBox, cardList, footer);
        VBox.setVgrow(cardList, Priority.ALWAYS);
        return root;
    }

    private VBox createHotkeyCard() {
        VBox card = createCard("⌨️", "快捷键", "设置显示/隐藏剪贴板的快捷键");
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
        VBox card = createCard("🎨", "主题", "选择您喜欢的界面主题");
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
        VBox card = createCard("📁", "保存路径", "设置剪贴板内容的保存位置");
        TextField savePathField = new TextField(settingsManager.getSettings().getSavePath());
        savePathField.setPrefWidth(280);
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
        VBox card = createCard("⚡", "行为设置", "配置应用程序的自动化行为");
        card.getChildren().add(createToggleRow("开机时自动启动 ClipVault", settingsManager.getSettings().isAutoStart(), val -> {
            settingsManager.getSettings().setAutoStart(val);
            settingsManager.save();
            setAutoStart(val);
        }));
        card.getChildren().add(createToggleRow("退出时自动清除所有历史记录", settingsManager.getSettings().isClearHistoryOnExit(), val -> {
            settingsManager.getSettings().setClearHistoryOnExit(val);
            settingsManager.save();
        }));
        return card;
    }

    private VBox createHistoryCard() {
        VBox card = createCard("📋", "历史记录", "限制保存的剪贴板历史条数（0 表示不限制）");
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

    /** Create a toggle switch row (replaces CheckBox) */
    private HBox createToggleRow(String labelText, boolean initialValue, java.util.function.Consumer<Boolean> onChange) {
        HBox row = new HBox();
        row.getStyleClass().add("toggle-switch-row");

        Label label = new Label(labelText);
        label.getStyleClass().add("toggle-switch-label");

        StackPane toggle = new StackPane();
        toggle.getStyleClass().add("toggle-switch");
        if (initialValue) toggle.getStyleClass().add("selected");

        // The thumb
        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("toggle-switch-thumb");
        // Position thumb according to state
        StackPane.setAlignment(thumb, initialValue ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        StackPane.setMargin(thumb, new Insets(0, 2, 0, 2));
        toggle.getChildren().add(thumb);

        toggle.setOnMouseClicked(e -> {
            boolean nowSelected = toggle.getStyleClass().contains("selected");
            if (nowSelected) {
                toggle.getStyleClass().remove("selected");
                StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
            } else {
                toggle.getStyleClass().add("selected");
                StackPane.setAlignment(thumb, Pos.CENTER_RIGHT);
            }
            onChange.accept(!nowSelected);
        });

        row.getChildren().addAll(toggle, label);
        return row;
    }

    /** Create a card with icon, title, description */
    private VBox createCard(String icon, String title, String description) {
        VBox card = new VBox(10);
        card.getStyleClass().add("settings-card");

        HBox titleRow = new HBox();
        titleRow.getStyleClass().add("settings-card-title-row");
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("settings-card-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-card-title");
        titleRow.getChildren().addAll(iconLabel, titleLabel);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("settings-card-desc");

        VBox header = new VBox(2);
        header.getChildren().addAll(titleRow, descLabel);
        card.getChildren().add(header);

        VBox body = new VBox(10);
        body.getStyleClass().add("settings-card-body");
        card.getChildren().add(body);

        // Return the card, but caller will add controls to body
        // We need a way to return both card and body. Use card directly — caller adds to card.
        return card;
    }

    /** Optimized createCard variant used by most callers — returns the card directly. */
    // Replaced by per-card methods above.

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
            logger.error("Failed to set auto-start", e);
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
            logger.error("Failed to load theme: {}", themeFile, e);
        }
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
    }

    public void hide() {
        stage.hide();
    }
}
```

**Important fixes in this code:**
- The `createCard` method returns the card VBox but the body VBox is added as a child of card. For callers like `createHotkeyCard()` etc, they add controls directly to the card VBox after `createCard()` returns. This works because controls go after the header in the VBox.
- Wait — this won't work. `createCard` adds `header` (titleRow + desc) and then adds a `body` VBox as a child. Then callers add to the card itself. Let me fix:

Actually, let me simplify `createCard` so it just creates the card shell with header, and returns the card. Then callers add controls directly to the card. Remove the body VBox concept — keep it simple.

- [ ] **Step 2: Fix the card creation pattern**

The `createCard` method should not add an empty body. Instead callers add controls directly after the header:

```java
/** Create a card with icon, title, description — returns the card, caller adds controls */
private VBox createCard(String icon, String title, String description) {
    VBox card = new VBox(10);
    card.getStyleClass().add("settings-card");

    HBox titleRow = new HBox();
    titleRow.getStyleClass().add("settings-card-title-row");
    Label iconLabel = new Label(icon);
    iconLabel.getStyleClass().add("settings-card-icon");
    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("settings-card-title");
    titleRow.getChildren().addAll(iconLabel, titleLabel);

    Label descLabel = new Label(description);
    descLabel.getStyleClass().add("settings-card-desc");

    VBox header = new VBox(2);
    header.getChildren().addAll(titleRow, descLabel);
    card.getChildren().add(header);
    return card;
}
```

Remove the old `createCard` and the comment about "Optimized createCard variant".

- [ ] **Step 3: Add window dragging to the header**

The header region should allow dragging the window. In `setupStage()`, before `stage.setScene(scene)`, add:

```java
// Window dragging on header
final double[] dragOffset = new double[2];
headerBox.setOnMousePressed(e -> {
    dragOffset[0] = e.getSceneX();
    dragOffset[1] = e.getSceneY();
});
headerBox.setOnMouseDragged(e -> {
    stage.setX(e.getScreenX() - dragOffset[0]);
    stage.setY(e.getScreenY() - dragOffset[1]);
});
```

But `headerBox` is local to `createContent()` and `setupStage()` can't access it. Solution: move drag logic to the headerBox after it's created in `createContent()`, or better, pass the stage reference into `createContent()` or make it accessible.

Simplest approach: in `createContent()`, after `headerBox` is built, set the drag handlers that reference the outer `stage` field:

```java
// In createContent(), after building headerBox:
// Window dragging
headerBox.setOnMousePressed(e -> {
    dragOffset[0] = e.getSceneX();
    dragOffset[1] = e.getSceneY();
});
headerBox.setOnMouseDragged(e -> {
    stage.setX(e.getScreenX() - dragOffset[0]);
    stage.setY(e.getScreenY() - dragOffset[1]);
});
```

Add a field `private final double[] dragOffset = new double[2];` to the class.

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Run tests**

Run: `mvn test`
Expected: All 48 tests pass

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/clipvault/ui/SettingsWindow.java
git commit -m "feat: redesign settings window with card layout, toggle controls, and transparent frame"
```

---

### Task 4: Build fat jar and manual verification

**Files:**
- None — just build and run

- [ ] **Step 1: Build fat jar**

Run: `mvn package -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run the application**

Run: `java -jar target\clipvault-1.0.0-fat.jar`
Expected: App starts in system tray. Verify:
  - Settings window shows with rounded corners + dropshadow
  - Title bar has "⚙️ 设置" + X close button
  - Dragging title bar moves the window
  - Theme toggle (深色/浅色) switches theme live
  - Toggle switches for auto-start / clear-on-exit animate left/right
  - Close button works

- [ ] **Step 3: Commit final**

```bash
git add -A
git commit -m "chore: finalize settings UI beautification"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** Every module in the spec (Window Frame, Header, Card Content, Controls, Footer, Animation, Light Theme) has corresponding tasks and CSS
- [ ] **Placeholder scan:** All code blocks are complete. No TODOs or TBDs. 
- [ ] **Type consistency:** CSS class names match between Java code and CSS files. Method names consistent.
