# Shortcut Grid UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a compact hold-to-open shortcut grid for boolean toggles and numeric sliders, with a full-screen UI for managing shortcuts and browsing all configs.

**Architecture:** Keep input fixes, shortcut persistence, config indexing, and rendering separated. Quick overlay only edits existing shortcut entries; full UI owns shortcut add/remove/manual ID management and all-config browsing.

**Tech Stack:** Java 21, Fabric client, MaLiLib config/hotkey APIs, Minecraft Screen API, Sponge Mixin accessor, JUnit Jupiter.

---

## File Structure

- `src/main/java/fastui/yure/config/ShortcutEntry.java`: persisted shortcut record.
- `src/main/java/fastui/yure/config/ShortcutConfigStore.java`: in-memory shortcut list and JSON conversion helpers.
- `src/main/java/fastui/yure/config/FastMasaConfigHandler.java`: read/write shortcuts with existing config.
- `src/client/java/fastui/yure/client/mixin/KeyBindingAccessor.java`: accessor for bound key.
- `src/client/java/fastui/yure/client/input/BoundKeyReader.java`: reads vanilla key binding codes through accessor.
- `src/client/java/fastui/yure/client/input/FastMasaInputHandler.java`: opens quick screen with INGAME hotkey.
- `src/client/java/fastui/yure/client/gui/QuickConfigScreen.java`: uses GLFW physical keys for release close.
- `src/client/java/fastui/yure/client/gui/QuickConfigPanel.java`: compact sci-fi shortcut grid.
- `src/client/java/fastui/yure/client/gui/FastMasaConfigGui.java`: full UI tabs for Shortcuts and All Configs.
- `src/client/java/fastui/yure/client/index/ConfigIndex.java`: indexed config reference records.
- `src/client/java/fastui/yure/client/index/ConfigIndexService.java`: scans MaLiLib config screens.
- `src/client/java/fastui/yure/client/shortcut/ShortcutResolver.java`: resolves shortcut entries to configs.
- `src/client/java/fastui/yure/client/shortcut/ShortcutControl.java`: toggle/slider render metadata and edit behavior.
- `src/main/resources/assets/fast-masa-config/lang/*.json`: translations.

## Tasks

### Task 1: Input and Cleanup

- [ ] Delete template mixins and replace with a real `KeyBindingAccessor` client mixin.
- [ ] Add client mixin config to `fabric.mod.json`.
- [ ] Change quick hotkey default settings to `INGAME`.
- [ ] Close quick screen by checking GLFW physical state of the configured hotkey keys, not MaLiLib held state.
- [ ] Remove template hello log from `FastMasaConfig`.

### Task 2: Shortcut Model

- [ ] Add `ShortcutEntry` and `ShortcutConfigStore`.
- [ ] Persist shortcuts in `fast-masa-config.json` under `Shortcuts`.
- [ ] Add tests for manual ID parsing and JSON roundtrip.

### Task 3: Config Index and Supported Controls

- [ ] Build config index from registered MaLiLib config screens.
- [ ] Support only boolean toggles and integer/float/double sliders.
- [ ] Resolve shortcut IDs to indexed config refs.
- [ ] Commit changes through `MasaConfigEditor` and `ConfigManager.onConfigsChanged(modId)`.

### Task 4: Full UI Management

- [ ] Replace simple MaLiLib config screen with custom full UI.
- [ ] Add tabs: `Shortcuts` and `All Configs`.
- [ ] Shortcuts tab shows grid/list, supports remove and manual ID add.
- [ ] All Configs tab shows `Mod > Group > Config`, only supported configs have `+`.

### Task 5: Quick Grid UI

- [ ] Quick overlay renders configured shortcuts as compact grid cells.
- [ ] Boolean cell toggles on click.
- [ ] Numeric cell behaves as slider drag with configured step.
- [ ] Empty shortcut state tells user to configure shortcuts in full UI.

### Task 6: Verify

- [ ] Run `./gradlew.bat test`.
- [ ] Run `./gradlew.bat build`.
- [ ] Manual in-game checks: INGAME hotkey opens once, no flicker, release closes, movement continues, ModMenu opens full UI, shortcuts modify values and save.
