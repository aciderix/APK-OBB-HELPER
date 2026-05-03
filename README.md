# OBB Installer Hub

Android app that installs an APK and copies its OBB into `Android/obb/<package>/`
on Android 13+ (Xiaomi/HyperOS included) without root, without a PC, without
Shizuku.

## How it works

The trick: the hub re-signs the game APK with the hub's own key and patches the
game's manifest to add `android:sharedUserId="com.aciderix.hub.shared"`. Android
then assigns the **same Linux UID** to the hub and to every game it installs.
Because the hub shares UID with the game, it has full read/write access to the
game's `Android/obb/<package>/` directory — same as if the game itself were
writing there.

Pipeline on each install:

1. Stage the picked APK to cache.
2. Patch the binary `AndroidManifest.xml` (in-process AXML parser) to add the
   shared UID attribute.
3. Re-sign the patched APK with the bundled keystore using Google's `apksig`.
4. Install via `PackageInstaller`. The user taps "Install" once.
5. Copy the OBB straight to `/sdcard/Android/obb/<package>/<filename>` via
   plain `FileOutputStream`. No SAF, no permission grant.

## Trade-offs (read this)

- **Game signature changes.** Updates from Play Store are no longer possible
  for any game installed through the hub. Re-installing a fresh game APK +
  OBB through the hub stays trivial.
- **Online anti-cheat blocks the modified signature.** Fine for solo /
  emulators / sideload-only games, fatal for online competitive titles.
- A handful of games verify their own signature in code; those refuse to run
  after re-signing. Detected at first launch (game crashes immediately).
- The hub itself has `sharedUserId` declared. **If you previously installed
  an older version of the hub without sharedUserId, you must uninstall it
  first** — Android refuses to migrate a UID on update.

## Two input modes

- **Bundled**: drop a `.apk` and a `.obb` into `app/src/main/assets/`. They
  get auto-detected on launch. Useful for distributing a one-shot installer
  for a specific game (in-app generator coming next).
- **User pick**: leave `assets/` empty. The user picks the APK and OBB from
  their files.

## Build

CI builds an APK on every push (see `.github/workflows/build.yml`). Download
the artifact from the workflow run.

Local:

```
./gradlew :app:assembleDebug
```

The signing keystore lives in `keys/hub.keystore` and is committed on purpose
— it has to be the same key that signs the hub APK and that the hub uses at
runtime to re-sign games. If you rotate it, every previously-installed game
must be re-installed.

## Requirements on the device

- Android 11+ (min SDK 30, target SDK 34).
- Allow "Install unknown apps" for the hub on first launch.
- The OBB filename should already match `main.<versionCode>.<package>.obb`
  (or `patch.…`) — that is what the target game expects.
