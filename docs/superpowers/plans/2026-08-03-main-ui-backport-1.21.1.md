# Main UI Backport to Minecraft 1.21.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Minecraft 1.21.1 分支复刻 main 的新配置界面、扫描索引和快捷面板行为。

**Architecture:** 以 1.21.1 的 `Screen`、`DrawContext` 和 MaLiLib 0.21.10 作为运行时边界。将 main 的页面状态、筛选和快捷项逻辑迁移到现有 1.21.1 配置/扫描模型，所有渲染和输入调用按 1.21.1 API 适配。

**Tech Stack:** Java 21, Minecraft 1.21.1, Fabric Loom 1.14, Fabric API, MaLiLib 0.21.10, JUnit 5。

---

### Task 1: 扩展配置扫描与索引

**Files:**
- Create: `src/client/java/fastui/yure/client/scan/ConfigScreenSourceService.java`
- Create: `src/client/java/fastui/yure/client/scan/ConfigGuiGroupScanner.java`
- Modify: `src/client/java/fastui/yure/client/MasaConfigProbe.java`
- Modify: `src/client/java/fastui/yure/client/index/ConfigIndexService.java`
- Modify: `src/client/java/fastui/yure/client/scan/ConfigScanSummaryService.java`
- Test: `src/test/java/fastui/yure/client/scan/ConfigScreenSourceServiceTest.java`

- [ ] **Step 1: 迁移来源去重测试并运行失败测试**

```java
assertEquals(List.of("registry", "modmenu"),
        ConfigScreenSourceService.deduplicateSourceIds(List.of("registry", "modmenu", "registry")));
```

Run: `./gradlew.bat test --tests fastui.yure.client.scan.ConfigScreenSourceServiceTest`

- [ ] **Step 2: 实现 Registry/ModMenu 来源服务和复杂分组扫描器**

实现 `ConfigScreenSourceService` 的 Registry 与反射 ModMenu entrypoint 收集，按 mod id 去重；实现 `ConfigGuiGroupScanner` 对 enum、嵌套对象、索引列表与 getter/setter 的受控遍历，并在 `finally` 恢复临时状态。

- [ ] **Step 3: 将扫描结果接入 ConfigIndexService 并运行扫描测试**

Run: `./gradlew.bat test --tests fastui.yure.client.scan.ConfigScreenSourceServiceTest`
Expected: PASS

### Task 2: 迁移全屏三页配置界面

**Files:**
- Modify: `src/client/java/fastui/yure/client/gui/FastMasaConfigGui.java`
- Create: `src/client/java/fastui/yure/client/gui/GuiHitTest.java`
- Modify: `src/main/java/fastui/yure/config/ShortcutConfigStore.java`
- Modify: `src/main/java/fastui/yure/config/ShortcutEntry.java`
- Modify: `src/main/resources/assets/fast-masa-config/lang/en_us.json`
- Modify: `src/main/resources/assets/fast-masa-config/lang/zh_cn.json`
- Test: `src/test/java/fastui/yure/client/gui/GuiHitTestTest.java`

- [ ] **Step 1: 写入矩形命中测试并确认失败**

```java
assertTrue(GuiHitTest.contains(10, 10, 20, 20, 29, 29));
assertFalse(GuiHitTest.contains(10, 10, 20, 20, 30, 29));
```

Run: `./gradlew.bat test --tests fastui.yure.client.gui.GuiHitTestTest`

- [ ] **Step 2: 实现自绘 Screen 的三页状态、搜索与筛选**

`FastMasaConfigGui` 保存当前页、搜索词、mod/group 过滤器和行滚动位置；使用 `DrawContext` 渲染三页标签、搜索输入、过滤按钮和可见行。Generic 页调用现有配置编辑器；Shortcuts 页调用 `ShortcutConfigStore` 的添加、删除、排序；All Configs 页调用 `ConfigIndexService` 添加/删除快捷项。

- [ ] **Step 3: 添加热键输入、数值编辑和重置入口**

复用 1.21.1 MaLiLib 的配置类型、`MasaConfigEditor` 与 `IHotkey`，不使用 26.1.2 的 `GuiKeybindSettings`。所有鼠标和键盘回调保持 `Screen` 的 1.21.1 签名。

- [ ] **Step 4: 运行 GUI 逻辑测试**

Run: `./gradlew.bat test --tests fastui.yure.client.gui.GuiHitTestTest`
Expected: PASS

### Task 3: 迁移快捷面板双模式和输入行为

**Files:**
- Modify: `src/client/java/fastui/yure/client/gui/QuickConfigPanel.java`
- Modify: `src/client/java/fastui/yure/client/gui/QuickConfigScreen.java`
- Create: `src/client/java/fastui/yure/client/gui/QuickPanelItem.java`
- Modify: `src/client/java/fastui/yure/client/gui/QuickPanelLayout.java`
- Modify: `src/client/java/fastui/yure/client/shortcut/ShortcutResolver.java`
- Modify: `src/client/java/fastui/yure/client/input/FastMasaInputHandler.java`
- Test: `src/test/java/fastui/yure/client/gui/QuickPanelLayoutTest.java`

- [ ] **Step 1: 迁移双模式布局测试并确认失败**

```java
assertEquals(QuickConfigPanel.PanelMode.SHORTCUTS,
        QuickConfigPanel.PanelMode.SHORTCUTS);
assertEquals(QuickConfigPanel.PanelMode.ENABLED_BOOLEANS,
        QuickConfigPanel.PanelMode.ENABLED_BOOLEANS);
```

Run: `./gradlew.bat test --tests fastui.yure.client.gui.QuickPanelLayoutTest`

- [ ] **Step 2: 实现 Quick/Enabled 模式和模式切换**

创建 `QuickPanelItem` 保存配置、显示名、mod/group 和控制类型。`QuickConfigPanel` 添加内部 `PanelMode` 枚举、绘制模式标签并暴露 `getModeAt`；`QuickConfigScreen` 在 Quick 模式解析 `ShortcutConfigStore`，在 Enabled 模式筛选当前 true 的布尔配置，操作后刷新列表。

- [ ] **Step 3: 迁移热键再次关闭和移动键状态同步**

保留物理按键检查；当关闭策略不是松开关闭时，打开热键再次触发关闭。继续使用 1.21.1 的 `KeyBinding.setPressed` 与 `matchesKey`，不引入 26.1.2 输入事件类型。

- [ ] **Step 4: 运行快捷面板测试**

Run: `./gradlew.bat test --tests fastui.yure.client.gui.QuickPanelLayoutTest`
Expected: PASS

### Task 4: 集成验证和分支交付

**Files:**
- Modify: `src/main/java/fastui/yure/config/FastMasaConfigs.java`
- Modify: `src/main/java/fastui/yure/config/FastMasaConfigHandler.java`
- Test: `src/test/java/fastui/yure/client/MasaConfigProbeTest.java`

- [ ] **Step 1: 同步配置默认值和持久化入口**

将 main 中支持新界面的配置项与语言键同步到 1.21.1 分支，保留旧快捷项文件格式的读取路径。

- [ ] **Step 2: 运行完整测试和构建**

Run: `./gradlew.bat test && ./gradlew.bat build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 启动隔离的 1.21.1 客户端并检查日志**

Run: `./gradlew.bat runClient`
Expected log: `Loading Minecraft 1.21.1`, `fast-masa-config`, `malilib 0.21.10`

- [ ] **Step 4: 提交并推送迁移结果**

```bash
git add src build.gradle gradle.properties docs/superpowers
git commit -m "feat: backport main UI to Minecraft 1.21.1"
git push
```
