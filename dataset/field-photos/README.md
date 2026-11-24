# User-supplied field photographs

This folder records photographs supplied for the VISTA El Bayadh project.

- `manifest.json` inventories the original filenames and their review status.
- Place the original JPEG files in `incoming/` using the exact `sourceFileName`.
- The Android build copies only photos with a provisional `landmarkId` into the APK's local recognition-reference assets.
- Photos without a confirmed site assignment remain out of the runtime index and are not shown under a guessed landmark.
- The build creates `app/src/main/assets/recognition/index.json` with local paths only. No external image URL is used by the installed app.

The files currently available in the ChatGPT Library have been inventoried, but their binary bytes have not yet been committed to this repository. This is why the manifest is ready while the Android image index remains empty until the originals are placed in `incoming/`.
