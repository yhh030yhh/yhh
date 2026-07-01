package com.clipvault.ui;

import com.clipvault.App;
import com.clipvault.model.ClipItem;
import com.clipvault.storage.HistoryManager;
import com.clipvault.ui.components.ImageCard;
import com.clipvault.ui.components.TextCard;
import com.clipvault.win32.WindowsInputManager;
import com.clipvault.win32.Win32.*;
import com.sun.jna.Function;
import javafx.animation.Interpolator;
import javafx.scene.CacheHint;
import com.sun.jna.NativeLibrary;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.application.Platform;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainPanel {
    private static final Logger logger = Logger.getLogger(MainPanel.class.getName());

    private static final double PANEL_WIDTH = 440;
    private static final double PANEL_HEIGHT = 420;
    private static final double SHADOW_PADDING = 24;
    private static final double ANIMATION_DURATION_MS = 120;

    private final HistoryManager historyManager;
    private final App app;
    private final Stage stage;
    private final StackPane root;
    private final VBox contentBox;
    private final TextField searchField;
    private final WindowsInputManager inputManager;
    private final ParallelTransition showAnimation;
    private final ParallelTransition hideAnimation;

    private Runnable onHideCallback;
    private ScrollPane scrollPane;
    private HWND nativeHwnd = null;
    private volatile boolean contentDirty = true;
    private volatile boolean panelVisible = false;
    private volatile boolean isRefreshing = false;
    private volatile boolean preloadStarted = false;

    private static final int WM_NCLBUTTONDOWN = 0x00A1;
    private static final int HTCAPTION = 2;
    private static final NativeLibrary user32Lib = NativeLibrary.getInstance("user32");

    public MainPanel(HistoryManager historyManager, App app) {
        this.historyManager = historyManager;
        this.app = app;
        this.inputManager = new WindowsInputManager();

        this.stage = new Stage();
        this.root = new StackPane();
        this.contentBox = new VBox(0);
        this.searchField = new TextField();

        this.showAnimation = buildShowAnimation();
        this.hideAnimation = buildHideAnimation();

        setupStage();
        setupUI();
        setupEvents();
        refreshContent();
        preloadStage();
        startPreload();
    }

    private ParallelTransition buildShowAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(ANIMATION_DURATION_MS), root);
        scale.setFromX(0.93);
        scale.setFromY(0.93);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.setOnFinished(e -> {
            root.setCache(false);
            root.setCacheHint(CacheHint.DEFAULT);
            root.snapshot(null, null);
        });
        return pt;
    }

    private ParallelTransition buildHideAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(80), root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(80), root);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.93);
        scale.setToY(0.93);

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.setOnFinished(e -> {
            root.setCache(false);
            root.setCacheHint(CacheHint.DEFAULT);
            root.setOpacity(0.0);
            root.setScaleX(1.0);
            root.setScaleY(1.0);
            panelVisible = false;
            stage.hide();
        });
        return pt;
    }

    private void setupStage() {
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.NONE);
        stage.setWidth(PANEL_WIDTH + SHADOW_PADDING * 2);
        stage.setHeight(PANEL_HEIGHT + SHADOW_PADDING * 2);
        stage.setAlwaysOnTop(false);
        stage.setResizable(false);

        Scene scene = new Scene(root, PANEL_WIDTH + SHADOW_PADDING * 2, PANEL_HEIGHT + SHADOW_PADDING * 2);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);

        scene.setOnMouseClicked(e -> {
            if (e.getTarget() == scene.getRoot()) {
                hide();
            }
        });
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hide();
            }
        });
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && panelVisible) {
                hide();
            }
        });
    }

    /** Pre-create the native window so first hotkey show() is instant */
    private void preloadStage() {
        stage.setX(-10000);
        stage.setY(-10000);
        stage.show();
        stage.hide();
    }

    private void startPreload() {
        if (preloadStarted) return;
        preloadStarted = true;

        new Thread(() -> {
            try {
                Thread.sleep(500);
                
                List<ClipItem> items = historyManager.getSortedItems();
                int preloadCount = Math.min(20, items.size());
                
                for (int i = 0; i < preloadCount; i++) {
                    ClipItem item = items.get(i);
                    if (item.isImage()) {
                        ImageCard.preloadImage(item.getFileName(), historyManager);
                        Thread.sleep(50);
                    }
                }
                
                logger.info("Preloaded " + preloadCount + " recent items");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Preload failed", e);
            }
        }).start();
    }

    private void setupUI() {
        // Outer root: transparent background with dropshadow, padded for shadow spread
        root.getStyleClass().add("main-panel");
        root.setPadding(new Insets(SHADOW_PADDING));
        root.setOpacity(0.0);

        // Content container: opaque background with CSS rounded corners — the visual panel
        // Dropshadow on root follows this container's rounded shape
        BorderPane contentContainer = new BorderPane();
        contentContainer.getStyleClass().add("inner-panel");
        contentBox.getStyleClass().add("content-box");

        // --- Top bar ---
        BorderPane topBar = new BorderPane();
        topBar.getStyleClass().add("top-bar");

        Label titleText = new Label("ClipVault");
        titleText.getStyleClass().add("title-text");
        topBar.setLeft(titleText);

        // Native window dragging via Win32
        topBar.setOnMousePressed(e -> {
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
                    logger.log(Level.SEVERE, "Native drag via WM_NCLBUTTONDOWN failed", ex);
                }
            }
        });

        // --- Search area ---
        VBox searchArea = new VBox(8);
        searchArea.getStyleClass().add("search-area");

        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("搜索剪贴板...");

        searchArea.getChildren().add(searchField);

        VBox headerContainer = new VBox();
        headerContainer.getChildren().addAll(topBar, searchArea);
        contentContainer.setTop(headerContainer);

        // --- Scroll content ---
        scrollPane = new ScrollPane(contentBox);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(false);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        setupMiddleClickScroll();
        contentContainer.setCenter(scrollPane);

        // root → contentContainer (dropshadow on root follows contentContainer's rounded shape)
        root.getChildren().add(contentContainer);
    }

    private void setupMiddleClickScroll() {
        final boolean[] isScrolling = {false};
        final double[] anchorY = {0};
        final long[] lastTime = {0};

        final AnimationTimer autoScrollTimer = new AnimationTimer() {
            private static final double MAX_SPEED = 50.0;

            @Override
            public void handle(long now) {
                if (!isScrolling[0]) {
                    stop();
                    return;
                }

                java.awt.Point mousePos = java.awt.MouseInfo.getPointerInfo().getLocation();
                double currentY = mousePos.getY();
                double distance = currentY - anchorY[0];

                double speed = Math.min(Math.abs(distance) / 5.0, MAX_SPEED);
                double direction = distance > 0 ? 1 : -1;
                double scrollAmount = speed * direction;

                double contentHeight = contentBox.getHeight();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                double scrollableHeight = contentHeight - viewportHeight;

                if (scrollableHeight > 0) {
                    double deltaScroll = scrollAmount / scrollableHeight;
                    double newVValue = scrollPane.getVvalue() + deltaScroll;
                    newVValue = Math.max(0, Math.min(1, newVValue));
                    scrollPane.setVvalue(newVValue);
                }
            }
        };

        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                e.consume();
                isScrolling[0] = true;
                anchorY[0] = e.getScreenY();
                root.setCursor(javafx.scene.Cursor.MOVE);
                autoScrollTimer.start();
            }
        });

        root.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                e.consume();
                isScrolling[0] = false;
                root.setCursor(javafx.scene.Cursor.DEFAULT);
                autoScrollTimer.stop();
            }
        });

        root.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (isScrolling[0]) {
                isScrolling[0] = false;
                root.setCursor(javafx.scene.Cursor.DEFAULT);
                autoScrollTimer.stop();
            }
        });
    }

    private void setupEvents() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshContent());
    }

    private void refreshContent() {
        if (isRefreshing) {
            contentDirty = true;
            return;
        }
        isRefreshing = true;
        
        try {
            contentBox.getChildren().clear();

            List<ClipItem> items = getFilteredItems();

            List<ClipItem> pinnedItems = items.stream()
                    .filter(ClipItem::isPinned).collect(Collectors.toList());
            List<ClipItem> normalItems = items.stream()
                    .filter(item -> !item.isPinned()).collect(Collectors.toList());

            int delay = 0;
            int maxAnimationDelay = 400;
            int maxCardsWithAnimation = maxAnimationDelay / 20;
            int animatedCount = 0;
            
            for (ClipItem item : pinnedItems) {
                Node card = createCard(item);
                contentBox.getChildren().add(card);
                if (animatedCount < maxCardsWithAnimation) {
                    animateCardEnter(card, delay);
                    delay += 15;
                    animatedCount++;
                } else {
                    card.setOpacity(1);
                    card.setTranslateY(0);
                    card.setScaleX(1);
                    card.setScaleY(1);
                }
            }
            if (!pinnedItems.isEmpty() && !normalItems.isEmpty()) {
                contentBox.getChildren().add(new Separator());
            }
            for (ClipItem item : normalItems) {
                Node card = createCard(item);
                contentBox.getChildren().add(card);
                if (animatedCount < maxCardsWithAnimation) {
                    animateCardEnter(card, delay);
                    delay += 15;
                    animatedCount++;
                } else {
                    card.setOpacity(1);
                    card.setTranslateY(0);
                    card.setScaleX(1);
                    card.setScaleY(1);
                }
            }

            if (items.isEmpty()) {
                Text emptyText = new Text("暂无记录");
                emptyText.getStyleClass().add("empty-text");
                VBox emptyBox = new VBox(emptyText);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(50));
                contentBox.getChildren().add(emptyBox);
            }
        } finally {
            isRefreshing = false;
            if (contentDirty) {
                Platform.runLater(this::refreshContent);
            }
        }
    }

    private void animateCardEnter(Node card, int delayMs) {
        card.setOpacity(0);
        card.setTranslateY(10);
        card.setScaleX(0.98);
        card.setScaleY(0.98);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(card.opacityProperty(), 0),
                        new KeyValue(card.translateYProperty(), 10),
                        new KeyValue(card.scaleXProperty(), 0.98),
                        new KeyValue(card.scaleYProperty(), 0.98)),
                new KeyFrame(Duration.millis(delayMs),
                        new KeyValue(card.opacityProperty(), 0),
                        new KeyValue(card.translateYProperty(), 10),
                        new KeyValue(card.scaleXProperty(), 0.98),
                        new KeyValue(card.scaleYProperty(), 0.98)),
                new KeyFrame(Duration.millis(delayMs + 120),
                        new KeyValue(card.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(card.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(card.scaleYProperty(), 1, Interpolator.EASE_OUT))
        );
        timeline.play();
    }

    private List<ClipItem> getFilteredItems() {
        String keyword = searchField.getText();
        if (keyword != null && !keyword.trim().isEmpty()) {
            return historyManager.search(keyword);
        }
        return historyManager.getSortedItems();
    }

    private Node createCard(ClipItem item) {
        if (item.isText()) {
            return new TextCard(item, () -> paste(item), () -> delete(item), () -> togglePin(item));
        }
        return new ImageCard(item, historyManager, () -> paste(item), () -> delete(item), () -> togglePin(item));
    }

    private void paste(ClipItem item) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            if (item.isText()) {
                StringSelection selection = new StringSelection(item.getContent());
                clipboard.setContents(selection, null);
            } else if (item.isImage()) {
                java.nio.file.Path imagePath = historyManager.getImagePath(item.getFileName());
                File file = imagePath.toFile();
                if (file.exists()) {
                    ImageSelection imageSelection = new ImageSelection(javax.imageio.ImageIO.read(file), file);
                    clipboard.setContents(imageSelection, null);
                }
            }

            item.setLastUsedAt(java.time.LocalDateTime.now());
            historyManager.save();

            hideWithAnimation();

            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    inputManager.completePaste();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.log(Level.SEVERE, "Paste interrupted", e);
                }
            }).start();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to paste item", e);
        }
    }

    private void delete(ClipItem item) {
        historyManager.deleteItem(item.getId());
        
        if (item.isImage()) {
            clearSystemClipboard();
        }
        
        refreshContent();
    }

    private void clearSystemClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(""), null);
            logger.fine("Cleared system clipboard after deleting image");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to clear system clipboard", e);
        }
    }

    private void togglePin(ClipItem item) {
        historyManager.togglePin(item.getId());
        refreshContent();
    }

    public void show() {
        inputManager.captureForegroundWindow();
        hideAnimation.stop();

        final java.awt.Point mousePoint = java.awt.MouseInfo.getPointerInfo().getLocation();
        final int cursorX = (int) mousePoint.getX();
        final int cursorY = (int) mousePoint.getY();

        calculateAndSetClipboardPosition(cursorX, cursorY);

        root.setOpacity(0.0);
        root.setScaleX(0.93);
        root.setScaleY(0.93);

        root.setCache(true);
        root.setCacheHint(CacheHint.SPEED);

        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();

        nativeHwnd = User32.INSTANCE.GetForegroundWindow();
        panelVisible = true;

        if (contentDirty) {
            refreshContent();
            contentDirty = false;
        }

        searchField.requestFocus();
        showAnimation.playFromStart();
    }

    private void calculateAndSetClipboardPosition(int cursorX, int cursorY) {
        double margin = 10;

        Rectangle2D targetScreen = Screen.getPrimary().getVisualBounds();
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            if (cursorX >= bounds.getMinX() && cursorX < bounds.getMaxX() &&
                cursorY >= bounds.getMinY() && cursorY < bounds.getMaxY()) {
                targetScreen = bounds;
                break;
            }
        }

        double panelX = cursorX + 10;
        double panelY = cursorY + 10;

        if (panelX + PANEL_WIDTH > targetScreen.getMaxX()) {
            panelX = cursorX - PANEL_WIDTH - 10;
        }
        if (panelY + PANEL_HEIGHT > targetScreen.getMaxY()) {
            panelY = cursorY - PANEL_HEIGHT - 10;
        }
        panelX = Math.max(targetScreen.getMinX() + margin,
                Math.min(panelX, targetScreen.getMaxX() - PANEL_WIDTH - margin));
        panelY = Math.max(targetScreen.getMinY() + margin,
                Math.min(panelY, targetScreen.getMaxY() - PANEL_HEIGHT - margin));

        // Offset stage position by shadow padding so content appears at calculated position
        stage.setX(panelX - SHADOW_PADDING);
        stage.setY(panelY - SHADOW_PADDING);
        stage.setWidth(PANEL_WIDTH + SHADOW_PADDING * 2);
        stage.setHeight(PANEL_HEIGHT + SHADOW_PADDING * 2);
    }

    public void setOnHideCallback(Runnable callback) {
        this.onHideCallback = callback;
    }

    public void hide() {
        hideWithAnimation();
    }

    private void hideWithAnimation() {
        if (!panelVisible) return;

        panelVisible = false;
        showAnimation.stop();
        if (onHideCallback != null) {
            onHideCallback.run();
        }
        root.setCache(true);
        root.setCacheHint(CacheHint.SPEED);
        hideAnimation.playFromStart();
    }

    /** Mark clipboard content as changed so the next show() refreshes the UI. */
    public void markContentDirty() {
        this.contentDirty = true;
    }

    public void applyTheme(boolean dark) {
        String themeFile = dark ? "styles/dark-theme.css" : "styles/light-theme.css";
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            try {
                String css = getClass().getClassLoader().getResource(themeFile).toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load theme: " + themeFile, e);
            }
        }
    }

    public void refresh() {
        refreshContent();
    }

    // --- Inner classes ---

    private static class ImageSelection implements java.awt.datatransfer.Transferable, java.awt.datatransfer.ClipboardOwner {
        private final java.awt.Image image;
        private final File file;

        public ImageSelection(java.awt.Image image, File file) {
            this.image = image;
            this.file = file;
        }

        @Override
        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
            return new java.awt.datatransfer.DataFlavor[]{
                    java.awt.datatransfer.DataFlavor.imageFlavor,
                    java.awt.datatransfer.DataFlavor.javaFileListFlavor
            };
        }

        @Override
        public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
            return flavor.equals(java.awt.datatransfer.DataFlavor.imageFlavor) ||
                   flavor.equals(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
        }

        @Override
        public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) {
            if (flavor.equals(java.awt.datatransfer.DataFlavor.imageFlavor)) {
                return image;
            } else if (flavor.equals(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                java.util.List<File> list = new java.util.ArrayList<>();
                list.add(file);
                return list;
            }
            return null;
        }

        @Override
        public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, java.awt.datatransfer.Transferable contents) {
        }
    }

    private static class Separator extends javafx.scene.shape.Rectangle {
        public Separator() {
            getStyleClass().add("separator-line");
            setHeight(1);
            setWidth(300);
        }
    }
}