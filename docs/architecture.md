# 双影密探 ShadowSleuth 技术架构

> 本文档描述 **双影密探 ShadowSleuth** Android APK 的模块划分、数据流、匹配规则、权限模型以及性能优化策略。

---

## 1. 架构目标

作为一款面向 Android 用户的轻量工具，ShadowSleuth 的技术架构需要满足：

- **搜索快**：扫描和匹配不能让用户等太久。
- **体积小**：APK 尽量精简，少依赖第三方库。
- **省资源**：不占用过多内存和电量，不后台常驻。
- **易维护**：模块职责清晰，便于后续迭代。
- **隐私优先**：不联网、不上传、不保留用户数据。

因此，整体采用轻量级原生架构，避免引入重量级框架。

---

## 2. 系统模块划分

| 模块 | 文件 | 职责 |
|------|------|------|
| **UI 层** | `ui/scan`、`ui/results`、`ui/search`、`ui/preview` | 展示扫描页、结果页、搜索页、预览页，接收用户点击与长按事件 |
| **扫描器** | `data/ImageScanner.kt` | 通过 `MediaStore` + `ContentResolver` 查询本地图片元数据 |
| **匹配器** | `data/DuplicateFinder.kt` | 按文件名 / 文件大小 / 分辨率分组判定重复 |
| **状态容器** | `viewmodel/ScanViewModel.kt` | 持有扫描状态、搜索状态、匹配规则、删除请求与重试逻辑 |
| **权限处理** | `MainActivity.kt` | 启动时请求读取权限，删除时处理系统授权对话框 |

```
用户点击「开始扫描」
    ↓
MainActivity 检查读取权限
    ↓
ImageScanner 查询 MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    ↓
提取元数据：ID、URI、文件名、大小、添加时间、分辨率、MIME 类型
    ↓
DuplicateFinder 按规则分组（文件名 / 大小 / 分辨率）
    ↓
ScanViewModel 更新 ScanState.Complete
    ↓
ResultsScreen 以分组卡片展示
    ↓
长按图片 → 查看详情 / 删除
```

---

## 3. 数据流

### 3.1 批量扫描

1. **触发扫描**：用户在扫描页点击「开始扫描」。
2. **权限检查**：
   - Android 13+：请求 `READ_MEDIA_IMAGES`。
   - Android 10 ~ 12：请求 `READ_EXTERNAL_STORAGE`。
   - Android 9 及以下：同时请求 `READ_EXTERNAL_STORAGE` + `WRITE_EXTERNAL_STORAGE`（用于删除）。
3. **MediaStore 查询**：通过 `ContentResolver.query(...)` 一次性查询外部存储中的所有图片条目。
4. **过滤**：
   - 跳过 `SIZE <= 0` 的条目。
   - 跳过 `WIDTH <= 0` 或 `HEIGHT <= 0` 的条目（无效或损坏图片）。
   - 忽略小于用户设定阈值的图片（默认 50KB）。
5. **元数据提取**：读取 `_ID`、`DISPLAY_NAME`、`SIZE`、`DATE_ADDED`、`WIDTH`、`HEIGHT`、`MIME_TYPE`，以及 Android 9 及以下的 `DATA` 路径。
6. **匹配分组**：使用 `groupBy` 按文件名、大小、分辨率分别分组，保留每组数量 >= 2 的结果。
7. **展示**：按每组图片数量降序展示。

### 3.2 单图搜索

1. 用户选择一张图片作为样本。
2. 如果 `allImages` 为空，先执行全库扫描。
3. 以样本图为基准，在 `allImages` 中匹配同名、同大小或同分辨率的图片。
4. 返回包含样本图自身的匹配分组。

---

## 4. 匹配规则

### 4.1 文件名匹配

- 比较 `ImageMetadata.displayName`（忽略大小写）。
- 适合发现同名重复下载/保存的图片。

### 4.2 文件大小匹配

- 比较 `ImageMetadata.sizeBytes`。
- 相同字节大小的图片极大概率是重复副本。

