# Main UI Backport to Minecraft 1.21.1

## Goal

在 `mc/1.21.1` 分支中复刻 `main` 的新配置界面与快捷面板功能，同时保持 Minecraft 1.21.1、Java 21 和 MaLiLib 0.21.10 的兼容性。

## Scope

全屏配置界面提供 Generic、Shortcuts、All Configs 三个页面。界面支持搜索、mod/group 筛选、快捷项添加/移除/排序、手工快捷项 ID 输入、布尔值切换、数值编辑、热键入口和配置重置。

快捷面板提供 Quick 与 Enabled 两种模式。Quick 显示持久化快捷项，Enabled 显示当前为 true 的可扫描布尔配置。面板支持模式切换、滑条拖动、移动键透传、松开热键关闭和再次触发热键关闭。

All Configs 的数据来源覆盖 MaLiLib Registry 与 ModMenu 配置入口。复杂配置页中的 enum、嵌套对象、索引列表和 getter/setter 分组通过受控扫描识别，扫描过程中恢复临时变更的状态。

## Architecture

`FastMasaConfigGui` 在 1.21.1 分支中使用自绘 `Screen` 承载界面状态和输入，不复制 26.1.2 的 `GuiGraphicsExtractor`、`KeyEvent` 或新版 MaLiLib GUI API。页面状态、筛选条件和可见行由独立的索引与扫描服务提供，界面只负责渲染和用户操作。

扫描链路为：`ConfigScreenSourceService` 获取模组配置来源，`ConfigGuiGroupScanner` 解析分组，`ConfigIndexService` 缓存并筛选结果，`ShortcutResolver` 将持久化快捷项解析为面板可操作项。配置更新继续写入现有 `ShortcutConfigStore` 和 MaLiLib 配置处理器。

## Compatibility Rules

- 不修改 Minecraft、Fabric Loader、Fabric API、MaLiLib 或 Java 版本。
- 不引入 26.1.2 专用类或新版 MaLiLib GUI 接口。
- 渲染使用 1.21.1 的 `DrawContext`，输入使用现有 `Screen` 回调。
- 只迁移 `main` 中能在 1.21.1 API 上等价实现的行为；无法等价的视觉细节降级为同信息层级的控件。

## Verification

1. 为筛选、扫描、命中测试、布局和快捷项解析补充或迁移单元测试。
2. 执行 `./gradlew.bat test` 和 `./gradlew.bat build`。
3. 使用 `./gradlew.bat runClient` 在 `run/version/1.21` 启动，确认 1.21.1、MaLiLib 和 Fast Masa Config 加载。
4. 手动验证三页切换、搜索筛选、快捷项维护、Quick/Enabled 面板模式、滑条、移动键透传与关闭行为。
