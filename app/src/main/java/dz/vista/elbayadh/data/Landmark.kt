package dz.vista.elbayadh.data

import org.json.JSONObject

data class LandmarkSource(
    val title: String,
    val url: String?,
    val publisher: String?,
    val note: String?
)

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
    val audioEn: String?,
    val sources: List<LandmarkSource> = emptyList(),
    val originAr: String = "", val originFr: String = "", val originEn: String = "",
    val historyAr: String = "", val historyFr: String = "", val historyEn: String = "",
    val detailsAr: String = "", val detailsFr: String = "", val detailsEn: String = ""
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
    fun origin(language: CatalogueLanguage): String = when (language) {
        CatalogueLanguage.AR -> originAr
        CatalogueLanguage.FR -> originFr.ifBlank { originAr }
        CatalogueLanguage.EN -> originEn.ifBlank { originAr }
    }
    fun history(language: CatalogueLanguage): String = when (language) {
        CatalogueLanguage.AR -> historyAr
        CatalogueLanguage.FR -> historyFr.ifBlank { historyAr }
        CatalogueLanguage.EN -> historyEn.ifBlank { historyAr }
    }
    fun details(language: CatalogueLanguage): String = when (language) {
        CatalogueLanguage.AR -> detailsAr
        CatalogueLanguage.FR -> detailsFr.ifBlank { detailsAr }
        CatalogueLanguage.EN -> detailsEn.ifBlank { detailsAr }
    }

    companion object {
        fun fromJson(json: JSONObject): Landmark {
            val sourceArray = json.optJSONArray("sources")
            val sources = buildList {
                if (sourceArray != null) {
                    for (index in 0 until sourceArray.length()) {
                        val item = sourceArray.optJSONObject(index) ?: continue
                        val title = item.optString("title").trim()
                        if (title.isNotBlank()) add(
                            LandmarkSource(
                                title = title,
                                url = item.optString("url").takeIf(String::isNotBlank),
                                publisher = item.optString("publisher").takeIf(String::isNotBlank),
                                note = item.optString("note").takeIf(String::isNotBlank)
                            )
                        )
                    }
                }
            }
            return Landmark(
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
                audioEn = json.optString("audioEn").takeIf(String::isNotBlank),
                sources = sources,
                originAr = json.optString("originAr"), originFr = json.optString("originFr"), originEn = json.optString("originEn"),
                historyAr = json.optString("historyAr"), historyFr = json.optString("historyFr"), historyEn = json.optString("historyEn"),
                detailsAr = json.optString("detailsAr"), detailsFr = json.optString("detailsFr"), detailsEn = json.optString("detailsEn")
            )
        }
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
