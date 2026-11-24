package dz.vista.elbayadh.data

import android.content.Context

/**
 * Small offline persistence layer for saved destination IDs.
 * IDs, rather than copied landmark objects, keep favourites compatible with
 * future catalogue content updates.
 */
class FavoritesStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("vista_favorites", Context.MODE_PRIVATE)

    fun allIds(): Set<String> =
        preferences.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()

    fun contains(landmarkId: String): Boolean = landmarkId in allIds()

    fun add(landmarkId: String) {
        require(landmarkId.isNotBlank())
        preferences.edit().putStringSet(KEY_IDS, allIds() + landmarkId).apply()
    }

    fun remove(landmarkId: String) {
        preferences.edit().putStringSet(KEY_IDS, allIds() - landmarkId).apply()
    }

    fun toggle(landmarkId: String): Boolean {
        val next = allIds().toMutableSet()
        val nowSaved = if (next.add(landmarkId)) true else {
            next.remove(landmarkId)
            false
        }
        preferences.edit().putStringSet(KEY_IDS, next).apply()
        return nowSaved
    }

    companion object {
        private const val KEY_IDS = "landmark_ids"
    }
}
