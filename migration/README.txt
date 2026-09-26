OLD APP DATA TRANSFER

Android does not permit the new app (com.weddingworkout.tracker.v2) to directly read the old app's private data (com.weddingworkout.tracker).

Keep the old app installed until the transfer is complete.

Windows:
1. Install Android Platform Tools (adb) on the PC.
2. Enable Developer options + USB debugging on the phone.
3. Connect the phone by USB and authorize the PC.
4. Run export-old-workout-data.bat on the PC.
5. The script exports the old data and copies old_workout.xml to the phone's Downloads folder.
6. In the new app: Saved Workouts > Import Old App Data > Choose old_workout.xml.

macOS/Linux:
Run export-old-workout-data.sh instead.
