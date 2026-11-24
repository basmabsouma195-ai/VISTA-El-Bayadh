package dz.vista.elbayadh.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.IOException

/**
 * Reads only image references packaged inside the APK. This class has no
 * network fallback and is not a visual-recognition model.
 */
data class RecognitionReferenceImage(
    val id: String,
    val landmarkId: String,
    val fileName: String,
    val assetPath: String,
    val labelAr: String,
    val labelFr: String,
    val labelEn: String,
    val attribution: String,
    val license: String,
    val sourcePage: String,
    val licenseUrl: String,
    val runtimeApproved: Boolean,
    val verificationStatus: String,
    val datasetRole: String
)

class OfflineRecognitionImageIndex(context: Context) {
    private val assets = context.applicationContext.assets

    @Throws(IOException::class)
    fun load(): List<RecognitionReferenceImage> {
        val raw = assets.open("recognition/index.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(raw)
        require(root.optString("mode") == "offline_local_assets_only") {
            "Recognition index must use bundled local assets only"
        }
        val images = root.optJSONArray("images") ?: return emptyList()
        return buildList {
            for (i in 0 until images.length()) {
                val item = images.optJSONObject(i) ?: continue
                val path = item.optString("assetPath")
                if (path.isBlank() || path.contains("://")) continue
                // Seed-only web photos and provisional mappings are never inference references.
                if (!item.optBoolean("runtimeApproved", false)) continue
                if (item.optString("verificationStatus") != "verified") continue
                add(
                    RecognitionReferenceImage(
                        id = item.optString("id"),
                        landmarkId = item.optString("landmarkId"),
                        fileName = item.optString("fileName"),
                        assetPath = path,
                        labelAr = item.optString("labelAr"),
                        labelFr = item.optString("labelFr"),
                        labelEn = item.optString("labelEn"),
                        attribution = item.optString("attribution"),
                        license = item.optString("license"),
                        sourcePage = item.optString("sourcePage"),
                        licenseUrl = item.optString("licenseUrl"),
                        runtimeApproved = item.optBoolean("runtimeApproved", false),
                        verificationStatus = item.optString("verificationStatus"),
                        datasetRole = item.optString("datasetRole")
                    )
                )
            }
        }
    }

    /**
     * Preview-only image set: approved references plus user-confirmed field photos.
     * Signage and plaques are excluded because they do not represent the landmark scene.
     * These images are never promoted to calibrated recognition references.
     */
    fun loadPreview(): List<RecognitionReferenceImage> {
        val approved = load()
        val gallery = runCatching {
            val raw = assets.open("content/field-photo-gallery.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val images = JSONObject(raw).optJSONArray("images") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until images.length()) {
                    val item = images.optJSONObject(i) ?: continue
                    if (!item.optBoolean("galleryEnabled", false)) continue
                    if (item.optBoolean("recognitionApproved", false)) continue
                    if (item.optString("locationStatus") != "user_confirmed_location") continue
                    val type = item.optString("imageType")
                    if (type != "architecture" && type != "landscape") continue
                    val landmarkId = item.optString("landmarkId").trim()
                    val path = item.optString("assetPath").trim()
                    if (landmarkId.isBlank() || path.isBlank() || path.contains("://")) continue
                    val exists = runCatching { assets.open(path).use { it.read() } }.isSuccess
                    if (!exists) continue
                    add(RecognitionReferenceImage(
                        id = item.optString("id"),
                        landmarkId = landmarkId,
                        fileName = item.optString("sourceFileName"),
                        assetPath = path,
                        labelAr = "",
                        labelFr = "",
                        labelEn = "",
                        attribution = "",
                        license = "",
                        sourcePage = "",
                        licenseUrl = "",
                        runtimeApproved = false,
                        verificationStatus = "user_confirmed_location",
                        datasetRole = "gallery_preview_only"
                    ))
                }
            }
        }.getOrDefault(emptyList())
        return (approved + gallery).distinctBy { it.assetPath }
    }

    /** Decodes a bounded local preview to avoid loading full camera-sized images. */
    @Throws(IOException::class)
    fun decodePreview(image: RecognitionReferenceImage, maxDimension: Int = 640): Bitmap? {
        require(maxDimension > 0)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assets.open(image.assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return assets.open(image.assetPath).use { BitmapFactory.decodeStream(it, null, options) }
    }
}
