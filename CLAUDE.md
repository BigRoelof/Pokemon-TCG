# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`, package `com.example.pokemontcg`) for keeping a personal Pokémon TCG "chase list" (wishlist). Cards are searched via the public Pokémon TCG API (`https://api.pokemontcg.io/v2`, no API key configured) and saved locally in Room. Kotlin, Jetpack Compose + Material 3, Retrofit/Gson, Coil, Coroutines/StateFlow, KSP. The README is written in Dutch.

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew installDebug         # build + install on a connected device/emulator
./run_app.sh                   # boot the `Pixel_9` AVD if needed (NVIDIA PRIME offload auto-detected), install, launch
~/Android/Sdk/platform-tools/adb emu kill   # stop the emulator
./gradlew lint                 # Android lint
./gradlew testDebugUnitTest    # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "com.example.pokemontcg.SomeTest.someMethod"   # single test
./gradlew connectedDebugAndroidTest   # instrumented tests (app/src/androidTest); NOTE: uninstalls the app afterwards, wiping its data
```

To run instrumented tests without losing app data, install both APKs (`assembleDebug assembleDebugAndroidTest`, then `adb install -r` each) and run `adb shell am instrument -w -e class <TestClass> com.example.pokemontcg.test/androidx.test.runner.AndroidJUnitRunner`. If a physical phone is also connected, pass `-s emulator-5554` to adb (`run_app.sh` hangs with multiple devices). The Android SDK is expected at `~/Android/Sdk`. If every API call fails on the emulator with a connection error, check the emulator clock (`adb shell date`): a stale snapshot breaks TLS; cold boot with `-no-snapshot-load`.

Toolchain is very recent (AGP 9.3, Gradle 9.6, Kotlin 2.2, KSP2). `gradle.properties` contains several compatibility flags (`android.builtInKotlin=false`, `android.newDsl=false`, etc.) that keep the classic `kotlin-android` plugin + `kotlinOptions` DSL working under AGP 9 — don't remove them without migrating the build scripts. Dependencies are declared inline in `app/build.gradle.kts` (no version catalog).

## Architecture

MVVM with a single repository and manual dependency injection:

- Manual DI: `PokemonTcgApplication` lazily creates an `AppContainer` that builds OkHttp → Retrofit `PokemonApi` → Room `PokemonDatabase`/`PokemonDao` → `PokemonRepository` → `ui/ViewModelFactory` once per process. `MainActivity` takes the factory from the container; it is passed down through `navigation/AppNavigation.kt` into every screen, which obtains its ViewModel with `viewModel(factory = factory)`. Adding a ViewModel means adding a branch to `ViewModelFactory.create`.
- `PokemonRepository` is the only data access point. Network calls return `Result<T>` and are retried on 5xx (the API returns 500/502 intermittently for valid requests); Room data is exposed as `Flow`. Plain search text becomes a quoted prefix match `name:"<query>*"` — the unquoted form is rejected for input with spaces or punctuation. Input already containing `:` is passed through as a raw API query. UI turns failures into messages via `ui/ErrorMessages.kt`.
- Two data shapes: `CardDto` (API, `data/api/model`) is used by Search and Details; `ChaseCardEntity` (Room, table `chase_cards`) is used by Home. The repository maps DTO → entity when saving. Details shows the saved Room copy first (offline support), then refreshes from the API.
- Each screen (`ui/home`, `ui/search`, `ui/details`) has a sealed `*UiState` class and exposes `StateFlow`s collected with `collectAsState()`. Search debounces manually (cancel previous `Job` + `delay(500)`).
- Navigation routes: `home` (start) → `search` → `details/{cardId}`, defined in `Routes`.
- Room schemas are exported to `app/schemas/` (commit them). Changing `ChaseCardEntity` requires a version bump plus a migration in `data/database/Migrations.kt` (steps documented there); `MigrationTest` (androidTest) verifies the oldest schema migrates to the current one.
- `obtained` is toggled from Home (row checkbox) and Details (switch, only for saved cards). Home filtering and the progress counts live in the pure function `buildHomeState` (unit-tested).
