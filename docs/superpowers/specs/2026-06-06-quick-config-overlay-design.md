# Quick Config Overlay Design

## 目标

为 `fast-masa-config` 增加本 mod 自身配置项，并提供一个按住热键显示、松开自动关闭的中央小型示例 UI。

## 交互

- 按住配置热键时打开中央小 UI。
- 松开热键时自动关闭 UI。
- 打开 UI 后释放鼠标，允许点击面板控件。
- 鼠标移动不再转动视角。
- 游戏不暂停，玩家移动、跳跃、潜行等操作继续生效。
- 首版 UI 只做示例信息和扫描入口，不直接做批量配置修改。

## 推荐实现

使用轻量 `Screen` 弹层实现：

- `shouldPause()` 返回 `false`。
- 热键按下时 `MinecraftClient#setScreen(new QuickConfigScreen())`。
- Screen 内检测热键不再按住时关闭自身。
- 对移动类按键做放行处理，避免 Screen 默认键盘处理阻断玩家移动。

不采用 HUD overlay + 手动鼠标释放作为首版方案，因为它更容易和 Minecraft 鼠标锁定状态冲突。

## 目录规划

```text
src/client/java/fastui/yure/client/
  FastMasaConfigClient.java
  config/
    FastMasaConfigs.java
    FastMasaConfigHandler.java
  input/
    FastMasaInputHandler.java
  gui/
    QuickConfigScreen.java
    QuickConfigPanel.java
```

## 本 mod 配置项

- `openQuickConfig`: `ConfigHotkey`，长按打开快捷配置 UI。
- `panelWidth`: `ConfigInteger`，控制快捷面板宽度。
- `panelScale`: `ConfigDouble`，控制快捷面板缩放。
- `showScanSummary`: `ConfigBoolean`，是否显示扫描统计摘要。
- `releaseToClose`: `ConfigBoolean`，松开热键后自动关闭。
- `closeOnInventoryKey`: `ConfigBoolean`，背包键或 ESC 关闭面板。

## MaLiLib 集成

- 使用 `ConfigManager.registerConfigHandler()` 让本 mod 配置持久化到 MaLiLib 管理的配置文件。
- 使用 `Registry.CONFIG_SCREEN.registerConfigScreenFactory()` 让本 mod 配置也能出现在 MaLiLib 配置入口中。
- 使用 `InputEventHandler.getKeybindManager().registerKeybindProvider()` 注册热键。
- 热键和 UI 行为使用 MaLiLib `ConfigHotkey` 与 `IKeybindProvider`，不新增 Fabric KeyBinding 依赖。

## 配置修改一致性

后续示例 UI 或完整 UI 修改 MaLiLib 配置时，必须走已有 `MasaConfigEditor`：

- 修改前先校验用户输入。
- 写入时调用目标配置对象的 setter 或 MaLiLib 等价接口。
- 应用后调用 `ConfigManager.getInstance().onConfigsChanged(modId)`。

这样保证修改效果与在 MaLiLib UI 中手动修改一致。

## 测试

- 单元测试覆盖本 mod 配置 handler 的读写结构。
- 单元测试继续覆盖 `MasaConfigEditor` 的校验与提交行为。
- 完整构建使用 `./gradlew.bat build` 验证。
- 热键弹层需要进游戏手动验证：按住显示、鼠标释放、松手关闭、移动键不中断。
