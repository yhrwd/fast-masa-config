# Fast Masa Config

## English

Not enough convenient keybinds? Keep forgetting the ones you do have? Fast Masa Config was made for exactly that very ordinary Minecraft problem.

This is a client-side Fabric mod that reads configuration options exposed by installed MaLiLib-based mods and puts the settings you use most into a handy quick panel. Hold `Right Shift`, and you can toggle supported switches, change modes, or adjust numeric sliders without repeatedly digging through full configuration screens. Your blocks still need digging; your settings do not.

One codebase ships separate jars for every supported Minecraft version, currently from 1.21 through 26.3 — pick the file matching your game version; each file's version page lists the exact MaLiLib and Fabric API it needs.

### Features

- Automatically scans MaLiLib config screens and organizes entries by mod and config group.
- Opens the quick panel by holding `Right Shift`; the keybind can be changed.
- Keeps the game running and tries to preserve movement input while the panel is open.
- Closes on key release, the inventory key, or `Esc`, depending on your settings.
- Create, rename, hide, delete, and reorder custom groups.
- Drag and collapse floating group windows; positions, collapsed state, and expanded numeric rows are persisted.
- Toggle boolean values directly and adjust integer, float, and double values with expandable sliders.
- Search and filter the full config list by mod, group, and added/missing state.
- Add or remove entries from any shortcut group to build task-specific workflows.
- Create quick-message groups and send chat messages or `/` commands with variable substitution: `${player}`, `${x}`, `${y}`, `${z}`, `${px}`, `${py}`, `${pz}`, `${dimension}`, `${world}`, plus overworld and Nether coordinates `${overworld_x}`, `${overworld_z}`, `${nether_x}`, `${nether_z}` (aliases: `ow_x`, `ow_z`, `nx`, `nz`). Unknown variables are preserved as written.
- The Tools tab includes entity render filtering: hide selected entities in blacklist mode or render only selected entities in whitelist mode. The entity selector is searchable by name and ID.
- Optionally replace the vanilla block-breaking cracks with a shrinking block outline and translucent fill; configure outline/fill colors, transparency, line width, and local or multiplayer progress display.
- Includes config scanning and CSV export commands for compatibility diagnostics.

### Usage

After installing, hold `Right Shift` in game to open the quick panel. Use the full config screen to select entries, create groups, and arrange them to your liking.

If an entry is missing, the mod it belongs to may expose its MaLiLib config in a non-standard way. These client commands can help inspect what was discovered:

```text
/fastmasaconfig scan
/fastmasaconfig scan csv
/fastmasaconfig scan fallback
/fastmasaconfig scan fallback csv
```

### Compatibility and dependencies

This is a client-side mod and does not need to be installed on the server. Check the Minecraft version shown on the current file's version page and install:

- Fabric Loader
- Fabric API
- MaLiLib

The exact MaLiLib version depends on the Minecraft version; the version page always lists the right one. Mod Menu is optional and provides a convenient entry point to the full configuration screen.

Most mods that use standard MaLiLib config screens can be scanned, but implementations differ: group names, display names, or individual entries may not always be recognized. Booleans show as toggles and integers/floats/doubles as sliders; colors are edited on the full config screen. Strings, enum lists, and complex hotkeys are not quick-panel targets.

### Screenshots

The screenshots below show an older UI version. They demonstrate the general features and interaction model, but the current layout and visual styling may look different. The screenshots are old; the mod is not pretending otherwise.

