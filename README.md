# LanxX GAME ASSET

Android Native Java project. Ten uploaded UnityFS asset files are bundled in `app/src/main/assets/gameassets/`.

## Behavior
- POWER ON extracts the bundled files into the app-owned external files directory:
  `Android/data/com.lanxx.gameasset/files/gameassets/`
- POWER OFF deletes the files extracted by LanxX from its own app-owned `gameassets` folder.
- POWER ON extracts the bundled files again.
- No broad storage permission is requested because the destination belongs to this app.

## Important Android limitation
Android 11+ restricts access to another app's `Android/data` directory. This project does not write into Free Fire's private data directory and does not modify the game. The files are kept in LanxX's own app storage.

## Build
Open this folder in Android Studio and build the `app` module, or use the included GitHub Actions workflow.


## Shizuku integration
- The app detects whether the Shizuku service is running.
- It requests Shizuku permission and displays the grant state.
- This build uses Shizuku for connection/permission status only. It does not write to or delete another game's private asset directory.
- Asset extraction and cleanup remain limited to LanxX's own app-specific folder.
