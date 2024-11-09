# OnePlus Battery Waster

[![API](https://img.shields.io/badge/API-33%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=33)
[![Releases](https://img.shields.io/github/downloads/Xposed-Modules-Repo/be.mygod.oplus.batterywaster/total.svg)](https://github.com/Xposed-Modules-Repo/be.mygod.oplus.batterywaster/releases)
[![Language: Java](https://img.shields.io/github/languages/top/Xposed-Modules-Repo/be.mygod.oplus.batterywaster.svg)](https://github.com/Xposed-Modules-Repo/be.mygod.oplus.batterywaster/search?l=java)

Disable OnePlus battery saving features that hurt user experience (Android 13+).

Only tested on OxygenOS 14.
File a PR if anything is not working.

## List of patches included

* `android`:
  - Allow background app startup (by e.g. FCM) in game mode.
  - Prevent unnecessary notification removals.
  - Patch permissionless dangerous system service calls.
* `com.android.settings`:
  - Restore AOSP notification settings.
  - Allow disabling/modifying useless notifications (e.g. battery alerts).
    Also requires patching `android`.
* `com.oplus.battery`:
  - Disable all poorly-implemented thermal control features.
