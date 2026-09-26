# Workout Tracker V2.2

Offline Android dumbbell workout tracker.

## V2.2 interface

- Dark-gray background, dark cards, white text, blue controls.
- App opens directly to the current calendar day's workout.
- The workout page has a single navigation button: **Saved Workouts**.
- Saturday and Sunday open as rest days.
- Android system Back / back-swipe follows the same navigation as the in-app Back buttons.

## Routine

- Monday — Push: 16 working sets
- Tuesday — Pull: 16 working sets
- Wednesday — Legs + Core: 17 working sets
- Thursday — Upper: 18 working sets
- Friday — Shoulders + Arms + Glutes: 18 working sets

All programmed resistance work uses dumbbells.

## Saving and progression

Each workout is stored under its exact calendar date using `session_YYYY-MM-DD`. Saving today's workout can only replace today's record; it cannot overwrite another day.

When a new workout is opened, the app carries forward the previous working weights for matching exercises while leaving the new rep fields blank. When the same exercise has different programming on different weekdays, the app prefers the previous occurrence from that weekday and falls back to the most recent earlier use.

## Saved Workouts

Saved weeks are identified by the Monday date only. No Week 1 / Week 2 labels are shown.

The Saved Workouts screen shows 10 saved weeks at a time. **Older 10** and **Newer 10** move between groups of ten saved weeks.

Opening a week shows a read-only log grouped under each actual workout date. Each exercise lists the stored weight and reps for every completed set. Back buttons are separated at the bottom of each screen.

## Legacy data

The previous installed app uses Android-private app storage. Android's application sandbox prevents a separately signed replacement app from directly reading another installed app's private SharedPreferences.

V2.2 includes a one-time best-effort automatic migration. If Android restores the old `workout` SharedPreferences into this package, or if this build is installed as a signature-compatible update, `log_YYYY-MM-DD` entries are automatically converted into legacy saved weeks and compatible dumbbell weights are carried into the new routine.

Data already imported into V2.1 is preserved automatically when V2.2 is installed over V2.1.

## Build with GitHub Actions

Push the project contents to a GitHub repository. `.github/workflows/build-apk.yml` builds `app-debug.apk` and uploads it as the `Workout-Tracker-V2-APK` workflow artifact.

## Stable APK signing

V2.2 includes a dedicated personal debug keystore and the Gradle build uses it for every debug APK. Future builds from this project can install over V2.2 without changing the signing certificate, so Android can keep the app's saved data across upgrades.

The original pre-V2 app was signed with a different key. Android will not let this project impersonate or directly update that package.


## V2.2.2 changes
- Blue barbell launcher icon on a dark-gray background.
- One-time legacy import button appears on Saved Workouts until old data is imported.
- The export scripts now also copy `old_workout.xml` back to the phone's Downloads folder for easy selection.
- Existing V2.2.x daily workout data is preserved when installing this as an update.
