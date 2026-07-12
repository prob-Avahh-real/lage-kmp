# Changelog

## [Unreleased] — Engineering Workflow v2

### Fixed
- 🔴 **CRITICAL**: `System.currentTimeMillis()` in commonMain crashes on iOS (replaced with `Clock.System.now().toEpochMilliseconds()`) — `HttpClient.kt`
- 🔴 **CRITICAL**: AuthRepository force unwrap (`!!`) causes NullPointerException if API shape changes — `AuthRepository.kt`
- 🔴 **CRITICAL**: Missing `kotlinx.coroutines.delay` import in commonMain — `HttpClient.kt`
- 🟡 **HIGH**: Navigation Compose alpha version (`2.8.0-alpha10`) used in production build — `libs.versions.toml`
- 🟡 **HIGH**: Race condition in `LageHttpClient.client` lazy initialization (replaced var+null with `by lazy`) — `HttpClient.kt`
- 🟡 **HIGH**: `ResponseCache` mutable state not thread-safe (added `Mutex.withLock`) — `HttpClient.kt`

### Added
- **Tests**: `ResponseCacheTest.kt` (11 test cases: TTL expiry, hits/misses, invalidation, clear, overwrite)
- **Tests**: `ModelsSerializationTest.kt` (8 test cases: serialization, deserialization, categories, edge cases)
- **Test dependencies**: `kotlinx-coroutines-test` in `shared/build.gradle.kts`
- **Lint**: `detekt-config.yml` — full static analysis configuration with KMP-specific rules
- **Lint**: detekt 1.23.7 plugin in `shared` and `composeApp` modules
- **CI**: detekt static analysis step in workflow
- **CI**: `allTests` target (cross-platform) replaces `test` target
- **Docs**: `README.md` — project overview, architecture, build commands
- **Docs**: `CLAUDE.md` — project reference for AI assistants

### Remaining Issues
- Missing `expect`/`actual` platform abstraction layer (`domain/`, `platform/` dirs empty)
- No repository interfaces (prevents test mocking)
- Swift `LageHttpClient.shared.warmup()` bridging will not compile (Kotlin object ≠ `.shared` pattern)
- `iOSNetworkConfiguration` class defined but never instantiated (dead code)
- Coil image cache policy uses defaults (no explicit disk/memory config)
- No request deduplication for rapid scroll-triggered fetches

---

## [1.0.0] — Initial KMP Release (Flutter→KMP migration)
- Flutter → Kotlin Multiplatform + Compose Multiplatform migration complete
- Song browsing with pagination (LazyColumn)
- Song search (full-text via Supabase)
- Annotation system (7 categories: lyric, melody, harmony, rhythm, production, instrumentation, structure)
- Like/unlike with optimistic cache invalidation
- Supabase REST API integration via Ktor
- TTL caching: 30s list, 5min detail, 60s annotations
- Retry with exponential backoff
- HTTP/2 + Brotli/Gzip compression
- Cloudflare CDN support
- Connection warmup (pre-connect)
- iOS: SwiftUI shell + Compose UI bridge
- ProGuard/R8 optimization for Android
- CI/CD pipeline (Android APK + iOS archive)
- Network optimization parameters (netopt)
