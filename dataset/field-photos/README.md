# User-supplied field photographs

This folder records photographs supplied for the VISTA El Bayadh project.

- `manifest.json` inventories the original filenames and their review status.
- Place the original JPEG files in `incoming/` using the exact `sourceFileName`.
- The Android build copies only photos with an explicit `landmarkId` and `runtimeApproved: true` into the APK's local recognition-reference assets.
- Photos without a reviewed site assignment remain out of the runtime index and are not shown under a guessed landmark.
- The build creates `app/src/main/assets/recognition/index.json` with local paths only. No external image URL is used by the installed app.

The files currently available in the ChatGPT Library have been inventoried, but their binary bytes have not yet been committed to this repository. This is why the manifest is ready while the Android image index remains empty until the originals are placed in `incoming/`.

## Latest photo collection (2026-11-24)

A separate `collection_manifest.json` records the 20 original user-supplied images retrieved from the owner's ChatGPT Library, their SHA-256 hashes, and visual triage notes. The owner also received a ZIP archive containing the 20 original files, a contact sheet, and an Arabic README.

Visible signage identifies only:
- `1000078252.jpg` and `1000078262.jpg`: **Gour Ben El Khas / قور بنت الخس** signage.
- `1000078254.jpg`: **Ksar Bent Khass / قصر بنت الخس** tourism sign.
- `1000078279.jpg`: **Ksar Boussemghoun / Zaouiat Sidi Ahmed Tidjani** heritage plaque.

All other images remain location-unconfirmed. No image in this intake is automatically approved for recognition or runtime. In particular, unclassified rock formations must not be labeled as Gour Brizina or Kef El Melh without independent confirmation.
