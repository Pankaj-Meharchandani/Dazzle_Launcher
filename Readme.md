# Dazzle Launcher

<p align="center">
  <img src="https://raw.githubusercontent.com/Pankaj-Meharchandani/Dazzle_Launcher/master/app/src/main/ic_launcher-playstore.png" alt="Dazzle Launcher icon" width="160" />
</p>

<p align="center">
  A calmer Android launcher that shuffles your apps to make phone habits less automatic.
</p>

<p align="center">
  <a href="https://github.com/Pankaj-Meharchandani/Dazzle_Launcher">Source</a>
  ·
  <a href="https://github.com/Pankaj-Meharchandani/Dazzle_Launcher/issues">Issues</a>
</p>

## Overview

Dazzle replaces the default Android home screen with a focused launcher built around app shuffling. Instead of keeping every app in a predictable position, Dazzle can periodically reorganize the apps on your home screen. This small interruption encourages you to pause and choose what you want to open rather than reaching for an app automatically.

The launcher can be used in two modes:

- **Classic** — a home screen with a separate app drawer.
- **Minimal** — all installed apps are available directly from the home screen.

## App shuffling

Shuffling is the core Dazzle experience.

- **Full Shuffle** — shuffle all apps shown on the home screen.
- **Dock Essential Apps** — Shuffle only selected apps.
- **Dock Essential Apps** — Apps in the fixed dock stay in place and are never shuffled.
- Long-press an app on the home screen to add it to or remove it from the dock.
- Use the app drawer to find any installed app when it is not currently visible on the home screen.
- Dedicated Screen Time page helps keep track of daily screen usage with individual app time.

Open **Settings → Shuffling** to choose Full Shuffle or Selective Shuffle. In Selective Shuffle, open **Select Apps** and mark the apps that should participate.

## Features

### Focused home screen

- Clean, gesture-driven home screen with time and date at a glance.
- Optional system wallpaper support.
- Optional frosted-glass blur in the app drawer.
- 12-hour and 24-hour clock formats.
- Five app icon shapes: hexagon, squircle, round, square, and octagon.
- Guided onboarding for first-time setup.

### Launcher modes and organization

- Home screen and app drawer launcher modes.
- Fixed dock for everyday essentials.
- App labels and icons loaded from installed applications.
- Multiple app pages with page indicators and swipe navigation.

### Widgets and useful information

- Add, replace, resize, move, and remove Android home-screen widgets.
- Built-in information widget options:
  - Screen time today
  - Battery and temperature
  - Next alarm
  - Upcoming calendar event
  - Unlock count for today
- Dedicated screen-time view with daily totals, per-app usage, and date navigation.

### Healthier phone habits

- **App limits** — select apps to grey out and block during configurable hours.
- **Force allow** — require a chosen condition before opening an app blocked by app limits.
- **Posture Alert** — use the device rotation sensor to remind you to raise the phone to eye level after holding it at an unhealthy angle.
- Configurable posture sensitivity, alert duration, and quiet hours.
- **Mindless Scroll Detector** — show a gentle “Take a mindful pause” reminder after staying in selected apps for too long.
- Choose the reminder interval from one minute for testing through one hour.
- Snooze a scroll reminder for five minutes or return to the home screen.

### Exercise and mindful pauses

- Open the dedicated **Exercise** option from the home screen for a guided pause.
- Choose between a **Breathing exercise** and a **Neck exercise**.
- Use exercises as a force-allow condition before opening apps blocked by App Limits.
- A blocked app can prompt you to complete the selected exercise before it opens, adding a deliberate pause to the habit loop.

## Requirements

- Android 11 / API 30 or newer
- Android Studio with a compatible JDK 11+
- Android SDK 36 for compiling the current project

## Getting started

1. Clone the repository:

   ```bash
   git clone https://github.com/Pankaj-Meharchandani/Dazzle_Launcher.git
   cd Dazzle_Launcher
   ```

2. Open the project in Android Studio.

3. Let Gradle sync and install the app on an Android 11+ device or emulator.

4. Open Dazzle and follow the onboarding guide.
5. Set Dazzle as your default launcher, then open **Settings → Shuffling** to enable Full Shuffle or configure Selective Shuffle.

## Build from the command line

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/app-debug.apk
```

To run the unit tests:

```bash
./gradlew test
```

## Permissions

Some features need Android system permissions. Dazzle requests or guides you to the relevant system settings only when the feature is enabled.

| Permission | Used for |
| --- | --- |
| Usage access | Screen-time statistics and app activity detection |
| Display over other apps | Posture alerts, app-limit prompts, and mindful-scroll nudges |
| Calendar access | Showing the next calendar event |
| Notification permission | Foreground-service notifications for posture monitoring |
| Phone state | Device and launcher state support |
| Vibration | Haptic posture and mindful-scroll reminders |
| Query all packages | Listing installed apps in the launcher and app drawer |
| Expand status bar | Launcher system UI behavior |
| Foreground service | Continuing posture monitoring while the feature is active |

## Project structure

```text
app/src/main/
├── java/com/example/dazzlelauncher/
│   ├── MainActivity.kt              # Launcher entry point and navigation
│   ├── HomeScreen.kt                # Home screen, pages, dock, and widgets
│   ├── AppDrawer.kt                 # Installed-app drawer
│   ├── LauncherViewModel.kt         # Launcher state and persisted settings
│   ├── SettingsScreen.kt            # Appearance, shuffle, health, and limit settings
│   ├── ScreentimePage.kt            # Usage statistics
│   ├── SelectiveShuffleScreen.kt    # Per-app shuffle selection
│   ├── PostureAlertService.kt       # Rotation-based posture monitoring
│   └── MindlessScrollNudge.kt       # Overlay reminder dialog
└── res/
    ├── drawable/dock.png           # Onboarding dock preview
    └── mipmap-*/                    # Launcher icons
```

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Activity, Core, Lifecycle, and Compose libraries
- Android AppWidget APIs
- Android UsageStats, Sensor, Calendar, and overlay APIs

## Privacy

Dazzle is a local launcher. The app uses Android system data such as installed packages, usage statistics, calendar events, alarms, battery state, and sensors only to provide the features described above. Review the requested Android permissions and disable any feature you do not want to use.

## Contributing

Issues and pull requests are welcome. For larger changes, open an issue first to discuss the proposed behavior and user experience.

## License

No license file is currently included in this repository. Until a license is added, the source should be treated as all rights reserved.
