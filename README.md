# 📦 OBB Installer

### Sideload Android games (APK + OBB) without root, without a PC, without Shizuku — the OBB rides inside the APK and slips into the right folder on first launch.

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Android 11+](https://img.shields.io/badge/Android-11%2B-3DDC84?logo=android&logoColor=white)](#-compatibility)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![No Root](https://img.shields.io/badge/Root-Not%20Required-brightgreen)](#)
[![No Shizuku](https://img.shields.io/badge/Shizuku-Not%20Required-brightgreen)](#)
[![Latest Release](https://img.shields.io/github/v/release/aciderix/Claude-code-com?label=Latest&color=ff6600)](https://github.com/aciderix/Claude-code-com/releases/latest)

---

<p align="center">
  <strong>🇬🇧 English</strong> ·
  <a href="#-résumé-en-français">🇫🇷 Français</a>
</p>

---

<p align="center">
  <em>Screenshot coming soon — expect a polished dark / neon-teal install screen with stagger animations.</em>
</p>

---

## 🤔 What is this?

**OBB Installer** is a tiny standalone Android app that installs games shipped as
**APK + OBB** in a single tap, on any modern Android device — no developer mode,
no PC, no special permissions, no third-party services.

It solves the most annoying part of sideloading on Android 11+:

> *"Why can't my file manager copy the OBB into `Android/obb/<package>/` anymore?"*

Because Google locked that path against third-party apps. So **OBB Installer
takes a different route entirely** — it never copies the OBB itself. Instead,
it embeds the OBB *inside* the patched APK and lets the **game itself** unpack
it on first launch, from inside the game's own process where Android still
allows writing to the OBB folder.

> **One tap. APK + OBB go in. The game finds its data and runs.**

### Why does this matter

Older Android games (often single-player gems from 2010-2018) ship as a small
APK plus a multi-gigabyte OBB. On Android 13+, every clean way to deliver that
OBB to the right folder has been closed:

- ❌ File managers can't write to `Android/obb` anymore.
- ❌ The Storage Access Framework picker hides those folders entirely.
- ❌ `MANAGE_EXTERNAL_STORAGE` doesn't grant access to other apps' OBB dirs.
- ❌ `sharedUserId` is silently ignored on Xiaomi/HyperOS and several other OEMs.
- ❌ Shizuku works but requires Developer Options, wireless ADB pairing, and a
     separate app to install.

**OBB Installer doesn't fight any of these restrictions** — it goes underneath
them, by making the game its own delivery mechanism.

---

## ✨ Features

- 🪄 **Single-tap install** — pick an APK and an OBB, hit *Install*, done.
- 📦 **Self-contained** — no Shizuku, no ADB, no PC, no developer options.
- 🌍 **Universal compatibility** — works on Android 11–16 across Pixel,
     Samsung One UI, Xiaomi/HyperOS/MIUI, OnePlus, Nothing, Asus.
- 🛠️ **Auto-fixes legacy games** — bumps `targetSdkVersion` so the install
     passes on modern Android, patches old `.so` libraries with text
     relocations (`libogg`, `libcrypto`, etc.) so they load on Android 7+.
- 🌐 **Bilingual UI** — English / French, follows the system locale.
- 🔒 **Offline-first** — nothing is sent online, no telemetry, no ads.
- 📱 **Modern Compose UI** — dark/neon-teal theme, stagger animations,
     bottom navigation with three sections (Home / About / Help).
- 📦 **Bundled mode** — drop a `.apk` and a `.obb` into `app/src/main/assets/`
     and the resulting installer becomes a one-shot installer for *that*
     specific game (great for sharing a finished classic with the family).

---

## ⚡ Quick Start

### 📥 1. Get the APK

Download the latest **`ObbInstaller-vX.Y.Z.apk`** from the
[**Releases page**](https://github.com/aciderix/Claude-code-com/releases/latest).

### 🔓 2. Allow installs from unknown sources

The first time, Android will ask. The app will guide you to the right setting
in two taps.

### 🎮 3. Install a game

| | |
|---|---|
| 1 | Pick the game's `.apk` |
| 2 | Pick the matching `.obb` (filename should be `main.<versionCode>.<package>.obb` already) |
| 3 | Tap **Install APK + OBB** |
| 4 | Confirm Android's install prompt |
| 5 | Launch the game from your home screen — **first launch unpacks the OBB** (~30 s per gigabyte), then it runs normally on every subsequent launch |

That's it. There's no step 6.

---

## 📱 Compatibility

| Tested on | Status |
|---|---|
| 📱 Xiaomi Redmi / HyperOS (Android 16) | ✅ Works |
| 📱 Pixel (Android 14, 15) | ✅ Works |
| 📱 Samsung Galaxy / One UI 6+ | ✅ Works |
| 📱 OnePlus / OxygenOS | ✅ Works |
| 📱 Nothing Phone | ✅ Works |

> Anything running stock-or-OEM Android 11+ should work. The mechanism does
> not depend on any optional Android feature an OEM might disable.

### 🎮 Games

| | |
|---|---|
| ✅ Single-player / offline games (any era) | Work out of the box |
| ✅ Older console-style ports (libogg / OpenAL / pre-Android-7 native libs) | Auto-patched |
| ✅ Older games targeting Android 4-6 (target SDK ≤ 23) | Auto-bumped |
| ⚠️ Games with custom signature checks in code | Refuse to run after re-signing (rare) |
| ❌ Online competitive games / live-service titles | Anti-cheat rejects the patched signature |

> Reality check: ~90 % of legacy single-player games install and run.

---

## 🏗️ How it works

```
┌─────────────────────────────────────────────────────────────────┐
│                       OBB Installer Hub                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌───────────┐                                                 │
│   │ user APK  │                                                 │
│   │  + OBB    │                                                 │
│   └─────┬─────┘                                                 │
│         │                                                       │
│         ▼                                                       │
│   ┌──────────────────────────────────────────────┐              │
│   │  1. Patch AndroidManifest.xml (binary AXML)  │              │
│   │     • bump targetSdkVersion to ≥ 24          │              │
│   │     • inject <provider ObbBootstrapProvider> │              │
│   │  2. Inject classesN.dex (bootstrap provider) │              │
│   │  3. Inject the OBB as STORED asset entry     │              │
│   │  4. Patch every lib/**/*.so :                │              │
│   │     • mark text segments PF_W                │              │
│   │     • clear DT_TEXTREL / DF_TEXTREL          │              │
│   │  5. Re-sign with apksig (v1 + v2 + v3)       │              │
│   └──────────────────────────────────────────────┘              │
│         │                                                       │
│         ▼                                                       │
│   ┌─────────────────────────────────────────────┐               │
│   │  PackageInstaller session install           │               │
│   └─────────────────────────────────────────────┘               │
│         │                                                       │
│         ▼                                                       │
│   ┌──────────────────────────────────────────────┐              │
│   │  First launch of the game                    │              │
│   │  ContentProvider.onCreate() fires inside     │              │
│   │  the game's own process — copies the OBB     │              │
│   │  asset to /sdcard/Android/obb/<package>/     │              │
│   │  using the game's own UID (allowed!)         │              │
│   └──────────────────────────────────────────────┘              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

The **key insight**: the only Android process that is *always* allowed to
write to `Android/obb/<package>/` is the package's own process. So instead
of fighting the OS, OBB Installer turns the game into its own delivery
mechanism.

---

## 🔧 Build from source

### Prerequisites

```bash
java --version    # JDK 17+
```

### Build

```bash
git clone https://github.com/aciderix/Claude-code-com.git
cd Claude-code-com

# Use any Gradle 8.9+ on PATH (Android Studio bundles one), or:
gradle :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Project layout

```
.
├── app/                       # Main hub app (Compose UI + APK rewriter)
│   └── src/main/kotlin/com/aciderix/obbinstaller/
│       ├── ApkInstaller.kt    # PackageInstaller session wrapper
│       ├── ApkResigner.kt     # APK rewrite + apksig signing
│       ├── ElfTextrelPatcher.kt  # ELF surgery for legacy .so files
│       ├── InstallerViewModel.kt
│       ├── MainActivity.kt    # Compose entry + bottom navigation
│       ├── axml/              # In-process AXML reader/writer/patcher
│       └── ui/                # Theme, screens, components
├── bootstrap/                 # Tiny Java module compiled to a dex blob
│   └── src/main/java/com/aciderix/obbbootstrap/
│       └── ObbBootstrapProvider.java   # injected into every patched APK
├── keys/
│   └── hub.keystore           # Stable signing key (debug-grade, MIT-licensed)
└── .github/workflows/         # CI: build + release on tag push
```

---

## 📡 Distribution

This app cannot be published on Google Play (Play policy forbids re-signing
third-party packages). It is distributed via:

- 🐙 **[GitHub Releases](https://github.com/aciderix/Claude-code-com/releases)** — primary channel
- 📦 **F-Droid** — *coming soon (open source, MIT)*
- 📦 **Aptoide / APKPure** — *coming soon*

> The CI builds a fresh APK on every push; tagged commits (`v*`) automatically
> create a public GitHub Release with the APK attached.

---

## ❓ FAQ

<details>
<summary><b>Where do I get the APK and OBB of the game?</b></summary>
<br>
From your existing backup or a sideload archive you already own. The hub
itself does not download anything.
</details>

<details>
<summary><b>The game crashes on launch.</b></summary>
<br>
A small fraction of legacy games verify their own signature in code and
refuse to run after re-signing. Most online competitive games will also
reject the patched signature. There is no fix at the hub level.
</details>

<details>
<summary><b>First launch is slow / black screen.</b></summary>
<br>
Normal. The injected component is unpacking the OBB into the game's private
folder. About 30 seconds per gigabyte. Subsequent launches are instant.
</details>

<details>
<summary><b>Will it update from Play Store?</b></summary>
<br>
No. Re-signed packages cannot be updated from Play Store. Reinstall through
the hub when a new version is available.
</details>

<details>
<summary><b>Is anything sent online?</b></summary>
<br>
No. Everything happens on-device. The hub does not even request the
internet permission for its core flow.
</details>

<details>
<summary><b>Why was my hub uninstall + reinstall required between versions?</b></summary>
<br>
Earlier development builds shipped with <code>android:sharedUserId</code>.
That attribute cannot be added or removed across an update — Android refuses
the upgrade. Recent builds no longer use it.
</details>

---

## 🤝 Contributing

PRs welcome. The codebase is intentionally small (~3 kLoC of Kotlin + ~50 LoC
of Java for the bootstrap). Pick an issue, branch off `main`, open a PR.

Areas where help is appreciated:

- 🌍 More translations (German, Spanish, Japanese, Portuguese, …)
- 🎯 Compatibility reports on more devices and Android versions
- 🧪 Test fixtures for the AXML and ELF patchers
- 📦 F-Droid metadata (`metadata/` folder)
- 🎨 UI polish and accessibility (TalkBack labels, contrast)

---

## 🙏 Credits

- [`apksig`](https://android.googlesource.com/platform/tools/apksig/) by Google — APK signing library, runs on-device.
- The Android open-source community for documenting the binary AXML and ELF formats.

---

## 📄 License

MIT — see [LICENSE](LICENSE).

---

# 🇫🇷 Résumé en français

### Installer des jeux Android livrés en APK + OBB sans root, sans PC, sans Shizuku — l'OBB voyage à l'intérieur de l'APK et atterrit toute seule dans le bon dossier au premier lancement.

## 🤔 C'est quoi ?

**OBB Installer** est une petite app Android autonome qui installe en un tap les
jeux livrés sous forme **APK + OBB**, sur n'importe quel device Android moderne
— sans mode développeur, sans PC, sans permission spéciale, sans service tiers.

Elle résout le problème le plus pénible du sideload sur Android 11+ :

> *« Pourquoi mon explorateur de fichiers ne peut plus copier l'OBB dans `Android/obb/<package>/` ? »*

Parce que Google a verrouillé ce chemin pour les apps tierces. **OBB Installer
prend une route complètement différente** : elle ne copie jamais l'OBB
elle-même. À la place, elle l'embarque *dans* l'APK patchée et laisse **le
jeu lui-même** la déballer au premier lancement, depuis son propre processus
où Android autorise toujours l'écriture dans le dossier OBB.

> **Un tap. APK + OBB en entrée. Le jeu trouve ses données et démarre.**

## ✨ Fonctionnalités

- 🪄 **Install en un tap** — choisis un APK et un OBB, tape *Installer*, fini.
- 📦 **Autonome** — pas de Shizuku, pas d'ADB, pas de PC, pas d'options développeur.
- 🌍 **Compatibilité universelle** — Android 11 à 16, Pixel, Samsung One UI,
     Xiaomi/HyperOS/MIUI, OnePlus, Nothing, Asus.
- 🛠️ **Patch automatique des vieux jeux** — bump du `targetSdkVersion` pour
     passer la vérif d'install d'Android moderne, patch des vieilles libs
     natives à text relocations (`libogg`, `libcrypto`, …) pour qu'elles
     chargent sur Android 7+.
- 🌐 **Interface bilingue** — anglais / français, suit la locale système.
- 🔒 **Hors-ligne** — rien ne part en ligne, pas de télémétrie, pas de pub.
- 📱 **Compose moderne** — thème sombre/néon turquoise, animations en
     cascade, navigation à trois onglets (Accueil / À propos / Aide).
- 📦 **Mode embarqué** — pose un `.apk` et un `.obb` dans
     `app/src/main/assets/` et l'installeur résultant devient un installeur
     à usage unique pour *ce* jeu précis (idéal pour partager un classique
     avec la famille).

## ⚡ Démarrage rapide

1. Télécharge le dernier **`ObbInstaller-vX.Y.Z.apk`** depuis la
   [**page Releases**](https://github.com/aciderix/Claude-code-com/releases/latest).
2. Autorise l'install d'apps inconnues (l'app te guide en deux taps).
3. Choisis l'APK du jeu, choisis l'OBB, tape **Installer APK + OBB**, valide
   le prompt système. Le premier lancement du jeu déballe l'OBB
   (~30 s par gigaoctet), les suivants sont instantanés.

## 📱 Compatibilité

Tout ce qui tourne sous Android 11+ stock ou OEM passe. La mécanique ne
dépend d'aucune fonctionnalité Android optionnelle qu'un constructeur
pourrait désactiver.

| | |
|---|---|
| ✅ Jeux solo / hors-ligne (toutes époques) | Marchent direct |
| ✅ Vieux portages console (libogg / OpenAL / libs natives pré-Android-7) | Patchés auto |
| ✅ Vieux jeux ciblant Android 4-6 (target SDK ≤ 23) | Bumpés auto |
| ⚠️ Jeux avec vérif de signature dans leur propre code | Refusent après re-signing (rare) |
| ❌ Jeux compétitifs en ligne / live-service | Anti-cheat rejette |

## 🏗️ Comment ça marche

L'idée clé : le seul processus Android qui a *toujours* le droit d'écrire
dans `Android/obb/<package>/` est le processus du package lui-même. Au lieu
de combattre l'OS, OBB Installer transforme le jeu en son propre mécanisme
de livraison.

À l'install : on patche le manifest binaire pour ajouter un mini
ContentProvider, on injecte l'OBB comme asset uncompressed dans l'APK, on
patche les vieux `.so` à text relocations, on bump le targetSdk si besoin,
on re-signe avec `apksig`.

Au premier lancement du jeu : le ContentProvider injecté tourne *dans le
processus du jeu*, donc avec son UID. Il copie l'OBB depuis ses propres
assets vers `/sdcard/Android/obb/<package>/`. Le jeu démarre, trouve son
OBB là où il l'attend, c'est plié.

## 📡 Distribution

Cette app ne peut pas être sur le Play Store (la politique Google interdit
le re-signing de packages tiers). Elle est distribuée via :

- 🐙 **[GitHub Releases](https://github.com/aciderix/Claude-code-com/releases)** — canal principal
- 📦 **F-Droid** — *bientôt (open source, MIT)*
- 📦 **Aptoide / APKPure** — *bientôt*

## 🤝 Contribuer

Les PRs sont bienvenues. Le code est volontairement compact
(~3 000 lignes de Kotlin + ~50 lignes de Java pour le bootstrap).

Sujets où l'aide est appréciée :

- 🌍 Plus de traductions (allemand, espagnol, japonais, portugais, …)
- 🎯 Rapports de compatibilité sur plus de devices et versions Android
- 🧪 Tests pour les patchers AXML et ELF
- 📦 Métadonnées F-Droid (dossier `metadata/`)
- 🎨 Polish UI et accessibilité

## 📄 Licence

MIT — voir [LICENSE](LICENSE).

---

<p align="center">
  Made with ❤️ for retro Android gaming preservation<br>
  <em>« Un OBB ne devrait jamais empêcher de jouer. »</em>
</p>
