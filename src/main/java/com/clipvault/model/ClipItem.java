package com.clipvault.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClipItem {
    private String id;
    private String type;
    private String content;
    private String fileName;
    private boolean pinned;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public ClipItem() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
        this.pinned = false;
    }

    public static ClipItem createText(String content) {
        ClipItem item = new ClipItem();
        item.setType("text");
        item.setContent(content);
        return item;
    }

    public static ClipItem createImage(String fileName) {
        ClipItem item = new ClipItem();
        item.setType("image");
        item.setFileName(fileName);
        return item;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public boolean isText() {
        return "text".equals(type);
    }

    public boolean isImage() {
        return "image".equals(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClipItem clipItem = (ClipItem) obj;

        if (this.type != null && this.type.equals(clipItem.type)) {
            if ("text".equals(this.type)) {
                return this.content != null && this.content.equals(clipItem.content);
            } else if ("image".equals(this.type)) {
                return this.fileName != null && this.fileName.equals(clipItem.fileName);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, content, fileName);
    }
}
