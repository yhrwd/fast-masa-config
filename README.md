# Fast Masa Config

A client-side Fabric mod that puts frequently used MaLiLib settings into a compact in-game quick panel. English first; full documentation in Chinese below. 英文在前，完整中文文档在下方。

## English

### What it does

Fast Masa Config was created for a practical reason: Minecraft has a limited number of convenient keybinds, and rarely used shortcuts are easy to forget. The mod scans configuration options exposed by MaLiLib-based mods (Litematica, MiniHUD, Tweakeroo, …) and lets you place frequently used settings in floating quick groups. Switches, modes, and supported sliders can then be adjusted without repeatedly navigating through full configuration screens.

One codebase ships a separate jar per supported Minecraft version, currently 1.21 through 26.3. Install the file matching your game version; its Modrinth version page lists the exact MaLiLib and Fabric API requirements.

### Features

- Automatically scans MaLiLib config screens and organizes entries by mod and config group.
- Quick panel opens by holding `Right Shift` (rebindable); it never pauses the game and keeps movement keys working.
- Closes on key release, the inventory key, or `Esc`.
- Floating group windows: drag, collapse, hide; positions and states are persisted.
- Booleans toggle directly; integer/float/double entries expand into sliders.
- Full config screen with search and filtering by mod, group, and added/missing state.
- Quick messages: send chat messages or `/` commands with variables like `${player}`, `${x}`, `${y}`, `${z}`, `${dimension}`, plus overworld/Nether coordinate variables.
- Tools tab: entity render filtering (blacklist/whitelist with a searchable entity picker) and a dynamic block-breaking indicator with configurable outline/fill colors.
- Config scanning commands with CSV export for compatibility diagnostics.

### Supported versions

| Build target | Minecraft versions | MaLiLib |
|---|---|---|
| `26.3` (default) | 26.3 | 0.30.x |
| `26.2` | 26.2 | 0.29.x |
| `26.1.2` | 26.1.2 | 0.28.x |
| `1.21.11` | 1.21.11 | 0.27.x |
| `1.21.9-1.21.10` | 1.21.9 – 1.21.10 (one jar) | 0.26.x |
| `1.21.6-1.21.8` | 1.21.6 – 1.21.8 (one jar) | 0.25.x |
| `1.21.5` | 1.21.5 | 0.24.x |
| `1.21.4` | 1.21.4 | 0.23.x |
| `1.21.2-1.21.3` | 1.21.2 – 1.21.3 (one jar) | 0.22.x |
| `1.21-1.21.1` | 1.21 – 1.21.1 (one jar) | 0.21.x |

Exact Fabric API / MaLiLib / Mod Menu versions per target live in `versions/<target>/gradle.properties` and the matching `fabric.mod.json`. Java: 25 for 26.x targets, 21 for 1.21.x targets.

### Install & use

Requires Fabric Loader, Fabric API, and MaLiLib; Mod Menu is optional. Hold `Right Shift` in game to open the quick panel, then use the full config screen to pick entries and arrange groups. Diagnostics:

```text
/fastmasaconfig scan [csv] [fallback]
```

### Build from source

```bash
./gradlew build                              # default target 26.3
./gradlew -Ptarget=1.21.6-1.21.8 build       # any target = a versions/ directory name
./gradlew buildAll                           # build every target sequentially
./gradlew build_26.2 build_1.21.5            # or pick targets via build_<name> tasks
./gradlew -Ptarget=26.2 runClient            # dev client in run/<target>/
```

CI builds the whole matrix on every push to `main`; the matrix is derived automatically from the `versions/` directory listing, so adding or retiring a version target never touches CI. Releases are triggered by `mc<target>-v<version>` tags, which build the jar and publish it to GitHub Releases and Modrinth.

Note: the `1.21.4` target pins a MaLiLib commit that jitpack cannot build. CI builds it from source automatically; for local builds either keep a `malilib-*.jar` in the gitignored `libs/` folder or replicate the Maven Local alias from `.github/actions/build-malilib/action.yml`.

