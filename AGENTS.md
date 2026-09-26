# Repository Instructions

## Toolchain

- This is a single-module Fabric Loom project with multi-version targets; use the Gradle wrapper (`gradlew`/`gradlew.bat`), not a system Gradle installation.
- One Loom release (`loom_version` in root `gradle.properties`) builds every target; the two plugin ids (`net.fabricmc.fabric-loom` for 26.x, `net.fabricmc.fabric-loom-remap` for 1.21.x yarn/intermediary) are conditionally applied per target and must not be mixed within one target.
- Java release is per target (`java_release`): 25 for 26.x, 21 for 1.21.x. No per-shell JDK environment variables are needed: the machine-scope Temurin install at `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot` is auto-detected by Gradle toolchains (Windows Registry), and the Microsoft JDK 21 install is detected via `JAVA_HOME`. The Gradle daemon may run on 21 while a Java 25 toolchain compiles and runs.
- If a toolchain is ever not found, register it in the user-level `%USERPROFILE%\.gradle\gradle.properties` via `org.gradle.java.installations.paths=...`.
- Loom configuration cache is intentionally disabled in `gradle.properties` because of the Fabric Loom/IntelliJ compatibility issue; do not enable it casually.

## Source Layout

- Single main branch hosts all Minecraft versions. `src/` is the common baseline and always targets the newest game version; new features land here first, then get ported to older targets.
- `versions/<target>/gradle.properties` declares one build target (minecraft/loader/fabric api/malilib/mod versions, `loom_pipeline`, `java_release`, `game_versions`). Build it with `./gradlew -Ptarget=<target> ...`; no `-Ptarget` means `26.3`.
- Target folders that ship one jar across a compatible line use range names matching the shared group (`1.21-1.21.1`, `1.21.6-1.21.8`); their `fabric.mod.json` declares the full depends range (`">=1.21.6 <=1.21.8"`). 26.x targets are single-version (`26.2`, `26.3`) because the API churns every release. `game_versions` lists the Modrinth game versions a release covers; release tags follow `mc<target>-v<feature>`, e.g. `mc1.21.6-1.21.8-v5.4.0`.
- Version-specific API surface (MaLiLib signatures, key-code names, screen access, render hooks) is reached only through `fastui.yure.client.compat.TargetCompat`; each target overrides that class when its MaLiLib/vanilla line differs. Extend TargetCompat instead of duplicating whole GUI files per target.
- To add a new version target: copy the nearest `versions/<target>/`, update its properties (`loom_pipeline`, `java_release`, deps, `mod_version`, `game_versions`) and `fabric.mod.json` depends range, then add same-path override files in its `src/` only where the API differs. The step-by-step recipe is in README 本地开发.
- `versions/<target>/src/{main,client,test}/...` holds version-specific overrides: a file there replaces the same-path file in `src/`. `versions/<target>/common-excluded.txt` lists `src/`-relative paths of common files that do not exist for that target's feature generation. Overrides shrink as common code is made version-agnostic — prefer deleting an override over editing both copies.
- `src/main/java` contains environment-independent configuration models, stores, and MaLiLib config editing.
- `src/client/java` contains all Minecraft client entrypoints, scanning, input handling, and custom GUI code; do not move client-only imports into `src/main`.
- `src/main/resources` contains `fabric.mod.json`, translations, and assets. Keep new visible UI strings in both `zh_cn.json` and `en_us.json`.
- `src/test/java` contains JUnit 5 tests. Keep layout, hit-testing, stores, migration, and other logic with no Minecraft dependency in pure unit tests where possible.

## Commands

- Full tests: `./gradlew test` or Windows `./gradlew.bat test` (default target 26.3).
- Another version target: prefix any task with `-Ptarget=<target>`, e.g. `./gradlew -Ptarget=1.21.6-1.21.8 build`; valid targets are the directory names under `versions/` (no flag = `26.3`). Dev clients run in `run/<target>/`; jars accumulate in `build/libs/` with the target version in the filename.
- Focused test: `./gradlew test --tests fully.qualified.TestClass`.
- Client compilation: `./gradlew compileClientJava`.
- Build every target from a shell (no helper script): PowerShell `gci versions -Directory | % { ./gradlew.bat "-Ptarget=$($_.Name)" build }`; bash `for t in versions/*/; do ./gradlew "-Ptarget=$(basename $t)" build; done`. Jars accumulate in `build/libs/` (filenames embed the target version, so nothing clobbers).
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
