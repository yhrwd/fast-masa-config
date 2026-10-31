# Fast Masa Config

> **NOTICE**: This mod isn't a fully developed one yet, so please report any issues you find.

# English

Fast Masa Config is a client-side Fabric mod that provides quick access to configuration options from Masa mods.

It automatically discovers available settings exposed through MaLiLib and lets you organize frequently used options into a convenient quick-access panel, so you can change settings without repeatedly opening full configuration screens.

By default, press `Right Shift` to open the panel. The keybind is fully configurable; closing on hotkey release is optional.

---

## Features

- Automatically discovers configuration entries from installed MaLiLib-based mods.
- Organizes entries by mod and configuration group.
- Open the quick panel with `Right Shift` (configurable).
- Keeps the game running while the panel is open and preserves movement input whenever possible.
- Close the panel by releasing the hotkey, pressing the inventory key, or pressing `Esc`.
- Create, rename, hide, delete, and reorder custom groups.
- Floating group windows support dragging, collapsing, and persistent layout.
- On Minecraft 26.1.2 / 26.2 / 26.3, drag either side of configuration and quick-message windows to resize them; each group remembers its width.
- Toggle boolean values directly or adjust numeric values with expandable sliders.
- Search and filter the full configuration list by mod, group, and whether an entry has already been added to the quick panel.
- Organize configuration entries into custom shortcut groups.
- Optional dynamic block-breaking indicator that replaces vanilla crack textures with an outline and translucent fill.
- Customize colors, transparency, outline width, and multiplayer progress rendering.
- Includes configuration scanning and CSV export commands for compatibility diagnostics.

---

## Usage

After installation, hold `Right Shift` to open the quick panel.

Use the full configuration screen to add entries to your shortcut groups and organize them however you like.

If some configuration entries are missing, the corresponding mod may expose its MaLiLib configuration differently. The following client commands can be used to inspect the scan results:

```text
/fastmasaconfig scan
/fastmasaconfig scan csv
/fastmasaconfig scan fallback
/fastmasaconfig scan fallback csv
```

---

## Dependencies

This is a client-side mod and does not need to be installed on servers.

Install the following dependencies for your Minecraft version:

- Fabric Loader
- Fabric API
- MaLiLib

The required MaLiLib version depends on the Minecraft version. Please refer to the current Modrinth release page.

Mod Menu is optional and provides a convenient entry point to the full configuration screen.

---

## Screenshots

The latest version has updated the interface layout and visual style, so the actual in-game interface may differ from the screenshots.

