package dz.vista.elbayadh.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddingMathTest {
    @Test
    fun identicalVectorsHaveSimilarityOne() {
        assertEquals(
            1f,
            MobileNetV3RecognitionEngine.cosineSimilarity(
                floatArrayOf(1f, 2f, 3f),
                floatArrayOf(1f, 2f, 3f)
            ),
            0.00001f
        )
    }

    @Test
    fun orthogonalVectorsHaveSimilarityZero() {
        assertEquals(
            0f,
            MobileNetV3RecognitionEngine.cosineSimilarity(
                floatArrayOf(1f, 0f),
                floatArrayOf(0f, 1f)
            ),
            0.00001f
        )
    }

    @Test
    fun oppositeVectorsHaveSimilarityMinusOne() {
        assertEquals(
            -1f,
            MobileNetV3RecognitionEngine.cosineSimilarity(
                floatArrayOf(1f, 0f),
                floatArrayOf(-1f, 0f)
            ),
            0.00001f
        )
    }

    @Test
    fun rejectsInvalidOrZeroVectors() {
        assertEquals(-1f, MobileNetV3RecognitionEngine.cosineSimilarity(floatArrayOf(), floatArrayOf()), 0f)
        assertEquals(-1f, MobileNetV3RecognitionEngine.cosineSimilarity(floatArrayOf(0f), floatArrayOf(1f)), 0f)
        assertEquals(-1f, MobileNetV3RecognitionEngine.cosineSimilarity(floatArrayOf(Float.NaN), floatArrayOf(1f)), 0f)
        assertEquals(-1f, MobileNetV3RecognitionEngine.cosineSimilarity(floatArrayOf(1f), floatArrayOf(1f, 2f)), 0f)
    }
}
