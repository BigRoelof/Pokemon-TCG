# Pokémon TCG Chase List (Android)

Een moderne, native Android applicatie geschreven in Kotlin waarmee gebruikers hun persoonlijke Pokémon TCG "Chase List" kunnen bijhouden en beheren. 

De app downloadt de kaartgegevens uit de open-source dataset [pokemon-tcg-data](https://github.com/PokemonTCG/pokemon-tcg-data) naar een lokale Room database. Zoeken en kaartdetails werken daardoor volledig offline; bij elke start worden alleen nieuwe of gewijzigde sets opgehaald. (De oude Pokémon TCG API is verouderd en wordt niet meer gebruikt.)

---

## 🛠️ Technologie & Architectuur

* **Taal:** Kotlin
* **UI Framework:** Jetpack Compose & Material 3
* **Architectuur:** MVVM (Model-View-ViewModel) + Feature-based package structuur
* **Lokale Database:** Room Database (100% offline ondersteuning)
* **Netwerk:** Retrofit + Gson (voor het synchroniseren van de kaartdataset)
* **Afbeeldingen:** Coil (voor async afbeeldingen & caching)
* **Asynchroon:** Kotlin Coroutines & Flow / StateFlow
* **Build System:** Gradle Kotlin DSL (`.kts`) + KSP

---

## 📁 Projectstructuur

```text
com.example.pokemontcg/
├── data/
│   ├── api/          # Retrofit interface, endpoints & DTO models
│   ├── database/     # Room Entity, DAO & Database instantie
│   └── repository/   # PokemonRepository (unificatie van API & DB)
├── ui/
│   ├── home/         # HomeScreen & HomeViewModel (weergave van je Chase List)
│   ├── search/       # SearchScreen & SearchViewModel (live API zoekfunctie)
│   ├── details/      # DetailsScreen & DetailsViewModel (kaartdetails & opslaan/verwijderen)
│   ├── components/   # Herbruikbare UI onderdelen (PokemonCardRow, ErrorView, LoadingIndicator)
│   └── theme/        # Material 3 kleurenschema's en typografie
├── navigation/       # Navigation Compose routing (Home -> Search -> Details)
└── MainActivity.kt   # App entry point & handmatige Dependency Injection
```

---

## 🚀 Snel opstarten (Linux / CLI)

In de root van dit project bevindt zich het bash script `run_app.sh`. Dit script detecteert automatisch of de Android emulator draait, start deze zo nodig op (met hardwareversnelling), bouwt het project en start de app.

```bash
./run_app.sh
```

Om de emulator na gebruik weer netjes af te sluiten:
```bash
~/Android/Sdk/platform-tools/adb emu kill
```

---

## 🔧 Vereisten & Systeeminstellingen per OS

### Ubuntu / Debian

1. **Installeer KVM (Hardwareversnelling):**
   ```bash
   sudo apt update
   sudo apt install cpu-checker qemu-kvm -y
   sudo usermod -aG kvm $USER
   ```
   *Controleer met `kvm-ok` of KVM versnelling actief is.*

2. **Android SDK:** Zorg dat Android Studio is geïnstalleerd en dat het SDK pad op `~/Android/Sdk` staat.

---

### Fedora / RHEL

1. **Installeer KVM (Hardwareversnelling):**
   ```bash
   sudo dnf install qemu-kvm -y
   sudo usermod -aG kvm $USER
   ```

2. **Android SDK:** Zorg dat Android Studio geïnstalleerd is en dat een AVD (virtueel apparaat) genaamd `Pixel_9` is aangemaakt.

---

## ⚡ Hardware Acceleratie (NVIDIA / Hybrid Graphics)

Voor optimale prestaties schakelt `run_app.sh` automatisch NVIDIA PRIME render offloading in als er een NVIDIA GPU aanwezig is (`__NV_PRIME_RENDER_OFFLOAD=1`). 

Zorg in `~/.android/avd/<Jouw_AVD>.avd/config.ini` voor de volgende instellingen voor maximale snelheid en stabiliteit:
```ini
hw.gpu.enabled=yes
hw.gpu.mode=host
hw.ramSize=4096
```

---

## 📱 Functionaliteit (MVP)

* **Home:** Bekijk al je opgeslagen chase cards in een `LazyColumn`.
* **Zoeken:** Zoek offline in ruim 20.000 kaarten op naam.
* **Details:** Bekijk grote kaartafbeeldingen, set-informatie, nummering en voeg ze toe aan of verwijder ze uit je lokale lijst met één druk op de knop.
