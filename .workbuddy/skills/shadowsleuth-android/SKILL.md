---
name: shadowsleuth-android
description: Evolve the ShadowSleuth Android app (Kotlin + Jetpack Compose + MediaStore) by implementing user feedback, updating architecture docs, and releasing a new debug APK with a GitHub tag.
agent_created: true
---

# ShadowSleuth Android 项目迭代

## 概述

本技能用于在 ShadowSleuth 项目中按用户反馈进行版本迭代。典型任务包括：
- 修改 Jetpack Compose UI（主题、导航、页面、弹窗、滚动条）
- 调整 MediaStore 扫描与重复图片匹配逻辑（文件名、大小、dHash）
- 优化 Android 权限模型（读取、删除、MANAGE_EXTERNAL_STORAGE）
- dHash 缓存管理与 UI 展示（容量、创建时间）
- 更新项目文档（README、architecture、requirements、skills、project-experience）
- 构建 Debug APK 并推送到 GitHub 仓库与 Release

## 触发条件

- 用户要求修改 ShadowSleuth 的功能、UI 或权限。
- 用户要求更新项目文档并推送到 GitHub。
- 用户要求构建新版本 APK 并创建 GitHub tag / Release。

## 工作流

### 1. 理解当前状态

1. 读取项目根目录与 `app/src/main/java/com/shadowsleuth/app` 下相关源码。
2. 读取 `README.md`、`docs/architecture.md`、`docs/requirements.md` 等关键文档。
3. 读取 `.workbuddy/memory/MEMORY.md` 了解当前版本与关键约定。
4. 明确用户本次修改的范围与目标版本（通常为 `v1.2.X-debug`）。

### 2. 功能实现

1. 按需求修改 Kotlin 源码与 `res/values/strings.xml`。
2. 如需新依赖，更新 `app/build.gradle.kts`。
3. 如需新权限，更新 `app/src/main/AndroidManifest.xml`。
4. 保持 MVVM-like 架构：状态放 `ViewModel`，UI 只负责展示与事件转发。
5. 涉及权限时，优先在 `MainActivity` 或 `ScanViewModel` 中处理，并在界面提供引导。
6. dHash 相关功能：
   - `DHashCalculator.kt`：差分哈希，图片缩至 9×8，输出 64-bit Long，汉明距离 ≤ 10 为相似
   - `DuplicateGroup.MatchType.DHASH`：第三种匹配类型，`dHashValue` 字段存储哈希值
   - `ScanViewModel.dHashCache`：全局 id→dHash 缓存，删除图片时同步清除
   - `startDHashScan()`：全量 dHash 扫描，结果追加到 `ScanState.Complete`
   - `searchSampleByDHash(sample)`：单图 dHash 相似搜索，**优先从缓存读取**，避免重复计算

### 3. 文档同步

每次版本变更必须同步更新以下文档：
- `README.md`：项目简介、核心功能、快速开始、更新日志。
- `docs/architecture.md`：模块划分、数据流、匹配规则、权限模型、主题系统。
- `docs/requirements.md`：功能需求、非功能需求、明确不做。
- `docs/index.md`：文档导航。
- `docs/skills.md`：沉淀可复用技能。
- `docs/project-experience.md`：记录版本迭代、关键决策、踩坑记录。

### 4. 构建 APK

1. 更新 `app/build.gradle.kts` 中的 `versionCode`（递增）和 `versionName`。
2. 执行 `./gradlew assembleDebug`（Windows 使用 `gradlew.bat`）。
3. 构建环境：
   ```bash
   export JAVA_HOME="C:/Users/Fox/.workbuddy/android-tools/jdk-17.0.19+10"
   export ANDROID_HOME="C:/Users/Fox/.workbuddy/android-tools"
   export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
   export GRADLE_USER_HOME="C:/Users/Fox/.workbuddy/.gradle"
   cd /d/workbuddy/image && ./gradlew assembleDebug --no-daemon
   ```
4. 将生成的 `app/build/outputs/apk/debug/app-debug.apk` 复制到 `outputs/ShadowSleuth-debug.apk`。

### 5. 推送 GitHub

1. `git add .` 并提交，提交信息包含版本号与主要变更。
2. `git push origin main`。
3. 创建并推送 tag：`git tag -a vX.Y.Z-debug -m "..." && git push origin vX.Y.Z-debug`。
   - **注意**：Windows 下 `git tag -a` 的备注会以 GBK 编码，GitHub 按 UTF-8 解析会乱码，需用 `create_release.py` 脚本创建 Release。
4. 创建 GitHub Release 并上传 `outputs/ShadowSleuth-debug.apk`：
   - 使用项目 `outputs/create_release.py` 脚本（需通过透明代理 IP 访问 GitHub API）
   - Token 不要提交到仓库（已加入 `.gitignore`）

## 关键约定

- **主题模式**：浅色 / 深色 / 跟随系统，使用 `DataStore` + `ThemeViewModel` 管理。
- **匹配规则**：默认文件名 + 文件大小 + dHash 相似（汉明距离 ≤ 10），扫描页与搜索页不展示匹配选项。
- **排除规则**：保存时间完全一致（精确到秒）的组不视为重复（所有三种规则均适用）。
- **权限策略**：启动时请求 READ + WRITE；Android 10+ 提供 MANAGE_EXTERNAL_STORAGE 入口；删除时保留 `RecoverableSecurityException` 降级方案。
- **扫描触发**：进入 APP 不自动扫描，用户点击「开始扫描」后扫描并自动跳转结果页。
- **版本号**：递增 `versionCode`，`versionName` 形如 `1.0.X`。
- **dHash 缓存**：内存缓存，重启后清空；关于页展示占用容量（自动格式化 B/KB/MB）和创建时间。
- **导航**：底部导航栏使用 `popUpTo(Screen.Scan.route)` 返回扫描页，**不要使用** `findStartDestination()`（navigation-compose 2.7.7 不支持）。

## 常见修复

- Compose 状态不更新：检查是否使用 `MutableStateFlow` + `collectAsState()`。
- 图标未解析：确保使用 `androidx.compose.material.icons.filled.*` 导入。
- 删除权限不足：检查 `MANAGE_EXTERNAL_STORAGE` 或 `RecoverableSecurityException` 处理。
- `ripple()` 编译错误：新版 Compose 已废弃 `ripple()`，改用 `rememberRipple()`。
- 文档不同步：每次版本更新后检查 README 和 docs 中的版本号与功能描述。
- 底部导航按钮失效：检查 `popUpTo` 目标是否正确，下滑后 LazyColumn 状态不影响导航。

## 项目文件位置

| 文件 | 说明 |
|------|------|
| `README.md` | 项目入口、更新日志 |
| `docs/architecture.md` | 技术架构、数据流、权限模型、主题系统 |
| `docs/requirements.md` | 功能需求、非功能需求、明确不做 |
| `docs/skills.md` | 可复用技能与踩坑经验 |
| `docs/project-experience.md` | 版本迭代经验总结 |
| `.workbuddy/memory/MEMORY.md` | 项目长期记忆（版本、约定、技能文件位置） |
| `.workbuddy/memory/YYYY-MM-DD.md` | 每日工作日志 |
| `outputs/create_release.py` | GitHub Release 创建脚本（含 token，**不提交**） |
