package dz.vista.elbayadh.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Offline MobileNetV3 image-embedding engine.
 *
 * The ImageNet model is a generic feature extractor, not a landmark classifier.
 * Matching remains disabled until the local index has >=3 verified views per
 * landmark and a separately validated calibration.json is packaged.
 */
class MobileNetV3RecognitionEngine(context: Context) : LandmarkRecognitionEngine, Closeable {
    private val appContext = context.applicationContext
    private val index = OfflineRecognitionImageIndex(appContext)
    private var interpreter: Interpreter? = null
    private var referenceVectors: Map<String, List<FloatArray>> = emptyMap()
    private var minimumSimilarity = 1f
    private var minimumLead = 1f
    private var calibrated = false

    @Volatile
    var isModelLoaded: Boolean = false
        private set

    override val isReady: Boolean
        get() = isModelLoaded && calibrated && referenceVectors.isNotEmpty()

    /** Load the bundled model and precompute all approved local reference embeddings. */
    suspend fun prepare(): Boolean = withContext(Dispatchers.IO) {
        if (isModelLoaded) return@withContext isReady
        try {
            val modelBytes = appContext.assets.open(MODEL_ASSET).use { it.readBytes() }
            val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
                .order(ByteOrder.nativeOrder()).apply { put(modelBytes); rewind() }
            val runtime = Interpreter(modelBuffer, Interpreter.Options().setNumThreads(2))
            val inputShape = runtime.getInputTensor(0).shape()
            val outputShape = runtime.getOutputTensor(0).shape()
            require(inputShape.contentEquals(intArrayOf(1, 224, 224, 3))) {
                "Unexpected MobileNet input tensor: ${inputShape.contentToString()}"
            }
            require(outputShape.contentEquals(intArrayOf(1, EMBEDDING_SIZE))) {
                "Unexpected MobileNet output tensor: ${outputShape.contentToString()}"
            }
            interpreter = runtime
            isModelLoaded = true

            val references = index.load()
            val grouped = references.groupBy { it.landmarkId }
            // One photo is not enough to represent a real site from varied angles.
            if (grouped.isEmpty() || grouped.values.any { it.size < MIN_REFERENCES_PER_LANDMARK }) {
                return@withContext false
            }

            val vectors = grouped.mapValues { (_, images) ->
                images.mapNotNull { reference ->
                    index.decodePreview(reference, 640)?.useBitmap { bitmap -> embed(bitmap) }
                }
            }
            if (vectors.values.any { it.size < MIN_REFERENCES_PER_LANDMARK }) return@withContext false

            val calibration = runCatching {
                appContext.assets.open(CALIBRATION_ASSET).bufferedReader().use { JSONObject(it.readText()) }
            }.getOrNull() ?: return@withContext false
            if (calibration.optInt("schemaVersion") != 1 ||
                calibration.optBoolean("validatedOnHeldOutElBayadhSet") != true
            ) return@withContext false

            val minScore = calibration.optDouble("minimumCosineSimilarity", Double.NaN).toFloat()
            val lead = calibration.optDouble("minimumLeadOverSecond", Double.NaN).toFloat()
            if (!minScore.isFinite() || minScore !in -1f..1f ||
                !lead.isFinite() || lead !in 0f..2f
            ) return@withContext false

            referenceVectors = vectors
            minimumSimilarity = minScore
            minimumLead = lead
            calibrated = true
            true
        } catch (_: Exception) {
            close()
            false
        }
    }

    override suspend fun recognize(image: Bitmap): RecognitionDecision = withContext(Dispatchers.Default) {
        val runtime = interpreter ?: return@withContext RecognitionDecision.ModelUnavailable
        if (!isReady) return@withContext RecognitionDecision.ModelUnavailable

        val query = runCatching { embedWith(runtime, image) }.getOrNull()
            ?: return@withContext RecognitionDecision.Unknown
        val ranked = referenceVectors.map { (landmarkId, vectors) ->
            RecognitionCandidate(
                landmarkId = landmarkId,
                score = vectors.maxOfOrNull { cosineSimilarity(query, it) } ?: -1f
            )
        }.sortedByDescending { it.score }

        val best = ranked.firstOrNull() ?: return@withContext RecognitionDecision.Unknown
        val second = ranked.getOrNull(1)?.score ?: -1f
        if (best.score < minimumSimilarity) return@withContext RecognitionDecision.Unknown
        if (best.score - second < minimumLead) {
            return@withContext RecognitionDecision.Ambiguous(ranked.take(3))
        }
        RecognitionDecision.Match(best)
    }

    private fun embed(bitmap: Bitmap): FloatArray {
        val runtime = interpreter ?: error("Model is not loaded")
        return embedWith(runtime, bitmap)
    }

    private fun embedWith(runtime: Interpreter, source: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(source, INPUT_SIZE, INPUT_SIZE, true)
        val input = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            input.putFloat(((pixel shr 16) and 0xFF) / 255f)
            input.putFloat(((pixel shr 8) and 0xFF) / 255f)
            input.putFloat((pixel and 0xFF) / 255f)
        }
        input.rewind()
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        runtime.run(input, output)
        val vector = output[0]
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        require(norm.isFinite() && norm > 1e-12f) { "Invalid embedding vector" }
        return FloatArray(vector.size) { vector[it] / norm }
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        referenceVectors = emptyMap()
        calibrated = false
        isModelLoaded = false
    }

    companion object {
        const val MODEL_ASSET = "recognition/mobilenet_v3_small.tflite"
        const val CALIBRATION_ASSET = "recognition/calibration.json"
        const val EMBEDDING_SIZE = 1024
        const val INPUT_SIZE = 224
        const val MIN_REFERENCES_PER_LANDMARK = 3

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.isEmpty() || a.size != b.size) return -1f
            var dot = 0.0
            var normA = 0.0
            var normB = 0.0
            for (i in a.indices) {
                val x = a[i].toDouble()
                val y = b[i].toDouble()
                if (!x.isFinite() || !y.isFinite()) return -1f
                dot += x * y
                normA += x * x
                normB += y * y
            }
            if (normA <= 1e-24 || normB <= 1e-24) return -1f
            return (dot / (sqrt(normA) * sqrt(normB))).toFloat().coerceIn(-1f, 1f)
        }

        private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T {
            return try { block(this) } finally { if (!isRecycled) recycle() }
        }
    }
}
