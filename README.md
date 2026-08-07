# Fast Masa Config

## English

Fast Masa Config was created for a practical reason: Minecraft has a limited number of convenient keybinds, and rarely used shortcuts are easy to forget. The mod scans configuration options exposed by MaLiLib-based mods and lets you place frequently used settings in a compact in-game quick panel. Switches, modes, and supported sliders can then be adjusted without repeatedly navigating through full configuration screens.

This is a client-side Fabric mod. Before installing it, check the Minecraft version and dependency requirements listed on the relevant version page, especially the required MaLiLib version. For a complete compatibility list, refer to the corresponding version page on Modrinth.

## 中文

Fast Masa Config 的设计初衷很直接：Minecraft 中便于使用的按键数量有限，而不常用的快捷键也很容易遗忘。该 Mod 会扫描基于 MaLiLib 的 Mod 所公开的配置项，并将常用设置整理到紧凑的游戏内快捷面板中。这样无需反复打开完整配置界面，即可快速调整开关、模式以及受支持的数值滑条。

这是一个客户端 Fabric Mod。安装前请确认对应版本页面列出的 Minecraft 版本和依赖要求，尤其是 MaLiLib 的版本。完整的兼容性信息请以 Modrinth 对应版本页为准。

## 截图说明

以下截图展示的是旧版用户界面，仅用于说明功能和交互方式；当前版本的界面布局与视觉样式可能有所不同。

