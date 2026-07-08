# Fast Masa Config

Fast Masa Config 是一个 Fabric 客户端 Mod，用来给 MaLiLib 系 Mod 提供更快的配置访问方式。它会扫描已注册的 MaLiLib 配置界面，把常用的布尔值和数值配置整理到一个游戏内快捷面板里。

English summary: Fast Masa Config is a Fabric client-side helper for quickly accessing MaLiLib-based mod configs.

## 功能

- 按住热键打开游戏内快捷配置面板，默认热键为 `Left Alt + C`。
- 在快捷面板中直接切换布尔配置，或拖动滑条调整整数/小数配置。
- 在完整配置界面中搜索、筛选并添加快捷项。
- 支持按模组和配置分组筛选，方便管理来自多个 MaLiLib Mod 的配置。
- 提供“已启用”视图，快速查看当前开启的布尔配置项。
- 打开快捷面板时不暂停游戏，并尽量透传移动键，减少操作中断。
- 提供配置扫描命令和 CSV 导出，便于排查兼容性问题。

## 支持环境

当前主线面向以下环境开发：

- Minecraft `1.21.7`
- Fabric Loader `0.19.3` 或更高版本
- Java `21` 或更高版本
- Fabric API
- MaLiLib `0.25.x`

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

默认按住 `Left Alt + C` 打开快捷面板。面板默认显示手动添加的快捷项，也可以切换到“已启用”视图查看当前开启的布尔配置。

完整界面中有三个主要页签：

- `通用`：调整 Fast Masa Config 自身设置，例如快捷键、面板宽度、高度、缩放、透明度和关闭行为。
- `快捷方式`：管理已添加的快捷项，可以调整顺序、移除失效项，或手动输入配置 ID。
- `全部配置`：浏览已扫描到的 MaLiLib 配置项，并把需要高频操作的项目加入快捷面板。

手动添加快捷项时支持两种 ID 格式：

```text
modId/groupId/configName
modId:configName
```

优先使用完整的 `modId/groupId/configName`，这样同名配置项更不容易匹配错误。

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

字符串、颜色、选项列表和复杂热键配置暂不作为快捷面板的主要操作目标。

## 本地开发

推荐使用 Java 21。构建项目：

```bash
./gradlew build
```

运行测试：

```bash
./gradlew test
```

Windows 环境可以使用：

```powershell
.\gradlew.bat build
.\gradlew.bat test
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
