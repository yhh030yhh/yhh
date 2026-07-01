package com.clipvault.storage;

import com.clipvault.model.AppSettings;
import com.clipvault.model.ClipItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Minimal JSON reader/writer — replaces Jackson entirely.
 * Only supports the exact JSON structures used by AppSettings and ClipItem.
 */
public class JsonUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Write any object to JSON string */
    public static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, obj);
        return sb.toString();
    }

    /** Read Map (from JSON object) into the given AppSettings */
    public static void fillSettings(AppSettings s, Map<String, Object> map) {
        if (map == null) return;
        s.setHotkey(str(map, "hotkey", "CTRL+SHIFT+V"));
        s.setAutoStart(bool(map, "autoStart", false));
        s.setDarkTheme(bool(map, "darkTheme", true));
        s.setClearHistoryOnExit(bool(map, "clearHistoryOnExit", false));
        s.setMaxHistoryCount(intVal(map, "maxHistoryCount", 0));
        s.setFirstRun(bool(map, "firstRun", true));
        s.setSavePath(str(map, "savePath", ""));
    }

    /** Read List<Map> into List<ClipItem> */
    public static List<ClipItem> toItems(List<Map<String, Object>> list) {
        List<ClipItem> items = new ArrayList<>();
        if (list == null) return items;
        for (Map<String, Object> map : list) {
            ClipItem item = new ClipItem();
            item.setId(str(map, "id", UUID.randomUUID().toString()));
            item.setType(str(map, "type", null));
            item.setContent(str(map, "content", null));
            item.setFileName(str(map, "fileName", null));
            item.setPinned(bool(map, "pinned", false));
            item.setCreatedAt(localDt(map, "createdAt"));
            item.setLastUsedAt(localDt(map, "lastUsedAt"));
            items.add(item);
        }
        return items;
    }

    /** Write List<ClipItem> to JSON array string */
    public static String itemsToJson(List<ClipItem> items) {
        return toJson(items);
    }

    // --- Low-level JSON parsing ---

    /** Parse a JSON string into a Java object (Map, List, String, Number, Boolean) */
    @SuppressWarnings("unchecked")
    public static <T> T parse(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        return (T) parseValue(new Cursor(json.trim()));
    }

    private static Object parseValue(Cursor c) {
        skipSpace(c);
        if (c.atEnd()) return null;
        char ch = c.peek();
        if (ch == '{') return parseObject(c);
        if (ch == '[') return parseArray(c);
        if (ch == '"') return parseString(c);
        if (ch == 't' || ch == 'f') return parseBool(c);
        if (ch == 'n') return parseNull(c);
        return parseNumber(c);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseObject(Cursor c) {
        Map<String, Object> map = new LinkedHashMap<>();
        c.next(); // skip {
        skipSpace(c);
        if (c.peek() == '}') { c.next(); return map; }
        while (true) {
            skipSpace(c);
            String key = (String) parseString(c);
            skipSpace(c);
            c.next(); // skip :
            skipSpace(c);
            Object val = parseValue(c);
            map.put(key, val);
            skipSpace(c);
            if (c.peek() == '}') { c.next(); return map; }
            c.next(); // skip ,
        }
    }

    private static List<Object> parseArray(Cursor c) {
        List<Object> list = new ArrayList<>();
        c.next(); // skip [
        skipSpace(c);
        if (c.peek() == ']') { c.next(); return list; }
        while (true) {
            list.add(parseValue(c));
            skipSpace(c);
            if (c.peek() == ']') { c.next(); return list; }
            c.next(); // skip ,
        }
    }

    private static String parseString(Cursor c) {
        c.next(); // skip opening "
        StringBuilder sb = new StringBuilder();
        while (!c.atEnd()) {
            char ch = c.next();
            if (ch == '"') break;
            if (ch == '\\') {
                char esc = c.next();
                if (esc == 't') sb.append('\t');
                else if (esc == 'n') sb.append('\n');
                else if (esc == 'r') sb.append('\r');
                else if (esc == '\\') sb.append('\\');
                else if (esc == '"') sb.append('"');
                else sb.append(esc);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static Boolean parseBool(Cursor c) {
        if (c.peek() == 't') { c.skip(4); return Boolean.TRUE; }
        c.skip(5); return Boolean.FALSE;
    }

    private static Object parseNull(Cursor c) {
        c.skip(4); return null;
    }

    private static Number parseNumber(Cursor c) {
        StringBuilder sb = new StringBuilder();
        while (!c.atEnd() && "-0123456789.eE+".indexOf(c.peek()) >= 0) {
            sb.append(c.next());
        }
        String s = sb.toString();
        if (s.contains(".") || s.contains("e") || s.contains("E")) {
            return Double.parseDouble(s);
        }
        return Long.parseLong(s);
    }

    // --- Low-level JSON writing ---

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object obj) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String) {
            writeString(sb, (String) obj);
        } else if (obj instanceof Boolean || obj instanceof Number) {
            sb.append(obj);
        } else if (obj instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) obj).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(sb, e.getKey());
                sb.append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
        } else if (obj instanceof Collection) {
            sb.append('[');
            boolean first = true;
            for (Object item : (Collection<?>) obj) {
                if (!first) sb.append(',');
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
        } else if (obj instanceof AppSettings) {
            AppSettings s = (AppSettings) obj;
            writeValue(sb, settingsToMap(s));
        } else if (obj instanceof ClipItem) {
            writeValue(sb, itemToMap((ClipItem) obj));
        } else {
            sb.append(obj);
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(ch);
            }
        }
        sb.append('"');
    }

    // --- Model converters ---

    static Map<String, Object> settingsToMap(AppSettings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hotkey", s.getHotkey());
        m.put("autoStart", s.isAutoStart());
        m.put("darkTheme", s.isDarkTheme());
        m.put("clearHistoryOnExit", s.isClearHistoryOnExit());
        m.put("maxHistoryCount", s.getMaxHistoryCount());
        m.put("firstRun", s.isFirstRun());
        m.put("savePath", s.getSavePath());
        return m;
    }

    static Map<String, Object> itemToMap(ClipItem item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", item.getId());
        m.put("type", item.getType());
        m.put("content", item.getContent());
        m.put("fileName", item.getFileName());
        m.put("pinned", item.isPinned());
        m.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().format(FMT) : null);
        m.put("lastUsedAt", item.getLastUsedAt() != null ? item.getLastUsedAt().format(FMT) : null);
        return m;
    }

    // --- Helpers ---

    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v instanceof String ? (String) v : def;
    }

    private static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    private static int intVal(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    private static LocalDateTime localDt(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof String) {
            try { return LocalDateTime.parse((String) v, FMT); } catch (Exception e) { /* ignore */ }
        }
        return null;
    }

    // --- Internal cursor for parsing ---

    private static class Cursor {
        final String s; int pos;
        Cursor(String s) { this.s = s; }
        char peek() { return s.charAt(pos); }
        char next() { return s.charAt(pos++); }
        void skip(int n) { pos += n; }
        boolean atEnd() { return pos >= s.length(); }
    }

    private static void skipSpace(Cursor c) {
        while (!c.atEnd() && Character.isWhitespace(c.peek())) c.pos++;
    }
}