### Retiring a version target

To stop maintaining a target (for example the whole 1.21.x line someday), delete `versions/<target>/` — the CI matrix shrinks automatically on the next run. Update the support table above (or mark the row as unmaintained); already-published GitHub Releases and Modrinth files stay untouched. See [停止维护某个版本目标](#停止维护某个版本目标) for details.

### License & attribution

Licensed under `GPL-3.0-or-later`. Some floating-window visual behavior was adapted from public [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) implementations; those files retain attribution and GPL-3.0 notices. Independent project, not affiliated with or endorsed by Meteor Client. See [LICENSE](LICENSE).

---

## 中文

Fast Masa Config 的设计初衷很直接：Minecraft 中便于使用的按键数量有限，而不常用的快捷键也很容易遗忘。该 Mod 会扫描基于 MaLiLib 的 Mod（如 Litematica、MiniHUD、Tweakeroo 等）所公开的配置项，并将常用设置整理到悬浮快捷分组中。这样无需反复打开完整配置界面，即可快速调整开关、模式以及受支持的数值滑条。

一套代码为每个受支持的 Minecraft 版本分别构建 jar，目前覆盖 1.21 到 26.3。安装与自己游戏版本一致的文件，其 Modrinth 版本页会列出所需的 MaLiLib 和 Fabric API 版本。

### 截图说明

以下截图展示的是旧版用户界面，仅用于说明功能和交互方式；当前版本的界面布局与视觉样式可能有所不同。

![旧版快捷面板](https://cdn.modrinth.com/data/cached_images/f0539a0eb95e5877f503e5cf3af1f2d63f23362f.png)

![旧版快捷面板](https://cdn.modrinth.com/data/cached_images/2fcc46a9a05af551c4221e2f52e53923a2cb2aea.png)

![旧版配置界面](https://cdn.modrinth.com/data/cached_images/d4c389f366599a8d2ac70d5c2aa2738312a25590.png)

![旧版配置界面](https://cdn.modrinth.com/data/cached_images/089d26ffc01f85774d5e6c6c92c73d22a8ce5045.png)

### 主要功能

#### MaLiLib 配置扫描

- 自动发现已加载 Mod 注册的 MaLiLib 配置界面，并识别其中的配置分组、显示名称和配置 ID。
- 支持使用 `modId/groupId/configName` 或 `modId:configName` 标识快捷配置目标。
- 对已注册但分组结构不标准的配置界面提供反射扫描，方便兼容性排查。

#### 快捷面板

- 默认按住 `Right Shift` 打开快捷面板；按键可在完整配置界面中重新绑定。
- 面板以悬浮分组窗口显示，不会暂停游戏，并尽量保持移动键输入同步。
- 支持松开热键自动关闭，也支持按背包键或 `Esc` 关闭面板。
- 可调整悬浮菜单背景透明度。
- 悬浮菜单主题色可以在完整配置界面中自定义，默认使用暗色高对比度控件样式。
- 悬浮窗口支持拖拽、折叠和隐藏，窗口位置与状态会被保存。

#### 配置分组与快捷项

- 内置默认分组不可删除，但可以隐藏；用户分组支持新建、重命名、隐藏、删除和上下排序。
- 在“全部配置”页面按模组、配置分组、全部/已添加/未添加状态进行搜索和筛选。
- 可将配置项添加到指定分组、移除配置项并调整组内顺序。
- 默认分组保留完整配置入口，便于从快捷面板返回 MaLiLib 的原生配置界面。
- 布尔配置显示为开关；整数、浮点数和双精度数值显示为可展开的滑条。
- 数值项的展开状态会被保存；找不到的旧配置目标会被跳过，不影响其他快捷项。

#### 动态挖掘进度

- 开启后会关闭原版挖掘裂纹覆盖，改用一个随挖掘进度逐渐收缩的方块指示器。
- 指示器由完整连接的方块描边和半透明填充面组成，挖掘开始时尺寸最大，接近完成时收缩到最小尺寸。
- 描边与填充颜色会分别从“开始颜色”过渡到“完成颜色”，透明度由颜色设置控制，可呈现半透明效果。
- 描边、填充和多人挖掘进度可以独立开关；描边宽度也可以单独调整。
- 支持显示本地玩家的挖掘进度，以及多人游戏中由服务器同步的其他玩家挖掘进度。

#### 快捷消息与工具

- 可创建消息分组，在快捷面板中一键发送聊天消息或 `/` 指令；消息支持 `${player}`、`${x}`、`${y}`、`${z}`、`${px}`、`${py}`、`${pz}`、`${dimension}`、`${dimension_name}`、`${overworld_x}`、`${overworld_z}`、`${nether_x}` 和 `${nether_z}` 变量。
- 工具页提供实体渲染过滤。可选择黑名单模式隐藏列表中的实体，或开启白名单模式只渲染列表中的实体。
- 实体列表为可搜索的二级选择页，可按实体名称或命名空间 ID 搜索并点选，不需要手动输入 ID。
- 挖掘进度相关设置也集中在工具页，方便在游戏中快速调整。

#### 扫描与诊断

- 提供客户端命令扫描当前已加载的 MaLiLib 配置项。
- 支持将标准扫描和回退扫描结果导出为 CSV，便于报告兼容性问题或制作快捷配置清单。

### 支持环境

本仓库在单一 main 分支上同时维护多个 Minecraft 版本目标，每个目标产出一个独立 jar：

| 构建目标 | 覆盖的 Minecraft 版本 | MaLiLib |
|---|---|---|
| `26.3`（默认） | 26.3 | 0.30.x |
| `26.2` | 26.2 | 0.29.x |
| `26.1.2` | 26.1.2 | 0.28.x |
| `1.21.11` | 1.21.11 | 0.27.x |
| `1.21.9-1.21.10` | 1.21.9 ~ 1.21.10（单 jar 覆盖） | 0.26.x |
| `1.21.6-1.21.8` | 1.21.6 ~ 1.21.8（单 jar 覆盖） | 0.25.x |
| `1.21.5` | 1.21.5 | 0.24.x |
| `1.21.4` | 1.21.4 | 0.23.x |
| `1.21.2-1.21.3` | 1.21.2 ~ 1.21.3（单 jar 覆盖） | 0.22.x |
| `1.21-1.21.1` | 1.21 ~ 1.21.1（单 jar 覆盖） | 0.21.x |

各目标依赖的 Fabric API、MaLiLib、Mod Menu 和 mappings 版本，以 `versions/<目标>/gradle.properties` 与对应 `fabric.mod.json` 为准。Java 要求：26.x 目标为 25，1.21.x 目标为 21。

### 依赖

必需：

- Fabric Loader
- Fabric API
- MaLiLib

可选：

- Mod Menu：用于在 Mod Menu 中打开 Fast Masa Config 的完整配置界面。

兼容对象不是硬依赖。Fast Masa Config 会尝试扫描当前客户端中已经安装、并提供 MaLiLib 配置界面的 Mod，例如 Litematica、MiniHUD、Tweakeroo 等。

### 使用

默认按住 `Right Shift` 打开悬浮分组菜单。窗口不会暂停游戏；按住移动键后再打开菜单时，前进、后退、跳跃等按键会持续同步。

完整界面中有四个主要页签：

- `通用`：调整 Fast Masa Config 自身设置，例如快捷键、松开关闭和背包键关闭行为。
- `全部配置`：选择当前目标分组，浏览已扫描到的 MaLiLib 配置项，并将项目加入或移出该分组。
- `快捷消息`：管理消息分组和消息模板。发送时会解析动态变量：`${player}`/`${player_name}`（玩家名称）、`${x}` `${y}` `${z}`（方块坐标）、`${px}` `${py}` `${pz}`（精确坐标）、`${dimension}`/`${dimension_id}`（维度 ID）、`${world}`/`${dimension_name}`（维度简称）、`${overworld_x}` `${overworld_z}`（主世界坐标）和 `${nether_x}` `${nether_z}`（下界坐标），并支持 `ow_x`、`ow_z`、`nx`、`nz` 别名。未知变量会原样保留。
- `工具`：集中管理实体渲染过滤和动态挖掘进度。实体过滤关闭时不会影响任何实体的正常渲染。

### 分组与快捷操作

- 默认的 `Fast Masa Config` 分组不能删除，但可以隐藏；用户分组可创建、重命名、隐藏、删除和排序。
- 默认分组被隐藏后，任意可见窗口仍保留完整配置入口；全部隐藏时会显示恢复配置入口。
- 分组窗口通过标题栏拖拽，右侧按钮分别用于折叠和隐藏。窗口位置、折叠状态和已展开的数值项会保存。
- 布尔项的强调色表示 `true`；数值项点击行或箭头即可展开滑条，步长固定为 `1`。
- 在“通用”设置中可以启用动态挖掘进度显示，并分别调整描边、填充、远程进度和描边宽度；颜色项可直接打开 HSV 编辑器。

### 命令

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

### 兼容性说明

Fast Masa Config 主要依赖 MaLiLib 配置界面暴露出来的信息。大多数使用标准 MaLiLib 配置界面的 Mod 可以被扫描到，但不同 Mod 的配置界面实现方式不完全一致，分组名、显示名或部分配置项可能无法稳定识别。

当前主要支持以下配置类型：

- 布尔值：显示为开关。
- 整数、浮点数和双精度数值：显示为滑条。
- 颜色：在完整配置界面中显示色块并支持 HSV 编辑。

字符串、选项列表和复杂热键配置暂不作为快捷面板的主要操作目标；颜色配置目前仅在完整配置界面中编辑。

### 本地开发

仓库采用"单仓多版本"结构：`src/` 是公共基线，永远面向最新版本；每个 Minecraft 版本目标在 `versions/<目标>/` 下，用覆盖文件表达版本差异。

#### 环境要求

- JDK 25（默认目标 26.3 使用；Gradle 工具链自动探测，无需设置 `JAVA_HOME`）。
- 构建 1.21.x 目标时需要本机有 JDK 21 工具链。
- Windows / Linux / macOS 均可，使用仓库自带的 Gradle wrapper。

#### 构建指定版本

可用目标就是 `versions/` 下的目录名：

```bash
./gradlew -Ptarget=1.21.6-1.21.8 build     # 构建指定目标（含测试）
./gradlew build                             # 不带参数 = 默认目标 26.3
./gradlew -Ptarget=1.21-1.21.1 test         # 只跑指定目标的测试
./gradlew -Ptarget=26.2 compileClientJava   # 只编译指定目标的客户端代码
```

jar 统一输出到 `build/libs/`，文件名自带目标版本号（例如 `fast-masa-config-1.21.6-1.21.8-5.3.0.jar`），多个目标的产物可以共存。

`1.21.4` 目标的 MaLiLib 固定在 jitpack 无法构建的上游提交上。CI 会自动从源码构建并发布到 Maven Local；本地构建时要么在 gitignore 的 `libs/` 目录放一个 `malilib-*.jar`，要么按 `.github/actions/build-malilib/action.yml` 里的步骤制作 `~/.m2` 别名。

#### 构建全部目标

`buildAll` 任务会串行构建 `versions/` 下的全部目标（每个目标一个独立的 Gradle 子进程，含测试）：

```bash
./gradlew buildAll
```

- 顺序由 `versions/` 目录决定，某个目标失败后停止后续构建。
- 只想构建其中几个目标时，直接点名对应任务：`./gradlew build_26.2 build_1.21.5`。
- 可用任务用 `./gradlew tasks --group MultiVersion` 查看。
- CI 不走 `buildAll`，而是用 `versions/` 目录生成的构建矩阵并行构建，二者等价。

#### 运行开发客户端

```bash
./gradlew -Ptarget=26.3 runClient
```

每个目标使用独立的运行目录 `run/<目标>/`，配置和存档互不干扰。首次以某目标运行会自动下载对应的 Minecraft、mappings 和依赖。

#### 新增一个版本目标

1. 复制最接近的 `versions/<目标>/` 目录并改名；可共用的版本线用区间命名（如 `1.21.9-1.21.10`）。
2. 修改 `gradle.properties`：依赖版本、`loom_pipeline`（26.x 为 `standard`，1.21.x 为 `remap`）、`java_release`、`mod_version`、`game_versions`。
3. 修改目标覆盖目录里的 `fabric.mod.json` 依赖区间，使其覆盖整个组。
4. 该版本 API 有差异时，把差异文件按相同路径放进 `versions/<目标>/src/` 覆盖公共代码；该目标不存在的公共类登记到 `common-excluded.txt`。

新目录下次 CI 运行时会自动进入构建矩阵。

#### 停止维护某个版本目标

想停更某个版本（例如将来整条 1.21.x 线）时：

1. `git rm -r versions/<目标>`：CI 构建矩阵由 `versions/` 目录自动生成，删除目录后下次运行矩阵自动缩小，不需要改任何 CI 文件。
2. 更新 README 和 Modrinth 页面的支持表格；也可以不删行，而是把该行标注为"停止维护"，并说明已有版本仍可下载。
3. 已发布的 GitHub Release 和 Modrinth 文件不受影响，老版本玩家仍然可以下载使用。
4. 整条版本线退役（如 1.21.x 全线）：对线内每个目标目录执行第 1 步即可；建议先给该线发一个收尾版本（打 `mc<目标>-v<版本>` 标签）再删除。
5. 以后想恢复某个目标：从 git 历史 `git checkout <commit> -- versions/<目标>` 恢复目录即可。

#### 主要配置文件

- `gradle.properties`：全局构建参数和 Loom 版本（所有目标共用一个 Loom）。
- `versions/<目标>/gradle.properties`：该目标的 Minecraft、依赖、管线和 Java 版本。
- `versions/<目标>/src/`：版本覆盖源码与资源（含 `fabric.mod.json`）。
- `src/main/resources/fabric.mod.json`：默认目标的 Mod 元数据和依赖范围。

更新日志见 [MODRINTH.md](MODRINTH.md)。

### 项目结构

```text
src/main/java/fastui/yure/config/       通用配置、快捷项存储和配置编辑逻辑
src/client/java/fastui/yure/client/     Fabric 客户端入口、扫描、输入和 GUI
src/main/resources/                     fabric.mod.json、图标和语言文件
src/test/java/                          单元测试（无 Minecraft 依赖，随每个目标构建运行）
versions/<目标>/                        版本目标：gradle.properties、覆盖源码、common-excluded.txt
```

客户端代码使用 Loom 的 split environment source sets，Minecraft 客户端相关代码放在 `src/client/java`，通用配置和数据结构放在 `src/main/java`。构建 1.21.x 目标时走 yarn/intermediary 重映射管线（`fabric-loom-remap` 插件），26.x 目标走新的 MojMap 管线，由目标属性自动选择。

### 许可证

本项目使用 `GPL-3.0-or-later` 许可证。悬浮窗口的部分视觉行为参考了 [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) 的公开实现，相关源码文件保留了来源和 GPL-3.0 许可证说明。

Fast Masa Config 是独立项目，与 Meteor Client 及其开发团队没有官方隶属或背书关系。修改版发布时需要保留作者和许可证声明，并按 GPL 要求提供对应源码；本项目按原样提供，不包含任何担保。详见 [LICENSE](LICENSE)。