### 4.3 分辨率匹配

- 比较 `ImageMetadata.width × ImageMetadata.height`。
- 用于快速发现尺寸相同的图片组。
- 过滤 `0 × 0` 等无效分辨率。

### 4.4 组合使用

扫描页和搜索页均提供三个 Checkbox，用户可自由组合启用哪些规则。结果页顶部提供三个筛选 Chip，用于在已生成的结果中动态开关显示类别。

---

## 5. 数据模型

### 5.1 图片元数据（ImageMetadata）

```kotlin
data class ImageMetadata(
    val id: Long,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
)
```

### 5.2 重复分组（DuplicateGroup）

```kotlin
data class DuplicateGroup(
    val id: String,
    val matchType: MatchType,
    val images: List<ImageMetadata>
)
```

`MatchType` 枚举：
- `FILENAME`：文件名相同
- `SIZE`：文件大小相同
- `DIMENSIONS`：分辨率相同

### 5.3 状态模型

- `ScanState`：Idle / Scanning / Complete / Error
- `SearchState`：Idle / NoSample / Ready / Error

---

## 6. 权限与删除模型

### 6.1 读取权限

- Android 13+：`READ_MEDIA_IMAGES`
- Android 6 ~ 12：`READ_EXTERNAL_STORAGE`

### 6.2 删除权限

- **Android 10+（API 29+）**：使用分区存储。APP 调用 `ContentResolver.delete(uri)` 时，若图片不属于当前应用，系统会抛出 `RecoverableSecurityException`。APP 提取其中的 `IntentSender` 并启动系统授权对话框，用户点击「允许」后重试删除。
- **Android 9 及以下**：需要 `WRITE_EXTERNAL_STORAGE` 权限，启动时一并请求。

### 6.3 删除后状态更新

删除成功后，从 `allImages` 以及当前的 `ScanState.Complete` / `SearchState.Ready` 中移除对应图片，并重新过滤掉组成员不足 2 的分组。无需重新扫描全库。

---

## 7. 性能与资源优化策略

### 7.1 只读元数据，不加载原图

扫描阶段只读取 `MediaStore` 元数据，不加载原图。只有用户点击缩略图进入预览时，才通过 Coil 异步加载对应图片。

> 好处：大幅减少内存占用，扫描速度快，省电。

### 7.2 异步扫描

扫描任务放在 `Dispatchers.IO` 中执行，并通过 `Coroutine.isActive` 检查及时响应取消。

### 7.3 哈希表快速匹配

使用 `groupBy` 建立哈希表：
- 键：文件名 / 文件大小 / 分辨率字符串
- 值：相同键的图片列表

时间复杂度接近 O(n)，避免 O(n²) 两两比对。

### 7.4 小图与无效数据过滤

- 忽略小于阈值的图片。
- 忽略 `0 × 0` 分辨率的无效图片，减少噪点结果。

### 7.5 不后台常驻

扫描完成后立即释放资源，不保留后台服务或常驻进程。

---

## 8. 技术选型

- **Kotlin**：官方首选语言，协程处理异步任务。
- **Jetpack Compose**：声明式 UI，减少样板代码。
- **Material 3**：统一的设计组件与主题系统。
- **MediaStore / ContentResolver**：原生读取图片元数据，无需文件系统遍历。
- **Coil**：仅用于缩略图与原图加载，体积小且支持 Compose。

---

## 9. 安全与隐私

- **无网络请求**：不申请 `INTERNET` 权限，不上传任何图片。
- **本地处理**：所有扫描、匹配、删除操作都在本地完成。
- **数据不落盘**：扫描结果仅保存在内存中，应用退出后自动释放。

---

## 10. 后续可扩展点

- 增加感知哈希或相似度算法，提升重复识别准确率。
- 导出扫描结果到文本文件。
- 支持更多图片格式（BMP、RAW 等）。
- 增加批量删除与「保留最佳一张」智能建议。

---

*Last updated: 2026-06-23 (v1.0.6-debug)*
