package dz.vista.elbayadh.recognition

import android.graphics.Bitmap

/**
 * Contract for an on-device landmark matcher.
 *
 * Implementations must run without network access and return candidates only
 * when their model and reference index are loaded and compatible.
 */
interface LandmarkRecognitionEngine {
    val isReady: Boolean

    suspend fun recognize(image: Bitmap): RecognitionDecision
}

data class RecognitionCandidate(
    val landmarkId: String,
    /** Calibrated model confidence in the inclusive range 0.0..1.0. */
    val confidence: Float
)

sealed interface RecognitionDecision {
    data class Match(val candidate: RecognitionCandidate) : RecognitionDecision
    data class Ambiguous(val candidates: List<RecognitionCandidate>) : RecognitionDecision
    data object Unknown : RecognitionDecision
    data object ModelUnavailable : RecognitionDecision
}

/**
 * Conservative acceptance policy. Thresholds must be calibrated against a
 * held-out El Bayadh image set before a production model can use this policy.
 */
data class RecognitionPolicy(
    val minimumConfidence: Float,
    val minimumLeadOverSecond: Float
) {
    init {
        require(minimumConfidence in 0f..1f)
        require(minimumLeadOverSecond in 0f..1f)
    }

    fun decide(candidates: List<RecognitionCandidate>): RecognitionDecision {
        if (candidates.isEmpty()) return RecognitionDecision.Unknown
        if (candidates.any {
                it.landmarkId.isBlank() ||
                    !it.confidence.isFinite() ||
                    it.confidence !in 0f..1f
            }
        ) return RecognitionDecision.Unknown

        val ranked = candidates
            .distinctBy { it.landmarkId }
            .sortedByDescending { it.confidence }
        val best = ranked.firstOrNull() ?: return RecognitionDecision.Unknown
        if (best.confidence < minimumConfidence) return RecognitionDecision.Unknown

        val secondConfidence = ranked.getOrNull(1)?.confidence ?: 0f
        if (best.confidence - secondConfidence < minimumLeadOverSecond) {
            return RecognitionDecision.Ambiguous(ranked.take(3))
        }
        return RecognitionDecision.Match(best)
    }
}

/** Safe default until a real, validated model and image index are bundled. */
class UnavailableLandmarkRecognitionEngine : LandmarkRecognitionEngine {
    override val isReady: Boolean = false

    override suspend fun recognize(image: Bitmap): RecognitionDecision =
        RecognitionDecision.ModelUnavailable
}