![旧版快捷面板](https://cdn.modrinth.com/data/cached_images/f0539a0eb95e5877f503e5cf3af1f2d63f23362f.png)

![旧版快捷面板](https://cdn.modrinth.com/data/cached_images/2fcc46a9a05af551c4221e2f52e53923a2cb2aea.png)

![旧版配置界面](https://cdn.modrinth.com/data/cached_images/d4c389f366599a8d2ac70d5c2aa2738312a25590.png)

![旧版配置界面](https://cdn.modrinth.com/data/cached_images/089d26ffc01f85774d5e6c6c92c73d22a8ce5045.png)

## 功能

- 按住热键打开 Meteor 风格的悬浮分组菜单，默认热键为 `Right Shift`。
- 在悬浮窗口中直接切换布尔配置；点击数值项展开步长为 `1` 的滑条。
- 默认分组包含完整配置入口；用户分组可隐藏、拖拽、折叠和排序。
- 在完整配置界面的“全部配置”页选择目标分组后搜索、筛选、添加或移除配置项。
- 当前目标分组已添加的配置会优先显示，方便维护多个工作流。
- 可选的动态挖掘进度显示：用渐变方块描边和半透明填充替换原版裂纹动画，并支持远程挖掘进度。
- 打开快捷面板时不暂停游戏，并尽量透传移动键，减少操作中断。
- 提供配置扫描命令和 CSV 导出，便于排查兼容性问题。

## 支持环境

当前主线面向以下环境开发：

- Minecraft `26.2`
- Fabric Loader `0.19.3` 或更高版本
- Java `25` 或更高版本
- Fabric API
- MaLiLib `0.29.x`

不同 Minecraft 版本对应的 Fabric API、MaLiLib、Mod Menu 和 Yarn mappings 版本不同，请以对应分支的 `gradle.properties` 和 `fabric.mod.json` 为准。

## 依赖

必需：

- Fabric Loader
- Fabric API
- MaLiLib

可选：

- Mod Menu：用于在 Mod Menu 中打开 Fast Masa Config 的完整配置界面。

兼容对象不是硬依赖。Fast Masa Config 会尝试扫描当前客户端中已经安装、并提供 MaLiLib 配置界面的 Mod，例如 Litematica、MiniHUD、Tweakeroo 等。

## 使用

默认按住 `Right Shift` 打开悬浮分组菜单。窗口不会暂停游戏；按住移动键后再打开菜单时，前进、后退、跳跃等按键会持续同步。

完整界面中有两个主要页签：

- `通用`：调整 Fast Masa Config 自身设置，例如快捷键、松开关闭和背包键关闭行为。
- `全部配置`：选择当前目标分组，浏览已扫描到的 MaLiLib 配置项，并将项目加入或移出该分组。

## 分组与快捷操作

- 默认的 `Fast Masa Config` 分组不能删除，但可以隐藏；用户分组可创建、重命名、隐藏、删除和排序。
- 默认分组被隐藏后，任意可见窗口仍保留完整配置入口；全部隐藏时会显示恢复配置入口。
- 分组窗口通过标题栏拖拽，右侧按钮分别用于折叠和隐藏。窗口位置、折叠状态和已展开的数值项会保存。
- 布尔项的强调色表示 `true`；数值项点击行或箭头即可展开滑条，步长固定为 `1`。
- 在“通用”设置中可以启用动态挖掘进度显示，并分别调整描边、填充、远程进度和描边宽度；颜色项可直接打开 HSV 编辑器。

## 命令

Fast Masa Config 注册了一个客户端命令，用于扫描当前已加载的 MaLiLib 配置项：

```text
/fastmasaconfig scan
/fastmasaconfig scan csv
/fastmasaconfig scan fallback
/fastmasaconfig scan fallback csv
```

CSV 文件会导出到当前游戏运行目录：

- `fast-masa-config-scan.csv`
- `fast-masa-config-fallback-scan.csv`

这些命令主要用于开发和兼容性排查。普通使用一般不需要执行。

## 兼容性说明

Fast Masa Config 主要依赖 MaLiLib 配置界面暴露出来的信息。大多数使用标准 MaLiLib 配置界面的 Mod 可以被扫描到，但不同 Mod 的配置界面实现方式不完全一致，分组名、显示名或部分配置项可能无法稳定识别。

当前主要支持以下配置类型：

- 布尔值：显示为开关。
- 整数、浮点数和双精度数值：显示为滑条。
- 颜色：在完整配置界面中显示色块并支持 HSV 编辑。

字符串、选项列表和复杂热键配置暂不作为快捷面板的主要操作目标；颜色配置目前仅在完整配置界面中编辑。

## 本地开发

需要 Java 25。构建项目：

```bash
./gradlew build
```

运行测试：

```bash
./gradlew test
```

编译客户端源码（包括 Fabric 客户端入口和 Mixin）：

```bash
./gradlew compileClientJava
```

Windows 环境可以使用：

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat compileClientJava
```

本地开发时主要关注这些配置文件：

- `gradle.properties`：Minecraft、Yarn、Fabric Loader、Fabric API、MaLiLib、Mod Menu 和 Mod 版本。
- `build.gradle`：Loom、源码集、依赖来源、打包和测试配置。
- `src/main/resources/fabric.mod.json`：Mod 元数据、入口点、运行环境和依赖范围。

`libs/` 目录用于本地开发和兼容性测试，其中可能放有大量测试用 Mod jar。它们不是全部发布依赖，实际依赖请以 `gradle.properties`、`build.gradle` 和 `fabric.mod.json` 为准。

## 项目结构

```text
src/main/java/fastui/yure/config/       通用配置、快捷项存储和配置编辑逻辑
src/client/java/fastui/yure/client/     Fabric 客户端入口、扫描、输入和 GUI
src/main/resources/                     fabric.mod.json、图标和语言文件
src/test/java/                          单元测试
libs/                                   本地兼容性测试用 jar，不代表全部依赖
docs/                                   开发文档和计划记录
```

客户端代码使用 Loom 的 split environment source sets，Minecraft 客户端相关代码放在 `src/client/java`，通用配置和数据结构放在 `src/main/java`。

## 许可证

本项目使用 `GPL-3.0-or-later` 许可证。修改版发布时需要保留作者和许可证声明，并按 GPL 要求提供对应源码；本项目按原样提供，不包含任何担保。详见 [LICENSE](LICENSE)。
