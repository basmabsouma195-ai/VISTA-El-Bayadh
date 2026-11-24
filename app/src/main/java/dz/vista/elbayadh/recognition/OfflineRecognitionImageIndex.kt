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
    val licenseUrl: String
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
                        licenseUrl = item.optString("licenseUrl")
                    )
                )
            }
        }
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
