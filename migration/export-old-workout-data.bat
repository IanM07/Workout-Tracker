@echo off
setlocal
where adb >nul 2>nul
if errorlevel 1 (
  echo adb was not found.
  echo Install Android Platform Tools on this Windows PC, then run this file again.
  pause
  exit /b 1
)

echo Connect the phone by USB, enable USB debugging, and approve the computer on the phone.
echo.
adb devices
echo.
echo Exporting the old Workout Tracker data...
adb exec-out run-as com.weddingworkout.tracker cat shared_prefs/workout.xml > old_workout.xml
if errorlevel 1 (
  echo.
  echo Export failed. Keep the OLD Workout Tracker installed and retry after USB debugging is authorized.
  pause
  exit /b 1
)

echo Copying old_workout.xml to the phone's Downloads folder...
adb push old_workout.xml /sdcard/Download/old_workout.xml
if errorlevel 1 (
  echo The data was exported to old_workout.xml on this PC, but could not be copied back to the phone automatically.
  echo Copy old_workout.xml to the phone manually, then choose it from the new app.
  pause
  exit /b 1
)

echo.
echo Done. In Workout Tracker V2, open Saved Workouts ^> Import Old App Data,
echo then choose Download/old_workout.xml.
pause
endlocal
