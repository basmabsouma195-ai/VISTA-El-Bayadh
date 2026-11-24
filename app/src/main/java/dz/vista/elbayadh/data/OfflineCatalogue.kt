package dz.vista.elbayadh.data

import android.content.Context
import org.json.JSONObject
import java.io.IOException

/**
 * Reads the bundled catalogue only. No network request is made here, so this
 * repository remains usable in airplane mode. Remote catalogue updates can be
 * layered on later without changing the UI-facing model.
 */
class OfflineCatalogue(context: Context) {
    private val appContext = context.applicationContext

    @Throws(IOException::class)
    fun loadLandmarks(): List<Landmark> {
        val raw = appContext.assets.open("content/landmarks.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(raw)
        val records = root.optJSONArray("landmarks") ?: return emptyList()
        return buildList(records.length()) {
            for (index in 0 until records.length()) {
                val item = records.optJSONObject(index) ?: continue
                runCatching { Landmark.fromJson(item) }.getOrNull()?.let(::add)
            }
        }
    }
}
