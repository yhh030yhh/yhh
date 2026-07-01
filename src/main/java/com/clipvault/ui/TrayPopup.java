package com.clipvault.ui;

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/** JavaFX 自绘弹出菜单，替代 AWT PopupMenu，完美支持中文 */
public class TrayPopup {

    private final Stage stage;
    private final VBox root;

    public TrayPopup(boolean darkTheme) {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        stage.setWidth(180);
        stage.setAlwaysOnTop(true);

        root = new VBox(0);
        root.setPadding(new Insets(6, 0, 6, 0));

        String bgColor = darkTheme ? "#2d2d2d" : "#ffffff";
        String shadowColor = darkTheme ? "rgba(0,0,0,0.5)" : "rgba(0,0,0,0.2)";
        root.setStyle(String.format(
            "-fx-background-color: %s; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1; -fx-border-color: %s; -fx-effect: dropshadow(gaussian, %s, 12, 0, 0, 4);",
            bgColor,
            darkTheme ? "#444" : "#ddd",
            shadowColor
        ));

        Scene scene = new Scene(root, 180, -1);  // -1 = auto height from content
        stage.setScene(scene);
        stage.setMinWidth(180);
        stage.setMinHeight(0);

        // Auto-hide on focus loss
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) hide();
        });
    }

    public void addItem(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(34);
        btn.setPadding(new Insets(0, 18, 0, 18));
        String bg = root.getStyle();
        boolean dark = bg.contains("#2d");
        btn.setStyle(btnStyle(dark, false));
        btn.setFocusTraversable(false);

        btn.setOnMouseEntered(e -> btn.setStyle(btnStyle(dark, true)));
        btn.setOnMouseExited(e -> btn.setStyle(btnStyle(dark, false)));
        btn.setOnAction(e -> {
            hide();
            action.run();
        });

        root.getChildren().add(btn);
    }

    public void addSeparator() {
        javafx.scene.control.Label sep = new javafx.scene.control.Label();
        sep.setMaxHeight(1);
        sep.setMinHeight(1);
        sep.setPrefWidth(180);
        boolean dark = root.getStyle().contains("#2d");
        sep.setStyle("-fx-background-color: " + (dark ? "#404040" : "#e0e0e0") + ";");
        root.getChildren().add(sep);
    }

    public void show(double screenX, double screenY) {
        // Compute actual popup size from children (reliable before stage.show())
        double pw = 180;
        double ph = root.getPadding().getTop() + root.getPadding().getBottom();
        for (var child : root.getChildren()) {
            ph += child.prefHeight(pw);
        }
        double margin = 8;

        // Find the screen that contains the mouse position
        Rectangle2D screenBounds = null;
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            if (screenX >= b.getMinX() && screenX < b.getMaxX() &&
                screenY >= b.getMinY() && screenY < b.getMaxY()) {
                screenBounds = b;
                break;
            }
        }
        if (screenBounds == null) {
            screenBounds = Screen.getPrimary().getVisualBounds();
        }

        // Keep popup within screen visual bounds (excludes taskbar)
        if (screenX + pw > screenBounds.getMaxX()) {
            screenX = screenBounds.getMaxX() - pw - margin;
        }
        if (screenY + ph > screenBounds.getMaxY()) {
            screenY = screenBounds.getMaxY() - ph - margin;
        }
        if (screenX < screenBounds.getMinX()) {
            screenX = screenBounds.getMinX() + margin;
        }
        if (screenY < screenBounds.getMinY()) {
            screenY = screenBounds.getMinY() + margin;
        }

        stage.setX(screenX);
        stage.setY(screenY);
        stage.sizeToScene();
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    public void hide() {
        stage.hide();
    }

    private String btnStyle(boolean dark, boolean hover) {
        String bg, fg;
        if (dark) {
            bg = hover ? "#3a3a3a" : "transparent";
            fg = "#e0e0e0";
        } else {
            bg = hover ? "#f0f0f0" : "transparent";
            fg = "#333";
        }
        return String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-family: \"Microsoft YaHei\", \"Segoe UI\", sans-serif; -fx-cursor: hand; -fx-border-width: 0; -fx-background-radius: 0; -fx-alignment: center-left;",
            bg, fg
        );
    }
}
