# ClipVault 开发工作流改进 — 设计方案

**日期**：2026-05-31  
**状态**：待实现  
**目标用户**：Java 入门开发者

---

## 背景

ClipVault 是一个 Windows 剪贴板管理器（JavaFX 21 + Maven），共 10 个源文件，结构良好。当前缺少：
- 版本控制（没有 git）
- 自动化测试
- 系统化的调试流程

开发者处于 Java 入门阶段，需要一套**低成本、渐进式**的改进方案，而非一次性堆砌专业工具链。

## 设计目标

1. 改代码有安全网 — 改坏了能回退，能知道是否搞坏了别的功能
2. 出 bug 能快速定位 — 知道看哪里、怎么排查
3. 代码逻辑集中 — 改一处生效全局，不遗漏
4. 每一步独立、简单 — 学到就能用，不 overwhelm

---

## 第1步：Git 版本控制

### 要做什么
- `git init` 初始化仓库
- 创建 `.gitignore`（排除 `target/`、`.idea/`、`*.log`）
- 按模块分批提交：依赖配置 → 模型 → 存储 → 剪贴板/快捷键 → UI → 资源

### 核心命令（只需掌握5个）
`git status` `git add -A` `git commit -m "..."` `git branch <name>` `git checkout <name>` `git reset --hard HEAD`

### 改动文件
- 新建 `.gitignore`
- 不修改任何源码

---

## 第2步：JUnit 5 测试

### 测试优先级

| 优先级 | 目标类 | 测试内容 |
|--------|--------|---------|
| ⭐⭐⭐ | `ClipItem` | 创建文本/图片条目、相等判断、固定切换、时间戳 |
| ⭐⭐⭐ | `HistoryManager` | 增删改查、关键词搜索、类型/时间过滤、固定、去重 |
| ⭐⭐ | `AppSettings` | 默认值验证、JSON 反序列化 |
| ⭐ | `SettingsManager` | 读写设置文件、路径变更 |

**UI 类不测** — UI 测试复杂度高，入门阶段性价比低。

### 依赖添加
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

### 测试目录结构
```
src/
├── main/java/com/clipvault/...   （不变）
└── test/java/com/clipvault/
    ├── model/
    │   ├── ClipItemTest.java
    │   └── AppSettingsTest.java
    └── storage/
        ├── HistoryManagerTest.java
        └── SettingsManagerTest.java
```

### 运行方式
```bash
mvnw.cmd test          # 命令行
# 或在 IDEA 中右键测试类 → Run
```

---

## 第3步：日志与调试

### 关键位置补充日志

| 文件 | 位置 | 日志内容 | 级别 |
|------|------|---------|------|
| `ClipboardMonitor` | `checkClipboard()` 检测到新内容时 | "剪贴板内容变化，类型：{}" | INFO |
| `HotkeyManager` | 快捷键回调触发时 | "快捷键触发" | DEBUG |
| `HistoryManager` | `save()` 方法 | "保存 {} 条记录到 {}" | INFO |
| `MainPanel` | 搜索/过滤操作 | "搜索：{}，过滤类型：{}" | DEBUG |

### 调试定位流程
1. 打开 `%APPDATA%/ClipVault/logs/clipvault.log`
2. 看 ERROR/WARN 级别日志
3. 若无明显错误 → 改 `logback.xml` level 为 `TRACE` → 复现 bug
4. 日志中类名`com.clipvault.clipboard.ClipboardMonitor` 直接指明问题类

### IDEA 调试三招
| 操作 | 快捷键 | 用途 |
|------|--------|------|
| 断点 | 行号旁点击 | 程序跑到这里暂停 |
| Step Into | F7 | 进入方法内部 |
| Step Over | F8 | 执行当前行，不进方法 |

---

## 第4步：代码小整理

### 4.1 提取过滤逻辑
新建 `model/ClipItemFilter.java`，将 `HistoryManager` 和 `MainPanel` 中重复的过滤/排序代码集中到一个类。

**改动影响**：
- 新建 `ClipItemFilter.java`
- 修改 `HistoryManager.java`：过滤方法委托给 `ClipItemFilter`
- 修改 `MainPanel.java`：过滤方法委托给 `ClipItemFilter`

### 4.2 修复 hashCode
`ClipItem.hashCode()` 在 `type` 为 null 时回退到 `Object.hashCode()`，与 `equals()` 逻辑不一致。

**修复**：`hashCode()` 始终基于 `type`、`content`、`fileName` 计算，使用 `Objects.hash()`。

**改动影响**：仅修改 `ClipItem.java` 中 `hashCode()` 方法。

---

## 改 bug 标准流程（完成后）

```
1. git checkout -b fix-xxx          # 开分支
2. 写测试复现 bug → 测试失败       # 确认问题
3. 看日志 + IDEA 断点定位          # 找根因
4. 修代码 → 跑全部测试 → 通过      # 确认修复且无回归
5. git commit → merge               # 合入主分支
```

---

## 不做什么

- ❌ 不加 CI/CD — 单人项目，本地跑测试足够
- ❌ 不加 Mockito — 当前依赖简单，直接 new 即可
- ❌ 不测 UI — 复杂度高，收益低
- ❌ 不大重构 — 只做两处小整理，不影响现有功能
- ❌ 不加代码质量工具（SonarLint 等）— 入门阶段先聚焦基础

---

## 实现顺序

| 步骤 | 预计耗时 | 依赖 |
|------|---------|------|
| 第1步：Git | 30分钟 | 无 |
| 第2步：JUnit 5 | 1-2小时 | Git（每写完一个测试类就 commit） |
| 第3步：日志 | 30分钟 | 无 |
| 第4步：代码整理 | 1小时 | 第2步（整理前有测试保护） |
