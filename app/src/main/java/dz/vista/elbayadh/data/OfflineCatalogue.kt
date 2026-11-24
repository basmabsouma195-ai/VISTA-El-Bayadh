package dz.vista.elbayadh.data

import android.content.Context
import dz.vista.elbayadh.recognition.OfflineRecognitionImageIndex
import org.json.JSONObject
import java.io.IOException

/**
 * Reads the bundled catalogue and its optional local image index only.
 * No network access is used for catalogue or image loading.
 */
class OfflineCatalogue(context: Context) {
    private val appContext = context.applicationContext

    @Throws(IOException::class)
    fun load(): CatalogueSnapshot {
        val raw = appContext.assets.open("content/landmarks.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(raw)
        val provinceId = root.optJSONObject("province")?.optString("id").orEmpty()
        require(provinceId.isNotBlank()) { "Catalogue province.id is missing" }

        val localPhotosByLandmark = runCatching {
            OfflineRecognitionImageIndex(appContext).load()
                .filter { reference ->
                    runCatching {
                        appContext.assets.open(reference.assetPath).use { it.read() }
                    }.isSuccess
                }
                .groupBy({ it.landmarkId }, { it.assetPath })
        }.getOrDefault(emptyMap())
        val localImageByLandmark = localPhotosByLandmark.mapValues { it.value.firstOrNull().orEmpty() }

        val categoriesJson = root.optJSONArray("categories")
        val categories = buildList {
            if (categoriesJson != null) {
                for (index in 0 until categoriesJson.length()) {
                    val item = categoriesJson.optJSONObject(index) ?: continue
                    runCatching { CatalogueCategory.fromJson(item) }
                        .getOrNull()?.let(::add)
                }
            }
        }

        val landmarksJson = root.optJSONArray("landmarks")
        val landmarks = buildList {
            if (landmarksJson != null) {
                for (index in 0 until landmarksJson.length()) {
                    val item = landmarksJson.optJSONObject(index) ?: continue
                    runCatching {
                        Landmark.fromJson(item).copy(
                            // Only the packaged index may supply a runtime image.
                            image = localImageByLandmark[ item.optString("id") ]
                        )
                    }.getOrNull()?.let(::add)
                }
            }
        }

        val bundledSnapshot = CatalogueSnapshot(
            schemaVersion = root.optInt("schemaVersion", 1),
            contentVersion = root.optInt("contentVersion", 1),
            provinceId = provinceId,
            categories = categories,
            landmarks = landmarks,
            photosByLandmark = localPhotosByLandmark
        )
        // Seed once from APK assets, then serve catalogue data from local SQLite.
        val database = VistaDatabase(appContext)
        return try {
            database.seedIfNeeded(bundledSnapshot)
            database.readCatalogue(bundledSnapshot)
        } finally {
            database.close()
        }
    }
}
