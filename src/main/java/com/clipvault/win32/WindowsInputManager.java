package com.clipvault.win32;

import com.clipvault.win32.Win32.*;
import com.sun.jna.Native;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WindowsInputManager {
    private static final Logger logger = Logger.getLogger(WindowsInputManager.class.getName());
    private static final User32 User32 = Win32.User32.INSTANCE;

    private static final int VK_CONTROL = 0x11;
    private static final int VK_V = 0x56;
    private static final int KEYEVENTF_KEYUP = 0x0002;

    static {
        Native.register("user32");
    }

    private static native void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);

    private HWND foregroundWindow = null;

    public void captureForegroundWindow() {
        foregroundWindow = User32.GetForegroundWindow();
        if (foregroundWindow != null) {
            char[] windowTitle = new char[256];
            User32.GetWindowTextW(foregroundWindow, windowTitle, 256);
            logger.fine("Captured foreground window: " + new String(windowTitle).trim());
        } else {
            logger.warning("No foreground window found");
        }
    }

    public boolean completePaste() {
        if (foregroundWindow == null) {
            logger.warning("No window captured for paste");
            return false;
        }

        // Bring the captured window to foreground
        WINDOWPLACEMENT placement = new WINDOWPLACEMENT();
        User32.GetWindowPlacement(foregroundWindow, placement);
        if (placement.showCmd == 2) {
            User32.ShowWindow(foregroundWindow, Win32.SW_RESTORE);
        }

        boolean restored = User32.SetForegroundWindow(foregroundWindow);
        if (!restored) {
            // Retry once after a short delay
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            restored = User32.SetForegroundWindow(foregroundWindow);
        }

        if (!restored) {
            logger.warning("Failed to restore foreground window");
            return false;
        }

        // Send Ctrl+V
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        keybd_event((byte) VK_CONTROL, (byte) 0, 0, 0);
        keybd_event((byte) VK_V, (byte) 0, 0, 0);
        keybd_event((byte) VK_V, (byte) 0, KEYEVENTF_KEYUP, 0);
        keybd_event((byte) VK_CONTROL, (byte) 0, KEYEVENTF_KEYUP, 0);

        return true;
    }
}
