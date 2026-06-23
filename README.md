# 双影密探 ShadowSleuth — 智能相册清理

> 一款纯本地离线的 Android 重复图片查找工具，搜索快、体积小、不上传、让相册整理更安心。

---

## 项目简介

**双影密探 ShadowSleuth** 是一款面向 Android 用户的轻量化图库工具 APK。它无需联网、不后台上传，仅在本地通过「文件名」、「文件大小」和「图片分辨率」三套规则，快速找出手机里的重复图片，并以分组对比的形式展示出来，方便你一眼判断哪些图片需要后续处理。

适合人群：
- 手机相册里图片太多，想快速找出重复/相似素材的用户
- 希望工具轻量、省电、不占用系统资源的用户
- 注重隐私，不愿把照片上传到云端分析的用户

---

## 核心功能

- **批量全局查重**：扫描 DCIM、截图、下载、社交应用保存图片、Pictures 等目录
- **三规则匹配**：按文件名、文件字节大小、图片分辨率（宽 × 高）三套规则判定疑似重复
- **分组对比展示**：相同批次重复图分为一组，列表展示缩略图与元信息
- **单图定向检索**：从相册任选一张图片作为样本，全局找出同名、同大小或同分辨率的图片
- **原图预览**：点击缩略图进入全屏原图查看，保留时间、大小等元信息
- **轻量过滤**：可忽略小于指定 KB 的极小图片，减少无效结果
- **长按操作**：长按图片项可查看详细信息或删除图片（删除前带确认弹窗）
- **深色主题**：默认采用深色专业风格，减少视觉疲劳

---

## 产品特点

- ⚡ **搜索快**：只读取必要元数据，不阻塞 UI
- 📦 **体积小**：原生优先、少依赖第三方库，安装包尽量精简
- 🔋 **省资源**：异步扫描、低内存占用、不后台常驻
- 🔒 **纯本地**：不上传任何图片，无网络请求
- 🛡️ **权限友好**：适配 Android 10 ~ 14 分区存储，删除时按系统要求请求授权

---

## 架构与搜索原理

### 整体架构

APP 采用轻量级原生架构，核心模块职责清晰：

| 模块 | 文件 | 职责 |
|------|------|------|
| **UI 层** | `ui/scan`、`ui/results`、`ui/search`、`ui/preview` | 展示扫描、结果、搜索、预览四个页面 |
| **扫描器** | `data/ImageScanner.kt` | 通过 `MediaStore` + `ContentResolver` 读取本地图片元数据 |
| **匹配器** | `data/DuplicateFinder.kt` | 按文件名 / 文件大小 / 分辨率分组判定重复 |
| **状态容器** | `viewmodel/ScanViewModel.kt` | 持有扫描状态、搜索状态、匹配规则，并处理删除逻辑 |
| **权限处理** | `MainActivity.kt` | 启动时请求读取权限，删除时按系统要求请求用户授权 |

```
用户点击「开始扫描」
    ↓
MainActivity 检查读取权限（Android 13+ 用 READ_MEDIA_IMAGES，旧版用 READ_EXTERNAL_STORAGE）
    ↓
ImageScanner 查询 MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    ↓
提取元数据：ID、URI、文件名、大小、添加时间、分辨率、MIME 类型
    ↓
DuplicateFinder 按规则分组（文件名 / 大小 / 分辨率）
    ↓
ScanViewModel 更新 ScanState.Complete
    ↓
ResultsScreen 以分组卡片形式展示
    ↓
长按图片 → 查看详情 / 删除
```

### 搜索原理

1. **只读元数据**：扫描阶段不加载原图，只读取 `MediaStore` 提供的元数据。这避免了把大量图片解码到内存，显著降低内存占用和扫描时间。
2. **三规则匹配**：
   - **文件名相同**：`displayName.equals(..., ignoreCase = true)` 判定，适合找出同名重复下载/保存的图片。
   - **文件大小相同**：`sizeBytes` 完全一致。相同字节大小的图片极大概率是重复或高度相似的副本。
   - **分辨率相同**：`width × height` 完全一致。用于快速发现尺寸相同的图片组，但会过滤掉 `0 × 0` 等无效元数据。
3. **过滤策略**：
   - 小于用户设定阈值（默认 50KB）的图片被忽略。
   - 宽或高为 `0` 的图片被视为无效/损坏元数据，不计入重复结果。
4. **单图搜索**：先扫描全库得到 `allImages`，然后以选中的样本图为基准，在全库中匹配同名/同大小/同分辨率的图片。

### 删除机制

- **Android 10+（API 29+）**：使用分区存储，删除 MediaStore 图片时若系统抛出 `RecoverableSecurityException`，APP 会启动系统提供的授权对话框，用户确认后再次执行删除。
- **Android 9 及以下**：启动时一并请求 `WRITE_EXTERNAL_STORAGE` 权限，获得后直接删除。
- 删除成功后，APP 会从内存状态和结果列表中移除该图片，无需重新扫描。

