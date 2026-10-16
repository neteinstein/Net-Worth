import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Kotlin's top-level `initKoin()` is exported to Obj-C/Swift as `doInitKoin()` because
        // Kotlin/Native prefixes functions starting with `init` (an Obj-C reserved family).
        InitKoinKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
