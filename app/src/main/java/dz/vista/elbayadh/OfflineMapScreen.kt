package dz.vista.elbayadh

import android.graphics.PointF
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dz.vista.elbayadh.data.CatalogueLanguage
import dz.vista.elbayadh.data.Landmark
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val EL_BAYADH_STYLE = "asset://maps/el-bayadh-style.json"
private const val LANDMARK_SOURCE_ID = "vista-landmarks"
private const val LANDMARK_LAYER_ID = "vista-landmark-points"

/** Native MapLibre renderer backed by the verified Wilaya 32 PMTiles archive bundled in APK assets. */
@Composable
internal fun OfflineMapScreen(
    language: String,
    landmarks: List<Landmark>,
    onOpenLandmark: (Landmark) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestLandmarks by rememberUpdatedState(landmarks)
    val latestOnOpenLandmark by rememberUpdatedState(onOpenLandmark)
    val mapView = remember(context) { MapView(context) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onCreate(Bundle())
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        mapView.getMapAsync { map: MapLibreMap ->
            map.setStyle(EL_BAYADH_STYLE) { style ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(33.68, 1.02))
                    .zoom(7.2)
                    .build()

                val features = latestLandmarks.mapNotNull { landmark ->
                    val latitude = landmark.latitude
                    val longitude = landmark.longitude
                    if (latitude == null || longitude == null ||
                        latitude !in 31.0..35.0 || longitude !in -1.0..3.0
                    ) return@mapNotNull null

                    Feature.fromGeometry(Point.fromLngLat(longitude, latitude)).apply {
                        addStringProperty("landmark_id", landmark.id)
                    }
                }
                if (features.isNotEmpty()) {
                    style.addSource(GeoJsonSource(LANDMARK_SOURCE_ID, FeatureCollection.fromFeatures(features.toTypedArray())))
                    style.addLayer(
                        CircleLayer(LANDMARK_LAYER_ID, LANDMARK_SOURCE_ID).withProperties(
                            PropertyFactory.circleColor("#C39A58"),
                            PropertyFactory.circleRadius(6.5f),
                            PropertyFactory.circleStrokeColor("#FFF8EA"),
                            PropertyFactory.circleStrokeWidth(2.0f)
                        )
                    )
                }

                map.addOnMapClickListener { coordinate ->
                    val screenPoint: PointF = map.projection.toScreenLocation(coordinate)
                    val selectedId = map.queryRenderedFeatures(screenPoint, arrayOf(LANDMARK_LAYER_ID))
                        .firstOrNull()
                        ?.getStringProperty("landmark_id")
                    val selected = latestLandmarks.firstOrNull { it.id == selectedId }
                    if (selected != null) {
                        latestOnOpenLandmark(selected)
                        true
                    } else {
                        false
                    }
                }
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    // The map is intentionally independent of network-backed style, glyph, and tile services.
    Box(Modifier.fillMaxSize().background(Color(0xFF071B2D))) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { }
        )
    }
}