---

## GitHub 仓库

- **仓库地址**: https://github.com/th2006464/ShadowSleuth.git
- 源码、APK 构建产物及后续版本迭代均会在此仓库维护。

## 快速开始

本项目为 Android APK，已完成源码开发与 Debug 构建：

- **源码**：`app/` 目录下，Kotlin + Jetpack Compose + Material 3
- **构建方式**：使用 Gradle Wrapper，`./gradlew assembleDebug`
- **APK 产物**：`app/build/outputs/apk/debug/app-debug.apk`（Debug 包，约 17 MB）
- **GitHub 直接下载**：[outputs/ShadowSleuth-debug.apk](https://github.com/th2006464/ShadowSleuth/blob/main/outputs/ShadowSleuth-debug.apk)
- **版本标签**：[v1.0.6-debug](https://github.com/th2006464/ShadowSleuth/releases/tag/v1.0.6-debug)（深色主题 + 权限修复 + 0×0 过滤 + 架构说明）
- **构建环境**：OpenJDK 17 + Android SDK 34 + Gradle 8.2

### 本地构建

```bash
./gradlew assembleDebug
```

构建成功后 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

### 注意

- 当前为 **Debug 包**，仅供开发/测试安装。
- 正式发布需要生成签名密钥并构建 Release 版本。

---

## 更新日志

- **v1.0.6-debug**（当前）
  - 新增：深色专业主题，默认启用深色风格，界面更具专业感与神秘感
  - 新增：应用图标重新设计，融入放大镜元素，呼应「密探」搜索定位
  - 修复：长按删除图片时 Android 10+ 权限不足的问题，通过系统授权对话框引导用户确认
  - 修复：Android 9 及以下启动时一并请求 `WRITE_EXTERNAL_STORAGE`，确保删除可用
  - 修复：过滤掉分辨率 `0 × 0` 的无效图片，避免其被计入重复结果
  - 优化：更新 README，补充整体架构与搜索原理说明
  - 优化：清理项目无用文件，减小体积

- **v1.0.5-debug**
  - 新增：按图片分辨率（宽 × 高）匹配重复图片
  - 新增：扫描页、搜索页新增「按分辨率匹配」选项
  - 新增：结果页顶部新增筛选 Chip，可按文件名相同、文件大小相同、分辨率相同进行筛选
  - 新增：结果页下滑后，点击底部「结果」导航按钮自动回到顶部
  - 新增：结果页标题栏支持点击回到顶部，并增加回到顶部悬浮按钮
  - 修复：搜索结果页滑动后点击底部「扫描」按钮无法返回主页的问题
  - 优化：底部导航统一使用首页路由作为回退栈根节点，切换更稳定

- **v1.0.4-debug**
  - 新增：扁平化 UI，支持深色模式
  - 新增：重新设计 App 图标
  - 新增：结果页与搜索页长按图片项，弹出「详细信息」和「删除」菜单
  - 新增：图片详情弹窗，展示名称、大小、尺寸、格式、保存时间、路径
  - 新增：删除图片确认对话框，确认后删除并刷新结果
  - 优化：扫描页 Hero 卡片、缩略图、预览页布局

- **v1.0.3-debug**
  - 新增：启动 App 时立即请求相册权限
  - 新增：搜索页无权限时主动提示
  - 优化：扫描页匹配规则改用 Checkbox 勾选

- **v1.0.2-debug**
  - 新增：匹配结果改为列表式展示

- **v1.0.1-debug**
  - 修复：扫描页「忽略小于 X KB」拖动条无法拖动
  - 修复：搜索页选择图片后闪退（Android 10+ MediaStore.DATA 列缺失）
  - 优化：搜索图片读取增加 OpenableColumns 回退与异常保护

- **v1.0.0-debug**
  - 初始版本：完成扫描、结果、搜索、预览四个页面

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [docs/requirements.md](docs/requirements.md) | 功能需求、非功能需求与明确不做 |
| [docs/design-system.md](docs/design-system.md) | 设计原则、颜色、字体、组件规范 |
| [docs/architecture.md](docs/architecture.md) | 技术架构、数据流与性能优化策略 |
| [prototypes/index.html](prototypes/index.html) | UI 原型页面导航 |

---

## 截图预览

![UI 效果图](assets/screenshots/screen.png)

---

## 相关文件

- `Readme.txt`：原始需求来源
- `DESIGN.md`：原始设计规范
- `CLAUDE.md`：AI 编码行为准则
- `code.html`：原始 Scan Dashboard 原型

---

*Last updated: 2026-06-23 (v1.0.6-debug)*
