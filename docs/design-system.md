# 双影密探 ShadowSleuth 设计系统

> 本文档从 `DESIGN.md` 整理而来，定义 **双影密探 ShadowSleuth** 的视觉规范与设计令牌，用于保证所有 UI 原型和最终 Android 实现的一致性。

---

## 1. 设计原则

设计系统聚焦于 **实用（Utility）、效率（Efficiency）、可信赖（Trust）**。作为一款管理个人数据和存储的工具，UI 必须让人感到可靠、精确、不打扰。品牌个性偏向专业、克制，优先呈现内容（图片）而非装饰元素。

视觉风格遵循 **Corporate / Modern**，具体采用 **Material Design 3（Material You）** 的逻辑。通过高密度布局、清晰的功能标识和系统化的信息架构，确保用户始终掌控自己的设备清理过程。

---

## 2. 颜色系统

色板以深邃的专业蓝为主色，传达稳定与技术感。

### 2.1 主要颜色

| 名称 | 色值 | 用途 |
|------|------|------|
| Primary | `#003178` | 主按钮、重要操作 |
| On Primary | `#ffffff` | 主按钮上的文字 |
| Primary Container | `#0d47a1` | 选中态、进度指示 |
| On Primary Container | `#a1bbff` | 容器上的文字 |
| Inverse Primary | `#b0c6ff` | 深色背景上的主色强调 |

### 2.2 表面颜色

| 名称 | 色值 | 用途 |
|------|------|------|
| Surface | `#f7f9fc` | 主背景 |
| Surface Dim | `#d8dadd` | 暗色表面 |
| Surface Bright | `#f7f9fc` | 亮色表面 |
| Surface Container Lowest | `#ffffff` | 卡片、容器 |
| Surface Container Low | `#f2f4f7` | 次级容器 |
| Surface Container | `#eceef1` | 默认容器 |
| Surface Container High | `#e6e8eb` | 高亮容器 |
| Surface Container Highest | `#e0e3e6` | 最高层级容器、边框 |
| Surface Variant | `#e0e3e6` | 变体表面 |
| On Surface | `#191c1e` | 主文字 |
| On Surface Variant | `#434652` | 次级文字、说明文字 |
| Inverse Surface | `#2d3133` | 深色反转背景 |
| Inverse On Surface | `#eff1f4` | 深色反转背景上的文字 |
| Outline | `#737783` | 图标、轮廓 |
| Outline Variant | `#c3c6d4` | 浅色轮廓、分割线 |
| Surface Tint | `#2b5bb5` | 表面色调 |

### 2.3 辅助颜色

| 名称 | 色值 | 用途 |
|------|------|------|
| Secondary | `#486173` | 次级操作 |
| Secondary Container | `#c9e3f9` | 次级容器、标签 |
| Tertiary | `#003b43` | 第三层级操作 |
| Tertiary Container | `#00545e` | 第三层级容器 |
| Error | `#ba1a1a` | 错误、警告 |
| Error Container | `#ffdad6` | 错误背景容器 |

### 2.4 固定颜色（Fixed Colors）

| 名称 | 色值 | 用途 |
|------|------|------|
| Primary Fixed | `#d9e2ff` | 固定主色背景 |
| Primary Fixed Dim | `#b0c6ff` | 固定主色暗色 |
| Secondary Fixed | `#cbe6fb` | 固定次级色背景 |
| Tertiary Fixed | `#a2effd` | 固定第三色背景 |

（其余 on-*-fixed 变量详见 `prototypes/shared/colors.js`）

### 2.5 CSS 变量参考

```css
:root {
    --primary: #003178;
    --on-primary: #ffffff;
    --primary-container: #0d47a1;
    --on-primary-container: #a1bbff;
    --surface: #f7f9fc;
    --on-surface: #191c1e;
    --on-surface-variant: #434652;
    --error: #ba1a1a;
    --error-container: #ffdad6;
}
```

完整变量定义请见 `prototypes/shared/styles.css`。

---

## 3. 字体系统

主界面使用 **Inter**，确保在不同屏幕密度下都有最佳可读性。为了强调工具属性，**JetBrains Mono** 用于标签和技术数据，形成清晰的「技术层」视觉区分。

### 3.1 字体层级

