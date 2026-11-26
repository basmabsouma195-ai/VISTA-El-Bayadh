package dz.vista.elbayadh

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dz.vista.elbayadh.data.CatalogueLanguage
import dz.vista.elbayadh.data.Landmark
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val EL_BAYADH_STYLE = "asset://maps/el-bayadh-style.json"
private const val LANDMARK_SOURCE_ID = "vista-landmarks"
private const val LANDMARK_LAYER_ID = "vista-landmark-points"
private const val ROADS_LAYER_ID = "roads-major"
private const val WATER_LAYER_ID = "water"
private const val SATELLITE_SOURCE_ID = "vista-satellite-source"
private const val SATELLITE_LAYER_ID = "vista-satellite-layer"
private const val TERRAIN_SOURCE_ID = "vista-terrain-source"
private const val TERRAIN_LAYER_ID = "vista-terrain-layer"

private val MapNavy = Color(0xFF10263D)
private val MapGold = Color(0xFFC39A58)
private val MapIvory = Color(0xFFF7F1E7)

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
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showLayers by remember { mutableStateOf(false) }
    var roadsVisible by remember { mutableStateOf(true) }
    var waterVisible by remember { mutableStateOf(true) }
    var baseMap by remember { mutableStateOf("standard") }
    var locationMessage by remember { mutableStateOf("") }

    fun applyLayerVisibility() {
        val style = mapRef?.style ?: return
        style.getLayer(ROADS_LAYER_ID)?.setProperties(
            PropertyFactory.visibility(if (roadsVisible) Property.VISIBLE else Property.NONE)
        )
        style.getLayer(WATER_LAYER_ID)?.setProperties(
            PropertyFactory.visibility(if (waterVisible) Property.VISIBLE else Property.NONE)
        )
        val rasterVisible = baseMap != "standard"
        style.getLayer(SATELLITE_LAYER_ID)?.setProperties(PropertyFactory.visibility(if (baseMap == "satellite") Property.VISIBLE else Property.NONE))
        style.getLayer(TERRAIN_LAYER_ID)?.setProperties(PropertyFactory.visibility(if (baseMap == "terrain") Property.VISIBLE else Property.NONE))
        listOf("background", "landcover", "landuse", "water", "waterway", "place-points", "buildings").forEach { id ->
            style.getLayer(id)?.setProperties(PropertyFactory.visibility(if (rasterVisible) Property.NONE else Property.VISIBLE))
        }
    }

    fun setBaseMap(mode: String) {
        baseMap = mode
        val style = mapRef?.style ?: return
        if (style.getSource(SATELLITE_SOURCE_ID) == null) {
            style.addSource(RasterSource(SATELLITE_SOURCE_ID, TileSet("satellite", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"), 256))
            style.addLayerBelow(RasterLayer(SATELLITE_LAYER_ID, SATELLITE_SOURCE_ID), ROADS_LAYER_ID)
        }
        if (style.getSource(TERRAIN_SOURCE_ID) == null) {
            style.addSource(RasterSource(TERRAIN_SOURCE_ID, TileSet("terrain", "https://a.tile.opentopomap.org/{z}/{x}/{y}.png"), 256))
            style.addLayerBelow(RasterLayer(TERRAIN_LAYER_ID, TERRAIN_SOURCE_ID), ROADS_LAYER_ID)
        }
        applyLayerVisibility()
    }

    val catalogueLanguage = when (language.lowercase()) {
        "french" -> CatalogueLanguage.FR
        "english" -> CatalogueLanguage.EN
        else -> CatalogueLanguage.AR
    }
    val matchingLandmarks = landmarks.filter {
        searchText.isNotBlank() && it.name(catalogueLanguage).contains(searchText.trim(), ignoreCase = true)
    }.take(6)
    val mappedMatches = matchingLandmarks.filter { it.latitude != null && it.longitude != null }

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
            mapRef = map
            map.setStyle(EL_BAYADH_STYLE) { style ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(33.05, 1.14))
                    .zoom(8.5)
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
                            PropertyFactory.circleRadius(7.5f),
                            PropertyFactory.circleStrokeColor("#FFF8EA"),
                            PropertyFactory.circleStrokeWidth(2.2f)
                        )
                    )
                }

                map.addOnMapClickListener { coordinate ->
                    val screenPoint = map.projection.toScreenLocation(coordinate)
                    val selectedId = map.queryRenderedFeatures(screenPoint, LANDMARK_LAYER_ID)
                        .firstOrNull()?.properties()?.get("landmark_id")?.asString
                    val selected = latestLandmarks.firstOrNull { it.id == selectedId }
                    if (selected != null) {
                        latestOnOpenLandmark(selected)
                        true
                    } else false
                }
                applyLayerVisibility()
            }
        }

        onDispose {
            mapRef = null
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val allowed = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = runCatching {
                manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }.getOrNull()
            if (location != null) {
                mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 14.0))
                locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "تم تحديد موقعك" else "Your location found"
            } else {
                locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "فعّل GPS وانتظر تحديد الموقع ثم حاول مجدداً" else "Enable GPS, wait for a fix and retry"
            }
        } else {
            locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "اسمح للتطبيق بالوصول إلى موقعك" else "Allow location access to use this feature"
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF071B2D))) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize(), update = { })

        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = MapNavy.copy(alpha = 0.96f), tonalElevation = 5.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MapGold)
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (catalogueLanguage == CatalogueLanguage.AR) "ابحث عن معلم أو منطقة..." else "Search landmarks and places...", color = MapIvory.copy(alpha = 0.65f), fontSize = 14.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MapIvory,
                            unfocusedTextColor = MapIvory,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MapGold
                        )
                    )
                    IconButton(onClick = { searchText = "" }) {
                        Text("×", color = MapIvory, fontSize = 22.sp)
                    }
                }
            }

            if (searchText.isNotBlank()) {
                Surface(shape = RoundedCornerShape(16.dp), color = MapNavy.copy(alpha = 0.97f)) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        if (matchingLandmarks.isEmpty()) {
                            Text(if (catalogueLanguage == CatalogueLanguage.AR) "لا توجد نتائج مطابقة" else "No matching results", color = MapIvory, modifier = Modifier.padding(10.dp))
                        } else matchingLandmarks.forEach { landmark ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val lat = landmark.latitude
                                    val lon = landmark.longitude
                                    if (lat != null && lon != null) {
                                        mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 13.0))
                                        onOpenLandmark(landmark)
                                    }
                                }.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(landmark.name(catalogueLanguage), color = MapIvory, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                Text(if (landmark.latitude != null && landmark.longitude != null) "على الخريطة" else "الموقع قيد التحديد", color = MapGold, fontSize = 10.sp)
                            }
                        }
                        if (mappedMatches.isEmpty() && matchingLandmarks.isNotEmpty()) {
                            Text(if (catalogueLanguage == CatalogueLanguage.AR) "هذه المعالم موثقة في الدليل، لكن إحداثياتها الدقيقة لم تُعتمد بعد." else "Coordinates are not yet verified for these places.", color = MapIvory.copy(alpha = 0.72f), fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapControlButton(Icons.Filled.ZoomIn, "Zoom in") { mapRef?.animateCamera(CameraUpdateFactory.zoomIn()) }
            MapControlButton(Icons.Filled.ZoomOut, "Zoom out") { mapRef?.animateCamera(CameraUpdateFactory.zoomOut()) }
            MapControlButton(Icons.Filled.MyLocation, "My location") {
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fine || coarse) {
                    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val location = runCatching {
                        manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    }.getOrNull()
                    if (location != null) mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 14.0))
                    else locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "لم يصل GPS إلى موقعك بعد" else "Waiting for a GPS fix"
                } else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
            MapControlButton(Icons.Filled.Layers, "Map layers") { showLayers = !showLayers }
        }

        if (showLayers) {
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 66.dp),
                shape = RoundedCornerShape(16.dp),
                color = MapNavy.copy(alpha = 0.97f),
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (catalogueLanguage == CatalogueLanguage.AR) "طبقات الخريطة" else "Map layers", color = MapGold, fontSize = 13.sp)
                    Text(if (catalogueLanguage == CatalogueLanguage.AR) "الطرق الرئيسية  ${if (roadsVisible) "✓" else "○"}" else "Main roads  ${if (roadsVisible) "✓" else "○"}", color = MapIvory, modifier = Modifier.clickable { roadsVisible = !roadsVisible; applyLayerVisibility() })
                    Text(if (catalogueLanguage == CatalogueLanguage.AR) "المياه  ${if (waterVisible) "✓" else "○"}" else "Water  ${if (waterVisible) "✓" else "○"}", color = MapIvory, modifier = Modifier.clickable { waterVisible = !waterVisible; applyLayerVisibility() })
                    Text(if (catalogueLanguage == CatalogueLanguage.AR) "الخريطة العادية" else "Standard", color = if (baseMap == "standard") MapGold else MapIvory, modifier = Modifier.clickable { setBaseMap("standard") })
                    Text(if (catalogueLanguage == CatalogueLanguage.AR) "الأقمار الصناعية" else "Satellite", color = if (baseMap == "satellite") MapGold else MapIvory, modifier = Modifier.clickable { setBaseMap("satellite") })
                    Text(if (catalogueLanguage == CatalogueLanguage.AR) "التضاريس" else "Terrain", color = if (baseMap == "terrain") MapGold else MapIvory, modifier = Modifier.clickable { setBaseMap("terrain") })
                }
            }
        }

        if (locationMessage.isNotBlank()) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 58.dp), shape = RoundedCornerShape(10.dp), color = MapNavy.copy(alpha = 0.95f)) {
                Text(locationMessage, color = MapIvory, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MapNavy.copy(alpha = 0.92f)
        ) {
            Text(
                if (catalogueLanguage == CatalogueLanguage.AR) "خريطة ولاية البيض • تعمل دون إنترنت" else "El Bayadh map • Offline",
                color = MapIvory,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(shape = RoundedCornerShape(14.dp), color = MapNavy.copy(alpha = 0.96f), tonalElevation = 5.dp) {
        IconButton(onClick = onClick, modifier = Modifier.size(46.dp)) {
            Icon(icon, contentDescription = description, tint = MapIvory)
        }
    }
}
