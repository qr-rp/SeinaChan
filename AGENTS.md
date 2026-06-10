# Seina Chan — Development Guide

**Seina Chan** is a native Android client for [Hermes Agent](https://github.com/NousResearch/hermes-agent). It talks to Hermes Gateway via WebSocket JSON-RPC and reimplements the Claude.com design system (cream/coral/dark-navy) in Jetpack Compose + Material3.

---

## Project Structure

```
SeinaChan/
├── apps/android/               # Android app — the main deliverable
│   └── gradle/libs.versions.toml  # Version catalog (single source of truth)
│
├── hermes-agent/               # Git submodule (gitignored, NOT part of build)
├── .trae/                      # Planning specs from Trae IDE
├── DESIGN.md                   # Claude.com design system reference (gitignored)
└── test_hermes_api.py          # Local Hermes API test script (gitignored)
```

**The root has NO build system.** All development happens in `apps/android/`. `hermes-agent/` is a submodule included for documentation/API reference; it is gitignored from the root and has its own `AGENTS.md` inside.

---

## Build & Run

```bash
# All commands run from apps/android/
cd apps/android

# Build debug APK
./gradlew assembleDebug

# Run on connected device/emulator
./gradlew installDebug

# Check Kotlin/Compose compiler diagnostics
./gradlew lint
```

- **compileSdk = 35**, **minSdk = 26**, **targetSdk = 35**
- **Kotlin 2.0.21**, **AGP 8.7.3**, **Compose BOM 2024.12.01**
- Aliyun Maven mirrors configured in `settings.gradle.kts` for Chinese network access. No proxy switch needed — they're always-on fallbacks before google/mavenCentral.
- `org.gradle.jvmargs=-Xmx4096m` in `gradle.properties` — Gradle daemon needs sufficient heap.

---

## Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| UI | Jetpack Compose + Material3 | `material3` from BOM |
| Architecture | MVI + ViewModel + Repository | Unidirectional data flow |
| DI | Hilt | `hilt-android:2.54` + `hilt-navigation-compose:1.2.0` |
| Network | Ktor Client 3.0.3 | OkHttp engine, WebSocket support |
| Serialization | Kotlinx Serialization 1.7.3 | `kotlinx.serialization.json` |
| Async | Kotlin Coroutines + Flow | StateFlow for UI state, SharedFlow for events |
| Image loading | Coil 2.6.0 | Compose-native |
| Local storage | DataStore Preferences | Connection config + session persistence |

---

## Architecture & Key Conventions

### Package Layout (`apps/android/app/src/main/java/com/seina/chan/`)

```
di/AppModule.kt              # Hilt module — provides HttpClient singleton
data/
  remote/
    HermesWsClient.kt         # WebSocket JSON-RPC client (ConnectionState + events Flow)
    HermesApiService.kt       # REST API: sessions, messages, status, model info
    GatewayEvent.kt           # JSON-RPC event data classes (message.delta, tool.*, etc.)
  repository/
    ConnectionRepository.kt   # Connection URL, token, state management
    SessionRepository.kt      # Session CRUD
    ChatRepository.kt         # Send/receive messages, streaming, interactive events
  model/
    Session.kt
    ChatMessage.kt
    ConnectionConfig.kt
service/
  HermesConnectionService.kt  # Foreground service — keeps WebSocket alive in background
ui/
  theme/                      # DESIGN.md token system: Color.kt, Type.kt, Shape.kt, Spacing.kt, Theme.kt
  components/                 # Reusable: SeinaButton, SeinaTextField, SeinaSnackbar, MessageBubble, etc.
  screens/
    connect/                  # ConnectScreen — enter Hermes URL + token
    chat/                     # ChatScreen — transcript + composer + dialogs
    sessions/                 # SessionListScreen — mobile drawer / tablet list pane
    settings/                 # SettingsScreen — model, tools, connection config
  navigation/
    SeinaNavHost.kt           # Routes: connect / chat / settings
    AdaptivePane.kt           # Landscape split-pane (tablet) vs single-pane (phone)
util/
  FileLogger.kt               # Custom file-based logger (NOT Android Logcat)
```

### MVI Flow

```
UI Event → ViewModel.handleAction() → Repository → Ktor WS/HTTP → Hermes Gateway
                                                                ↓
UI ← StateFlow<UiState> ← ViewModel ← Repository ← Flow<GatewayEvent>
```

- Each screen has its own `ViewModel` + `UiState` data class.
- `ChatViewModel` is the most complex — it manages message streaming, tool calls, approval/clarify/secret dialogs, session lifecycle.
- `ChatRepository` owns a `MutableStateFlow<List<ChatMessage>>` that the ViewModel collects.

### WebSocket Client (`HermesWsClient`)

- Wraps Ktor WebSocket session with JSON-RPC protocol.
- Exposes `state: StateFlow<ConnectionState>` (Idle → Connecting → Open → Closed | Error).
- Exposes `events: SharedFlow<GatewayEvent>` for incoming server events.
- `request(method, params)` sends JSON-RPC request and returns a `CompletableDeferred` result.
- Exponential backoff reconnect (max 5 attempts) triggered when `shouldReconnect` is true.
- Uses `ConcurrentHashMap` for pending request tracking by numeric `reqId`.

### Foreground Service

- `HermesConnectionService` keeps the WebSocket alive when the app is backgrounded.
- `MainActivity` binds to it via `ServiceConnection` to share the same connection.
- Notification shown when in background, removed when app returns to foreground (`onResume`/`onPause`).
- Permissions required: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

---

## Design System

Token system in `ui/theme/` maps `DESIGN.md` to Compose:

- **Color.kt**: Light + dark color schemes. Key tokens: `Primary` (#CC785C coral), `Canvas` (#FAF9F5 cream), `SurfaceDark` (#181715 navy), `Ink` (#141413 warm black).
- **Type.kt**: Cormorant Garamond (serif display) + Inter (sans body) via Google Fonts. Serif at weight 400 with negative letter-spacing for headlines.
- **Shape.kt**: `xs=4dp, sm=6dp, md=8dp, lg=12dp, xl=16dp, pill=50%`.
- **Spacing.kt**: `xxs=4dp, xs=8dp, sm=12dp, md=16dp, lg=24dp, xl=32dp, xxl=48dp, section=96dp`.
- **Theme.kt**: `SeinaChanTheme` — wraps `MaterialTheme` with `LightColorScheme`/`DarkColorScheme` + custom typography + shapes.

**Important**: The design is deliberately warm (cream + coral), not cool-blue. The serif display font is the brand voice — do not replace with sans.

---

## Common Pitfalls

- **DO NOT run Gradle from root.** There's no `settings.gradle.kts` or `build.gradle.kts` at root. Always `cd apps/android` first.
- **Version catalog is the single source of truth.** `libs.versions.toml` pins every dependency. Do NOT hardcode versions in `app/build.gradle.kts`.
- **All Ktor artifacts use the same `3.0.3` version** from the catalog. Do not mix Ktor versions.
- **Kotlin serialization plugin** (`kotlin.plugin.serialization`) must be applied alongside `kotlinx-serialization-json` library.
- **Hilt uses `kapt`** (not KSP) in this project. The `kotlin("kapt")` plugin is applied in `app/build.gradle.kts`.
- **Foreground service** is required for reliable WebSocket persistence. Without it, Android kills the connection when the app is backgrounded.
- **`FileLogger`** is the custom logger — not Logcat. Check `util/FileLogger.kt` for output location. It writes to the app's internal storage.
- **No root-level pyproject.toml or package.json.** Python files in the root (`test_hermes_api.py`) are standalone test scripts for local Hermes API debugging.
- **`hermes-agent/` is a git submodule** with its own remote. Do not edit it as part of Seina Chan development — reference it for the Gateway protocol.
- **No network_security_config.xml hardening**: the `@xml/network_security_config` allows cleartext HTTP for local/LAN Hermes instances (`ws://` URLs). Do not ship this to production without review.

---

## Git Conventions

- Single branch (`master`), no remotes configured.
- Commit messages in Chinese using `<type>(<module>): <描述>` format (e.g. `feat(theme): 添加深色模式支持`).
- Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`.
- Do NOT commit without explicit user request. No automatic `git push`.
- Only `master` branch exists. No release branches, no tags.

---

## Testing

```bash
# From apps/android/
./gradlew test                            # Unit tests
./gradlew connectedAndroidTest            # Instrumentation tests (requires device/emulator)
```

- Unit tests use JUnit 4.13.2.
- Instrumentation tests use Espresso 3.6.1.
- No changes to `hermes-agent/` — its test suite (`scripts/run_tests.sh`) is separate.
