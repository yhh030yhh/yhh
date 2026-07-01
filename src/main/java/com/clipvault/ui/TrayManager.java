package com.clipvault.ui;

import com.clipvault.App;
import javafx.application.Platform;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrayManager {
    private static final Logger logger = Logger.getLogger(TrayManager.class.getName());
    private static final String ICON_TEXT = "ClipVault";

    private final App app;
    private final SystemTray tray;
    private final TrayIcon trayIcon;

    public TrayManager(App app) {
        this.app = app;

        if (!SystemTray.isSupported()) {
            logger.severe("System tray is not supported");
            throw new RuntimeException("System tray is not supported");
        }

        this.tray = SystemTray.getSystemTray();
        this.trayIcon = createTrayIcon();
        setupTrayIcon();
        addToTray();
    }

    private TrayIcon createTrayIcon() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 165, 0));
        g2d.fillRect(2, 2, 12, 12);
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillRect(4, 4, 8, 2);
        g2d.fillRect(4, 7, 6, 2);
        g2d.fillRect(4, 10, 4, 2);
        g2d.dispose();

        TrayIcon icon = new TrayIcon(image, ICON_TEXT, null);
        icon.setImageAutoSize(true);
        return icon;
    }

    private void setupTrayIcon() {
        trayIcon.addActionListener(e -> Platform.runLater(app::togglePanel));
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup();
            }
        });
    }

    private void showPopup() {
        Platform.runLater(() -> {
            boolean dark = app.getSettingsManager() != null
                    && app.getSettingsManager().getSettings().isDarkTheme();
            TrayPopup popup = new TrayPopup(dark);
            popup.addItem("显示剪贴板", () -> app.showPanel());
            popup.addSeparator();
            popup.addItem("设置", () -> app.showSettings());
            popup.addSeparator();
            popup.addItem("退出", () -> app.exit());
            Point loc = MouseInfo.getPointerInfo().getLocation();
            popup.show(loc.x, loc.y);
        });
    }

    private void addToTray() {
        try {
            tray.add(trayIcon);
            logger.info("Tray icon added");
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "Failed to add tray icon", e);
            throw new RuntimeException("Failed to add tray icon", e);
        }
    }

    public void destroy() {
        tray.remove(trayIcon);
        logger.info("Tray icon removed");
    }
}
