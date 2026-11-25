# Focus Companion

A native Android app (Kotlin + Jetpack Compose, **no WebView**) built to help
with ADHD executive dysfunction and the coding-hyperfocus → studying
transition specifically.

4 core screens: **Now & Briefing**, **I'm Stuck / Unblocker**, **AI Study &
Day Planner**, **Quick Brain Dump** — plus a **Settings** screen for your
Gemini API key.

---

## 1. What's inside

- 100% Kotlin, Jetpack Compose, Material 3, Navigation-Compose
- Room database for local-first persistence (works fully offline except the
  AI calls themselves)
- A pluggable `CloudSyncInterface` (`data/repository/CloudSyncInterface.kt`)
  — defaults to a no-op so nothing requires a server; swap in a real
  Firestore/REST implementation later without touching the UI
- `GeminiRepository` calls the Gemini REST API directly with **your own**
  API key (sent only to `generativelanguage.googleapis.com`, straight from
  your phone — never through any server of ours, because there isn't one)
  - Primary model: `gemini-4-hydrogen`
  - Fallback model (toggle in Settings, or automatic if the primary call
    fails): `gemini-3.5-flash-lite`
  - Speech: `gemini-3.8-flash-tts` when online, with automatic, silent
    fallback to Android's built-in `TextToSpeech` if that fails for any
    reason — so the briefing always speaks
  - **Note:** these exact model name strings are what you specified. Google
    renames/retires Gemini model IDs periodically — if a call fails with a
    "model not found" error, open `GeminiRepository.kt` and update
    `primaryModel` / `fallbackModel` / `ttsModel` to whatever's currently
    live on your account.
- `SpeechRecognitionManager` wraps Android's `SpeechRecognizer` for voice
  input on the Stuck screen
- `AlarmScheduler` uses `AlarmManager.setExactAndAllowWhileIdle` (with a
  graceful fallback to inexact scheduling if exact-alarm permission isn't
  granted) for the daily morning briefing, restored automatically after
  reboot by `BootCompletedReceiver`
- `SecurePrefsManager` stores your API key and settings in
  `EncryptedSharedPreferences` (AES-256), never in plain text
- No Hilt/Dagger — a small hand-rolled `AppContainer` (see `di/`) wires
  everything together, so the project has zero annotation-processing
  surprises

---

## 2. Push this to GitHub

From the folder containing this README:

```bash
git init
git add .
git commit -m "Initial commit: Focus Companion"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

(Create the empty repo on GitHub first if you haven't already — no need to
add a README/.gitignore there, this project already has them.)

---

## 3. Build the APK on GitHub Actions (no Android Studio needed)

The workflow at `.github/workflows/build-apk.yml` runs automatically on
every push to `main`/`master`, and you can also trigger it manually:

1. Go to your repo on GitHub → **Actions** tab
2. Select **Build Debug APK** in the left sidebar
3. Click **Run workflow** (or just push a commit — it runs automatically)
4. Wait for the green checkmark (first run takes a few minutes while the
   Android SDK downloads; later runs are faster)
5. Click into the finished run → scroll to **Artifacts** → download
   **app-debug** → unzip it → you have `app-debug.apk`

---

## 4. Install the APK on your phone

1. Copy `app-debug.apk` to your phone (email it to yourself, use a cloud
   drive, or `adb install app-debug.apk` over USB with Developer Options →
   USB debugging enabled)
2. Tap the file on your phone. If this is your first time installing an
   APK outside the Play Store, Android will prompt you to allow installs
   from that source (Settings → Apps → Special access → Install unknown
   apps) — allow it for whichever app you used to open the file
3. Install, then open **Focus Companion**
4. Grant the microphone and notification permissions when prompted (both
   are optional — the app still works without them, just without voice
   input / alarm notifications)

---

## 5. Add your Gemini API key

1. Get a free key at <https://aistudio.google.com/app/apikey>
2. In the app, tap the gear icon (top right of the **Now** screen) to open
   **Settings**
3. Paste your key into **API key**, tap **Save settings**
4. Optional: set a **morning briefing time** (24-hour `HH:mm`, e.g.
   `07:30`) — on Android 12+ you may be prompted to allow exact alarms the
   first time; tap **Allow exact alarms** if you see that button in
   Settings

That's it — go to the **Planner** tab, describe your available hours,
energy, and goals, and generate your first day.

---

## 6. Project structure

```
ADHDFocusCompanion/
├── settings.gradle.kts
├── build.gradle.kts
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/…
│       └── java/com/adhdcompanion/focus/
│           ├── App.kt                      (Application + DI container)
│           ├── MainActivity.kt
│           ├── di/AppContainer.kt
│           ├── model/Models.kt
│           ├── data/
│           │   ├── local/                  (Room entities, DAOs, DB)
│           │   ├── remote/                 (Gemini REST client)
│           │   ├── repository/             (BlockRepository, BrainDumpRepository, CloudSyncInterface)
│           │   └── prefs/SecurePrefsManager.kt
│           ├── tts/SpeechManager.kt
│           ├── speech/SpeechRecognitionManager.kt
│           ├── alarm/                      (AlarmScheduler, AlarmReceiver, BootCompletedReceiver)
│           ├── notification/NotificationHelper.kt
│           ├── navigation/AppNavigation.kt
│           └── ui/
│               ├── theme/
│               ├── components/
│               ├── screens/                (NowBriefingScreen, StuckScreen, PlannerScreen, BrainDumpScreen, SettingsScreen)
│               └── viewmodel/
└── .github/workflows/build-apk.yml
```

## 7. Building locally instead (optional)

If you do have Android Studio: **File → Open**, point it at this folder,
let it sync, then **Run ▶**. Minimum SDK 26 (Android 8.0), target/compile
SDK 34.

## 8. Wiring in real cloud sync (optional)

`data/repository/CloudSyncInterface.kt` ships a `NoOpCloudSync` (the
default, used in `AppContainer.kt`) and a `RestCloudSync` skeleton. To add
Firestore or your own backend: implement `CloudSyncInterface`, then swap
the one line in `AppContainer.kt` that constructs `NoOpCloudSync()`.
