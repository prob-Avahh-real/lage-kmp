import SwiftUI
import shared  // KMP shared framework

/// iOS App 入口 — SwiftUI 原生壳，嵌入 KMP Compose UI
///
/// 性能优化:
///   - URLSession 默认 HTTP/2 多路复用 + 连接池
///   - Network.framework 自动管理连接生命周期
///   - Ktor Darwin 引擎共享 shared 模块的连接池

@main
struct LaGeApp: App {
    init() {
        // 启动时预热 Supabase 连接
        // shared 模块的 LageHttpClient 底层使用 URLSession，
        // 首次 HTTP 请求前会完成 DNS + TCP + TLS 握手
        Task {
            do {
                try await shared.LageHttpClient.shared.warmup()
                print("[LaGe] 预热完成")
            } catch {
                print("[LaGe] 预热失败 (可忽略): \(error)")
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

/// SwiftUI ContentView — 嵌入 KMP Compose UI
struct ContentView: View {
    var body: some View {
        // 方式 1: 全量使用 Compose Multiplatform UI
        ComposeView()
            .ignoresSafeArea(.all)

        // 方式 2 (可切换): 混合 SwiftUI + Compose
        // 对于需要原生 iOS 动画或交互的界面，保留 SwiftUI 实现
        // ComposeMultiplatformView()
    }
}

/// Compose UI 桥接
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // 调用 shared 模块的 ComposeUIViewController
        // 实际项目中由 Compose Multiplatform 插件生成
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
