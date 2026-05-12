# BoringdroidSettings

The settings app for boringdroid. Injects two entries into the stock
AOSP Settings dashboard via `EXTRA_SETTINGS` so users can toggle
boringdroid-specific modes without a standalone Settings app launcher.

## Build

`BoringdroidSettings` ships as an AOSP module — the build is Soong.
From the AOSP root:

```shell
source build/envsetup.sh
lunch boringdroid_x86_64-userdebug
m BoringdroidSettings
```

`Android.bp` declares the module as `android_app` with `platform_apis`
and the platform certificate so the EXTRA_SETTINGS entries resolve
against the system Settings app.

## Test

Instrumentation tests live under `app/src/androidTest/` and build as
`BoringdroidSettingsTests.apk`:

```shell
m BoringdroidSettings BoringdroidSettingsTests
bash .claude/scripts/run-boringdroid-settings-tests.sh
```

The script builds both APKs, installs them against a running
`boringdroid_x86_64-userdebug` emulator, drops adb to shell uid, and
runs every class in `com.boringdroid.settings`. The suite covers the
App display behavior screen (filter chips, multi-select bulk bar,
single-app mode sheet) and the About Boringdroid screen (hero card
action buttons, Project / Authors / System cards).

Any change touching this module must run the suite and confirm green
before being committed (see [boringdroid/CLAUDE.md](../../../../boringdroid/CLAUDE.md#verification-for-boringdroidsettings)).
