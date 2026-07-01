# Settings Window UI Beautification

## Goal

Redesign the ClipVault settings window to match the main panel's visual language — transparent window, rounded corners, dropshadow, and card-based layout grouping. The current `StageStyle.UTILITY` window has an OS-native title bar that clashes with the dark theme.

## Scope

SettingsWindow.java + CSS only. No new dependencies. No changes to MainPanel, business logic, or window positioning.

## Design

### Module 1 — Window Frame

- Change `StageStyle.UTILITY` → `StageStyle.TRANSPARENT`
- Apply same visual treatment as main panel: `-fx-background-color: transparent`, dropshadow, padding for shadow spread
- Set window size to 500 × 520 (wider than main panel's 340 to accommodate settings content comfortably)
- Shadow constants: `SHADOW_PADDING = 24`, same dropshadow config: `blur=20, offsetX=4, offsetY=8`
- Z-order: root (transparent) → contentContainer (opaque, rounded bg) — same nested pattern as MainPanel

### Module 2 — Header / Title Bar

- Custom title row: left "⚙️ 设置" (icon + title), right close button "✕"
- Title bar doubles as drag region — use JavaFX `setOnMousePressed` + `setOnMouseDragged` for positioning (no JNA/Window dragging)
- Subtitle line below: "自定义您的 ClipVault 体验"
- Font hierarchy: title 22px bold, subtitle 12px secondary
- Bottom border: 1px separator line
- Close button style matches MainPanel's close button

### Module 3 — Content Area: Card-based Grouping

**Layout change:**
- Replace section-divider borders with individual rounded cards (`-fx-background-radius: 8`, `-fx-background-color: #252525` in dark)
- Cards separated by 12px vertical gap (via VBox spacing)
- Inside each card: icon/emoji prefix, section title (15px bold), description (12px secondary), controls area

**Control replacements:**

| Old Control | New Control |
|-------------|-------------|
| RadioButton (主题) | Custom ToggleButton capsule — selected fills blue (#4a9eff) bg, unselected transparent |
| CheckBox (开机启动/退出清除) | Custom ToggleSwitch — iOS-style track + thumb, blue when on |
| TextField (快捷键) | Keep current style (dark bg, blue focus glow) — behaves as hotkey capture field |
| TextField (保存路径) | Same as above |
| Spinner (历史条数) | Keep current style with better arrow button colors |
| Button (浏览/重置) | Keep current primary/secondary style |
| Button (关闭) | Keep current primary style, may reposition to center |

**ToggleSwitch implementation:**
- Custom region via CSS: `-fx-background-radius: 10`, width/height ratio ~2:1, animated thumb position on selection change
- Use a `Node` subclass (or Label with CSS) rather than CheckBox to get full style control

**ToggleButton (theme picker) implementation:**
- Standard JavaFX `ToggleButton` with `-fx-background-radius: 6`, grouped in an HBox
- Selected: `-fx-background-color: #4a9eff`, `-fx-text-fill: white`
- Unselected: `-fx-background-color: transparent`, `-fx-border-color: #555`, `-fx-text-fill: #ccc`

### Module 4 — Footer

- Bottom bar: `bg: #252525`, `padding: 16 24`
- "关闭" button centered (or right-aligned, TBD at implementation)
- Button style: same `settings-button-primary` (blue bg, white text, border-radius 6)

### Module 5 — Animation

- Window show/hide: reuse FadeTransition pattern from MainPanel (120ms fade)
- No scaling animation (smaller window would look odd), just opacity

### Module 6 — Light Theme

- Window bg: `#f5f5f5`, cards: `#ffffff`
- ToggleSwitch on bg: `#4a9eff`, off bg: `#ccc`
- Title bar: white bg, dark text
- All colors inverted appropriately

## Affected Files

| File | Changes |
|------|---------|
| `src/main/java/com/clipvault/ui/SettingsWindow.java` | Window frame, layout restructure, title bar, drag logic, card layout, control types |
| `src/main/resources/styles/dark-theme.css` | Rewrite `.settings-*` classes, add `.settings-card`, `.toggle-switch`, `.theme-button` styles |
| `src/main/resources/styles/light-theme.css` | Same structural changes for light theme |

## What NOT to Change

- No new Java dependencies
- No changes to MainPanel, App, or business logic
- Window positioning remains `centerOnScreen()`
- Hotkey capture logic unchanged
- Settings persistence unchanged

## Element Breakdown

### Color Palette (Dark)

| Token | Value | Usage |
|-------|-------|-------|
| --bg-window | `#1e1e1e` | Root VBox background |
| --bg-card | `#252525` | Section card background |
| --bg-header | `#252525` | Title bar area |
| --bg-footer | `#252525` | Bottom bar |
| --bg-input | `#2d2d2d` | Text field / spinner bg |
| --border | `#333333` | Separator / card borders |
| --border-input | `#404040` | Input field border |
| --text-primary | `#ffffff` | Title, section title |
| --text-secondary | `#e0e0e0` | Labels, input text |
| --text-tertiary | `#a0a0a0` / `#808080` | Descriptions |
| --accent | `#4a9eff` | Primary button, toggle on, focus glow |
| --accent-hover | `#3a8eef` | Hover states |

### Color Palette (Light)

| Token | Value | Usage |
|-------|-------|-------|
| --bg-window | `#f5f5f5` | Root VBox background |
| --bg-card | `#ffffff` | Section card background |
| --bg-header | `#ffffff` | Title bar area |
| --bg-footer | `#ffffff` | Bottom bar |
| --bg-input | `#f0f0f0` | Text field / spinner bg |
| --border | `#e8e8e8` | Separator / card borders |
| --border-input | `#d0d0d0` | Input field border |
| --text-primary | `#1a1a1a` | Title, section title |
| --text-secondary | `#333333` | Labels, input text |
| --text-tertiary | `#888888` / `#999999` | Descriptions |
| --accent | `#4a90d9` | Primary button, toggle on, focus glow |
| --accent-hover | `#3a80c9` | Hover states |

## Layout Structure

```xml
<VBox root>  <!-- -fx-background-color: --bg-window, transparent outer -->
  <StackPane contentContainer>  <!-- opaque bg, border-radius: 8, dropshadow via parent -->
    <VBox>
      <VBox header>     <!-- Title row + subtitle, close button -->
      <VBox cards>      <!-- Vertical list of card VBoxes -->
        <VBox card>     <!-- Section: icon + title + desc + controls -->
        <VBox card>
        ...
      </VBox>
      <HBox footer>     <!-- Close button -->
    </VBox>
  </StackPane>
</VBox>
```
