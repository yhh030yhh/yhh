package com.clipvault.ui.components;

import com.clipvault.model.ClipItem;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public class TextCard extends Card {
    
    public TextCard(ClipItem item, Runnable onPaste, Runnable onDelete, Runnable onTogglePin) {
        super(item, onPaste, onDelete, onTogglePin);
        initializeContent();
    }
    
    @Override
    protected void setupContent() {
        String content = item.getContent();
        if (content.length() > 100) {
            content = content.substring(0, 100) + "...";
        }
        
        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("text-preview");
        contentLabel.setMaxHeight(28);
        contentLabel.setPrefHeight(28);
        contentLabel.setMinHeight(28);
        
        Region contentRegion = new Region();
        contentRegion.getStyleClass().add("content-region");
        
        setupBottomRow();
        
        setCenter(contentLabel);
        setBottom(bottomRow);
    }
}