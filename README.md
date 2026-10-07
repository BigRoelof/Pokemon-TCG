# Pocketdex (Android)

Pocketdex is een native Android app in Kotlin om je Pokémon TCG verzameling bij te houden: welke kaarten je hebt, op welke kaarten je nog jaagt (je *chase list*) en hoe je ze in je binders hebt gestoken.

De kaartgegevens komen uit de open-source dataset [pokemon-tcg-data](https://github.com/PokemonTCG/pokemon-tcg-data) en worden in een lokale Room database gezet. Zoeken en kaartdetails werken daardoor volledig offline; bij elke start worden alleen nieuwe of gewijzigde sets opgehaald. Prijzen (Cardmarket, in euro's) komen van [TCGdex](https://tcgdex.dev).

---

## ⬇️ Downloaden

Download de nieuwste `Pocketdex-<versie>.apk` bij [Releases](https://github.com/BigRoelof/Pokemon-TCG/releases) en open het bestand op je Android-telefoon (Android 7.0 of nieuwer). Sta zo nodig "Installeren uit onbekende bronnen" toe voor je browser of bestandsbeheer. Nieuwe versies installeer je gewoon over de oude heen; je collectie blijft bewaard.

---

## 📱 Functionaliteit

* **Collectie:** alle kaarten die je hebt, met totale waarde, sorteren (nieuwste, prijs, naam, set) en filteren op set. Kies je een set, dan zie je hoeveel van die set je compleet hebt.
* **Chase list:** de kaarten die je nog zoekt. Tik op de bal om een kaart te *vangen*: hij verhuist naar je collectie (met Ongedaan maken).
* **Binders:** maak zelf binders aan en vul ze met kaarten uit je collectie, op pagina's van 3 × 3 vakjes zoals een echte binder. Swipe door de pagina's, laat vakjes leeg voor kaarten die nog komen, en verplaats een kaart door hem lang in te drukken en op een ander vakje te tikken. Een kaart kan in meerdere binders zitten.
* **Zoeken:** zoek offline in ruim 20.000 kaarten op naam, setnaam, setcode of nummer (bijv. `charizard 151` of `obf 125`), en filter op set, type en zeldzaamheid.
* **Details:** grote kaartafbeelding, set-informatie, Cardmarket-prijs en knoppen om de kaart aan je collectie, chase list of binders toe te voegen.

---

## 🛠️ Technologie & Architectuur

* **Taal:** Kotlin
* **UI:** Jetpack Compose & Material 3, met een eigen Pokémon-stijl (vaste merkkleuren, Lilita One en Nunito)
* **Architectuur:** MVVM met één repository en handmatige dependency injection (`AppContainer`)
* **Lokale database:** Room (catalogus, collectie, chase list, binders en prijzencache)
* **Netwerk:** Retrofit + Gson (dataset-synchronisatie en prijzen)
* **Afbeeldingen:** Coil
* **Asynchroon:** Kotlin Coroutines & Flow / StateFlow
* **Build:** Gradle Kotlin DSL (`.kts`) + KSP

---

## 📁 Projectstructuur

```text
com.example.pokemontcg/
├── data/
│   ├── catalog/      # Synchronisatie van de kaartdataset (CatalogSync)
│   ├── database/     # Room entities, DAO's, migraties
│   ├── model/        # UI-modellen (Card, TrackedCard, sortering, binder-vakjes)
│   ├── preferences/  # Opgeslagen voorkeuren (sortering)
│   ├── prices/       # Cardmarket-prijzen via TCGdex
│   └── repository/   # PokemonRepository: het enige toegangspunt tot de data
├── ui/
│   ├── collection/   # Collectie-scherm
│   ├── chase/        # Chase list-scherm (vangen + ongedaan maken)
│   ├── binders/      # Binders-overzicht, binder-pagina's en kaartkiezer
│   ├── lists/        # Gedeeld kaartrooster, sortering en set-voortgang
│   ├── search/       # Zoeken met filters
│   ├── details/      # Kaartdetails, prijs en acties
│   ├── components/   # Herbruikbare onderdelen (Pokédex-header, kaarttegels, kiezers)
│   └── theme/        # Kleuren, lettertypes en thema
├── navigation/       # Navigation Compose routes
└── MainActivity.kt   # Startpunt van de app
```

Tests: `./gradlew testDebugUnitTest` (JVM) en `./gradlew connectedDebugAndroidTest` (op een toestel; let op: dit verwijdert daarna de app en zijn gegevens).

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
