# LOCKED — Crystal Clear dock movement: Option B / Magnetic Spotlight

**Owner selection:** B (2026-11-24). **Status:** motion reference locked. **DO NOT build or modify APK just to make this reference.**

## Authoritative visual reference

- Video filename: `GlassTV_Movement_02_Magnetic_Spotlight.mp4`
- SHA-256: `3be54adc9c03fc7f5965c4ee0b9fa0a1f2864b1a5669ecfb9199aecac9294537`
- Reference format: 1280 x 720, 30 fps, 231 frames, 7.70 seconds.
- This video was rendered from the *same approved Crystal Clear screenshot/background*, with animation overlays. It depicts a **static visual mockup**, not live weather or a runnable Android UI.
- Retain the reference MP4 alongside this specification when implementing. SHA-256 identifies the exact file; prose alone is not a substitute for visual frame comparisons.

## LOCKED timing / motion

1. **470 milliseconds per focus transition** between adjacent dock tiles, with no bounce or overshoot.
2. Position interpolation: quintic smootherstep `s(t) = 6t^5 - 15t^4 + 10t^3`, t clamped to [0,1]. This gives zero velocity at the start and end.
3. The source tile's soft white rim/interior light fades out while the destination tile's same lighting fades in. The cards, logos and labels themselves stay stationary, constant size and sharp.
4. A **wide, very diffuse pool of icy-white/aqua light under the dock** moves continuously under the cards from source centre to target centre using the same eased timing.
5. No visible moving ring, scanning line, narrow travelling strip, lens flare, bouncy zoom, wobble, card translation or trailing streak. The sense of motion should come from *smooth luminance migration*.
6. Soft settled light remains under the focused app. The transition looks continuous in both directions and when DPAD inputs reverse.
7. Keep white dock typography, native frosted/transparent cards, crystal edges, the owner's exact alpine wallpaper, upper clock, prayer and weather widgets unchanged. Weather click remains disabled; no windy video.

## Sequence represented by the video

Initial YouTube focus, then:
- t=0.68s: YouTube → Netflix
- t=1.80s: Netflix → Prime Video
- t=2.93s: Prime Video → Spotify
- t=4.05s: Spotify → Apps
- t=5.20s: Apps → Spotify
- t=6.30s: Spotify → Prime Video

Each leg lasts 0.47s. Motion is intentionally not an always-running animation.

## Engineering constraints (for later, NOT part of this approval)

- Compare future physical TV focus movements against individual reference frames, timing, light envelope and texture.
- On MiTV, prefer a reused hardware-accelerated translated texture/alpha animation rather than CPU bitmap blur or per-frame asset allocation.
- Keep the existing SmartTube immediate-launch behaviour, launcher app listing, and remote/DPAD navigation.
- Do not uninstall, disable or alter the prior `com.gathin.tvhome.referencev781v851immediate` or `com.gathin.tvhome.crystalclearhome` packages while iterating.
- Build/render a new APK **only after a separate explicit request**. No code or APK changes authorized by the B selection.
