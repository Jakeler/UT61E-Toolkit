Android UT61E Toolkit
===================================
App to display, analyze and log (CSV) measurements for a UNI-T UT61E multimeter. Requires a Bluetooth module mod, for example with a HM-11 module.

Mod: https://blog.ja-ke.tech/multimeter/2018/03/09/UT61E-ble.html

Screenshots and usage description:
https://blog.ja-ke.tech/ut61e/android/2018/04/02/UT61E-android-logging.html

Building
-------
Fully rewritten in Kotlin with Jetpack Compose (Material 3). Requires JDK 17+ for the Gradle daemon (JDK 21 recommended) and an Android SDK with platform 37.

```
git clone --recursive <repo>
./gradlew assembleDebug
./gradlew test
```

Modules:
- `app` — single-activity Compose app (scan, live view with chart/stats/alarms, CSV logging via SAF, log viewer, settings)
- `ut61e_decoder_lib` — pure-Kotlin UT61E frame decoder ([git submodule](https://github.com/Jakeler/ut61e_decoder))

Powered by
-------
https://github.com/patrykandpatrick/vico
