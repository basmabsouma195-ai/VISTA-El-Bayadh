package dz.vista.elbayadh

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dz.vista.elbayadh.data.CatalogueLanguage
import dz.vista.elbayadh.data.Landmark
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val VISTA_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private val MapNavy = Color(0xFF10263B)

private data class MapMunicipality(
    val ar: String,
    val fr: String,
    val en: String,
    val latitude: Double,
    val longitude: Double
)

/** Native vector map with a bounded, user-initiated offline-area download. */
@Composable
internal fun MapScreen(
    language: CatalogueLanguage,
    landmarks: List<Landmark>,
    favoriteIds: Set<String>,
    onFavoriteToggle: (String) -> Unit,
    onOpenLandmark: (Landmark) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ar = language == CatalogueLanguage.AR
    val fr = language == CatalogueLanguage.FR
    fun tr(a: String, f: String, e: String) = if (ar) a else if (fr) f else e

    remember(context) {
        MapLibre.getInstance(context)
        val dispatcher = Dispatcher().apply { maxRequests = 4; maxRequestsPerHost = 4 }
        val client = OkHttpClient.Builder().dispatcher(dispatcher).addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("User-Agent", "VISTA-El-Bayadh/0.1.2 (Android)").build())
        }.build()
        HttpRequestUtil.setOkHttpClient(client)
    }

    val mapView = remember(context) { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var query by remember { mutableStateOf("") }
    var showLayers by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var progressText by remember { mutableStateOf("") }
    var downloadedBytes by remember { mutableStateOf(0L) }
    val mappedLandmarkCount = landmarks.count { it.latitude != null && it.longitude != null }

    val municipalities = remember {
        listOf(
            MapMunicipality("البيض", "El Bayadh", "El Bayadh", 33.6825, 1.0307),
            MapMunicipality("الرقاصة", "Rogassa", "Rogassa", 34.0182474, 0.9262211),
            MapMunicipality("ستيتن", "Stitten", "Stitten", 33.7563995, 1.2241683),
            MapMunicipality("بريزينة", "Brézina", "Brezina", 33.0984038, 1.2571394),
            MapMunicipality("غسول", "Ghassoul", "Ghassoul", 33.3765868, 1.2038386),
            MapMunicipality("بوعلام", "Boualem", "Boualem", 33.7281417, 1.5345064),
            MapMunicipality("بوقطب", "Bougtob", "Bougtob", 34.0405202, 0.0870670),
            MapMunicipality("الخيثر", "El Kheïther", "El Kheither", 34.1489752, 0.0675861),
            MapMunicipality("الكاف الأحمر", "Kef El Ahmar", "Kef El Ahmar", 33.8504062, 0.6332841),
            MapMunicipality("كراكدة", "Krakda", "Krakda", 33.3211117, 0.9596477),
            MapMunicipality("الشقيق", "Cheguig", "Cheguig", 34.0856989, 1.1484752),
            MapMunicipality("سيدي عامر", "Sidi Ameur", "Sidi Ameur", 33.76833, 1.43306),
            MapMunicipality("توسمولين", "Tousmouline", "Tousmouline", 33.65167, 0.30861),
            MapMunicipality("سيدي سليمان", "Sidi Slimane", "Sidi Slimane", 33.832116, 1.731609),
            MapMunicipality("سيدي طيفور", "Sidi Tifour", "Sidi Tifour", 33.7205714, 1.6871133),
            MapMunicipality("الأبيض سيدي الشيخ", "El Abiodh Sidi Cheikh", "El Abiodh Sidi Cheikh", 32.896484, 0.5463928),
            MapMunicipality("عين العراك", "Aïn El Orak", "Ain El Orak", 33.4101, 0.73853),
            MapMunicipality("عرباوة", "Arbaouat", "Arbaouat", 33.0882649, 0.5812401),
            MapMunicipality("بوسمغون", "Boussemghoun", "Boussemghoun", 32.8678479, 0.0232401),
            MapMunicipality("شلالة", "Chellala", "Chellala", 33.0170236, 0.0896971),
            MapMunicipality("البنود", "El Bnoud", "El Bnoud", 32.3096365, 0.2450916),
            MapMunicipality("المحرة", "El Mehara", "El Mehara", 33.3131942, 0.3645784)
        )
    }
    val matchingLandmarks = remember(landmarks, query, language) {
        if (query.isBlank()) emptyList() else landmarks.filter {
            it.name(language).contains(query.trim(), true)
        }.take(7)
    }
    val matchingMunicipalities = remember(municipalities, query, language) {
        if (query.isBlank()) emptyList() else municipalities.filter { names ->
            val name = when (language) {
                CatalogueLanguage.AR -> names.ar
                CatalogueLanguage.FR -> names.fr
                else -> names.en
            }
            name.contains(query.trim(), true) ||
                names.ar.contains(query.trim(), true) ||
                names.fr.contains(query.trim(), true) ||
                names.en.contains(query.trim(), true)
        }.take(7)
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF4F0E8))) {
        AndroidView(
            factory = {
                mapView.apply {
                    onCreate(Bundle())
                    getMapAsync { nativeMap ->
                        map = nativeMap
                        nativeMap.setStyle(VISTA_MAP_STYLE) { style ->
                            addLandmarkMarkers(style, landmarks)
                        }
                        nativeMap.addOnMapClickListener { tappedPoint ->
                            val screenPoint = nativeMap.projection.toScreenLocation(tappedPoint)
                            val markerHits = nativeMap.queryRenderedFeatures(
                                screenPoint,
                                VISTA_LANDMARK_LAYER_ID
                            )
                            if (markerHits.isNotEmpty()) {
                                val nearest = landmarks
                                    .filter { it.latitude != null && it.longitude != null }
                                    .minByOrNull { landmark ->
                                        val latDelta = landmark.latitude!! - tappedPoint.latitude
                                        val lonDelta = landmark.longitude!! - tappedPoint.longitude
                                        latDelta * latDelta + lonDelta * lonDelta
                                    }
                                if (nearest != null) {
                                    onOpenLandmark(nearest)
                                    true
                                } else false
                            } else false
                        }
                        nativeMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                            .target(LatLng(33.6825, 1.0307)).zoom(11.2).build()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 7.dp) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text(tr("ابحث عن مكان أو معلم", "Rechercher un lieu ou un site", "Search a place or landmark"), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MapNavy) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (matchingLandmarks.isNotEmpty() || matchingMunicipalities.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Column {
                        matchingMunicipalities.forEach { names ->
                            val name = when (language) {
                                CatalogueLanguage.AR -> names.ar
                                CatalogueLanguage.FR -> names.fr
                                else -> names.en
                            }
                            Column(
                                Modifier.fillMaxWidth().clickable {
                                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                        LatLng(names.latitude, names.longitude), 12.8
                                    ))
                                    query = name
                                }.padding(horizontal = 15.dp, vertical = 10.dp)
                            ) {
                                Text(name, color = MapNavy, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(tr("بلدية · عرض مركزها على الخريطة", "Commune · afficher son centre sur la carte", "Municipality · show its centre on the map"), color = Color(0xFF687582), fontSize = 10.sp)
                            }
                        }
                        matchingLandmarks.forEach { place ->
                            Column(
                                Modifier.fillMaxWidth().clickable {
                                    val lat = place.latitude
                                    val lon = place.longitude
                                    if (lat != null && lon != null) {
                                        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15.5))
                                    }
                                    query = place.name(language)
                                    onOpenLandmark(place)
                                }.padding(horizontal = 15.dp, vertical = 10.dp)
                            ) {
                                Text(place.name(language), color = MapNavy, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    if (place.latitude != null && place.longitude != null)
                                        tr("معلم · موقع محدد على الخريطة", "Site · position sur la carte", "Landmark · mapped location")
                                    else tr("معلم · الموقع غير محدد على الخريطة", "Site · position non géolocalisée", "Landmark · location not mapped"),
                                    color = Color(0xFF687582), fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            MapControl(Icons.Default.Add, tr("تكبير", "Zoom in", "Zoom in")) { map?.animateCamera(CameraUpdateFactory.zoomIn()) }
            MapControl(Icons.Default.Remove, tr("تصغير", "Zoom out", "Zoom out")) { map?.animateCamera(CameraUpdateFactory.zoomOut()) }
            MapControl(Icons.Default.MyLocation, tr("مركز ولاية البيض", "Centrer sur El Bayadh", "Center El Bayadh")) {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(33.6825, 1.0307), 11.2))
            }
            MapControl(Icons.Default.Layers, tr("طبقات الخريطة", "Couches", "Map layers")) { showLayers = true }
        }

        Card(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(11.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("ولاية البيض", "Wilaya d’El Bayadh", "El Bayadh Province"), color = MapNavy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(tr("خريطة تفاعلية قابلة للحفظ دون إنترنت", "Carte interactive avec téléchargement hors ligne", "Interactive map with offline downloads"), color = Color(0xFF687582), fontSize = 11.sp)
                        Text(
                            tr("${landmarks.size} معلم مسجل · ${mappedLandmarkCount} محدد على الخريطة",
                                "${landmarks.size} sites répertoriés · ${mappedLandmarkCount} géolocalisés",
                                "${landmarks.size} landmarks listed · ${mappedLandmarkCount} mapped"),
                            color = Color(0xFF687582), fontSize = 10.sp
                        )
                    }
                    val currentProgress = progress
                    Button(
                        onClick = {
                            progress = 0.01f
                            progressText = ""
                            beginOfflineDownload(context, map) { p, bytes, message ->
                                progress = p; downloadedBytes = bytes; progressText = message
                            }
                        },
                        enabled = map != null && (currentProgress == null || currentProgress == 0f || currentProgress >= 1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (currentProgress != null && currentProgress >= 1f) tr("تم الحفظ", "Enregistrée", "Saved") else tr("حفظ المنطقة", "Télécharger", "Download"))
                    }
                }
                progress?.let { p ->
                    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                    Text(
                        if (p >= 1f) tr("اكتمل حفظ المنطقة", "Téléchargement terminé", "Area saved offline")
                        else tr("جارٍ الحفظ: ${(p * 100).toInt()}% · ${downloadedBytes / (1024 * 1024)} MB", "Téléchargement : ${(p * 100).toInt()}% · ${downloadedBytes / (1024 * 1024)} MB", "Downloading: ${(p * 100).toInt()}% · ${downloadedBytes / (1024 * 1024)} MB"),
                        color = MapNavy, fontSize = 11.sp
                    )
                }
                if (progressText.isNotBlank()) Text(progressText, color = Color(0xFF687582), fontSize = 11.sp)
            }
        }
    }

    if (showLayers) {
        AlertDialog(
            onDismissRequest = { showLayers = false },
            title = { Text(tr("أنماط الخريطة", "Styles de carte", "Map styles")) },
            text = { Text(tr(
                "الخريطة الأساسية متاحة الآن. لن نفعّل القمر الصناعي والتضاريس قبل اعتماد مصادر صور وبيانات تسمح بالاستخدام والتنزيل دون اتصال.",
                "Le fond cartographique est disponible. Les vues satellite et relief seront activées après validation de sources autorisant le téléchargement hors ligne.",
                "The standard map is available. Satellite and terrain will be enabled after validating imagery sources that permit offline downloads."
            )) },
            confirmButton = { TextButton(onClick = { showLayers = false }) { Text(tr("حسنًا", "OK", "OK")) } }
        )
    }
}

