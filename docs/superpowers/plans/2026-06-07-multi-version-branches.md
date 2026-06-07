# Multi-Version Branches Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用独立 Git 分支分别维护并构建 `1.21.7` 和 `1.21.11` 的 Fabric mod 产物。

**Architecture:** 不引入 Stonecutter 或额外 Gradle 插件，先用一条公共源码基线承载通用修复，再从同一个提交切出 `mc/1.21.7` 与 `mc/1.21.11`。版本分支只维护 Minecraft、Yarn、Fabric API、MaLiLib、Mod Menu 和 `fabric.mod.json` 依赖范围。

**Tech Stack:** Gradle、Fabric Loom、Fabric API、Yarn mappings、Sakura MaLiLib JitPack、Mod Menu、JUnit 5。

---

## Version Matrix

| Branch | Minecraft | Yarn | Fabric API | MaLiLib | Mod Menu | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `mc/1.21.7` | `1.21.7` | `1.21.7+build.8` | `0.129.0+1.21.7` | `1.21.7-0.25.2-sakura.3` | `15.0.0-beta.3` | 当前项目基线 |
| `mc/1.21.11` | `1.21.11` | `1.21.11+build.6` | `0.141.4+1.21.11` | `1.21.11-0.27.12` | `17.0.0` | 需要改用 JitPack MaLiLib 依赖 |

## File Structure

- `src/client/java/fastui/yure/client/gui/QuickCornerArrow.java`: 公共箭头几何修复，两条版本分支都保留。
- `src/client/java/fastui/yure/client/gui/QuickConfigPanel.java`: 公共渲染修复，两条版本分支都保留。
- `src/test/java/fastui/yure/client/gui/QuickCornerArrowTest.java`: 公共回归测试，两条版本分支都保留。
- `gradle.properties`: 每条版本分支维护自己的版本矩阵。
- `build.gradle`: `mc/1.21.11` 分支把 MaLiLib 从本地 `flatDir` jar 改为 JitPack 坐标。
- `src/main/resources/fabric.mod.json`: 每条版本分支维护自己的 `minecraft` 和 `malilib` 依赖范围。

## Branch Policy

- 公共 UI/逻辑修复先落在当前工作分支，验证后再带入两个版本分支。
- `mc/1.21.7` 不主动升级依赖，避免破坏当前可运行基线。
- `mc/1.21.11` 只做版本迁移相关改动，编译错误只在该分支修。
- 构建产物都在 `build/libs/`，发布时按分支重命名或在对应分支设置 archive name，避免两个版本都叫 `fast-masa-config-1.0.0.jar`。
- 当前会话不创建分支、不提交；执行时先让用户确认是否可以创建分支和提交。

### Task 1: Finish Shared Arrow Fix

**Files:**
- Modify: `src/client/java/fastui/yure/client/gui/QuickConfigPanel.java`
- Create: `src/client/java/fastui/yure/client/gui/QuickCornerArrow.java`
- Test: `src/test/java/fastui/yure/client/gui/QuickCornerArrowTest.java`

- [ ] **Step 1: Verify the targeted regression test**

Run: `./gradlew.bat test --tests fastui.yure.client.gui.QuickCornerArrowTest`

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 2: Verify the full test suite**

Run: `./gradlew.bat test`

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: Check worktree before branching**

Run: `git status --short`

Expected: only the arrow fix and plan files are changed. If unrelated files appear, leave them untouched and ask the user before staging anything.

### Task 2: Create the 1.21.7 Branch

**Files:**
- No file edits expected.

- [ ] **Step 1: Create or switch to the branch**

Run: `git switch -c mc/1.21.7`

Expected: branch `mc/1.21.7` exists and contains the shared arrow fix.

- [ ] **Step 2: Build current baseline**

Run: `./gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL` and `build/libs/fast-masa-config-1.0.0.jar` exists.

- [ ] **Step 3: Record artifact path**

Use: `build/libs/fast-masa-config-1.0.0.jar`

Recommended release name: `fast-masa-config-fabric-1.21.7-1.0.0.jar`.

