# OBB Installer

Android app that installs an APK and copies its OBB into `Android/obb/<package>/`
without root, without a PC, and works on Android 11 → 14.

## How it works

1. **APK** — installed via the system `PackageInstaller` API. The user taps
   "Install" once on the standard Android confirmation screen.
2. **OBB** — the app uses `ACTION_OPEN_DOCUMENT_TREE` with
   `DocumentsContract.EXTRA_INITIAL_URI` pre-pointing to
   `Android/obb/<target.package>`. The picker opens already positioned on the
   right folder; the user taps "Use this folder" → "Allow", and the OBB is
   streamed there via SAF.
3. The chosen tree URI permission is persisted, so the next OBB for the same
   package is one tap away.

## Two modes

- **Bundled**: drop a `.apk` and a `.obb` into `app/src/main/assets/`. They get
  auto-detected on launch. Useful for distributing a one-shot installer for a
  specific game.
- **User pick**: leave `assets/` empty. The user picks the APK and OBB from
  their files.

## Build

CI builds an APK on every push (see `.github/workflows/build.yml`). Download
the artifact from the workflow run.

Local build:

```
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Requirements on the device

- Android 11+ (min SDK 30, target SDK 34).
- Allow "Install unknown apps" for OBB Installer (the app guides you to the
  setting on first launch).
- The OBB filename should already match `main.<versionCode>.<package>.obb` (or
  `patch.…`) — that is what the target game expects.

## Known limitations

- Some OEM skins (notably Samsung One UI 6) restrict the SAF picker further;
  if pre-navigation fails, the user has to pick `Android/obb/<package>` manually
  — which the picker may also block. In that rare case fall back to a Shizuku
  build (not included).
- This works as long as Google does not close the
  `EXTRA_INITIAL_URI`-into-`Android/obb` path. Tested through Android 14.
