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
./gradlew connectedDebugAndroidTest   # instrumented/Compose UI tests (app/src/androidTest, needs a device)
```

There are currently no test sources; test dependencies (JUnit4, Espresso, Compose ui-test) are declared but unused. The Android SDK is expected at `~/Android/Sdk`.

Toolchain is very recent (AGP 9.3, Gradle 9.6, Kotlin 2.2, KSP2). `gradle.properties` contains several compatibility flags (`android.builtInKotlin=false`, `android.newDsl=false`, etc.) that keep the classic `kotlin-android` plugin + `kotlinOptions` DSL working under AGP 9 — don't remove them without migrating the build scripts. Dependencies are declared inline in `app/build.gradle.kts` (no version catalog).

## Architecture

MVVM with a single repository and manual dependency injection:

- `MainActivity` builds everything by hand: OkHttp → Retrofit `PokemonApi` → Room `PokemonDatabase`/`PokemonDao` → `PokemonRepository` → `ui/ViewModelFactory`. The factory is passed down through `navigation/AppNavigation.kt` into every screen, which obtains its ViewModel with `viewModel(factory = factory)`. Adding a ViewModel means adding a branch to `ViewModelFactory.create`.
- `PokemonRepository` is the only data access point. Network calls return `Result<T>` (exceptions caught there); Room data is exposed as `Flow`. Plain search text is rewritten to the API's Lucene syntax `name:*<query>*`; input already containing `:` is passed through as a raw API query.
- Two data shapes: `CardDto` (API, `data/api/model`) is used by Search and Details; `ChaseCardEntity` (Room, table `chase_cards`) is used by Home. The repository maps DTO → entity when saving. Details always re-fetches from the API by card id, even for saved cards.
- Each screen (`ui/home`, `ui/search`, `ui/details`) has a sealed `*UiState` class and exposes `StateFlow`s collected with `collectAsState()`. Search debounces manually (cancel previous `Job` + `delay(500)`).
- Navigation routes: `home` (start) → `search` → `details/{cardId}`, defined in `Routes`.
- Room database is version 1 with `exportSchema = false` and no migrations — any entity change requires adding a version bump and a migration (or destructive fallback).
- The `obtained` column exists in the entity but no UI reads or writes it yet.
