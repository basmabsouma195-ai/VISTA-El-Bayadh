package dz.vista.elbayadh.data

/** An offline itinerary containing ordered landmark IDs from the local catalogue. */
data class TripPlan(
    val id: String,
    val title: String,
    val landmarkIds: List<String>,
    val updatedAtEpochMillis: Long
) {
    init {
        require(id.isNotBlank()) { "Trip id cannot be blank" }
        require(title.isNotBlank()) { "Trip title cannot be blank" }
        require(landmarkIds.none(String::isBlank)) { "Landmark IDs cannot be blank" }
    }
}
