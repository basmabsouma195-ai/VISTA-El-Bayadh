package dz.vista.elbayadh.data

import android.content.Context
import org.json.JSONObject
import java.io.IOException

/**
 * Reads the bundled catalogue only. Loading this data never makes a network
 * request, so the same content is available in airplane mode.
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
                    runCatching { Landmark.fromJson(item) }.getOrNull()?.let(::add)
                }
            }
        }

        return CatalogueSnapshot(
            schemaVersion = root.optInt("schemaVersion", 1),
            contentVersion = root.optInt("contentVersion", 1),
            provinceId = provinceId,
            categories = categories,
            landmarks = landmarks
        )
    }
}
