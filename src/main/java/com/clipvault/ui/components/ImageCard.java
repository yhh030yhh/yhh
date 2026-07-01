package com.clipvault.ui.components;

import com.clipvault.model.ClipItem;
import com.clipvault.storage.HistoryManager;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCard extends Card {
    
    private static final ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 50;
    
    private static Popup previewPopup;
    private static ImageView previewImageView;
    private javafx.animation.PauseTransition hoverTimer;
    private javafx.animation.PauseTransition hideTimer;
    private boolean isPreviewShowing = false;
    private double lastMouseX, lastMouseY;
    private static String lastPreviewedImagePath = null;
    
    private final HistoryManager historyManager;
    private ImageView imageView;
    
    public ImageCard(ClipItem item, HistoryManager historyManager, Runnable onPaste, Runnable onDelete, Runnable onTogglePin) {
        super(item, onPaste, onDelete, onTogglePin);
        this.historyManager = historyManager;
        initializeContent();
    }
    
    @Override
    protected void setupContent() {
        final Path imagePath = historyManager.getImagePath(item.getFileName());
        final Path thumbPath = historyManager.getThumbnailPath(item.getFileName().replace(".png", "_thumb.png"));
        
        if (!Files.exists(imagePath)) {
            Label missingLabel = new Label("文件丢失");
            missingLabel.getStyleClass().add("missing-label");
            setCenter(missingLabel);
            return;
        }
        
        imageView = new ImageView();
        imageView.setFitHeight(28);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("card-image");
        
        HBox imageContainer = new HBox();
        imageContainer.getChildren().add(imageView);
        imageContainer.setAlignment(Pos.CENTER_LEFT);
        imageContainer.setMaxWidth(Double.MAX_VALUE);
        imageContainer.setFillHeight(true);
        imageContainer.setMaxHeight(28);
        imageContainer.setPrefHeight(28);
        imageContainer.setMinHeight(28);
        HBox.setHgrow(imageView, Priority.SOMETIMES);
        
        setupBottomRow();
        
        setCenter(imageContainer);
        setBottom(bottomRow);
        
        String cacheKey = imagePath.toString();
        Image cachedImage = imageCache.get(cacheKey);
        
        if (cachedImage != null) {
            imageView.setImage(cachedImage);
        } else if (Files.exists(thumbPath)) {
            javafx.application.Platform.runLater(() -> {
                try {
                    String thumbUrl = thumbPath.toUri().toASCIIString();
                    Image thumbImage = new Image(thumbUrl, true);
                    thumbImage.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() >= 1.0 && !thumbImage.isError()) {
                            javafx.application.Platform.runLater(() -> {
                                imageView.setImage(thumbImage);
                                cacheImage(cacheKey, thumbImage);
                            });
                        }
                    });
                } catch (Exception e) {
                }
            });
        } else {
            javafx.application.Platform.runLater(() -> {
                try {
                    String imageUrl = imagePath.toUri().toASCIIString();
                    Image image = new Image(imageUrl, true);
                    image.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                            javafx.application.Platform.runLater(() -> {
                                imageView.setImage(image);
                                cacheImage(cacheKey, image);
                            });
                        }
                    });
                } catch (Exception e) {
                }
            });
        }
        
        setupHoverPreview();
    }
    
    private void setupHoverPreview() {
        hoverTimer = new javafx.animation.PauseTransition(Duration.millis(1000));
        hoverTimer.setOnFinished(e -> {
            if (!isPreviewShowing) {
                showPreview();
            }
        });
        
        hideTimer = new javafx.animation.PauseTransition(Duration.millis(300));
        hideTimer.setOnFinished(e -> hidePreview());
        
        setOnMouseEntered(e -> {
            lastMouseX = e.getScreenX();
            lastMouseY = e.getScreenY();
            if (hideTimer != null) {
                hideTimer.stop();
            }
            if (item.isImage()) {
                hoverTimer.playFromStart();
            }
        });
        
        setOnMouseMoved(e -> {
            lastMouseX = e.getScreenX();
            lastMouseY = e.getScreenY();
            if (isPreviewShowing && hideTimer != null) {
                hideTimer.stop();
                updatePreviewPosition();
            }
        });
        
        setOnMouseExited(e -> {
            hoverTimer.stop();
            if (hideTimer != null) {
                hideTimer.playFromStart();
            }
        });
    }
    
    private void updatePreviewPosition() {
        if (previewPopup == null || !isPreviewShowing) {
            return;
        }
        try {
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            double screenWidth = screen.getVisualBounds().getWidth();
            double screenHeight = screen.getVisualBounds().getHeight();
            
            double currentWidth = previewImageView.getFitWidth();
            double currentHeight = previewImageView.getFitHeight();
            
            double previewWidth = currentWidth + 16;
            double previewHeight = currentHeight + 16;
            
            double previewX = lastMouseX > screenWidth / 2 ? lastMouseX - previewWidth - 15 : lastMouseX + 15;
            double previewY = lastMouseY - previewHeight - 15;
            
            if (previewX < 10) previewX = 10;
            if (previewX + previewWidth > screenWidth - 10) previewX = screenWidth - previewWidth - 10;
            if (previewY < 10) previewY = lastMouseY + 15;
            if (previewY + previewHeight > screenHeight - 10) previewY = screenHeight - previewHeight - 10;
            
            previewPopup.setX(previewX);
            previewPopup.setY(previewY);
        } catch (Exception e) {
        }
    }
    
    private void showPreview() {
        try {
            Path imagePath = historyManager.getImagePath(item.getFileName());
            if (!Files.exists(imagePath)) {
                return;
            }
            
            if (lastPreviewedImagePath != null && lastPreviewedImagePath.equals(imagePath.toString())) {
                if (isPreviewShowing) {
                    updatePreviewPosition();
                    return;
                }
            }
            
            lastPreviewedImagePath = imagePath.toString();
            
            String imageUrl = imagePath.toUri().toASCIIString();
            Image fullImage = new Image(imageUrl, false);
            
            if (fullImage.isError()) {
                return;
            }
            
            if (previewPopup == null) {
                previewPopup = new Popup();
                previewPopup.setAutoHide(false);
                previewPopup.setAutoFix(true);
                
                HBox previewRoot = new HBox();
                previewRoot.setAlignment(Pos.CENTER);
                previewRoot.setStyle(
                    "-fx-background-color: rgba(20, 20, 20, 0.9);" +
                    "-fx-background-radius: 6;" +
                    "-fx-padding: 8;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.2, 0, 3);"
                );
                previewRoot.setMouseTransparent(true);
                
                previewImageView = new ImageView();
                previewImageView.setPreserveRatio(true);
                previewImageView.setSmooth(true);
                previewImageView.setMouseTransparent(true);
                
                previewRoot.getChildren().add(previewImageView);
                previewPopup.getContent().add(previewRoot);
            }
            
            previewImageView.setImage(fullImage);
            
            double fixedPreviewWidth = 600;
            double fixedPreviewHeight = 450;
            
            double imgWidth = fullImage.getWidth();
            double imgHeight = fullImage.getHeight();
            
            double widthRatio = fixedPreviewWidth / imgWidth;
            double heightRatio = fixedPreviewHeight / imgHeight;
            double scaleRatio = Math.min(widthRatio, heightRatio);
            
            previewImageView.setFitWidth(imgWidth * scaleRatio);
            previewImageView.setFitHeight(imgHeight * scaleRatio);
            
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            double screenWidth = screen.getVisualBounds().getWidth();
            double screenHeight = screen.getVisualBounds().getHeight();
            
            double previewWidth = imgWidth * scaleRatio + 16;
            double previewHeight = imgHeight * scaleRatio + 16;
            
            double previewX = lastMouseX > screenWidth / 2 ? lastMouseX - previewWidth - 15 : lastMouseX + 15;
            double previewY = lastMouseY - previewHeight - 15;
            
            if (previewX < 10) previewX = 10;
            if (previewX + previewWidth > screenWidth - 10) previewX = screenWidth - previewWidth - 10;
            if (previewY < 10) previewY = lastMouseY + 15;
            if (previewY + previewHeight > screenHeight - 10) previewY = screenHeight - previewHeight - 10;
            
            javafx.stage.Window owner = this.getScene().getWindow();
            if (!previewPopup.isShowing()) {
                previewPopup.show(owner, previewX, previewY);
            } else {
                previewPopup.setX(previewX);
                previewPopup.setY(previewY);
            }
            isPreviewShowing = true;
            
        } catch (Exception e) {
            System.err.println("预览加载失败: " + e.getMessage());
        }
    }
    
    private void hidePreview() {
        if (previewPopup != null && previewPopup.isShowing()) {
            previewPopup.hide();
        }
        isPreviewShowing = false;
    }
    
    private static synchronized void cacheImage(String key, Image image) {
        if (imageCache.size() >= MAX_CACHE_SIZE) {
            int toRemove = MAX_CACHE_SIZE / 2;
            java.util.Iterator<String> iterator = imageCache.keySet().iterator();
            while (iterator.hasNext() && toRemove > 0) {
                iterator.next();
                iterator.remove();
                toRemove--;
            }
        }
        imageCache.put(key, image);
    }
    
    public static void preloadImage(String fileName, HistoryManager historyManager) {
        Path imagePath = historyManager.getImagePath(fileName);
        Path thumbPath = historyManager.getThumbnailPath(fileName.replace(".png", "_thumb.png"));
        String cacheKey = imagePath.toString();
        
        if (imageCache.containsKey(cacheKey)) {
            return;
        }
        
        Path loadPath = Files.exists(thumbPath) ? thumbPath : imagePath;
        if (!Files.exists(loadPath)) {
            return;
        }
        
        new Thread(() -> {
            try {
                String imageUrl = loadPath.toUri().toASCIIString();
                Image image = new Image(imageUrl, true);
                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                        javafx.application.Platform.runLater(() -> {
                            cacheImage(cacheKey, image);
                        });
                    }
                });
            } catch (Exception e) {
            }
        }).start();
    }
}