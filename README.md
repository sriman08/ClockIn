# ClockIn

**clock.in** is an independent, standalone edition (`com.clockin.lockscreen`) built with Kotlin, Jetpack Compose, Room, DataStore, WorkManager, and Android's `WallpaperManager.FLAG_LOCK` API. Because it uses a separate application ID, database, and preferences store, it installs cleanly with zero conflicts.

## Lock-Screen Design
- Pure black (`#000000`) canvas with crisp off-white (`#FAF9F6`) hand-drawn typography and thin decorative architectural lines.
- Displays **only** two primary elements in their exact reference positions:
  1. **DAY** (upper-right with thin horizontal callout pointer line).
  2. **TASK OF THE DAY** (centered below the thin open-top horizontal bracket tray, pulled live from the local Room task database).
- Strictly excludes dots, circles, weather, battery, network, unlock text, and duplicate clocks.
- **Selectable Themes:** Default Black & White plus **Yellow** (`#FFD426`), **Pink** (`#FF4F87`), **Green** (`#23C552`), **Lime** (`#A3E635`), **Purple** (`#8B5CF6`), and **Grey** (`#9CA3AF`).

## Android Lock-Screen Implementation Note
Android does not allow ordinary third-party apps to replace its secure system lock screen (PIN/biometric lock). clock.in uses the official `WallpaperManager.setBitmap(..., WallpaperManager.FLAG_LOCK)` API to render the exact minimalist design onto the lock screen while leaving security and the home screen untouched—plus a built-in **Save PNG & Set Manually** fallback (`Pictures/clock.in`) for OEMs that restrict third-party lock-screen updates.
