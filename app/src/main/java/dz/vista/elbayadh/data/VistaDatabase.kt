package dz.vista.elbayadh.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline-first catalogue database. The bundled JSON is the install seed;
 * this database is the local source of truth after seeding/synchronisation.
 * No network APIs or remote image URLs are accepted here.
 */
class VistaDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE categories (
                id TEXT PRIMARY KEY NOT NULL,
                title_ar TEXT NOT NULL,
                title_fr TEXT NOT NULL,
                title_en TEXT NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE landmarks (
                id TEXT PRIMARY KEY NOT NULL,
                category_id TEXT NOT NULL,
                name_ar TEXT NOT NULL,
                name_fr TEXT NOT NULL,
                name_en TEXT NOT NULL,
                description_ar TEXT NOT NULL,
                description_fr TEXT NOT NULL,
                description_en TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                verification TEXT NOT NULL,
                image_asset TEXT,
                audio_ar TEXT,
                audio_fr TEXT,
                audio_en TEXT,
                FOREIGN KEY(category_id) REFERENCES categories(id)
            )"""
        )
        db.execSQL("CREATE INDEX index_landmarks_category ON landmarks(category_id)")
        db.execSQL("""CREATE TABLE landmark_photos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            landmark_id TEXT NOT NULL,
            asset_path TEXT NOT NULL,
            position INTEGER NOT NULL DEFAULT 0,
            UNIQUE(landmark_id, asset_path),
            FOREIGN KEY(landmark_id) REFERENCES landmarks(id)
        )""")
        db.execSQL("CREATE INDEX index_landmark_photos_landmark ON landmark_photos(landmark_id)")
        db.execSQL("CREATE TABLE catalogue_meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        db.execSQL("""CREATE TABLE landmark_sources (id INTEGER PRIMARY KEY AUTOINCREMENT, landmark_id TEXT NOT NULL, title TEXT NOT NULL, url TEXT, publisher TEXT, note TEXT, FOREIGN KEY(landmark_id) REFERENCES landmarks(id))""")
        db.execSQL("CREATE INDEX index_landmark_sources_landmark ON landmark_sources(landmark_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versioned migrations are added here; never erase visitor data on upgrade.
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS catalogue_meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        }
        if (oldVersion < 3) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS landmark_photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                landmark_id TEXT NOT NULL,
                asset_path TEXT NOT NULL,
                position INTEGER NOT NULL DEFAULT 0,
                UNIQUE(landmark_id, asset_path),
                FOREIGN KEY(landmark_id) REFERENCES landmarks(id)
            )""")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_landmark_photos_landmark ON landmark_photos(landmark_id)")
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS landmark_sources (id INTEGER PRIMARY KEY AUTOINCREMENT, landmark_id TEXT NOT NULL, title TEXT NOT NULL, url TEXT, publisher TEXT, note TEXT, FOREIGN KEY(landmark_id) REFERENCES landmarks(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_landmark_sources_landmark ON landmark_sources(landmark_id)")
        }
    }

    @Synchronized
    fun seedIfNeeded(snapshot: CatalogueSnapshot) {
        val currentVersion = readableDatabase.rawQuery(
            "SELECT value FROM catalogue_meta WHERE key='content_version'", null
        ).use { if (it.moveToFirst()) it.getString(0).toIntOrNull() ?: 0 else 0 }
        if (currentVersion >= snapshot.contentVersion && currentVersion > 0) return

        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("landmark_sources", null, null)
            db.delete("landmark_photos", null, null)
            db.delete("landmarks", null, null)
            db.delete("categories", null, null)
            snapshot.categories.forEach { category ->
                db.insertOrThrow("categories", null, ContentValues().apply {
                    put("id", category.id)
                    put("title_ar", category.titleAr)
                    put("title_fr", category.titleFr)
                    put("title_en", category.titleEn)
                })
            }
            snapshot.landmarks.forEach { landmark ->
                db.insertOrThrow("landmarks", null, ContentValues().apply {
                    put("id", landmark.id)
                    put("category_id", landmark.categoryId)
                    put("name_ar", landmark.nameAr)
                    put("name_fr", landmark.nameFr)
                    put("name_en", landmark.nameEn)
                    put("description_ar", landmark.descriptionAr)
                    put("description_fr", landmark.descriptionFr)
                    put("description_en", landmark.descriptionEn)
                    if (landmark.latitude == null) putNull("latitude") else put("latitude", landmark.latitude)
                    if (landmark.longitude == null) putNull("longitude") else put("longitude", landmark.longitude)
                    put("verification", landmark.verification.value)
                    put("image_asset", landmark.image?.takeIf { !it.contains("://") })
                    put("audio_ar", landmark.audioAr?.takeIf { !it.contains("://") })
                    put("audio_fr", landmark.audioFr?.takeIf { !it.contains("://") })
                    put("audio_en", landmark.audioEn?.takeIf { !it.contains("://") })
                })
            }
            snapshot.landmarks.forEach { landmark ->
                landmark.sources.forEach { source ->
                    db.insertOrThrow("landmark_sources", null, ContentValues().apply {
                        put("landmark_id", landmark.id)
                        put("title", source.title)
                        put("url", source.url)
                        put("publisher", source.publisher)
                        put("note", source.note)
                    })
                }
            }
            snapshot.photosByLandmark.forEach { (landmarkId, paths) ->
                paths.distinct().forEachIndexed { position, assetPath ->
                    if (!assetPath.contains("://") && assetPath.startsWith("recognition/")) {
                        db.insertOrThrow("landmark_photos", null, ContentValues().apply {
                            put("landmark_id", landmarkId)
                            put("asset_path", assetPath)
                            put("position", position)
                        })
                    }
                }
            }
            db.execSQL(
                "INSERT OR REPLACE INTO catalogue_meta(key,value) VALUES('content_version',?)",
                arrayOf(snapshot.contentVersion.toString())
            )
            db.execSQL(
                "INSERT OR REPLACE INTO catalogue_meta(key,value) VALUES('province_id',?)",
                arrayOf(snapshot.provinceId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun readCatalogue(fallback: CatalogueSnapshot): CatalogueSnapshot {
        val db = readableDatabase
        val categories = mutableListOf<CatalogueCategory>()
        db.rawQuery("SELECT id,title_ar,title_fr,title_en FROM categories ORDER BY id", null).use { c ->
            while (c.moveToNext()) categories += CatalogueCategory(c.getString(0), c.getString(1), c.getString(2), c.getString(3))
        }
        val landmarks = mutableListOf<Landmark>()
        db.rawQuery(
            """SELECT id,category_id,name_ar,name_fr,name_en,description_ar,description_fr,description_en,
                latitude,longitude,verification,image_asset,audio_ar,audio_fr,audio_en
                FROM landmarks ORDER BY name_ar""", null
        ).use { c ->
            while (c.moveToNext()) landmarks += Landmark(
                id = c.getString(0), categoryId = c.getString(1),
                nameAr = c.getString(2), nameFr = c.getString(3), nameEn = c.getString(4),
                descriptionAr = c.getString(5), descriptionFr = c.getString(6), descriptionEn = c.getString(7),
                latitude = if (c.isNull(8)) null else c.getDouble(8),
                longitude = if (c.isNull(9)) null else c.getDouble(9),
                verification = VerificationStatus.fromValue(c.getString(10)),
                image = c.getString(11), audioAr = c.getString(12), audioFr = c.getString(13), audioEn = c.getString(14)
            )
        }
        val version = db.rawQuery("SELECT value FROM catalogue_meta WHERE key='content_version'", null)
            .use { if (it.moveToFirst()) it.getString(0).toIntOrNull() ?: fallback.contentVersion else fallback.contentVersion }
        val province = db.rawQuery("SELECT value FROM catalogue_meta WHERE key='province_id'", null)
            .use { if (it.moveToFirst()) it.getString(0) else fallback.provinceId }
        return CatalogueSnapshot(
            schemaVersion = fallback.schemaVersion,
            contentVersion = version,
            provinceId = province,
            categories = categories.ifEmpty { fallback.categories },
            landmarks = landmarks.ifEmpty { fallback.landmarks },
            photosByLandmark = buildMap<String, List<String>> {
                db.rawQuery("SELECT landmark_id,asset_path FROM landmark_photos ORDER BY landmark_id,position", null).use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0)
                        val path = c.getString(1)
                        put(id, get(id).orEmpty() + path)
                    }
                }
            }.ifEmpty { fallback.photosByLandmark }
        )
    }

    companion object {
        private const val DATABASE_NAME = "vista_el_bayadh.db"
        private const val DATABASE_VERSION = 4
    }
}
