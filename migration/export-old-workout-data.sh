#!/usr/bin/env bash
set -euo pipefail
if ! command -v adb >/dev/null 2>&1; then
  echo "adb was not found. Install Android Platform Tools on this computer first." >&2
  exit 1
fi
adb devices
adb exec-out run-as com.weddingworkout.tracker cat shared_prefs/workout.xml > old_workout.xml
adb push old_workout.xml /sdcard/Download/old_workout.xml
echo "Done. In Workout Tracker V2, open Saved Workouts > Import Old App Data and choose Download/old_workout.xml."
