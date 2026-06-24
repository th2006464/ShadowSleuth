## v1.3.1 - 3 个 Bug 修复

### 修复
- **底部导航栏 Scan 按钮无法返回扫描页**：从 Results 页点击 Scan 按钮时改用 `popBackStack()` 回退，返回到 Scan 页面
- **dHash 搜索选取图片报错**：系统相册选取的图片宽高未知时，增加 `computeFast` 兜底计算 + URI 匹配已有缓存
- **同时间戳图片被 dHash 排除**：dHash 内容匹配不再受 `allSameSaveTime` 限制，不同路径内容相同的图片正确识别