@Composable
private fun MapModeChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        color = if (selected) MapNavy else Color.White,
        contentColor = if (selected) Color.White else Color(0xFF7D8790),
        shape = RoundedCornerShape(22.dp), shadowElevation = 3.dp
    ) {
        Text(label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MapControl(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(Modifier.size(43.dp).clickable(onClick = onClick), shape = CircleShape, color = Color.White, shadowElevation = 5.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description, tint = MapNavy) }
    }
}

private fun beginOfflineDownload(
    context: Context,
    map: MapLibreMap?,
    onProgress: (Float, Long, String) -> Unit
) {
    val center = map?.cameraPosition?.target ?: LatLng(33.6825, 1.0307)
    val bounds = LatLngBounds.Builder()
        .include(LatLng(center.latitude - 0.012, center.longitude - 0.015))
        .include(LatLng(center.latitude + 0.012, center.longitude + 0.015))
        .build()
    val manager = OfflineManager.getInstance(context)
    manager.setOfflineMapboxTileCountLimit(3000L)
    val definition = OfflineTilePyramidRegionDefinition(
        VISTA_MAP_STYLE, bounds, 10.0, 14.0, context.resources.displayMetrics.density
    )
    val metadata = JSONObject()
        .put("name", "VISTA-El-Bayadh-" + System.currentTimeMillis())
        .put("centerLat", center.latitude)
        .put("centerLon", center.longitude)
        .toString().toByteArray(Charsets.UTF_8)

    manager.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
        override fun onCreate(offlineRegion: OfflineRegion) {
            offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                override fun onStatusChanged(status: OfflineRegionStatus) {
                    val required = status.requiredResourceCount.coerceAtLeast(1L)
                    val percent = (status.completedResourceCount.toFloat() / required).coerceIn(0f, 1f)
                    val bytes = status.completedResourceSize
                    if (bytes > 50L * 1024L * 1024L) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        onProgress(0f, bytes, "تم إيقاف التنزيل عند تجاوز 50 MB؛ يمكنك اختيار نطاق أصغر وإعادة المحاولة.")
                    } else if (status.isComplete) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        onProgress(1f, bytes, "تم حفظ المنطقة على الجهاز.")
                    } else {
                        onProgress(percent, bytes, "يُحفظ نطاق محلي محدود. يمكنك متابعة استخدام الخريطة.")
                    }
                }

                override fun onError(error: OfflineRegionError) {
                    onProgress(0f, 0L, "تعذّر تنزيل بعض موارد الخريطة: ${error.message}")
                }

                override fun mapboxTileCountLimitExceeded(limit: Long) {
                    offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                    onProgress(0f, 0L, "المنطقة أكبر من الحد المسموح؛ قرّب الخريطة واختر نطاقًا أصغر.")
                }
            })
            offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
            onProgress(0.01f, 0L, "بدأ تنزيل المنطقة…")
        }

        override fun onError(error: String) {
            onProgress(0f, 0L, "تعذّر إنشاء تنزيل دون اتصال: $error")
        }
    })
}

