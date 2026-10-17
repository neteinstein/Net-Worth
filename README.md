# Net Worth

Family finances on Android, iOS and the web, from one Kotlin Multiplatform codebase: import bank extracts,
see where the money went, catch spending spikes and look ahead. The family's data is one JSON file in their
own Google Drive, shared between the parents.

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
- [docs/google-cloud-setup.md](docs/google-cloud-setup.md) — OAuth clients needed for Google sign-in
- [docs/database.md](docs/database.md) — the shared database file and how it syncs
- [docs/design/](docs/design/README.md) — the design the app is built from
