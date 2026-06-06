# Quick Config Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add MaLiLib-backed config for this mod and a hold-to-open central quick config overlay.

**Architecture:** Keep config registration, input handling, and UI in separate client packages. Use MaLiLib `ConfigHotkey`, `IKeybindProvider`, config handler registration, and a non-pausing Minecraft `Screen` that closes when the configured hotkey is released.

**Tech Stack:** Java 21, Fabric client entrypoint, MaLiLib config/hotkey APIs, Minecraft client Screen API, JUnit Jupiter.

---

## File Structure

- `src/client/java/fastui/yure/client/FastMasaConfigClient.java`: Client entrypoint, delegates registration.
- `src/client/java/fastui/yure/client/config/FastMasaConfigs.java`: Defines this mod's config options.
- `src/client/java/fastui/yure/client/config/FastMasaConfigHandler.java`: Loads/saves config through MaLiLib config handler APIs.
- `src/client/java/fastui/yure/client/input/FastMasaInputHandler.java`: Registers hotkeys and opens the overlay on key action.
- `src/client/java/fastui/yure/client/gui/QuickConfigScreen.java`: Non-pausing hold-to-close screen.
- `src/client/java/fastui/yure/client/gui/QuickConfigPanel.java`: Central panel drawing and click behavior.
- `src/test/java/fastui/yure/config/MasaConfigEditorTest.java`: Existing editor tests remain.

## Tasks

### Task 1: Implement MaLiLib Configs

- [ ] Create `FastMasaConfigs` with `ConfigHotkey`, `ConfigInteger`, `ConfigDouble`, and `ConfigBoolean` entries.
- [ ] Create `FastMasaConfigHandler` that implements MaLiLib `IConfigHandler` and reads/writes config groups.
- [ ] Register handler and config screen factory from the client entrypoint.

### Task 2: Implement Hotkey Input

- [ ] Create `FastMasaInputHandler` implementing `IKeybindProvider`.
- [ ] Register the quick overlay hotkey in MaLiLib keybind manager.
- [ ] Set a hotkey callback to open `QuickConfigScreen` on press.

### Task 3: Implement Overlay UI

- [ ] Create `QuickConfigScreen` with `shouldPause()` returning `false`.
- [ ] Close the screen while ticking if `releaseToClose` is enabled and the configured keybind is no longer held.
- [ ] Keep movement keys available by not treating movement key events as UI-only actions.
- [ ] Create `QuickConfigPanel` for drawing the central sample panel and buttons.

### Task 4: Verify

- [ ] Run `./gradlew.bat test`.
- [ ] Run `./gradlew.bat build`.
- [ ] Manually verify in game: hold hotkey opens overlay, mouse is available, view stops rotating, movement continues, release closes overlay.