private const val VISTA_LANDMARK_SOURCE_ID = "vista-landmarks-source"
private const val VISTA_LANDMARK_LAYER_ID = "vista-landmarks-layer"

/** Render every geocoded catalogue landmark as a native map marker. */
private fun addLandmarkMarkers(
    style: org.maplibre.android.maps.Style,
    landmarks: List<Landmark>
) {
    val features = landmarks.mapNotNull { landmark ->
        val latitude = landmark.latitude ?: return@mapNotNull null
        val longitude = landmark.longitude ?: return@mapNotNull null
        if (!latitude.isFinite() || !longitude.isFinite()) return@mapNotNull null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return@mapNotNull null
        Feature.fromGeometry(Point.fromLngLat(longitude, latitude))
    }
    if (features.isEmpty()) return

    style.addSource(
        GeoJsonSource(
            VISTA_LANDMARK_SOURCE_ID,
            FeatureCollection.fromFeatures(features.toTypedArray())
        )
    )
    style.addLayer(
        CircleLayer(VISTA_LANDMARK_LAYER_ID, VISTA_LANDMARK_SOURCE_ID).withProperties(
            PropertyFactory.circleColor("#D97706"),
            PropertyFactory.circleRadius(6.5f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2.0f),
            PropertyFactory.circleOpacity(0.96f)
        )
    )
}
