package dz.vista.elbayadh.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionPolicyTest {
    private val policy = RecognitionPolicy(
        minimumConfidence = 0.70f,
        minimumLeadOverSecond = 0.15f
    )

    @Test
    fun acceptsOnlyClearHighConfidenceWinner() {
        val result = policy.decide(
            listOf(
                RecognitionCandidate("ksar-bent-khass", 0.91f),
                RecognitionCandidate("ksar-ghassoul", 0.62f)
            )
        )

        assertEquals(
            RecognitionDecision.Match(RecognitionCandidate("ksar-bent-khass", 0.91f)),
            result
        )
    }

    @Test
    fun rejectsLowConfidenceScene() {
        assertEquals(
            RecognitionDecision.Unknown,
            policy.decide(listOf(RecognitionCandidate("unknown-site", 0.69f)))
        )
    }

    @Test
    fun refusesCloseCompetingCandidates() {
        val result = policy.decide(
            listOf(
                RecognitionCandidate("ksar-bent-khass", 0.88f),
                RecognitionCandidate("ksar-ghassoul", 0.82f)
            )
        )

        assertTrue(result is RecognitionDecision.Ambiguous)
    }

    @Test
    fun rejectsInvalidConfidenceValues() {
        assertEquals(
            RecognitionDecision.Unknown,
            policy.decide(listOf(RecognitionCandidate("bad", Float.NaN)))
        )
        assertEquals(
            RecognitionDecision.Unknown,
            policy.decide(listOf(RecognitionCandidate("bad", 1.1f)))
        )
    }

    @Test
    fun emptyCandidateSetIsUnknown() {
        assertEquals(RecognitionDecision.Unknown, policy.decide(emptyList()))
    }
}
