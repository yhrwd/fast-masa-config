# Repository Instructions

## Toolchain

- This is a single-module Fabric Loom project; use the Gradle wrapper (`gradlew`/`gradlew.bat`), not a system Gradle installation.
- Java 25 is required by `build.gradle`, `fabric.mod.json`, and CI. The README's older Java 21 note is stale.
- On Windows PowerShell, set the repository's required JDK before Gradle commands when Java 25 is not already active:
  ```powershell
  $env:JAVA_HOME="C:\Users\Yhrza\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
  $env:JDK25_HOME="C:\Users\Yhrza\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
  ```
- Loom configuration cache is intentionally disabled in `gradle.properties` because of the Fabric Loom/IntelliJ compatibility issue; do not enable it casually.

## Source Layout

- `src/main/java` contains environment-independent configuration models, stores, and MaLiLib config editing.
- `src/client/java` contains all Minecraft client entrypoints, scanning, input handling, and custom GUI code; do not move client-only imports into `src/main`.
- `src/main/resources` contains `fabric.mod.json`, translations, and assets. Keep new visible UI strings in both `zh_cn.json` and `en_us.json`.
- `src/test/java` contains JUnit 5 tests. Keep layout, hit-testing, stores, migration, and other logic with no Minecraft dependency in pure unit tests where possible.

## Commands

- Full tests: `./gradlew test` or Windows `./gradlew.bat test`.
- Focused test: `./gradlew test --tests fully.qualified.TestClass`.
- Client compilation: `./gradlew compileClientJava`.
- CI-equivalent verification: `./gradlew build --no-daemon`.
- Release tags are `v*` or `mc*-v*`; CI builds the jar and excludes `*-sources.jar` and `*-dev.jar` from the GitHub release.

## Architecture Constraints

- MaLiLib config targets are resolved from `ConfigIndexService`; preserve the `modId/groupId/configName` target fields and use `ShortcutControl`/`MasaConfigEditor` to change external mod values so their own config files and notifications stay synchronized.
- GUI drawing and hit testing must use the same computed rectangles. Keep pure geometry separate from `GuiContext`/`RenderUtils` so it can be unit tested.
- The quick panel supports boolean controls and integer/float/double sliders. Do not silently add unsupported string, color, enum, or complex hotkey controls to the runtime panel.
- Group and shortcut configuration is persisted by `FastMasaConfigHandler` in `fast-masa-config.json`. Group state is in `Groups`; existing `Shortcuts` data is migrated for old files and must not be deleted manually or discarded during upgrades.
- Persist group window positions, collapsed state, and expanded rows only when state changes; never write configuration from a per-frame render path.

## Verification

- After GUI or input changes, run `./gradlew test compileClientJava`; after config or build changes, run `./gradlew test build`.
- Before claiming a GUI change is complete, manually check in a dev client: group selection, add/remove/reorder, floating-window drag/close/collapse, numeric-row expansion, slider dragging, reload persistence, and stale target handling.

## Licensing

- The repository is `GPL-3.0-or-later`. Preserve the license and copyright notices when modifying covered code. Do not copy external GUI source without recording its provenance and satisfying its license obligations.
