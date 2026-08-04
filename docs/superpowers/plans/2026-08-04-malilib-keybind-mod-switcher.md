# MaLiLib Keybind and Mod Switcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 1.21.1 配置页接入 MaLiLib 0.21.10 的真实热键捕获、热键设置和模组切换下拉控件。

**Architecture:** `FastMasaConfigGui` 改为继承 `GuiBase` 并实现 `IKeybindConfigGui`。保留现有三页的索引、筛选、快捷项和自绘行内容，仅把搜索/手工输入/按钮/热键/下拉组件迁移到 MaLiLib 的控件集合与生命周期；配置变更统一通过 `ConfigManager` 和 `updateUsedKeys()` 通知。

**Tech Stack:** Minecraft 1.21.1, Yarn 1.21.1, Java 21, MaLiLib 1.21-0.21.10, Fabric Loom。

---

### Task 1: Migrate FastMasaConfigGui lifecycle and native controls

**Files:**
- Modify: `src/client/java/fastui/yure/client/gui/FastMasaConfigGui.java`

- [ ] Replace `Screen` inheritance with `GuiBase` and implement `IKeybindConfigGui`, mapping `init`, rendering, mouse, keyboard, and close handling to the MaLiLib 0.21.10 lifecycle.
- [ ] Replace vanilla text fields/buttons with `GuiTextFieldGeneric`, `ButtonGeneric`, `ConfigButtonKeybind`, and `GuiKeybindSettings`; keep the existing tab/search/filter/action state and row drawing.
- [ ] Add `WidgetDropDownList<ModInfo>` from `Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()`, select the current registered mod, and switch screens through each `ModInfo` supplier.
- [ ] Route active keybind selection through `setActiveKeybindButton`, notify listeners after capture, call `InputEventHandler.getKeybindManager().updateUsedKeys()`, and preserve the existing held-key suppression behavior.
- [ ] Run `./gradlew.bat test` and `./gradlew.bat build`; fix only 1.21.1 compile/runtime incompatibilities.

### Task 2: Verify runtime loading and deliver

**Files:**
- Modify: `docs/superpowers/plans/2026-08-04-malilib-keybind-mod-switcher.md`

- [ ] Run `./gradlew.bat runClient` with the existing isolated 1.21.1 run directory and inspect logs for Minecraft 1.21.1, MaLiLib 0.21.10, and Fast Masa Config loading.
- [ ] Inspect `git diff` and `git status`, stage only the implementation and this plan, and commit with `feat: use malilib keybind and mod switcher`.
- [ ] Confirm no push is performed and record the commit SHA plus any runtime verification limitation.
