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
    /** Raw cosine-similarity score; never display this as a probability. */
    val score: Float
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
    val minimumScore: Float,
    val minimumLeadOverSecond: Float
) {
    init {
        require(minimumScore in -1f..1f)
        require(minimumLeadOverSecond in 0f..1f)
    }

    fun decide(candidates: List<RecognitionCandidate>): RecognitionDecision {
        if (candidates.isEmpty()) return RecognitionDecision.Unknown
        if (candidates.any {
                it.landmarkId.isBlank() ||
                    !it.score.isFinite() ||
                    it.score !in -1f..1f
            }
        ) return RecognitionDecision.Unknown

        val ranked = candidates
            .distinctBy { it.landmarkId }
            .sortedByDescending { it.score }
        val best = ranked.firstOrNull() ?: return RecognitionDecision.Unknown
        if (best.score < minimumScore) return RecognitionDecision.Unknown

        val secondConfidence = ranked.getOrNull(1)?.score ?: -1f
        if (best.score - secondConfidence < minimumLeadOverSecond) {
            return RecognitionDecision.Ambiguous(ranked.take(3))
        }
        return RecognitionDecision.Match(best)
    }
}

/**
 * Conservative gate for preview-only suggestions.
 * These are temporary guardrails, not calibrated accuracy thresholds.
 * Replace them only after evaluation on separate El Bayadh and unknown-scene sets.
 */
object PreviewSuggestionPolicy {
    const val MINIMUM_SIMILARITY = 0.70f
    const val MINIMUM_LEAD_OVER_SECOND = 0.12f

    fun filter(candidates: List<RecognitionCandidate>): List<RecognitionCandidate> {
        if (candidates.isEmpty()) return emptyList()
        if (candidates.any {
                it.landmarkId.isBlank() || !it.score.isFinite() || it.score !in -1f..1f
            }
        ) return emptyList()

        val ranked = candidates.distinctBy { it.landmarkId }.sortedByDescending { it.score }
        val best = ranked.firstOrNull() ?: return emptyList()
        if (best.score < MINIMUM_SIMILARITY) return emptyList()
        val second = ranked.getOrNull(1)?.score
        if (second != null && best.score - second < MINIMUM_LEAD_OVER_SECOND) {
            return emptyList()
        }
        return ranked.take(3)
    }
}

/** Safe default until a real, validated model and image index are bundled. */
class UnavailableLandmarkRecognitionEngine : LandmarkRecognitionEngine {
    override val isReady: Boolean = false

    override suspend fun recognize(image: Bitmap): RecognitionDecision =
        RecognitionDecision.ModelUnavailable
}
