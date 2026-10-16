# Net Worth

Track what you own and what you owe — on Android, iOS and the web, from one Kotlin Multiplatform codebase.

- **Stack:** Kotlin Multiplatform · Compose Multiplatform · MVVM + Clean Architecture · Koin · Navigation 3
- **Targets:** Android (minSdk 32, targetSdk 37) · iOS 17+ (arm64 device + Apple-silicon simulator) · Web (Wasm)

```bash
cp androidApp/google-services.json.ci androidApp/google-services.json
./gradlew :androidApp:installDebug                    # Android
./gradlew :composeApp:wasmJsBrowserDevelopmentRun     # Web
open iosApp/iosApp.xcodeproj                          # iOS (macOS + Xcode)
./gradlew testDebugUnitTest                           # shared unit tests
```

- [AGENTS.md](AGENTS.md) — architecture, module layout, conventions, gotchas (start here)
- [CI_CD.md](CI_CD.md) — pipeline jobs, secrets, signing status
