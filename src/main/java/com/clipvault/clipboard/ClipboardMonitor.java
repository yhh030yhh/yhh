package com.clipvault.clipboard;

import com.clipvault.model.ClipItem;
import com.clipvault.storage.HistoryManager;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClipboardMonitor {
    private static final Logger logger = Logger.getLogger(ClipboardMonitor.class.getName());
    private static final long POLL_INTERVAL_MS = 500;

    private final HistoryManager historyManager;
    private final ScheduledExecutorService executor;
    private final Clipboard clipboard;
    private String lastTextContent;
    private String lastImageHash;
    private volatile boolean running = false;
    private Runnable onChangeCallback;

    public ClipboardMonitor(HistoryManager historyManager) {
        this.historyManager = historyManager;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    public void start() {
        if (running) return;
        running = true;

        lastTextContent = getCurrentText();
        lastImageHash = getCurrentImageHash();

        executor.scheduleAtFixedRate(this::checkClipboard, POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        logger.info("Clipboard monitor started");
    }

    public void stop() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Clipboard monitor stopped");
    }

    public void setOnChangeCallback(Runnable callback) {
        this.onChangeCallback = callback;
    }

    private volatile boolean isProcessing = false;

    private void checkClipboard() {
        if (!running || isProcessing) return;
        isProcessing = true;

        try {
            String currentText = getCurrentText();
            if (currentText != null && !currentText.equals(lastTextContent)) {
                lastTextContent = currentText;
                ClipItem item = ClipItem.createText(currentText);
                historyManager.addItem(item);
                logger.fine("剪贴板内容变化，类型：text");
                notifyChange();
                return;
            }

            String currentImageHash = getCurrentImageHash();
            if (currentImageHash != null && !currentImageHash.equals(lastImageHash)) {
                lastImageHash = currentImageHash;
                String fileName = saveCurrentImage();
                if (fileName != null) {
                    ClipItem item = ClipItem.createImage(fileName);
                    historyManager.addItem(item);
                    logger.fine("剪贴板内容变化，类型：image");
                    notifyChange();
                }
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Error checking clipboard", e);
        } finally {
            isProcessing = false;
        }
    }

    private void notifyChange() {
        if (onChangeCallback != null) {
            onChangeCallback.run();
        }
    }

    private String getCurrentText() {
        try {
            Transferable transferable = clipboard.getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) transferable.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            logger.finest("Could not get text from clipboard");
        }
        return null;
    }

    private String getCurrentImageHash() {
        try {
            Transferable transferable = clipboard.getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Image image = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                return hashImage(image);
            }
        } catch (Exception e) {
            logger.finest("Could not get image from clipboard");
        }
        return null;
    }

    private String hashImage(Image image) {
        BufferedImage bufferedImage = toBufferedImage(image);
        if (bufferedImage == null) return null;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            int sampleStep = Math.max(1, Math.min(width, height) / 100);

            // 将宽度和高度作为附加标识
            md.update((byte)(width >> 24));
            md.update((byte)(width >> 16));
            md.update((byte)(width >> 8));
            md.update((byte)width);
            md.update((byte)(height >> 24));
            md.update((byte)(height >> 16));
            md.update((byte)(height >> 8));
            md.update((byte)height);

            // 将采样像素的字节直接喂给 MD5，避免构建大字符串
            byte[] pixelBytes = new byte[4];
            for (int y = 0; y < height; y += sampleStep) {
                for (int x = 0; x < width; x += sampleStep) {
                    int rgb = bufferedImage.getRGB(x, y);
                    pixelBytes[0] = (byte)(rgb >> 24);
                    pixelBytes[1] = (byte)(rgb >> 16);
                    pixelBytes[2] = (byte)(rgb >> 8);
                    pixelBytes[3] = (byte)rgb;
                    md.update(pixelBytes);
                }
            }

            byte[] digest = md.digest();
            StringBuilder hex = new StringBuilder(32);
            for (byte b : digest) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.log(java.util.logging.Level.SEVERE, "MD5 algorithm not available", e);
            return String.valueOf(System.currentTimeMillis());
        }
    }

    private BufferedImage toBufferedImage(Image img) {
        if (img == null) return null;

        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        BufferedImage bufferedImage = new BufferedImage(
                img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return bufferedImage;
    }

    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 64;

    private String saveCurrentImage() {
        try {
            Transferable transferable = clipboard.getContents(null);
            if (transferable == null || !transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return null;
            }

            Image image = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
            BufferedImage bufferedImage = toBufferedImage(image);
            if (bufferedImage == null) return null;

            String fileName = UUID.randomUUID().toString().substring(0, 8) + ".png";
            Path imagePath = historyManager.getImagePath(fileName);

            Files.createDirectories(imagePath.getParent());

            File file = imagePath.toFile();
            if (ImageIO.write(bufferedImage, "PNG", file)) {
                if (file.exists() && file.length() > 0) {
                    generateThumbnail(bufferedImage, fileName);
                    logger.fine("Image saved: " + fileName + " (" + file.length() + " bytes)");
                    return fileName;
                }
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to save image", e);
        }
        return null;
    }

    private void generateThumbnail(BufferedImage originalImage, String fileName) {
        try {
            String thumbFileName = fileName.replace(".png", "_thumb.png");
            Path thumbPath = historyManager.getThumbnailPath(thumbFileName);

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            double widthRatio = (double) THUMBNAIL_WIDTH / originalWidth;
            double heightRatio = (double) THUMBNAIL_HEIGHT / originalHeight;
            double scaleRatio = Math.min(widthRatio, heightRatio);

            int scaledWidth = Math.max(1, (int) (originalWidth * scaleRatio));
            int scaledHeight = Math.max(1, (int) (originalHeight * scaleRatio));

            BufferedImage thumbnail = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
            g2d.dispose();

            File thumbFile = thumbPath.toFile();
            if (ImageIO.write(thumbnail, "PNG", thumbFile)) {
                logger.fine("Thumbnail saved: " + thumbFileName + " (" + thumbFile.length() + " bytes)");
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "Failed to generate thumbnail", e);
        }
    }
}
