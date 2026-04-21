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

Instrumentation tests run through the shared test runner used across
the boringdroid-owned apps:

```shell
m BoringdroidSettings
bash .claude/scripts/run-boringdroid-tests.sh
```