![Older quick panel UI](https://cdn.modrinth.com/data/cached_images/f0539a0eb95e5877f503e5cf3af1f2d63f23362f.png)

![Older quick panel UI](https://cdn.modrinth.com/data/cached_images/2fcc46a9a05af551c4221e2f52e53923a2cb2aea.png)

![Older config UI](https://cdn.modrinth.com/data/cached_images/d4c389f366599a8d2ac70d5c2aa2738312a25590.png)

![Older config UI](https://cdn.modrinth.com/data/cached_images/089d26ffc01f85774d5e6c6c92c73d22a8ce5045.png)

### License and attribution

Fast Masa Config is licensed under `GPL-3.0-or-later`. Some floating-window visual behavior was adapted from public Meteor Client implementations; the relevant source files retain their attribution and GPL-3.0 notices. Fast Masa Config is an independent project and is not affiliated with or endorsed by Meteor Client or its developers.

### Changelog

#### 5.3.1

- Fixed the startup crash on Minecraft 1.21.9 / 1.21.10 / 1.21.11.
- Fixed the block-break indicator not rendering on Minecraft 1.21.1 through 1.21.11; when enabled it still replaces the vanilla block-breaking cracks with an expanding outline and translucent fill.
- Restored quick panel backgrounds, sliders, and borders on Minecraft 1.21.6 and newer.
- "Release to close" now defaults to off: the quick panel stays open after you release the open hotkey, and closes with `Esc`, the inventory key, or the hotkey again.
- The scanned config list now rebuilds after switching the game language instead of keeping the old translations until restart.

---

## 中文

按键不够用，快捷键又总是记不住？Fast Masa Config 就是为这种很普通、也很真实的烦恼做的。

这是一个客户端 Fabric Mod。它会自动读取已经安装的、基于 MaLiLib 的 Mod 配置，把常用选项整理到一个随手可开的快捷面板里。以后不用在一层又一层的完整配置菜单里考古，只要按住 `Right Shift`，就能快速切换开关、调整模式或拖动支持的数值滑条。

一套代码为所有受支持的 Minecraft 版本分别构建 jar，目前覆盖 1.21 到 26.3——下载与自己游戏版本一致的文件即可，每个文件对应的版本页会列出所需的 MaLiLib 和 Fabric API 版本。

### 主要功能

- 自动扫描 MaLiLib 配置界面，并按模组和配置分组整理配置项。
- 默认按住 `Right Shift` 打开快捷面板，按键可以自行修改。
- 面板不会暂停游戏，并尽量保持移动键输入同步。
- 支持松开热键、按背包键或按 `Esc` 关闭面板。
- 支持创建、重命名、隐藏、删除和排序自定义分组。
- 分组窗口可以拖动和折叠，窗口位置、折叠状态和展开的数值项会保存。
- 布尔值可以直接切换；整数、浮点数和双精度数值可以展开为滑条调整。
- 完整配置界面支持按模组、分组、已添加/未添加状态搜索和筛选。
- 可以把配置项加入或移出指定快捷分组，打造自己的工作流面板。
- 可创建快捷消息分组，一键发送聊天消息或 `/` 指令；发送前会解析变量：`${player}`、`${x}`、`${y}`、`${z}`、`${px}`、`${py}`、`${pz}`、`${dimension}`、`${world}`，以及主世界/下界坐标 `${overworld_x}`、`${overworld_z}`、`${nether_x}`、`${nether_z}`（别名 `ow_x`、`ow_z`、`nx`、`nz`）。未知变量会原样保留。
- 工具页提供实体渲染过滤，可在黑名单模式隐藏选中的实体，或在白名单模式仅渲染选中的实体；实体选择器支持按名称或 ID 搜索。
- 可选的动态挖掘指示器会关闭原版裂纹动画，改用随进度收缩的方块描边和半透明填充；支持起始色、完成色、透明度、描边宽度设置，并可显示多人游戏中的其他玩家挖掘进度。
- 提供配置扫描和 CSV 导出命令，方便排查兼容性问题。

### 使用方式

安装后进入游戏，默认按住 `Right Shift` 打开快捷面板。首次使用时，可以从完整配置界面选择要加入的配置项，再按自己的习惯创建和排列分组。

如果某个配置项没有出现在面板里，不一定是它“闹脾气”：不同 Mod 暴露 MaLiLib 配置的方式并不完全一致。可以使用以下客户端命令查看扫描结果：

```text
/fastmasaconfig scan
/fastmasaconfig scan csv
/fastmasaconfig scan fallback
/fastmasaconfig scan fallback csv
```

### 兼容性与依赖

这是客户端 Mod，不需要安装到服务端。安装前请确认当前下载文件对应的 Minecraft 版本，并安装所需依赖：

- Fabric Loader
- Fabric API
- MaLiLib

MaLiLib 的具体版本要求会随 Minecraft 版本变化，以下载文件对应的版本页为准。Mod Menu 为可选依赖，用于从 Mod Menu 打开完整配置界面。

大多数使用标准 MaLiLib 配置界面的 Mod 都能被扫描到，但各 Mod 的实现不完全一致，分组名、显示名或个别配置项可能无法稳定识别。布尔值显示为开关，整数/浮点/双精度显示为滑条；颜色在完整配置界面中编辑。字符串、选项列表和复杂热键暂不作为快捷面板的操作目标。

### 截图说明

下面的截图来自旧版用户界面，主要用于展示功能和交互方式。当前版本已经更新了界面布局和视觉样式，所以实际游戏画面可能与截图不同。截图没有穿越，只是 UI 先更新了。

![旧版快捷面板](https://cdn.modrinth.com/data/cached_images/f0539a0eb95e5877f503e5cf3af1f2d63f23362f.png)

![旧版快捷面板](https://cdn.modrinth.com/data/cached_images/2fcc46a9a05af551c4221e2f52e53923a2cb2aea.png)

![旧版配置界面](https://cdn.modrinth.com/data/cached_images/d4c389f366599a8d2ac70d5c2aa2738312a25590.png)

![旧版配置界面](https://cdn.modrinth.com/data/cached_images/089d26ffc01f85774d5e6c6c92c73d22a8ce5045.png)

### 最近更新

#### 5.3.1

- 修复 Minecraft 1.21.9 / 1.21.10 / 1.21.11 进游戏崩溃的问题。
- 修复挖掘指示动画在 1.21.1 ~ 1.21.11 全线不渲染的问题；开启后仍会以扩展轮廓加半透明填充替代原版挖掘裂纹。
- 恢复 1.21.6 及以上版本快捷面板的背景、滑条和边框显示。
- "松开关闭"默认改为关闭：松开打开热键后面板保持打开，用 `Esc`、背包键或再次按热键关闭。
- 切换游戏语言后，已扫描的配置列表会用新语言重建，不再停留在旧翻译直到重启。

#### 5.3.0

- 适配 Minecraft 26.3：Loader 0.19.5、Fabric API 0.161.0+26.3、MaLiLib 0.30.1、Mod Menu 21。
- 全屏配置页的模组/分组循环按钮改为下拉选择器，条目多时可直接跳选，展开后支持按名称过滤。
- 全屏配置页配色改为 Minecraft 原生灰白风格。
- 全屏配置页数值框新增点击手动输入：回车提交并按范围钳制，非法输入自动回退，点击其他位置或滚动列表自动提交。
- 快捷面板数值输入迁移到 26.3 新输入系统，恢复手动键入；数值框左缘与开关按钮列对齐。
- 修复顶部标题栏列表计数与下拉框重合的问题，以及 26.3 下屏幕状态读取失效的问题。
- 注意：如果输入法把右 Shift 设为中英文切换，游戏内该按键会被输入法拦截，请关闭该快捷键或把面板热键改为其他按键。

#### 5.2.0

- 全屏配置界面改用 MaLiLib 风格半透明背景、暗色主题和高对比度控件。
- 工具页重新整理实体渲染过滤、白名单和实体选择器，搜索输入不再每输入一个字符就失去焦点。
- 白名单开启时只渲染列表中的实体；关闭时隐藏列表中的实体；关闭实体过滤时保留原版渲染结果。
- 实体过滤在原版可见性判断之后执行，避免影响距离、视锥和其他渲染条件。
- 快捷消息变量说明、悬浮菜单主题色和快捷菜单可扫描配置项均已完善。

### 许可证与声明

本项目使用 `GPL-3.0-or-later` 许可证。悬浮窗口的部分视觉行为参考了 [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) 的公开实现，相关源码文件保留了来源和 GPL-3.0 许可证说明。Fast Masa Config 是独立项目，与 Meteor Client 及其开发团队没有官方隶属或背书关系。
