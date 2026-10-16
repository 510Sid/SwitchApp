# Final Complete App: 1-Click Dual SIM Data Switcher + Home Widget + Quick Tile

## Included Project Structure
- `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`: Complete Gradle build configuration ready to open directly in Android Studio.
- `app/src/main/AndroidManifest.xml`: Registers permissions (`READ_PHONE_STATE`, `ACCESS_NETWORK_STATE`, `VIBRATE`, `WRITE_SECURE_SETTINGS`), `MainActivity`, `SimSwitchWidget`, and `SimQuickTileService`.
- `app/src/main/java/com/example/simswitcher/SimSwitchHelper.kt`: Detects the active SIM, inactive SIM target, signal bars (`SignalStrength`), and link speed (`NetworkCapabilities`); executes the manual 1-click switch to the inactive SIM; triggers a 40ms haptic pulse; and saves custom SIM nicknames and switch history in `SharedPreferences`.
- `app/src/main/java/com/example/simswitcher/SimSwitchWidget.kt` + `res/layout/sim_widget.xml` + `res/xml/sim_widget_info.xml`: Home Screen Widget with live signal/speed indicator and 1-click button targeting the inactive SIM.
- `app/src/main/java/com/example/simswitcher/SimQuickTileService.kt`: Swipe-down Notification Shade Quick Settings Tile for 1-click switching from inside any app.
- `app/src/main/java/com/example/simswitcher/MainActivity.kt`: App screen with the 1-click button, custom SIM nickname editor, and recent manual switch history log.

## How to Build the APK & Enable 1-Click Switching
1. Unzip this archive and open the folder in **Android Studio**.
2. To install directly to your phone, click **Run (▶)**, or to generate a standalone `.apk` file, click **Build → Build Bundle(s) / APK(s) → Build APK(s)** (output in `app/build/outputs/apk/debug/app-debug.apk`).
3. Run this **one-time ADB command** in Android Studio's Terminal while your phone is connected via USB:
   ```bash
   adb shell pm grant com.example.simswitcher android.permission.WRITE_SECURE_SETTINGS
   ```
4. Add the **1-Click SIM Switcher** widget to your Home Screen or the **Switch SIM Data** tile to your Quick Settings shade!
