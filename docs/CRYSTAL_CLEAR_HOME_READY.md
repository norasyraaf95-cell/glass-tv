# Crystal Clear V2 — Home-ready on MiTV-AESP0

This branch modifies **only HOME eligibility, distinct release identity and CI checks** from the
approved Crystal Clear V2 preview. It intentionally makes NO visual or movement changes.

## Packages
- Working V8.5.1 Immediate (preserve): `com.gathin.tvhome.referencev781v851immediate`
- Crystal Clear V1 preview (preserve): `com.gathin.tvhome.crystalclearpreview`
- Crystal Clear V2 preview (preserve): `com.gathin.tvhome.crystalclearv2`
- Crystal Clear V2 HOME-ready: `com.gathin.tvhome.crystalclearhome`

## Setup order using Bugjaeger (no root, no OEM disabling)
1. Download `GlassTV_CrystalClear_Home.apk` from the successful GitHub Actions artifact.
2. Install it while leaving the existing packages enabled.
3. Copy **only** `config.json` and the user's exact `wallpaper.jpg` from V2 preview:
   ```sh
   mkdir -p /sdcard/Android/data/com.gathin.tvhome.crystalclearhome/files
   cp /sdcard/Android/data/com.gathin.tvhome.crystalclearv2/files/config.json /sdcard/Android/data/com.gathin.tvhome.crystalclearhome/files/config.json
   cp /sdcard/Android/data/com.gathin.tvhome.crystalclearv2/files/wallpaper.jpg /sdcard/Android/data/com.gathin.tvhome.crystalclearhome/files/wallpaper.jpg
   ```
   Weather and prayer data are fetched/cached in the new package. Do not copy `windy_clouds.mp4`.
4. Test without changing Home:
   ```sh
   am start -n com.gathin.tvhome.crystalclearhome/com.gathin.tvhome.MainActivity
   ```
   Check all five app tiles, SmartTube launch and back.
5. Before changing Home, **capture previous Home**:
   ```sh
   cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
   ```
6. After confirming the new launcher works, user explicitly chooses to switch:
   ```sh
   cmd package set-home-activity --user 0 com.gathin.tvhome.crystalclearhome/com.gathin.tvhome.MainActivity
   ```
   Verify resolved activity with the command above and press the physical remote Home key. An OEM
   launcher might still intercept Home even if PackageManager reports success: do not disable
   system launchers or accessibility services without assessing the device.
7. Reversible: if prior default was the working V8.5.1 Immediate, restore with
   ```sh
   cmd package set-home-activity --user 0 com.gathin.tvhome.referencev781v851immediate/com.gathin.tvhome.MainActivity
   ```
   If previous resolver output names another HOME component, use that exact prior component.

No command here uninstalls, disables, clears data from or modifies other apps.
