package com.clipvault.win32;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * Minimal Win32 JNA mappings — replaces jna-platform dependency.
 * Only defines the types and functions used by ClipVault.
 */
public final class Win32 {

    private Win32() {}

    // ===== Windows Data Types =====

    public static class HWND extends PointerType {
        public HWND() {}
        public HWND(Pointer p) { setPointer(p); }
    }

    public static class WPARAM extends IntegerType {
        public WPARAM() { super(Native.POINTER_SIZE); }
        public WPARAM(long val) { super(Native.POINTER_SIZE, val, true); }
    }

    public static class LPARAM extends IntegerType {
        public LPARAM() { super(Native.POINTER_SIZE); }
        public LPARAM(long val) { super(Native.POINTER_SIZE, val, true); }
    }

    public static class LRESULT extends IntegerType {
        public LRESULT() { super(Native.POINTER_SIZE); }
        public LRESULT(long val) { super(Native.POINTER_SIZE, val, true); }
    }

    public static class RECT extends Structure {
        public int left;
        public int top;
        public int right;
        public int bottom;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("left", "top", "right", "bottom");
        }
    }

    public static class POINT extends Structure {
        public int x;
        public int y;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("x", "y");
        }
    }

    public static class MSG extends Structure {
        public HWND hwnd;
        public int message;
        public WPARAM wParam;
        public LPARAM lParam;
        public int time;
        public POINT pt;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("hwnd", "message", "wParam", "lParam", "time", "pt");
        }

        // Custom constructor needed for inner class Structure
        public MSG() {
            super();
        }
    }

    public static class WINDOWPLACEMENT extends Structure {
        public int length;
        public int flags;
        public int showCmd;
        public POINT ptMinPosition = new POINT();
        public POINT ptMaxPosition = new POINT();
        public RECT rcNormalPosition = new RECT();

        public WINDOWPLACEMENT() {
            length = size();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("length", "flags", "showCmd", "ptMinPosition", "ptMaxPosition", "rcNormalPosition");
        }
    }

    // ===== User32 API =====

    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        HWND GetForegroundWindow();
        boolean GetWindowRect(HWND hWnd, RECT rect);
        int GetWindowTextW(HWND hWnd, char[] lpString, int nMaxCount);
        boolean GetWindowPlacement(HWND hWnd, WINDOWPLACEMENT lpwndpl);
        boolean ShowWindow(HWND hWnd, int nCmdShow);
        boolean SetForegroundWindow(HWND hWnd);
        LRESULT SendMessageW(HWND hWnd, int msg, WPARAM wParam, LPARAM lParam);
        boolean RegisterHotKey(HWND hWnd, int id, int fsModifiers, int vk);
        boolean UnregisterHotKey(HWND hWnd, int id);
        int GetMessageW(MSG msg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax);
        boolean PeekMessageW(MSG msg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax, int wRemoveMsg);
        boolean TranslateMessage(MSG msg);
        int DispatchMessageW(MSG msg);
        boolean PostThreadMessageW(int threadId, int msg, WPARAM wParam, LPARAM lParam);
    }

    // ===== Kernel32 API =====

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        int GetCurrentThreadId();
    }

    // ===== Constants =====

    public static final int WM_NCLBUTTONDOWN = 0x00A1;
    public static final int HTCAPTION = 2;
    public static final int WM_HOTKEY = 0x0312;
    public static final int WM_QUIT = 0x0012;
    public static final int WM_USER = 0x0400;
    public static final int SW_RESTORE = 9;
    public static final int PM_NOREMOVE = 0;
}
