# Store submissions — copy-paste cheat sheet

Concrete forms / issues / MRs to file for each store, with the exact text
to paste. The canonical metadata lives in `fastlane/metadata/android/`
and is auto-picked up by F-Droid-style stores.

---

## 1. 🛡️ IzzyOnDroid (recommended first stop — fast review)

**Form:** open a new "Inclusion Request" issue at:
https://gitlab.com/IzzyOnDroid/repo/-/issues/new

**Title:**
```
Inclusion request: OBB Installer (com.aciderix.obbinstaller)
```

**Body (paste as-is):**
```
**App information**

- Package: `com.aciderix.obbinstaller`
- Source: https://github.com/aciderix/APK-OBB-HELPER
- Latest release: https://github.com/aciderix/APK-OBB-HELPER/releases/latest
- License: MIT
- Categories: System / Tools
- Description: see fastlane metadata in the repo
  (https://github.com/aciderix/APK-OBB-HELPER/tree/main/fastlane/metadata/android/en-US)

**Build**

- CI publishes a signed APK on every `v*` tag. The release asset is
  named `ObbInstaller-vX.Y.Z.apk`. The signing key is committed at
  `keys/hub.keystore` so the runtime patcher inside the app can re-sign
  game APKs with the same identity (used by an injected ContentProvider
  to extract bundled OBB into the game's own private folder on first
  launch). The key is debug-grade, MIT-licensed, intentionally public.

**AntiFeatures**

- None at the framework level. The app:
  - is fully open source (MIT)
  - does not require root
  - does not call any network at runtime (only INTERNET permission for
    optional future updater, currently unused)
  - does not embed proprietary blobs or non-free dependencies

**What it does**

Sideloads Android games shipped as APK + OBB without root, PC, or
Shizuku. The OBB is embedded inside the patched APK and a tiny
ContentProvider (compiled from the open `:bootstrap` Gradle module)
copies it into `Android/obb/<package>/` on first launch from inside
the game's process — bypassing scoped-storage restrictions on
Android 11+ without any privileged API.

Thanks for considering!
```

The IzzyOnDroid maintainers usually pick up these requests within a few
days and reply with any follow-up questions. Once accepted, they
auto-build and publish on every tagged release.

---

## 2. 🛒 Aptoide

**Form:** https://en.aptoide.com/uploader (create a free account first)

| Field | Value |
|---|---|
| **App name** | OBB Installer |
| **Package** | com.aciderix.obbinstaller |
| **Category** | Tools |
| **Sub-category** | System |
| **Tags** | `apk-installer`, `obb`, `sideload`, `xapk`, `package-installer`, `android-game`, `no-root` |
| **Short description (≤80 chars)** | Sideload Android games (APK + OBB) in one tap. No root, no PC, no Shizuku. |
| **Description (long)** | paste from `fastlane/metadata/android/en-US/full_description.txt` |
| **Website** | https://apkobb.fr/ |
| **Source code** | https://github.com/aciderix/APK-OBB-HELPER |
| **Email** | (your support email) |
| **Icon** | upload `fastlane/metadata/android/en-US/images/icon.png` |
| **Screenshot 1** | upload `fastlane/metadata/android/en-US/images/phoneScreenshots/01_home.jpg` |
| **APK** | download from the latest release, e.g. `ObbInstaller-v0.1.0.apk` |

**Localization (FR):**
- Short / long descriptions in `fastlane/metadata/android/fr-FR/`
- Add a French entry once the English one is approved (Aptoide allows
  per-language listings).

**Aptoide also offers** a "Roboto" auto-publish from Git tags. You can
plug it in once the manual upload is approved.

---

## 3. 🛒 APKPure

**Form:** https://apkpure.net/account/upload-app (create a free account)

| Field | Value |
|---|---|
| **App name** | OBB Installer |
| **Package** | com.aciderix.obbinstaller |
| **Category** | Tools |
| **Short description** | Sideload Android games (APK + OBB) in one tap. No root, no PC, no Shizuku. |
| **Description** | paste from `fastlane/metadata/android/en-US/full_description.txt` |
| **Website** | https://apkobb.fr/ |
| **Privacy policy** | https://github.com/aciderix/APK-OBB-HELPER/blob/main/PRIVACY.md |
| **Email** | (your support email) |
| **Icon / screenshot / APK** | same files as Aptoide |

> ⚠️ APKPure asks for a Privacy Policy URL even though the app sends
> nothing online. Add a one-paragraph `PRIVACY.md` at the repo root
> (template at the bottom of this file) so the URL above resolves.

---

## 4. 📦 F-Droid (bigger lift, longest review)

**Form:** open a Merge Request at
https://gitlab.com/fdroid/fdroiddata

Add a file `metadata/com.aciderix.obbinstaller.yml`:

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

**MR title:** `New app: com.aciderix.obbinstaller (OBB Installer)`

**MR description:**
```
This MR adds OBB Installer, a no-root / no-PC sideload tool for
Android games shipped as APK + OBB. Source: https://github.com/aciderix/APK-OBB-HELPER

The fastlane metadata directory in the upstream repo provides
descriptions, icon and screenshots in en-US and fr-FR.

The app re-signs picked APKs with a key committed in `keys/hub.keystore`
so an injected ContentProvider (from the open :bootstrap Gradle module)
can copy the bundled OBB into the game's own Android/obb folder at
first launch, from inside the game's process — bypassing scoped-storage
restrictions without any privileged API.

License: MIT. No tracking, no online calls at runtime.
```

F-Droid review can ping back with notes about the embedded keystore,
the AXML/ELF in-process patchers, etc. The full design is documented
in the upstream README and the editorial team usually approves quickly
once the rationale is clear.

---

## 📜 PRIVACY.md template (drop at repo root for APKPure)

```markdown
# Privacy Policy

OBB Installer does not collect, store or transmit any personal data.

- The app does not contact any backend.
- Files you pick (APKs and OBBs) are processed entirely on-device.
- No analytics, no telemetry, no third-party SDK.
- The `INTERNET` permission is declared in the manifest but is not used
  at runtime. It is reserved for an optional future in-app updater that
  would only ever fetch this app's own GitHub Releases asset.

Source code: https://github.com/aciderix/APK-OBB-HELPER

If you have questions: open an issue on GitHub.
```
```

---

## ⏱️ Suggested order

1. **Today** — IzzyOnDroid issue + APKPure form. ~30 min total.
2. **+1 week** — Aptoide upload once IzzyOnDroid is live (using their
   build as social proof in the description).
3. **+2 weeks** — F-Droid MR once a v0.2.0 / v1.0 has shipped (gives a
   tag history that satisfies their "active maintenance" check).