![快捷面板](https://cdn.modrinth.com/data/cached_images/c24e696e23c1f216a8101c6486eea35fc2514a4c.png)

![配置界面](https://cdn.modrinth.com/data/cached_images/cb885969f57e177db4be3bd80cdc38aca8b3fbb0_0.webp)

![配置界面](https://cdn.modrinth.com/data/cached_images/69dad963c07f42c54068ebda68cf661bb65f9327_0.webp)

![配置界面](https://cdn.modrinth.com/data/cached_images/751b6f230de44bf40cefde4d8930fcd0105da263_0.webp)

![配置界面](https://cdn.modrinth.com/data/cached_images/501ef92aadbef3af0f468838e2333212fcfd977f_0.webp)
---

## Latest update: 5.4.0

- Resize configuration and quick-message windows from either edge, with a horizontal resize cursor and edge highlight.
- Each group saves its width. Long option names have more room when windows are widened.
- Updated icon and default floating-menu accent: `#488796`. Existing saved colors stay unchanged; use Reset to apply the new default.
- Released for Minecraft 26.1.2, 26.2, and 26.3. [Full changelog](https://github.com/yhrwd/fast-masa-config/blob/main/CHANGELOG.md).

## Feedback

Found a bug, compatibility issue, or have a feature request?

Please report it on **[GitHub Issues](https://github.com/yhrwd/fast-masa-config/issues)**.

---

## 中文

> **注** ：这还不是一个成熟的mod，如果发现问题请及时反馈

Fast Masa Config 是一个客户端 Fabric Mod，用于快速访问和管理 Masa 系 Mod 的常用配置。

它会自动读取已安装 Mod 提供的配置，并提供一个快捷配置面板，让你无需频繁打开完整配置界面，就能在游戏过程中快速切换常用功能、调整模式或修改数值。

默认按住 `Right Shift` 即可打开快捷面板，按键可自行修改。

---

## 功能

- 自动扫描已安装的 MaLiLib 配置，并按 Mod 和配置分组整理。
- 默认按 `Right Shift` 打开快捷面板，支持修改快捷键；可选开启松开热键时关闭。
- 面板打开时不会暂停游戏，并尽可能保持移动输入正常。
- 支持松开快捷键、按背包键或按 `Esc` 关闭面板。
- 支持创建、重命名、隐藏、删除及排序自定义分组。
- 分组窗口支持拖动、折叠，并自动保存窗口位置、折叠状态及展开的数值项。
- Minecraft 26.1.2 / 26.2 / 26.3 支持拖动配置分组和快捷消息浮窗的左右边缘调整宽度，每组独立保存。
- 布尔值支持一键切换；整数、浮点数和双精度数值支持滑条调节。
- 完整配置界面支持按 Mod、配置分组以及是否已添加到快捷面板进行搜索和筛选。
- 可自由将配置项加入或移出快捷分组，按自己的使用习惯组织配置。
- 提供可选的动态挖掘指示器，可替代原版裂纹动画，使用描边和半透明填充显示挖掘进度。
- 挖掘效果支持自定义颜色、透明度、描边宽度，并可显示多人游戏中其他玩家的挖掘进度。
- 提供配置扫描及 CSV 导出命令，便于排查兼容性问题。

---

## 使用

安装后进入游戏，默认按 `Right Shift` 打开快捷面板。

首次使用时，可以在完整配置界面中选择需要加入快捷面板的配置项，并根据自己的使用习惯创建和调整分组。

如果某些配置项没有被识别，通常是因为对应 Mod 使用了不同的 MaLiLib 配置实现方式。可以使用以下客户端命令查看扫描结果：

```text
/fastmasaconfig scan
/fastmasaconfig scan csv
/fastmasaconfig scan fallback
/fastmasaconfig scan fallback csv
```

---

## 依赖

这是一个客户端 Mod，无需安装到服务端。

安装前请确认下载页面对应的 Minecraft 版本，并安装以下依赖：

- Fabric Loader
- Fabric API
- MaLiLib

MaLiLib 的具体版本要求会随 Minecraft 版本变化，请以当前 Modrinth 页面列出的依赖为准。

Mod Menu 为可选依赖，可用于直接打开完整配置界面。

---

## 截图

见上文

最新版本已经更新设计了界面布局和视觉样式，因此实际游戏中的界面可能与截图存在差异。

---

## 最近更新：5.4.0

- 配置分组和快捷消息浮窗均支持左右边缘缩放，悬停或拖动时显示横向双箭头光标并高亮边缘。
- 每组独立保存宽度，拉宽后更容易查看较长的配置名称。
- 更新图标，默认悬浮菜单主题色改为 `#488796`；已有颜色设置会保留，可通过“重置”应用新默认色。
- 本次发布覆盖 Minecraft 26.1.2、26.2、26.3。[完整更新日志](https://github.com/yhrwd/fast-masa-config/blob/main/CHANGELOG.md)。

## 反馈

如果发现 Bug、兼容性问题，或有功能建议，欢迎前往 **[GitHub Issues](https://github.com/yhrwd/fast-masa-config/issues)** 反馈。

---

## License

Fast Masa Config is licensed under **GPL-3.0-or-later**.

Some floating-window behavior is adapted from public Meteor Client implementations. The relevant source files retain their original copyright notices and GPL-3.0 license headers.

Fast Masa Config is an independent project and is **not affiliated with or endorsed by Meteor Client or its developers**.
