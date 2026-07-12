# 拉歌 (LaGe)

> 像拉片一样拉歌 — 深度音乐分析平台
> Kotlin Multiplatform + Compose Multiplatform

## 概述

拉歌 (LaGe) 是一个音乐深度分析平台，让用户对歌曲进行逐帧式标注和分析，支持 Android 和 iOS 平台。后端基于 Supabase (PostgREST)，使用 Cloudflare CDN 加速全球访问。

## 架构

```
lage-kmp/
├── shared/                    # KMP 共享模块 (业务逻辑)
│   └── src/
│       ├── commonMain/        # 跨平台: models, repositories, network
│       ├── commonTest/        # 跨平台测试
│       ├── androidMain/       # Android 平台实现
│       └── iosMain/           # iOS 平台实现
├── composeApp/                # Compose Multiplatform UI
│   └── src/
│       ├── commonMain/        # 跨平台 UI (Compose)
│       ├── androidMain/       # Android 主入口
│       └── iosMain/           # iOS 主入口
├── iosApp/                    # iOS 原生入口 (SwiftUI)
├── gradle/                    # Gradle 构建配置
├── netopt_output/             # 网络优化参数
└── .github/workflows/        # CI/CD
```

### 技术栈

| 层 | 技术 |
|-------|--------|
| UI | Compose Multiplatform (Material 3) |
| 网络 | Ktor Client (HTTP/2, Brotli, 连接池) |
| 序列化 | kotlinx.serialization |
| 图片加载 | Coil 3 |
| 导航 | Navigation Compose |
| 后端 | Supabase (PostgREST) |
| CDN | Cloudflare |

## 开发

### 前置条件

- JDK 17
- Gradle 8.10
- Android SDK 35
- Xcode 16+ (macOS)

### 构建

```bash
# Android
./gradlew :composeApp:assembleRelease

# iOS Framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 运行测试
./gradlew :shared:allTests

# 静态分析
./gradlew :shared:detekt :composeApp:detekt
```

### 配置

网络配置位于 `shared/src/commonMain/resources/network.properties`：

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
LAGE_CDN_URL=https://api.yourdomain.com  # 可选
```

环境变量优先级高于配置文件。

## 功能

- [x] 歌曲浏览 + 分页加载
- [x] 歌曲搜索
- [x] 歌曲标注 (7 分类)
- [x] 点赞功能
- [x] HTTP/2 + Brotli 压缩
- [x] TTL 缓存 (30s/5min/60s)
- [x] 自动重试 + 指数退避
- [x] Cloudflare CDN 支持
- [x] 连接预热
- [ ] 音频播放器
- [ ] 用户认证 UI
- [ ] 标注编辑
- [ ] 离线支持
