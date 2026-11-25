package dz.vista.elbayadh.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class OfflineMapPin(
    val latitude: Double,
    val longitude: Double,
    val title: String
)

class OfflineMapPinStore(context: Context) {
    private val preferences = context.getSharedPreferences("vista_offline_map_pins", Context.MODE_PRIVATE)

    fun all(): List<OfflineMapPin> = runCatching {
        val array = JSONArray(preferences.getString(KEY_PINS, "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val latitude = item.optDouble("latitude", Double.NaN)
                val longitude = item.optDouble("longitude", Double.NaN)
                if (latitude.isNaN() || longitude.isNaN() ||
                    latitude !in -90.0..90.0 || longitude !in -180.0..180.0
                ) continue
                add(OfflineMapPin(latitude, longitude, item.optString("title", "Saved place")))
            }
        }
    }.getOrDefault(emptyList())

    fun add(pin: OfflineMapPin) {
        val pins = all().toMutableList().apply { add(pin) }
        save(pins)
    }

    fun clear() {
        preferences.edit().remove(KEY_PINS).apply()
    }

    private fun save(pins: List<OfflineMapPin>) {
        val array = JSONArray()
        pins.forEach { pin ->
            array.put(JSONObject()
                .put("latitude", pin.latitude)
                .put("longitude", pin.longitude)
                .put("title", pin.title))
        }
        preferences.edit().putString(KEY_PINS, array.toString()).apply()
    }

    private companion object {
        const val KEY_PINS = "saved_pins_json"
    }
}
