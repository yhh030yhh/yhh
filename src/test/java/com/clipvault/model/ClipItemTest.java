package com.clipvault.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ClipItemTest {

    private ClipItem textItem;
    private ClipItem imageItem;

    @BeforeEach
    void setUp() {
        textItem = ClipItem.createText("Hello World");
        imageItem = ClipItem.createImage("test.png");
    }

    @Test
    void testCreateTextItem() {
        assertEquals("text", textItem.getType());
        assertEquals("Hello World", textItem.getContent());
        assertNull(textItem.getFileName());
        assertFalse(textItem.isPinned());
        assertNotNull(textItem.getId());
        assertNotNull(textItem.getCreatedAt());
        assertNotNull(textItem.getLastUsedAt());
    }

    @Test
    void testCreateImageItem() {
        assertEquals("image", imageItem.getType());
        assertEquals("test.png", imageItem.getFileName());
        assertNull(imageItem.getContent());
        assertFalse(imageItem.isPinned());
    }

    @Test
    void testIsTextAndIsImage() {
        assertTrue(textItem.isText());
        assertFalse(textItem.isImage());

        assertFalse(imageItem.isText());
        assertTrue(imageItem.isImage());
    }

    @Test
    void testPinnedToggle() {
        assertFalse(textItem.isPinned());

        textItem.setPinned(true);
        assertTrue(textItem.isPinned());

        textItem.setPinned(false);
        assertFalse(textItem.isPinned());
    }

    @Test
    void testEqualsWithTextItems() {
        ClipItem text1 = ClipItem.createText("Hello");
        ClipItem text2 = ClipItem.createText("Hello");
        ClipItem text3 = ClipItem.createText("World");

        assertEquals(text1, text2);
        assertNotEquals(text1, text3);
    }

    @Test
    void testEqualsWithImageItems() {
        ClipItem image1 = ClipItem.createImage("test.png");
        ClipItem image2 = ClipItem.createImage("test.png");
        ClipItem image3 = ClipItem.createImage("other.png");

        assertEquals(image1, image2);
        assertNotEquals(image1, image3);
    }

    @Test
    void testEqualsWithDifferentTypes() {
        assertNotEquals(textItem, imageItem);
    }

    @Test
    void testHashCodeConsistency() {
        ClipItem item1 = ClipItem.createText("Hello");
        ClipItem item2 = ClipItem.createText("Hello");

        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    void testHashCodeDifferentForDifferentContent() {
        ClipItem item1 = ClipItem.createText("Hello");
        ClipItem item2 = ClipItem.createText("World");

        assertNotEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    void testLastUsedAtUpdate() {
        LocalDateTime originalTime = textItem.getLastUsedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        textItem.setLastUsedAt(LocalDateTime.now());
        assertNotEquals(originalTime, textItem.getLastUsedAt());
    }

    @Test
    void testIdIsUnique() {
        ClipItem item1 = ClipItem.createText("Hello");
        ClipItem item2 = ClipItem.createText("Hello");

        assertNotEquals(item1.getId(), item2.getId());
    }

    @Test
    void testNotEqualsWithNull() {
        assertNotEquals(textItem, null);
    }

    @Test
    void testNotEqualsWithDifferentClass() {
        assertNotEquals(textItem, "Hello World");
    }
}
