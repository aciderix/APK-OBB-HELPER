# Privacy Policy

OBB Installer does not collect, store, or transmit any personal data.

- The app does not contact any backend server.
- Files you pick (APKs and OBBs) are processed entirely on-device.
- No analytics, no telemetry, no third-party SDK is included.
- The `INTERNET` permission is declared in the manifest but is not used
  at runtime in v0.1.x. It is reserved for an optional future in-app
  updater that would only ever fetch this app's own GitHub Releases
  asset.
- The `REQUEST_INSTALL_PACKAGES` permission is required to use the
  Android `PackageInstaller` API on the user's behalf.
- The `QUERY_ALL_PACKAGES` permission is used solely to verify that a
  freshly-installed game has joined the hub's UID and to surface
  meaningful error messages when it has not. No package list is sent
  anywhere.

The patched APK that the hub installs contains a small open-source
ContentProvider (compiled from the `:bootstrap` Gradle module in this
repo). That provider runs only inside the patched game's process and
only does one thing: copy any `.obb` asset bundled inside the APK to
the game's private `Android/obb/<package>/` folder on first launch.

Source code (MIT): https://github.com/aciderix/APK-OBB-HELPER

If you have questions or concerns, please open an issue on GitHub.
