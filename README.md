# ClipVault

> Windows 剪贴板历史管理器 — 自动记录你复制的文本与图片，随时一键找回并粘贴。

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)](https://openjfx.io/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## 功能特性

- **自动监听剪贴板** — 后台 500ms 轮询，自动捕获文本与图片，重复内容自动去重
- **历史持久化** — 记录保存到 `%APPDATA%\ClipVault`，图片存为 PNG 并生成缩略图
- **全局快捷键唤起** — 默认 `Ctrl + Shift + V`，可在设置中自定义
- **搜索与过滤** — 关键词模糊搜索，支持按类型（全部/文本/图片）与时间过滤
- **固定条目** — 重要内容置顶，不受历史上限与清除操作影响
- **一键粘贴** — 选中条目自动回写剪贴板并模拟 `Ctrl+V` 粘贴到前台窗口
- **系统托盘** — 最小化到托盘，右键菜单快速操作
- **主题切换** — 深色 / 浅色主题
- **设置项** — 自定义保存路径、开机自启、退出清除历史、历史条数上限

---

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| UI | JavaFX 21 |
| 原生调用 | JNA 5.13.0（Win32 API） |
| 构建 | Maven |
| 测试 | JUnit 5.10.2 |
| 打包 | maven-shade-plugin（fat-jar）+ jpackage |

---

## 项目结构

```
src/main/java/com/clipvault/
├── Launcher.java              # 入口，配置中文字体后启动
├── App.java                   # 应用生命周期，统筹各模块
├── clipboard/ClipboardMonitor.java   # 剪贴板监听（文本/图片采集、去重）
├── hotkey/HotkeyManager.java          # 全局快捷键注册与消息循环
├── win32/
│   ├── Win32.java                      # JNA 接口定义
│   └── WindowsInputManager.java        # 模拟粘贴、窗口拖拽
├── model/
│   ├── ClipItem.java                   # 剪贴板条目（文本/图片、固定、时间戳）
│   └── AppSettings.java                # 应用设置
├── storage/
│   ├── HistoryManager.java             # 历史增删改查、搜索、持久化
│   ├── SettingsManager.java            # 设置读写
│   └── JsonUtil.java                   # JSON 工具
└── ui/
    ├── MainPanel.java                  # 主面板（透明圆角、搜索、卡片列表）
    ├── SettingsWindow.java             # 设置窗口
    ├── TrayManager.java                # 系统托盘
    ├── TrayPopup.java                  # 托盘菜单
    └── components/                     # Card / TextCard / ImageCard
```

---

## 环境要求

- **操作系统**：Windows x86-64
- **JDK**：21（开发）；分发版本自带 JRE，无需用户预装
- **Maven**：3.6+（开发）

---

## 快速开始

### 开发运行

```bash
# 编译运行
mvnw.cmd javafx:run

# 运行单元测试
mvnw.cmd test
```

### 构建发布

```bash
# 打包 fat-jar
打包.bat
# 等价于：mvn clean package -DskipTests
```

产物位于 `target/clipvault-1.0.0-fat.jar`，随后可用 jpackage 打包为 Windows 安装包。

### 运行已构建版本

```bash
运行 ClipVault.bat
```

---

## 数据目录

应用数据默认存储在 `%APPDATA%\ClipVault\`：

```
ClipVault/
├── history.json          # 历史记录
├── settings.json         # 用户设置
├── images/               # 原始图片
├── thumbnails/           # 缩略图
├── fonts/                # 中文字体
└── logs/clipvault.log    # 日志
```

可在设置中自定义保存路径。

---

## 快捷键

| 操作 | 快捷键 |
|------|--------|
| 唤起/隐藏主面板 | `Ctrl + Shift + V`（默认，可自定义） |

---

## 测试

单元测试覆盖模型层与存储层：

- `ClipItemTest` — 条目创建、相等、固定、时间戳
- `AppSettingsTest` — 默认值、序列化
- `HistoryManagerTest` — 增删改查、搜索、过滤、去重、上限
- `SettingsManagerTest` — 读写、路径变更

```bash
mvnw.cmd test
```

---

## 文档

- [开发工作流改进设计](docs/superpowers/specs/2026-05-31-dev-workflow-improvements-design.md)
- [设置窗口 UI 美化设计](docs/superpowers/specs/2026-06-03-settings-ui-beautify-design.md)

---

## 许可证

MIT
