package com.clipvault.ui.components;

import com.clipvault.model.ClipItem;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.css.PseudoClass;

import java.time.format.DateTimeFormatter;

public abstract class Card extends BorderPane {
    protected static final PseudoClass PINNED = PseudoClass.getPseudoClass("pinned");
    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    protected final ClipItem item;
    protected final Runnable onPaste;
    protected final Runnable onDelete;
    protected final Runnable onTogglePin;
    
    protected Label timeLabel;
    protected HBox bottomRow;
    protected ContextMenu contextMenu;
    
    public Card(ClipItem item, Runnable onPaste, Runnable onDelete, Runnable onTogglePin) {
        this.item = item;
        this.onPaste = onPaste;
        this.onDelete = onDelete;
        this.onTogglePin = onTogglePin;
        
        setupBaseUI();
        setupContextMenu();
        setupEvents();
        pseudoClassStateChanged(PINNED, item.isPinned());
    }
    
    protected void initializeContent() {
        setupContent();
    }
    
    private void setupBaseUI() {
        getStyleClass().add("card");
    }
    
    protected void setupBottomRow() {
        timeLabel = new Label(item.getLastUsedAt().format(FORMATTER));
        timeLabel.getStyleClass().add("timestamp");
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        timeLabel.setMaxWidth(Double.MAX_VALUE);
        
        bottomRow = new HBox();
        bottomRow.setSpacing(4);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        bottomRow.setMaxWidth(Double.MAX_VALUE);
        bottomRow.setMinHeight(13);
        bottomRow.setPrefHeight(13);
        bottomRow.setMaxHeight(13);
        bottomRow.getStyleClass().add("bottom-row");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottomRow.getChildren().add(spacer);
        
        if (item.isPinned()) {
            Label pinnedBadge = new Label("📌");
            pinnedBadge.getStyleClass().add("pinned-badge");
            bottomRow.getChildren().add(pinnedBadge);
        }
        
        bottomRow.getChildren().add(timeLabel);
    }
    
    protected abstract void setupContent();
    
    private void setupContextMenu() {
        contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("light-context-menu");
        
        MenuItem pasteItem = new MenuItem("粘贴");
        pasteItem.setOnAction(e -> {
            onPaste.run();
            contextMenu.hide();
        });
        
        MenuItem pinItem = new MenuItem(item.isPinned() ? "取消固定" : "固定");
        pinItem.setOnAction(e -> {
            onTogglePin.run();
            contextMenu.hide();
        });
        
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> {
            onDelete.run();
            contextMenu.hide();
        });
        
        contextMenu.getItems().addAll(pasteItem, pinItem, deleteItem);
    }
    
    private void setupEvents() {
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                e.consume();
                onPaste.run();
            }
            if (e.getButton() == MouseButton.SECONDARY) {
                e.consume();
                contextMenu.show(this, e.getScreenX(), e.getScreenY());
            }
        });
    }
    
    public ClipItem getItem() {
        return item;
    }
}