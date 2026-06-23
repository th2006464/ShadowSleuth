# 双影密探 ShadowSleuth 技能与经验文档

> 本文档汇总开发 ShadowSleuth 过程中沉淀的可复用技能、工作流程与踩坑经验，便于后续 AI 或开发者快速接手、复用和更新项目。

---

## 1. 项目概览

| 项目 | 说明 |
|------|------|
| 名称 | 双影密探 / ShadowSleuth |
| 类型 | Android APK（Kotlin + Jetpack Compose + Material 3） |
| 仓库 | https://github.com/th2006464/ShadowSleuth |
| 核心能力 | 本地扫描、文件名/大小匹配、EXIF 详情、图片删除 |
| 隐私原则 | 不上传、不联网、不保留数据 |

---

## 2. 可复用技能清单

### 2.1 Android 本地图片扫描

- **核心类**：`ImageScanner.kt`
- **能力**：使用 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` + `ContentResolver` 只读元数据。
- **关键点**：
  - 投影列包含 `_ID`、`DISPLAY_NAME`、`SIZE`、`DATE_ADDED`、`WIDTH`、`HEIGHT`、`MIME_TYPE`。
  - Android 9 及以下额外读取 `DATA` 路径列。
  - 过滤 `SIZE <= 0` 和 `WIDTH/HEIGHT <= 0` 的无效图片。
  - 在 `Dispatchers.IO` 中执行，避免阻塞 UI。

### 2.2 重复图片分组匹配

- **核心类**：`DuplicateFinder.kt`
- **能力**：按文件名和文件大小分组，使用 `groupBy` 实现 O(n) 复杂度。
- **规则**：
  - 文件名相同（忽略大小写）。
  - 文件大小相同（字节级）。
  - 保存时间完全一致（精确到秒）的组会被过滤，避免误报。
  - 仅保留组员数 >= 2 的组。

### 2.3 图片删除与权限模型

- **读取权限**：
  - Android 13+：`READ_MEDIA_IMAGES`
  - Android 6 ~ 12：`READ_EXTERNAL_STORAGE`
- **删除权限**：
  - Android 10+（API 29+）：分区存储，需 `MANAGE_EXTERNAL_STORAGE` 或每次通过 `RecoverableSecurityException` 弹系统授权框。
  - Android 9 及以下：`WRITE_EXTERNAL_STORAGE`。
- **最佳实践**：
  - 启动时一并请求 `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` + `WRITE_EXTERNAL_STORAGE`。
  - Android 10+ 在界面提供「前往系统设置开启完整存储权限」入口。
  - 捕获 `RecoverableSecurityException` 并启动系统授权对话框，用户确认后重试删除。

### 2.4 Jetpack Compose 主题切换

- **核心文件**：`ThemePreferences.kt`、`ThemeViewModel.kt`、`Theme.kt`
- **能力**：浅色 / 深色 / 跟随系统三种模式，使用 DataStore 持久化。
- **关键点**：
  - 在 `MainActivity` 中收集 `ThemeViewModel.themeMode` 并传入 `ShadowSleuthTheme(...)`。
  - `ThemeMode.SYSTEM` 使用 `isSystemInDarkTheme()` 决定当前模式。
  - 主题切换后立即生效，无需重启 Activity。

### 2.5 Compose 导航与状态管理

- **导航**：使用 `androidx.navigation:navigation-compose`。
- **状态管理**：`ViewModel` 持有 `StateFlow`，`collectAsState()` 在 UI 层订阅。
- **页面间共享**：使用同一个 `ScanViewModel`，所有页面共享 `allImages` 和扫描结果。
- **生命周期**：使用 `repeatOnLifecycle(Lifecycle.State.STARTED)` 收集一次性事件（如删除授权请求）。

### 2.6 EXIF 信息读取

- **核心类**：`ExifReader.kt`、依赖 `androidx.exifinterface:exifinterface:1.3.7`
- **能力**：读取拍摄设备、GPS、光圈、曝光时间、ISO、焦距等。
- **关键点**：
  - 通过 `ContentResolver.openInputStream(uri)` 读取 EXIF。
  - 使用 `BitmapFactory.Options` 获取图片尺寸作为补充。
  - GPS 信息使用 `latLong?.let { ... }` 安全取值，避免空指针。

### 2.7 GitHub 发布流程

- 本地构建 APK：`./gradlew assembleDebug`
- 复制 APK 到 `outputs/ShadowSleuth-debug.apk`。
- 推送代码：`git push origin main`
- 创建 tag：`git tag -a vX.Y.Z-debug -m "..."` 并 `git push origin vX.Y.Z-debug`
- 创建 GitHub Release 并上传 APK。

---

## 3. 常见坑与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 扫描页拖动条无法拖动 | 使用普通 `var` 而非 `StateFlow` | 改为 `MutableStateFlow` + `collectAsState()` |
| 搜索页选择图片后闪退 | Android 10+ 缺少 `MediaStore.DATA` 列 | 使用 `getColumnIndex()` 可选读取，并提供 `OpenableColumns` 回退 |
| 删除时提示权限不足 | Android 10+ 分区存储限制 | 请求 `MANAGE_EXTERNAL_STORAGE` 或捕获 `RecoverableSecurityException` |
| 结果中出现 `0 × 0` 图片 | MediaStore 中存在无效元数据 | 扫描时过滤 `width <= 0 \|\| height <= 0` |
| 纬度经度读取空指针 | `latLong` 可能为空 | 使用 `latLong?.let { ... }` 安全取值 |
| 图标导入歧义 | `Icon` 重载冲突 | 使用 `Icons.Filled.XXX` 完整路径 |

---

## 4. 后续扩展建议

- 增加感知哈希或相似度算法，提升重复识别准确率。
- 导出扫描结果到文本文件。
- 支持更多图片格式（BMP、RAW 等）。
- 增加批量删除与「保留最佳一张」智能建议。
- 增加应用内设置页，集中管理主题、权限、扫描阈值。

---

*Last updated: 2026-06-24 (v1.0.8-debug)*