| 层级 | 字体 | 字号 | 行高 | 字重 | 用途 |
|------|------|------|------|------|------|
| Display Large | Inter | 32px | 40px | 700 | 大标题、启动页 |
| Headline Medium | Inter | 24px | 32px | 600 | 页面标题 |
| Headline Small | Inter | 20px | 28px | 600 | 区块标题 |
| Headline Medium Mobile | Inter | 22px | 28px | 600 | 移动端顶部标题 |
| Body Large | Inter | 16px | 24px | 400 | 正文、按钮文字 |
| Body Medium | Inter | 14px | 20px | 400 | 说明文字 |
| Label Medium | JetBrains Mono | 12px | 16px | 500 | 文件大小、时间戳 |
| Label Small | JetBrains Mono | 10px | 14px | 500 | 文件路径、极小标签 |

### 3.2 使用建议

- **标题**：粗体、紧凑，用于明确区块锚点。
- **技术标签**：等宽字体，必要时大写，营造精确感。
- **文件大小示例**：`4.2 MB`、`1,024 KB` 使用 `Label Medium`。
- **文件路径示例**：`/storage/emulated/0/DCIM/...` 使用 `Label Small`。

---

## 4. 间距系统

所有间距基于 **8px 线性比例**，4px 用于更紧凑的控件间距。

| 令牌 | 数值 | 用途 |
|------|------|------|
| base / xs | 4px | 极紧凑间距、小图标边距 |
| sm | 8px | 控件内部间距、紧凑卡片间距 |
| md | 16px | 卡片内边距、屏幕边缘留白 |
| lg | 24px | 区块间距 |
| xl | 32px | 大区块间距 |
| edge-margin | 16px | 屏幕左右边距 |
| grid-gutter | 12px | 图片网格间距 |

---

## 5. 布局规范

### 5.1 网格

- 移动端：4 列网格
- 横屏 / 平板：8 列网格

### 5.2 密度

对比页面采用高密度布局，图片网格使用 12px 间距，兼顾可视性与信息密度。

### 5.3 安全区域

严格遵循 Android 状态栏和导航栏高度，确保内容不被系统覆盖。

---

## 6. 形状与圆角

| 令牌 | 数值 | 用途 |
|------|------|------|
| sm | 4px | 小标签、输入框 |
| DEFAULT | 8px | 小图标、按钮 |
| md | 12px | 中型卡片 |
| lg | 16px | 主内容卡片、缩略图、按钮 |
| xl | 24px | 大容器、底部抽屉 |
| full | 9999px | 药丸形、圆形选择指示器 |

---

## 7. 阴影与层级

采用 Material Design 3 的 **Tonal Layers** 理念，而非厚重阴影。

| 层级 | 说明 | 样式 |
|------|------|------|
| Level 0 (Surface) | 主背景 | `#f7f9fc` |
| Level 1 (Cards) | 卡片 | 纯白背景 + 1px `#e0e3e6` 边框 |
| Level 2 (Modals) | 模态/菜单 | 柔和阴影 `0 4px 12px rgba(0,0,0,0.05)` |
| Active States | 按下/选中 | 主色 8% 透明度 |

---

## 8. 组件库

### 8.1 按钮

| 类型 | 背景 | 文字 | 圆角 |
|------|------|------|------|
| Primary | `#0d47a1` | 白色 | 16px |
| Secondary | 透明 + 主色描边 | 主色 | 16px |
| Destructive | `#ba1a1a` | 白色 | 16px |

### 8.2 卡片（对比与信息）

- 背景：白色 `#ffffff`
- 圆角：16px
- 内边距：12px ~ 16px
- 必须包含等宽字体的文件大小标签

### 8.3 选择状态

- 缩略图右上角使用蓝色对勾图标
- 选中缩略图增加 3px 主色内边框

### 8.4 扫描与状态

- 进度条：细长线性进度条，用于后台扫描
- 状态指示器：小圆点（绿=完成、黄=需检查、蓝=扫描中）

### 8.5 图标

- 风格：Minimalist Outline，2px 描边
- 尺寸：24x24dp 边界框
- 使用 Material Symbols Outlined

### 8.6 列表

- 高密度列表项，高度 56px 或 64px
- 分割线：1px 浅灰 `#e0e3e6`，左侧 16px 缩进

---

## 9. 原型文件

设计令牌已在以下文件中实现：

- `prototypes/shared/tailwind-config.js` — Tailwind CSS 配置
- `prototypes/shared/styles.css` — CSS 变量与通用样式

原型页面：

- `prototypes/scan.html` — 扫描页
- `prototypes/results.html` — 扫描结果页
- `prototypes/search.html` — 单图检索页
- `prototypes/preview.html` — 全屏预览页
- `prototypes/index.html` — 原型导航

---

*Last updated: 2026-06-23*
