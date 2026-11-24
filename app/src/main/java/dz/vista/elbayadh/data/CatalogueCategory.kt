package dz.vista.elbayadh.data

import org.json.JSONObject

data class CatalogueCategory(
    val id: String,
    val titleAr: String,
    val titleFr: String,
    val titleEn: String
) {
    fun title(language: CatalogueLanguage): String = when (language) {
        CatalogueLanguage.AR -> titleAr
        CatalogueLanguage.FR -> titleFr.ifBlank { titleAr }
        CatalogueLanguage.EN -> titleEn.ifBlank { titleAr }
    }

    companion object {
        fun fromJson(json: JSONObject) = CatalogueCategory(
            id = json.getString("id"),
            titleAr = json.optString("titleAr"),
            titleFr = json.optString("titleFr"),
            titleEn = json.optString("titleEn")
        )
    }
}

data class CatalogueSnapshot(
    val schemaVersion: Int,
    val contentVersion: Int,
    val provinceId: String,
    val categories: List<CatalogueCategory>,
    val landmarks: List<Landmark>,
    val photosByLandmark: Map<String, List<String>> = emptyMap()
)
