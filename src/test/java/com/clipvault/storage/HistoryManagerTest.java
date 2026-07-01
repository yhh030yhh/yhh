package com.clipvault.storage;

import com.clipvault.model.ClipItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryManagerTest {

    @TempDir
    Path tempDir;

    private HistoryManager historyManager;

    @BeforeEach
    void setUp() throws IOException {
        historyManager = new HistoryManager(tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (historyManager != null) {
            historyManager.clearAll();
        }
    }

    @Test
    void testAddTextItem() {
        ClipItem item = ClipItem.createText("Hello World");
        historyManager.addItem(item);

        List<ClipItem> items = historyManager.getSortedItems();
        assertEquals(1, items.size());
        assertEquals("Hello World", items.get(0).getContent());
    }

    @Test
    void testAddImageItem() {
        ClipItem item = ClipItem.createImage("test.png");
        historyManager.addItem(item);

        List<ClipItem> items = historyManager.getSortedItems();
        assertEquals(1, items.size());
        assertEquals("test.png", items.get(0).getFileName());
    }

    @Test
    void testDeleteItem() {
        ClipItem item = ClipItem.createText("Hello");
        historyManager.addItem(item);

        List<ClipItem> items = historyManager.getSortedItems();
        assertEquals(1, items.size());

        String itemId = items.get(0).getId();
        historyManager.deleteItem(itemId);

        items = historyManager.getSortedItems();
        assertEquals(0, items.size());
    }

    @Test
    void testTogglePin() {
        ClipItem item = ClipItem.createText("Hello");
        historyManager.addItem(item);

        List<ClipItem> items = historyManager.getSortedItems();
        String itemId = items.get(0).getId();

        assertFalse(items.get(0).isPinned());

        historyManager.togglePin(itemId);
        items = historyManager.getSortedItems();
        assertTrue(items.get(0).isPinned());

        historyManager.togglePin(itemId);
        items = historyManager.getSortedItems();
        assertFalse(items.get(0).isPinned());
    }

    @Test
    void testDuplicateTextItem() {
        ClipItem item1 = ClipItem.createText("Hello");
        historyManager.addItem(item1);

        ClipItem item2 = ClipItem.createText("Hello");
        historyManager.addItem(item2);

        List<ClipItem> items = historyManager.getSortedItems();
        assertEquals(1, items.size());
    }

    @Test
    void testDuplicateImageItem() {
        ClipItem item1 = ClipItem.createImage("test.png");
        historyManager.addItem(item1);

        ClipItem item2 = ClipItem.createImage("test.png");
        historyManager.addItem(item2);

        List<ClipItem> items = historyManager.getSortedItems();
        assertEquals(1, items.size());
    }

    @Test
    void testSearchTextItems() {
        historyManager.addItem(ClipItem.createText("Hello World"));
        historyManager.addItem(ClipItem.createText("Goodbye World"));
        historyManager.addItem(ClipItem.createText("Foo Bar"));
        historyManager.addItem(ClipItem.createImage("test.png"));

        List<ClipItem> results = historyManager.search("World");
        assertEquals(2, results.size());

        results = historyManager.search("Hello");
        assertEquals(1, results.size());
    }

    @Test
    void testSearchCaseInsensitive() {
        historyManager.addItem(ClipItem.createText("Hello World"));

        List<ClipItem> results = historyManager.search("hello");
        assertEquals(1, results.size());

        results = historyManager.search("HELLO");
        assertEquals(1, results.size());
    }

    @Test
    void testSearchEmptyKeyword() {
        historyManager.addItem(ClipItem.createText("Hello World"));

        List<ClipItem> results = historyManager.search("");
        assertEquals(1, results.size());

        results = historyManager.search(null);
        assertEquals(1, results.size());
    }

    @Test
    void testFilterByTypeAll() {
        historyManager.addItem(ClipItem.createText("Hello"));
        historyManager.addItem(ClipItem.createText("World"));

        List<ClipItem> results = historyManager.filterByType("all");
        assertEquals(2, results.size());
    }

    @Test
    void testFilterByTypeText() {
        historyManager.addItem(ClipItem.createText("Hello"));
        historyManager.addItem(ClipItem.createText("World"));
        historyManager.addItem(ClipItem.createText("Foo"));

        List<ClipItem> results = historyManager.filterByType("text");
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(ClipItem::isText));
    }

    @Test
    void testFilterByTypeImage() {
        historyManager.addItem(ClipItem.createText("Hello"));
        historyManager.addItem(ClipItem.createText("World"));

        List<ClipItem> results = historyManager.filterByType("image");
        assertEquals(0, results.size());
    }

    @Test
    void testFilterByTimeAll() {
        historyManager.addItem(ClipItem.createText("Hello"));

        List<ClipItem> results = historyManager.filterByTime("all");
        assertEquals(1, results.size());
    }

    @Test
    void testPinnedItemsFirst() {
        ClipItem item1 = ClipItem.createText("First");
        historyManager.addItem(item1);

        ClipItem item2 = ClipItem.createText("Second");
        historyManager.addItem(item2);

        List<ClipItem> items = historyManager.getSortedItems();
        assertEquals(2, items.size());

        String firstId = items.get(1).getId();
        assertFalse(items.stream().anyMatch(ClipItem::isPinned));

        historyManager.togglePin(firstId);

        items = historyManager.getSortedItems();
        assertTrue(items.stream().anyMatch(ClipItem::isPinned));
        assertEquals(1, items.stream().filter(ClipItem::isPinned).count());
    }

    @Test
    void testClearAll() {
        historyManager.addItem(ClipItem.createText("Hello"));
        historyManager.addItem(ClipItem.createImage("test.png"));

        List<ClipItem> items = historyManager.getSortedItems();
        assertEquals(2, items.size());

        historyManager.clearAll();

        items = historyManager.getSortedItems();
        assertEquals(0, items.size());
    }

    @Test
    void testGetSortedItems() {
        historyManager.addItem(ClipItem.createText("First"));
        historyManager.addItem(ClipItem.createText("Second"));
        historyManager.addItem(ClipItem.createText("Third"));

        List<ClipItem> items = historyManager.getSortedItems();
        assertEquals(3, items.size());

        assertEquals("Third", items.get(0).getContent());
        assertEquals("Second", items.get(1).getContent());
        assertEquals("First", items.get(2).getContent());
    }

    @Test
    void testUpdateSavePath() throws IOException {
        Path newDir = Files.createTempDirectory("clipvault-test");
        String newPath = newDir.toString();

        historyManager.updateSavePath(newPath);

        Path expectedPath = newDir.resolve("history.json");
        assertTrue(Files.exists(newDir));
    }

    @Test
    void testImagePathResolution() {
        Path imagePath = historyManager.getImagePath("test.png");
        assertTrue(imagePath.endsWith("test.png"));
        assertTrue(imagePath.toString().contains("images"));
    }
}
