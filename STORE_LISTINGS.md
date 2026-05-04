# Store listings

Ready-to-paste content for every distribution channel.

The `fastlane/metadata/android/` tree at the root of the repo holds the
canonical metadata in the [Triple-T / fastlane format](https://docs.fastlane.tools/getting-started/android/setup/#fetch-your-app-metadata)
(used by F-Droid, Accrescent, IzzyOnDroid, etc.).

```
fastlane/metadata/android/
├── en-US/
│   ├── title.txt
│   ├── short_description.txt
│   ├── full_description.txt
│   ├── changelogs/1.txt
│   └── images/
│       ├── icon.png
│       └── phoneScreenshots/01_home.jpg
└── fr-FR/
    ├── title.txt
    ├── short_description.txt
    ├── full_description.txt
    ├── changelogs/1.txt
    └── images/
        ├── icon.png
        └── phoneScreenshots/01_accueil.jpg
```

---

## 📦 F-Droid

F-Droid is the gold-standard FOSS catalog. Inclusion process:

1. Open a Merge Request on https://gitlab.com/fdroid/fdroiddata adding
   `metadata/com.aciderix.obbinstaller.yml` (template below).
2. The maintainers do an editorial review (~days to weeks).
3. Once accepted, F-Droid will auto-rebuild every tagged release from
   source.

**fdroiddata YAML to submit (`metadata/com.aciderix.obbinstaller.yml`):**

```yaml
Categories:
  - System
License: MIT
SourceCode: https://github.com/aciderix/APK-OBB-HELPER
IssueTracker: https://github.com/aciderix/APK-OBB-HELPER/issues
Changelog: https://github.com/aciderix/APK-OBB-HELPER/releases

AutoName: OBB Installer

RepoType: git
Repo: https://github.com/aciderix/APK-OBB-HELPER.git

Builds:
  - versionName: '1.0'
    versionCode: 1
    commit: v0.1.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: '1.0'
CurrentVersionCode: 1
```

> The fastlane metadata in our repo is auto-picked up by F-Droid's
> indexer once the package is in fdroiddata. No extra step.

**Possible editorial concerns** (worth flagging upfront in the MR
description):

- The app re-signs third-party APKs to inject a ContentProvider. This
  is a legitimate sideload tool, comparable to App Manager or Aurora
  Store, but the editorial team may want a clear note in the
  description (already present in our `full_description.txt`).
- The keystore at `keys/hub.keystore` is intentionally checked in and
  used only for sharedUID-style re-signing of locally installed games.
  Mention it in the MR.

---

## 🛡️ IzzyOnDroid

[IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/info) mirrors and
auto-builds many community apps with looser editorial criteria than
F-Droid. Faster path to publication.

Submission: https://gitlab.com/IzzyOnDroid/repo/-/blob/master/CONTRIBUTING.md

The same fastlane metadata in our repo is consumed automatically.

---

## 📦 Aptoide

Web form at https://en.aptoide.com/uploader

**Title:**

```
OBB Installer
```

**Short description (≤ 80 chars):**

```
Sideload Android games (APK + OBB) in one tap. No root, no PC, no Shizuku.
```

**Long description:** copy from `fastlane/metadata/android/en-US/full_description.txt`.

**Category:** Tools

**Tags:** `apk-installer`, `obb`, `sideload`, `package-installer`,
`android-game`, `xapk`, `no-root`

**Icon:** `fastlane/metadata/android/en-US/images/icon.png`

**Screenshots:** `fastlane/metadata/android/en-US/images/phoneScreenshots/`

**APK:** download `ObbInstaller-v0.1.0.apk` from the GitHub release and
upload it.

---

## 📦 APKPure

Web form at https://apkpure.net/account/upload-app

Same content as Aptoide. APKPure also asks for:

**Website:** `https://github.com/aciderix/APK-OBB-HELPER`

**Privacy policy URL:** if asked, point to a stable `PRIVACY.md` in the
repo (the app does not collect any data — say so explicitly).

---

## 🌐 Direct download landing

If you want a public download page outside any store:

- The latest APK is permanently at:
  `https://github.com/aciderix/APK-OBB-HELPER/releases/latest/download/ObbInstaller-v0.1.0.apk`
  (substitute the actual asset name if it changes).
- The `latest` tag of the GitHub release page is itself a stable URL:
  `https://github.com/aciderix/APK-OBB-HELPER/releases/latest`

A static landing page on Netlify / Cloudflare Pages can simply iframe
or link to those URLs.

---

## ✍️ When you cut a new version

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add a new `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`
   file with the release notes.
3. Tag and push:

   ```bash
   git tag -a v0.2.0 -m "v0.2.0"
   git push origin v0.2.0
   ```

4. CI builds, signs, and publishes the GitHub Release automatically.
5. F-Droid / IzzyOnDroid will rebuild from the tag on their next index run.
6. For Aptoide / APKPure, re-upload the new APK manually.
