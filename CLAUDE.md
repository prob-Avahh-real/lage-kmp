# LaGe (拉歌) KMP — CLAUDE.md

## Project Overview
音乐深度分析平台 KMP (Kotlin Multiplatform + Compose Multiplatform) 项目。
用户可对歌曲进行逐帧式标注和分析 (类似视频拉片)。

## Tech Stack
- Kotlin 2.1.20 + Compose Multiplatform 1.7.3
- Ktor 3.1.1 (HTTP/2, Brotli/Gzip)
- kotlinx.serialization + kotlinx.coroutines + kotlinx-datetime
- Coil 3 (异步图片加载)
- Navigation Compose (Multiplatform)
- Supabase (PostgREST backend)

## Project Structure
```
composeApp/src/commonMain/   → Compose UI (HomeScreen, Theme, App entry)
shared/src/commonMain/       → Models, Network, Repositories
shared/src/commonTest/       → Unit tests (models, ResponseCache)
iosApp/                      → SwiftUI entry point
```

## Key Architecture Decisions
1. Singleton pattern: `LageHttpClient` as object, `by lazy` init
2. `ResponseCache<T>` with Mutex for thread-safe TTL caching
3. `Result<T>` return types for all repository methods
4. `runCatching` for error handling in repositories
5. ProGuard/R8 optimized for release (minified + resource shrinking)
6. CI/CD: Android APK + iOS archive + lint + test + detekt

## Current Deficiencies (identifed 2026-06-07)
1. No `expect`/`actual` declarations — all platform logic in repositories
2. No repository interfaces (prevents test mocking)
3. Missing domain/use-case layer
4. Coil cache policy uses defaults (no explicit config)
5. iOS `LageHttpClient.shared.warmup()` Swift bridging mismatch
6. `iOSNetworkConfiguration` class defined but never instantiated

## Build Commands
```bash
./gradlew :shared:allTests                    # Run all tests
./gradlew :shared:detekt                      # Static analysis
./gradlew :composeApp:assembleRelease          # Build Android release
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64  # Build iOS fw
```

## Configuration
- `gradle/libs.versions.toml` — Version catalog
- `detekt-config.yml` — Static analysis rules
- `network.properties` — Supabase + CDN URL configuration
- `proguard-rules.pro` — R8/ProGuard optimization rules