### Task 3: Create the 1.21.11 Branch

**Files:**
- Modify: `gradle.properties`
- Modify: `build.gradle`
- Modify: `src/main/resources/fabric.mod.json`

- [ ] **Step 1: Return to the shared source point**

Run: `git switch -`

Expected: the current branch is the shared source branch that contains the arrow fix.

- [ ] **Step 2: Create the 1.21.11 branch**

Run: `git switch -c mc/1.21.11`

Expected: branch `mc/1.21.11` exists and contains the shared arrow fix.

- [ ] **Step 3: Update Gradle version properties**

Edit `gradle.properties` to use these values:

```properties
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.6
loader_version=0.19.3
loom_version=1.16-SNAPSHOT
fabric_api_version=0.141.4+1.21.11
malilib_version=1.21.11-0.27.12
mod_menu_version=17.0.0
```

- [ ] **Step 4: Use remote MaLiLib on the high-version branch**

In `build.gradle`, replace the local jar dependency:

```gradle
modImplementation name: "malilib-fabric-${project.malilib_version}"
```

with the JitPack dependency:

```gradle
modImplementation "com.github.sakura-ryoko:malilib:${project.malilib_version}"
```

Keep `maven { url = 'https://jitpack.io' }` in `repositories`.

- [ ] **Step 5: Update Fabric dependency constraints**

Edit `src/main/resources/fabric.mod.json` dependencies to:

```json
"fabricloader": ">=0.19.3",
"minecraft": "~1.21.11",
"java": ">=21",
"fabric-api": "*",
"malilib": ">=0.27.0- <0.28.0-"
```

- [ ] **Step 6: Run compile first**

Run: `./gradlew.bat compileJava compileClientJava`

Expected: `BUILD SUCCESSFUL` or concrete Java API errors from MaLiLib/Minecraft mapping changes.

- [ ] **Step 7: Fix compile errors only if they are version-specific**

If compile fails, edit only files referenced by the compiler errors. Do not refactor unrelated UI code. Re-run `./gradlew.bat compileJava compileClientJava` after each small fix.

- [ ] **Step 8: Build high-version artifact**

Run: `./gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL` and `build/libs/fast-masa-config-1.0.0.jar` exists.

- [ ] **Step 9: Record artifact path**

Use: `build/libs/fast-masa-config-1.0.0.jar`

Recommended release name: `fast-masa-config-fabric-1.21.11-1.0.0.jar`.

### Task 4: Optional Artifact Naming Cleanup

**Files:**
- Modify: `build.gradle`

- [ ] **Step 1: Add versioned archive names if duplicate filenames become annoying**

Add this near `version = project.mod_version` and `group = project.maven_group`:

```gradle
base {
    archivesName = "${project.name}-fabric-${project.minecraft_version}"
}
```

- [ ] **Step 2: Build and confirm names**

Run: `./gradlew.bat clean build`

Expected for `mc/1.21.7`: `build/libs/fast-masa-config-fabric-1.21.7-1.0.0.jar`.

Expected for `mc/1.21.11`: `build/libs/fast-masa-config-fabric-1.21.11-1.0.0.jar`.

### Task 5: Maintenance Workflow

**Files:**
- No file edits expected.

- [ ] **Step 1: Put shared fixes on both branches**

After a shared fix is validated, apply it to each version branch with `git cherry-pick <commit>` or by repeating the same patch manually if the user does not want commits.

- [ ] **Step 2: Build each branch before release**

Run on `mc/1.21.7`: `./gradlew.bat clean build`.

Run on `mc/1.21.11`: `./gradlew.bat clean build`.

Expected: both branches produce one jar in `build/libs/`.

- [ ] **Step 3: Keep version bumps branch-local**

Only change `gradle.properties`, `build.gradle`, and `fabric.mod.json` for version bumps unless compiler errors prove source changes are required.

## Self-Review

- Spec coverage: covers branch split, `1.21.7`, `1.21.11`, dependency versions, artifact paths, and verification.
- Placeholder scan: no `TBD` or undefined tasks remain.
- Type consistency: file paths and Gradle property names match the current project.
