package com.clipvault.storage;

import com.clipvault.model.ClipItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HistoryManager {
    private static final Logger logger = Logger.getLogger(HistoryManager.class.getName());
    private static final String HISTORY_FILE = "history.json";
    private static final String IMAGES_DIR = "images";
    private static final String THUMBNAILS_DIR = "thumbnails";

    private Path historyPath;
    private Path imagesPath;
    private Path thumbnailsPath;
    private List<ClipItem> items;
    private String customSavePath;
    private final ScheduledExecutorService saveExecutor;
    private int cachedMaxHistoryCount = 0;
    private volatile boolean savePending = false;
    private static final long SAVE_DELAY_MS = 1500;

    private static final Comparator<ClipItem> SORT_BY_PINNED_THEN_USED =
        Comparator.comparing((ClipItem item) -> !item.isPinned())
            .thenComparing(ClipItem::getLastUsedAt)
            .reversed();

    public HistoryManager() {
        this(null);
    }

    public HistoryManager(String customSavePath) {
        this.customSavePath = customSavePath;
        this.historyPath = getHistoryPath();
        this.imagesPath = getImagesPath();
        this.thumbnailsPath = getThumbnailsPath();
        this.saveExecutor = Executors.newSingleThreadScheduledExecutor();

        ensureDirectoriesExist();
        load();
    }

    public void updateSavePath(String newPath) {
        this.customSavePath = newPath;
        this.historyPath = getHistoryPath();
        this.imagesPath = getImagesPath();
        this.thumbnailsPath = getThumbnailsPath();
        ensureDirectoriesExist();
        load();
    }

    private Path getHistoryPath() {
        if (customSavePath != null && !customSavePath.trim().isEmpty()) {
            return Paths.get(customSavePath, HISTORY_FILE);
        }
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, "ClipVault", HISTORY_FILE);
    }

    private Path getImagesPath() {
        if (customSavePath != null && !customSavePath.trim().isEmpty()) {
            return Paths.get(customSavePath, IMAGES_DIR);
        }
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, "ClipVault", IMAGES_DIR);
    }

    private Path getThumbnailsPath() {
        if (customSavePath != null && !customSavePath.trim().isEmpty()) {
            return Paths.get(customSavePath, THUMBNAILS_DIR);
        }
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, "ClipVault", THUMBNAILS_DIR);
    }

    private void ensureDirectoriesExist() {
        try {
            Files.createDirectories(historyPath.getParent());
            Files.createDirectories(imagesPath);
            Files.createDirectories(thumbnailsPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create storage directories", e);
        }
    }

    public void load() {
        if (Files.exists(historyPath)) {
            try {
                String json = new String(Files.readAllBytes(historyPath), StandardCharsets.UTF_8);
                List<Map<String, Object>> list = JsonUtil.parse(json);
                items = JsonUtil.toItems(list);
                removeMissingImageItems();
                logger.info("Loaded " + items.size() + " history items");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load history, starting fresh", e);
                items = new ArrayList<>();
            }
        } else {
            items = new ArrayList<>();
            logger.info("No history file found, starting fresh");
        }
    }

    private void removeMissingImageItems() {
        int originalSize = items.size();
        items.removeIf(item -> {
            if (item.isImage() && item.getFileName() != null) {
                Path imagePath = getImagePath(item.getFileName());
                return !Files.exists(imagePath);
            }
            return false;
        });
        int removed = originalSize - items.size();
        if (removed > 0) {
            logger.info("Removed " + removed + " missing image items");
            scheduleSave();
        }
    }

    public void save() {
        scheduleSave();
    }

    private void saveImmediately() {
        try {
            String json = JsonUtil.itemsToJson(items);
            Files.write(historyPath, json.getBytes(StandardCharsets.UTF_8));
            logger.info("Saved " + items.size() + " records to " + historyPath);
            savePending = false;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save history", e);
        }
    }

    private synchronized void scheduleSave() {
        if (!savePending) {
            savePending = true;
            saveExecutor.schedule(() -> {
                try {
                    performSave();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Save task failed", e);
                    savePending = false;
                }
            }, SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void performSave() {
        if (savePending) {
            saveImmediately();
        }
    }

    public synchronized List<ClipItem> getSortedItems() {
        return items.stream()
                .sorted(SORT_BY_PINNED_THEN_USED)
                .collect(Collectors.toList());
    }

    public synchronized void addItem(ClipItem newItem) {
        for (int i = 0; i < items.size(); i++) {
            ClipItem existing = items.get(i);
            if (existing.equals(newItem)) {
                existing.setLastUsedAt(LocalDateTime.now());
                if (existing.isPinned()) {
                    items.remove(i);
                    items.add(0, existing);
                }
                save();
                logger.fine("Updated existing item timestamp");
                return;
            }
        }

        items.add(0, newItem);

        refreshMaxHistoryCountCache();

        enforceMaxHistoryCount();

        save();
        logger.fine("Added new item: " + newItem.getType());
    }

    private void enforceMaxHistoryCount() {
        if (cachedMaxHistoryCount <= 0) return;

        int toRemove = items.size() - cachedMaxHistoryCount;
        if (toRemove <= 0) return;

        for (int i = items.size() - 1; i >= 0 && toRemove > 0; i--) {
            ClipItem item = items.get(i);
            if (item.isPinned()) continue;

            if (item.isImage()) {
                deleteImageFile(item.getFileName());
                deleteThumbnailFile(item.getFileName());
            }
            items.remove(i);
            toRemove--;
            logger.fine("Removed oldest item due to max history limit: " + item.getId());
        }
    }

    private void refreshMaxHistoryCountCache() {
        try {
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home");
            }
            Path settingsPath = Paths.get(appData, "ClipVault", "settings.json");
            if (Files.exists(settingsPath)) {
                String json = new String(Files.readAllBytes(settingsPath), StandardCharsets.UTF_8);
                Map<String, Object> root = JsonUtil.parse(json);
                if (root.containsKey("maxHistoryCount")) {
                    Object val = root.get("maxHistoryCount");
                    if (val instanceof Number) {
                        cachedMaxHistoryCount = ((Number) val).intValue();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.fine("Failed to refresh maxHistoryCount cache");
        }
        cachedMaxHistoryCount = 0;
    }

    public synchronized void deleteItem(String id) {
        items.removeIf(item -> {
            if (item.getId().equals(id)) {
                if (item.isImage()) {
                    deleteImageFile(item.getFileName());
                    deleteThumbnailFile(item.getFileName());
                }
                return true;
            }
            return false;
        });
        save();
        logger.fine("Deleted item: " + id);
    }

    public synchronized void togglePin(String id) {
        for (ClipItem item : items) {
            if (item.getId().equals(id)) {
                item.setPinned(!item.isPinned());
                save();
                logger.fine("Toggled pin for item: " + id);
                return;
            }
        }
    }

    public synchronized void clearAll() {
        for (ClipItem item : items) {
            if (item.isImage()) {
                deleteImageFile(item.getFileName());
                deleteThumbnailFile(item.getFileName());
            }
        }
        items.clear();
        saveImmediately();
        logger.info("All history cleared");
    }

    public synchronized List<ClipItem> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getSortedItems();
        }

        String lowerKeyword = keyword.toLowerCase();
        return items.stream()
                .filter(item -> item.isText() && item.getContent() != null &&
                        item.getContent().toLowerCase().contains(lowerKeyword))
                .sorted(SORT_BY_PINNED_THEN_USED)
                .collect(Collectors.toList());
    }

    public synchronized List<ClipItem> filterByType(String type) {
        return items.stream()
                .filter(item -> {
                    if ("all".equals(type)) return true;
                    if ("text".equals(type)) return item.isText();
                    if ("image".equals(type)) return item.isImage();
                    return true;
                })
                .sorted(SORT_BY_PINNED_THEN_USED)
                .collect(Collectors.toList());
    }

    public synchronized List<ClipItem> filterByTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime weekStart = todayStart.minusWeeks(1);
        LocalDateTime monthStart = todayStart.minusMonths(1);

        return items.stream()
                .filter(item -> {
                    LocalDateTime itemTime = item.getLastUsedAt();
                    if (itemTime == null) return false;

                    switch (timeRange) {
                        case "today":
                            return !itemTime.isBefore(todayStart);
                        case "yesterday":
                            return !itemTime.isBefore(yesterdayStart) && itemTime.isBefore(todayStart);
                        case "week":
                            return !itemTime.isBefore(weekStart);
                        case "month":
                            return !itemTime.isBefore(monthStart);
                        default:
                            return true;
                    }
                })
                .sorted(SORT_BY_PINNED_THEN_USED)
                .collect(Collectors.toList());
    }

    public Path getImagePath(String fileName) {
        return imagesPath.resolve(fileName);
    }

    public Path getThumbnailPath(String fileName) {
        return thumbnailsPath.resolve(fileName);
    }

    private void deleteImageFile(String fileName) {
        if (fileName != null) {
            try {
                Files.deleteIfExists(imagesPath.resolve(fileName));
                logger.fine("Deleted image file: " + fileName);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to delete image file: " + fileName, e);
            }
        }
    }

    private void deleteThumbnailFile(String fileName) {
        if (fileName != null) {
            try {
                String thumbFileName = fileName.replace(".png", "_thumb.png");
                Files.deleteIfExists(thumbnailsPath.resolve(thumbFileName));
                logger.fine("Deleted thumbnail file: " + thumbFileName);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to delete thumbnail file: " + fileName, e);
            }
        }
    }

    public void shutdown() {
        if (savePending) {
            saveExecutor.shutdownNow();
            try {
                if (!saveExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    logger.warning("Save executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            saveImmediately();
        } else {
            saveExecutor.shutdownNow();
        }
    }
}
