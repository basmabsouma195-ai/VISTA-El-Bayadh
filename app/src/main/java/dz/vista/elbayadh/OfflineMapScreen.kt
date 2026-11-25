package dz.vista.elbayadh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Deliberately empty map destination.
 *
 * The previous Mapsforge implementation is retired. The map tab remains in the
 * navigation bar, but no map renderer, data package, location layer, or fake
 * placeholder content is started here. A replacement map system will be designed
 * and integrated as a separate, testable feature.
 */
@Composable
internal fun OfflineMapScreen(
    language: String,
    landmarks: List<dz.vista.elbayadh.data.Landmark>,
    onOpenLandmark: (dz.vista.elbayadh.data.Landmark) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071B2D))
    )
}
