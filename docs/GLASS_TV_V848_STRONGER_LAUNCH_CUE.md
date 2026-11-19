# GlassTV V8.4.8 — stronger premium OK launch cue

Owner feedback on V8.4.7: OK animation now works but is still too subtle at sofa viewing distance. V8.4.7 successfully preserved Satin Light Bridge, Prayer Time, Weather/Windy Clouds, and HOME.

## Scoped animation change

Compared with V8.4.7:
- Press compression: 0.978 -> **0.95** on selected SmartTube dock card.
- Dim of rest of Home: max 17% -> **28%**; selected card is excluded.
- Silver border alpha: max 43% -> **74%** and 1.25dp width.
- One **short, non-glowing** silver satin glint crosses the upper card edge before launch.
- 180ms is now the default. 0ms V8.4.6 baseline and 120ms remain available for A/B via `glass_tv_launch_cue_ms` when starting the isolated activity.
- Timing stays based on Choreographer and uptime, not global Android animator scale. No Android settings changed.
- Other apps launch immediately as previously; no edits to Satin Light Bridge, Prayer Time, Weather, artwork, Home, or manifest.

Release suffix: `.referencev781v848`, versionCode 32, unique from V8.4.5/V8.4.6/V8.4.7. APK build uses GitHub Actions and does **not** install to the TV or set HOME.

## Physical TV validation

To keep the original, approved visual assets, copy the external files from the currently working V8.4.7 package to V8.4.8 after sideloading. Launch the new package explicitly instead of setting HOME immediately.

For timing A/B (only after installing experimental APK):
```powershell
.\adb.exe shell am start -n com.gathin.tvhome.referencev781v848/com.gathin.tvhome.MainActivity --ei glass_tv_launch_cue_ms 120
.\adb.exe shell am start -n com.gathin.tvhome.referencev781v848/com.gathin.tvhome.MainActivity --ei glass_tv_launch_cue_ms 180
```

Compare black gap, visibility from sofa, launch response and no ghost dim / stale focus after HOME/Back. Keep V8.4.7 as known-good Home until the owner confirms this experiment.

## Cleanup of old installed test launchers

An Android TV package cannot be uninstalled remotely by editing GitHub. Identify installed packages on the actual TV and the resolved HOME component by read-only ADB before removal. **Never** remove the active HOME or vendor/stock launcher. Do not use wildcard `pm uninstall`. Once the device's exact installed custom package list is known, selectively uninstall explicitly identified obsolete versions, keeping at least one known-good rollback until V8.4.8 passes physical tests.
