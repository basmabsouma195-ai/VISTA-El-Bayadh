package dz.vista.elbayadh.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local-only itinerary storage. The ordered list stores catalogue IDs, so a
 * content refresh can update destination details without breaking saved trips.
 */
class TripPlansStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("vista_trip_plans", Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<TripPlan> {
        val raw = preferences.getString(KEY_TRIPS, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                runCatching { item.toTripPlan() }.getOrNull()?.let(::add)
            }
        }.sortedByDescending { it.updatedAtEpochMillis }
    }

    @Synchronized
    fun create(title: String, landmarkIds: List<String> = emptyList()): TripPlan {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Trip title cannot be blank" }
        val trip = TripPlan(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            landmarkIds = landmarkIds.distinct(),
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        save(all() + trip)
        return trip
    }

    @Synchronized
    fun rename(tripId: String, title: String): Boolean {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Trip title cannot be blank" }
        val current = all()
        if (current.none { it.id == tripId }) return false
        save(current.map {
            if (it.id == tripId) it.copy(title = cleanTitle, updatedAtEpochMillis = System.currentTimeMillis()) else it
        })
        return true
    }

    @Synchronized
    fun addLandmark(tripId: String, landmarkId: String): Boolean {
        require(landmarkId.isNotBlank())
        val current = all()
        val trip = current.firstOrNull { it.id == tripId } ?: return false
        if (landmarkId in trip.landmarkIds) return true
        save(current.map {
            if (it.id == tripId) it.copy(
                landmarkIds = it.landmarkIds + landmarkId,
                updatedAtEpochMillis = System.currentTimeMillis()
            ) else it
        })
        return true
    }

    @Synchronized
    fun removeLandmark(tripId: String, landmarkId: String): Boolean {
        val current = all()
        val trip = current.firstOrNull { it.id == tripId } ?: return false
        if (landmarkId !in trip.landmarkIds) return true
        save(current.map {
            if (it.id == tripId) it.copy(
                landmarkIds = it.landmarkIds - landmarkId,
                updatedAtEpochMillis = System.currentTimeMillis()
            ) else it
        })
        return true
    }

    @Synchronized
    fun moveLandmark(tripId: String, fromIndex: Int, toIndex: Int): Boolean {
        val current = all()
        val trip = current.firstOrNull { it.id == tripId } ?: return false
        if (fromIndex !in trip.landmarkIds.indices || toIndex !in trip.landmarkIds.indices) return false
        val reordered = trip.landmarkIds.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        save(current.map {
            if (it.id == tripId) it.copy(
                landmarkIds = reordered,
                updatedAtEpochMillis = System.currentTimeMillis()
            ) else it
        })
        return true
    }

    @Synchronized
    fun delete(tripId: String): Boolean {
        val current = all()
        if (current.none { it.id == tripId }) return false
        save(current.filterNot { it.id == tripId })
        return true
    }

    private fun save(trips: List<TripPlan>) {
        val array = JSONArray()
        trips.forEach { trip ->
            array.put(JSONObject().apply {
                put("id", trip.id)
                put("title", trip.title)
                put("landmarkIds", JSONArray(trip.landmarkIds))
                put("updatedAtEpochMillis", trip.updatedAtEpochMillis)
            })
        }
        check(preferences.edit().putString(KEY_TRIPS, array.toString()).commit()) {
            "Unable to persist itinerary"
        }
    }

    private fun JSONObject.toTripPlan(): TripPlan {
        val ids = optJSONArray("landmarkIds") ?: JSONArray()
        val landmarkIds = buildList {
            for (index in 0 until ids.length()) {
                ids.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
        return TripPlan(
            id = getString("id"),
            title = getString("title"),
            landmarkIds = landmarkIds,
            updatedAtEpochMillis = optLong("updatedAtEpochMillis")
        )
    }

    companion object {
        private const val KEY_TRIPS = "saved_trips_json"
    }
}
