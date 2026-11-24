package dz.vista.elbayadh.data

import org.json.JSONObject

/** A single editorially reviewed destination in the offline El Bayadh catalogue. */
data class Landmark(
    val id: String,
    val categoryId: String,
    val nameAr: String,
    val nameFr: String,
    val nameEn: String,
    val descriptionAr: String,
    val descriptionFr: String,
    val descriptionEn: String,
    val latitude: Double?,
    val longitude: Double?,
    val verification: VerificationStatus,
    val image: String?,
    val audioAr: String?,
    val audioFr: String?,
    val audioEn: String?
) {
    fun name(language: CatalogueLanguage): String = when (language) {
        CatalogueLanguage.AR -> nameAr
        CatalogueLanguage.FR -> nameFr.ifBlank { nameAr }
        CatalogueLanguage.EN -> nameEn.ifBlank { nameAr }
    }

    fun description(language: CatalogueLanguage): String = when (language) {
        CatalogueLanguage.AR -> descriptionAr
        CatalogueLanguage.FR -> descriptionFr.ifBlank { descriptionAr }
        CatalogueLanguage.EN -> descriptionEn.ifBlank { descriptionAr }
    }

    companion object {
        fun fromJson(json: JSONObject): Landmark = Landmark(
            id = json.getString("id"),
            categoryId = json.getString("categoryId"),
            nameAr = json.optString("nameAr"),
            nameFr = json.optString("nameFr"),
            nameEn = json.optString("nameEn"),
            descriptionAr = json.optString("descriptionAr"),
            descriptionFr = json.optString("descriptionFr"),
            descriptionEn = json.optString("descriptionEn"),
            latitude = json.takeIf { it.has("latitude") && !it.isNull("latitude") }?.optDouble("latitude"),
            longitude = json.takeIf { it.has("longitude") && !it.isNull("longitude") }?.optDouble("longitude"),
            verification = VerificationStatus.fromValue(json.optString("verification", "under_review")),
            image = json.optString("image").takeIf(String::isNotBlank),
            audioAr = json.optString("audioAr").takeIf(String::isNotBlank),
            audioFr = json.optString("audioFr").takeIf(String::isNotBlank),
            audioEn = json.optString("audioEn").takeIf(String::isNotBlank)
        )
    }
}

enum class VerificationStatus(val value: String) {
    VERIFIED("verified"),
    ORAL_HISTORY("oral_history"),
    UNDER_REVIEW("under_review");

    companion object {
        fun fromValue(value: String): VerificationStatus =
            entries.firstOrNull { it.value == value } ?: UNDER_REVIEW
    }
}

enum class CatalogueLanguage { AR, FR, EN }
