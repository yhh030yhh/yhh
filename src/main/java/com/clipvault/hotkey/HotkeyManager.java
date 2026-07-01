package com.clipvault.hotkey;

import com.clipvault.win32.Win32;
import com.clipvault.win32.Win32.*;
import com.sun.jna.Native;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HotkeyManager {
    private static final Logger logger = Logger.getLogger(HotkeyManager.class.getName());

    private static final User32 User32 = Win32.User32.INSTANCE;

    private static final int MOD_ALT = 0x0001;
    private static final int MOD_CONTROL = 0x0002;
    private static final int MOD_SHIFT = 0x0004;
    private static final int MOD_WIN = 0x0008;

    private static final int HOTKEY_ID = 1;

    private String hotkeyString;
    private int hotkeyModifiers;
    private int hotkeyCode;
    private Runnable onHotkeyPressed;
    private Runnable onRegistrationFailed;
    private volatile boolean running = false;
    private volatile boolean registered = false;
    private volatile int messageThreadId = 0;
    private Thread messageThread;

    public HotkeyManager(String hotkeyString) {
        this.hotkeyString = hotkeyString;
        parseHotkey(hotkeyString);
    }

    private void parseHotkey(String hotkey) {
        String[] parts = hotkey.toUpperCase().split("\\+");
        hotkeyModifiers = 0;
        hotkeyCode = 0;

        for (String part : parts) {
            part = part.trim();
            switch (part) {
                case "WIN":
                case "WINDOWS":
                    hotkeyModifiers |= MOD_WIN;
                    break;
                case "CTRL":
                case "CONTROL":
                    hotkeyModifiers |= MOD_CONTROL;
                    break;
                case "ALT":
                    hotkeyModifiers |= MOD_ALT;
                    break;
                case "SHIFT":
                    hotkeyModifiers |= MOD_SHIFT;
                    break;
                default:
                    hotkeyCode = getKeyCode(part);
                    break;
            }
        }
    }

    private int getKeyCode(String key) {
        if (key.length() == 1) {
            return key.charAt(0);
        }

        switch (key) {
            case "F1": return 0x70;
            case "F2": return 0x71;
            case "F3": return 0x72;
            case "F4": return 0x73;
            case "F5": return 0x74;
            case "F6": return 0x75;
            case "F7": return 0x76;
            case "F8": return 0x77;
            case "F9": return 0x78;
            case "F10": return 0x79;
            case "F11": return 0x7A;
            case "F12": return 0x7B;
            case "UP": return 0x26;
            case "DOWN": return 0x28;
            case "LEFT": return 0x25;
            case "RIGHT": return 0x27;
            case "SPACE": return 0x20;
            case "ESC":
            case "ESCAPE": return 0x1B;
            default: return 0;
        }
    }

    public void setOnHotkeyPressed(Runnable callback) {
        this.onHotkeyPressed = callback;
    }

    public void setOnRegistrationFailed(Runnable callback) {
        this.onRegistrationFailed = callback;
    }

    public synchronized void register() {
        if (running) {
            logger.fine("Hotkey already registered");
            return;
        }

        logger.info("Attempting to register hotkey: " + hotkeyString + ", modifiers: " + hotkeyModifiers + ", code: " + hotkeyCode);

        if (hotkeyCode == 0) {
            logger.severe("Invalid hotkey: " + hotkeyString);
            if (onRegistrationFailed != null) {
                onRegistrationFailed.run();
            }
            return;
        }

        running = true;
        messageThread = new Thread(this::runMessageLoop, "ClipVault-Hotkey");
        messageThread.setDaemon(true);
        messageThread.start();
    }

    private void runMessageLoop() {
        messageThreadId = Win32.Kernel32.INSTANCE.GetCurrentThreadId();

        MSG msg = new MSG();
        User32.PeekMessageW(msg, null, Win32.WM_USER, Win32.WM_USER, Win32.PM_NOREMOVE);

        boolean success = User32.RegisterHotKey(null, HOTKEY_ID, hotkeyModifiers, hotkeyCode);
        if (!success) {
            running = false;
            registered = false;
            logger.severe("Failed to register hotkey: " + hotkeyString + ", Win32 error: " + Native.getLastError());
            if (onRegistrationFailed != null) {
                onRegistrationFailed.run();
            }
            return;
        }

        registered = true;
        logger.info("Hotkey registered: " + hotkeyString);

        try {
            while (running) {
                int result = User32.GetMessageW(msg, null, 0, 0);
                if (result == -1) {
                    logger.severe("Hotkey message loop failed, Win32 error: " + Native.getLastError());
                    break;
                }
                if (result == 0) {
                    break;
                }

                if (msg.message == Win32.WM_HOTKEY && msg.wParam.intValue() == HOTKEY_ID) {
                    logger.fine("Hotkey pressed");
                    if (onHotkeyPressed != null) {
                        onHotkeyPressed.run();
                    }
                }

                User32.TranslateMessage(msg);
                User32.DispatchMessageW(msg);
            }
        } finally {
            User32.UnregisterHotKey(null, HOTKEY_ID);
            registered = false;
            running = false;
            messageThreadId = 0;
            logger.info("Hotkey unregistered");
        }
    }

    public synchronized void unregister() {
        if (!running) return;

        running = false;
        int threadId = messageThreadId;
        if (threadId != 0) {
            User32.PostThreadMessageW(
                    threadId,
                    Win32.WM_QUIT,
                    new WPARAM(0),
                    new LPARAM(0)
            );
        }

        if (messageThread != null && Thread.currentThread() != messageThread) {
            try {
                messageThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void updateHotkey(String newHotkey) {
        unregister();
        this.hotkeyString = newHotkey;
        parseHotkey(newHotkey);
        register();
    }

    public String getHotkeyString() {
        return hotkeyString;
    }

    public boolean isRegistered() {
        return registered;
    }
}
