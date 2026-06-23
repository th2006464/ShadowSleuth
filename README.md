# 双影密探 ShadowSleuth - 智能相册清理

> 一款纯本地离线的 Android 重复图片查找工具，搜索快、体积小、不上传、不删除，让相册整理更安心。

---

## 项目简介

**双影密探 ShadowSleuth（智能相册清理）** 是一款面向 Android 用户的轻量化图库工具 APK。它无需联网、不执行任何删除操作，仅通过「文件名」和「文件大小」两套规则，在本地快速找出手机里的重复图片，并以分组对比的形式展示出来，方便你一眼判断哪些图片需要后续处理。

适合人群：
- 手机相册里图片太多，想快速找出重复/相似素材的用户
- 希望工具轻量、省电、不占用系统资源的用户
- 注重隐私，不愿把照片上传到云端分析的用户

---

## 核心功能

- **批量全局查重**：扫描 DCIM、截图、下载、微信/QQ 保存图片、Pictures 等目录
- **双规则匹配**：按文件名完全一致或文件字节大小完全一致判定疑似重复
- **分组对比展示**：相同批次重复图分为一组，并排展示双缩略图
- **单图定向检索**：从相册任选一张图片作为样本，全局找出同名/同大小的图片
- **原图预览**：点击缩略图进入全屏原图查看，保留时间、大小等元信息
- **轻量过滤**：可忽略小于指定 KB 的极小图片，减少无效结果
- **权限友好**：适配 Android 10 ~ 14 分区存储，缺失权限时引导手动开启

---

## 产品特点

- ⚡ **搜索快**：只读取必要元数据，分块扫描，不阻塞 UI
- 📦 **体积小**：原生优先、少依赖第三方库，安装包尽量精简
- 🔋 **省资源**：异步扫描、低内存占用、不后台常驻
- 🔒 **纯本地**：不上传任何图片，无网络请求
- 🛡️ **只读不删**：无删除、无移动、无清理，仅在本地展示结果

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [docs/requirements.md](docs/requirements.md) | 功能需求、非功能需求与明确不做 |
| [docs/design-system.md](docs/design-system.md) | 设计原则、颜色、字体、组件规范 |
| [docs/architecture.md](docs/architecture.md) | 技术架构、数据流与性能优化策略 |
| [prototypes/index.html](prototypes/index.html) | UI 原型页面导航 |

---

## GitHub 仓库

- **仓库地址**: https://github.com/th2006464/ShadowSleuth.git
- 源码、APK 构建产物及后续版本迭代均会在此仓库维护。

## 快速开始

本项目为 Android APK，已完成初始版本的源码开发与 Debug 构建：

- **源码**：`app/` 目录下，Kotlin + Jetpack Compose + Material 3
- **构建方式**：使用 Gradle Wrapper，`./gradlew assembleDebug`
- **APK 产物**：`app/build/outputs/apk/debug/app-debug.apk`（Debug 包，约 16 MB）
- **GitHub 直接下载**：[outputs/ShadowSleuth-debug.apk](https://github.com/th2006464/ShadowSleuth/blob/main/outputs/ShadowSleuth-debug.apk)
- **版本标签**：[v1.0.0-debug](https://github.com/th2006464/ShadowSleuth/releases/tag/v1.0.0-debug)（可在此手动创建正式 Release）
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

## 截图预览

![UI 效果图](assets/screenshots/screen.png)

---

## 相关文件

- `Readme.txt`：原始需求来源
- `DESIGN.md`：原始设计规范
- `CLAUDE.md`：AI 编码行为准则
- `code.html`：原始 Scan Dashboard 原型

---

*Last updated: 2026-06-23*
