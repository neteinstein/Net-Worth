import ComposeApp
import SwiftUI
import UIKit

/// Hosts the shared Compose UI (`App()` in commonMain).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Edge-to-edge: Compose applies safe-area insets itself (Scaffold / WindowInsets).
        ComposeView()
            .ignoresSafeArea()
    }
}
