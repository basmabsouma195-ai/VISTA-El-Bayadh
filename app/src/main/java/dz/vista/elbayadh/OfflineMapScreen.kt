package dz.vista.elbayadh

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.Location
import android.location.LocationListener
import android.os.Looper
import android.os.Handler
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.LineString

private const val EL_BAYADH_STYLE = "asset://maps/el-bayadh-style.json"
private const val SEARCH_SOURCE_ID = "vista-search-target"
private const val SEARCH_LAYER_ID = "vista-search-target-point"
private const val ROADS_LAYER_ID = "roads-major"
private const val ROADS_MINOR_LAYER_ID = "roads-minor"
private const val WATER_LAYER_ID = "water"
private const val SATELLITE_SOURCE_ID = "vista-satellite-source"
private const val SATELLITE_LAYER_ID = "vista-satellite-layer"
private const val TERRAIN_SOURCE_ID = "vista-terrain-source"
private const val TERRAIN_LAYER_ID = "vista-terrain-layer"
private const val USER_SOURCE_ID = "vista-user-location"
private const val USER_LAYER_ID = "vista-user-location-point"
private const val ROUTE_SOURCE_ID = "vista-road-route"
private const val ROUTE_LAYER_ID = "vista-road-route-line"
private const val LOCAL_PLACES_SOURCE_ID = "vista-local-places"
private const val LOCAL_PLACES_LAYER_ID = "vista-local-place-markers"
private const val LOCAL_PLACES_LABEL_LAYER_ID = "vista-local-place-labels"

private data class PlaceSearchResult(val name: String, val latitude: Double, val longitude: Double, val landmarkId: String? = null)
private data class LocalMapPlace(
    val name: String,
    val nameAr: String?,
    val nameFr: String?,
    val nameEn: String?,
    val latitude: Double,
    val longitude: Double,
    val category: String
) {
    fun displayName(language: CatalogueLanguage): String = when (language) {
        CatalogueLanguage.AR -> nameAr ?: name
        CatalogueLanguage.FR -> nameFr ?: name
        CatalogueLanguage.EN -> nameEn ?: name
    }

    fun matches(query: String): Boolean = listOfNotNull(name, nameAr, nameFr, nameEn)
        .any { it.contains(query, ignoreCase = true) }
}

private fun loadLocalMapPlaces(context: Context): List<LocalMapPlace> {
    val root = JSONObject(context.assets.open("maps/places.json").bufferedReader().use { it.readText() })
    val items = root.getJSONArray("items")
    return (0 until items.length()).mapNotNull { index ->
        val item = items.getJSONObject(index)
        val latitude = item.optDouble("latitude", Double.NaN)
        val longitude = item.optDouble("longitude", Double.NaN)
        val name = item.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        if (!latitude.isFinite() || !longitude.isFinite()) return@mapNotNull null
        LocalMapPlace(
            name,
            item.optString("name_ar").takeIf { it.isNotBlank() },
            item.optString("name_fr").takeIf { it.isNotBlank() },
            item.optString("name_en").takeIf { it.isNotBlank() },
            latitude,
            longitude,
            item.optString("category", "place")
        )
    }
}


private data class RoadRouteResult(val feature: Feature, val distanceMeters: Double, val durationSeconds: Double)
private data class WilayaPolygon(val rings: List<List<Pair<Double, Double>>>)
private data class WilayaMapBoundary(
    val polygons: List<WilayaPolygon>,
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double
)

private fun loadWilayaMapBoundary(context: Context): WilayaMapBoundary {
    val root = JSONObject(context.assets.open("maps/boundary.json").bufferedReader().use { it.readText() })
    val geometry = root.getJSONObject("geometry")
    val bounds = root.getJSONArray("bbox_wsen")
    fun parsePolygon(ringsJson: JSONArray): WilayaPolygon {
        val rings = (0 until ringsJson.length()).map { ringIndex ->
            val points = ringsJson.getJSONArray(ringIndex)
            (0 until points.length()).map { pointIndex ->
                val point = points.getJSONArray(pointIndex)
                point.getDouble(0) to point.getDouble(1)
            }
        }
        return WilayaPolygon(rings)
    }
    val coordinates = geometry.getJSONArray("coordinates")
    val polygons = when (geometry.getString("type")) {
        "Polygon" -> listOf(parsePolygon(coordinates))
        "MultiPolygon" -> (0 until coordinates.length()).map { parsePolygon(coordinates.getJSONArray(it)) }
        else -> emptyList()
    }
    return WilayaMapBoundary(
        polygons,
        bounds.getDouble(0),
        bounds.getDouble(1),
        bounds.getDouble(2),
        bounds.getDouble(3)
    )
}

private fun pointInRing(longitude: Double, latitude: Double, ring: List<Pair<Double, Double>>): Boolean {
    var inside = false
    var previous = ring.lastIndex
    for (index in ring.indices) {
        val (xi, yi) = ring[index]
        val (xj, yj) = ring[previous]
        if ((yi > latitude) != (yj > latitude)) {
            val crossingLongitude = (xj - xi) * (latitude - yi) / (yj - yi) + xi
            if (longitude < crossingLongitude) inside = !inside
        }
        previous = index
    }
    return inside
}

private fun isInsideWilaya(latitude: Double, longitude: Double, polygons: List<WilayaPolygon>): Boolean {
    return polygons.any { polygon ->
        val outer = polygon.rings.firstOrNull() ?: return@any false
        pointInRing(longitude, latitude, outer) &&
            polygon.rings.drop(1).none { hole -> pointInRing(longitude, latitude, hole) }
    }
}

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
    val wilayaMapBoundary = remember(context) { runCatching { loadWilayaMapBoundary(context) }.getOrNull() }
    val wilayaPolygons = wilayaMapBoundary?.polygons.orEmpty()
    val searchViewbox = wilayaMapBoundary?.let { "${it.west},${it.north},${it.east},${it.south}" } ?: "-0.4159,34.4482,2.3603,30.7017"
    // Center on a verified catalogue place in El Bayadh city.\n    val initialMapCenter = LatLng(33.682515, 1.0307273)
    val localMapPlaces = remember(context) {
        val loaded = runCatching { loadLocalMapPlaces(context) }
            .onFailure { Log.e("VISTA_MAP", "Failed to load local offline places", it) }
            .getOrDefault(emptyList())
        Log.i("VISTA_MAP", "Loaded offline place index: ${loaded.size} features")
        loaded
    }
    val latestOnOpenLandmark by rememberUpdatedState(onOpenLandmark)
    val mapView = remember(context) { MapView(context) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showLayers by remember { mutableStateOf(false) }
    var roadsVisible by remember { mutableStateOf(true) }
    var waterVisible by remember { mutableStateOf(true) }
    var baseMap by remember { mutableStateOf("standard") }
    var locationMessage by remember { mutableStateOf("") }
    var remotePlaces by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var selectedTarget by remember { mutableStateOf<PlaceSearchResult?>(null) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isSearchingPlaces by remember { mutableStateOf(false) }
    var searchTrigger by remember { mutableStateOf(0) }
    var activeLocationListener by remember { mutableStateOf<LocationListener?>(null) }
    var routeTrigger by remember { mutableStateOf(0) }
    var routeDistanceMeters by remember { mutableStateOf<Double?>(null) }
    var routeDurationSeconds by remember { mutableStateOf<Double?>(null) }
    var routeMessage by remember { mutableStateOf("") }
    var isRouting by remember { mutableStateOf(false) }

    fun applyLayerVisibility() {
        val style = mapRef?.style ?: return
        val rasterVisible = baseMap != "standard"
        listOf(ROADS_LAYER_ID, ROADS_MINOR_LAYER_ID).forEach { id ->
            style.getLayer(id)?.setProperties(
                PropertyFactory.visibility(if (roadsVisible) Property.VISIBLE else Property.NONE)
            )
        }
        listOf(WATER_LAYER_ID, "waterway").forEach { id ->
            style.getLayer(id)?.setProperties(
                PropertyFactory.visibility(if (!rasterVisible && waterVisible) Property.VISIBLE else Property.NONE)
            )
        }
        listOf("background", "landcover", "landuse", "buildings").forEach { id ->
            style.getLayer(id)?.setProperties(
                PropertyFactory.visibility(if (rasterVisible) Property.NONE else Property.VISIBLE)
            )
        }
        style.getLayer(SATELLITE_LAYER_ID)?.setProperties(PropertyFactory.visibility(if (baseMap == "satellite") Property.VISIBLE else Property.NONE))
        style.getLayer(TERRAIN_LAYER_ID)?.setProperties(PropertyFactory.visibility(if (baseMap == "terrain") Property.VISIBLE else Property.NONE))
        listOf("place-points", "vista-place-labels", "vista-road-labels", SEARCH_LAYER_ID, USER_LAYER_ID, ROUTE_LAYER_ID).forEach { id ->
            style.getLayer(id)?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
        }
    }

    fun setBaseMap(mode: String) {
        baseMap = mode
        val style = mapRef?.style ?: return
        if (style.getSource(SATELLITE_SOURCE_ID) == null) {
            style.addSource(RasterSource(SATELLITE_SOURCE_ID, TileSet("satellite", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"), 256))
            style.addLayerBelow(RasterLayer(SATELLITE_LAYER_ID, SATELLITE_SOURCE_ID), "roads-minor")
        }
        if (style.getSource(TERRAIN_SOURCE_ID) == null) {
            style.addSource(RasterSource(TERRAIN_SOURCE_ID, TileSet("terrain", "https://a.tile.opentopomap.org/{z}/{x}/{y}.png"), 256))
            style.addLayerBelow(RasterLayer(TERRAIN_LAYER_ID, TERRAIN_SOURCE_ID), "roads-minor")
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

    val matchingMapPlaces = if (searchText.isBlank()) emptyList() else localMapPlaces
        .asSequence()
        .filter { it.matches(searchText.trim()) }
        .filter { place ->
            if (wilayaPolygons.isNotEmpty()) isInsideWilaya(place.latitude, place.longitude, wilayaPolygons)
            else place.latitude in 30.7017..34.4482 && place.longitude in -0.4159..2.3603
        }
        .take(8)
        .toList()

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
                    .target(initialMapCenter)
                    .zoom(12.0)
                    .build()
                map.cameraPosition.target?.let { target -> Log.i("VISTA_MAP", "Initial camera: ${target.latitude}, ${target.longitude}, zoom=${map.cameraPosition.zoom}") }

                // Keep the map uncluttered on first opening: only the selected search result is pinned.
                style.addSource(GeoJsonSource(SEARCH_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray<Feature>())))
                style.addLayer(CircleLayer(SEARCH_LAYER_ID, SEARCH_SOURCE_ID).withProperties(
                    PropertyFactory.circleColor("#C39A58"), PropertyFactory.circleRadius(9f),
                    PropertyFactory.circleStrokeColor("#FFF8EA"), PropertyFactory.circleStrokeWidth(2.5f)
                ))
                style.addSource(GeoJsonSource(USER_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray<Feature>())))
                style.addLayer(CircleLayer(USER_LAYER_ID, USER_SOURCE_ID).withProperties(
                    PropertyFactory.circleColor("#1677FF"), PropertyFactory.circleRadius(9f),
                    PropertyFactory.circleStrokeColor("#FFFFFF"), PropertyFactory.circleStrokeWidth(3f)
                ))
                style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray<Feature>())))
                style.addLayer(LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    PropertyFactory.lineColor("#2878FF"),
                    PropertyFactory.lineWidth(5.5f),
                    PropertyFactory.lineOpacity(0.94f)
                ))

                // Render the bundled offline place catalogue directly as GeoJSON.
                runCatching {
                    val localPlaceFeatures = localMapPlaces.map { place ->
                        Feature.fromGeometry(Point.fromLngLat(place.longitude, place.latitude)).apply {
                            addStringProperty("place_name", place.displayName(catalogueLanguage))
                            addStringProperty("category", place.category)
                        }
                    }
                    Log.i("VISTA_MAP", "Prepared ${localPlaceFeatures.size} offline GeoJSON place features")
                    style.addSource(GeoJsonSource(LOCAL_PLACES_SOURCE_ID, FeatureCollection.fromFeatures(localPlaceFeatures)))
                    Log.i("VISTA_MAP", "Added offline place GeoJSON source")
                    style.addLayer(CircleLayer(LOCAL_PLACES_LAYER_ID, LOCAL_PLACES_SOURCE_ID).withProperties(
                        PropertyFactory.circleColor("#B98236"),
                        PropertyFactory.circleRadius(5.0f),
                        PropertyFactory.circleStrokeColor("#FFFDF8"),
                        PropertyFactory.circleStrokeWidth(1.2f)
                    ))
                    Log.i("VISTA_MAP", "Added offline place marker layer")
                    style.addLayer(SymbolLayer(LOCAL_PLACES_LABEL_LAYER_ID, LOCAL_PLACES_SOURCE_ID).withProperties(
                        PropertyFactory.textField("{place_name}"),
                        PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                        PropertyFactory.textSize(11.5f),
                        PropertyFactory.textColor("#34485A"),
                        PropertyFactory.textHaloColor("#FFFDF8"),
                        PropertyFactory.textHaloWidth(1.2f),
                        PropertyFactory.textOffset(arrayOf(0f, 0.8f)),
                        PropertyFactory.textAllowOverlap(false)
                    ))
                    Log.i("VISTA_MAP", "Added offline place label layer")
                }.onFailure { Log.e("VISTA_MAP", "Failed to add offline place layers", it) }

                map.addOnMapClickListener { coordinate ->
                    val screenPoint = map.projection.toScreenLocation(coordinate)
                    val selectedId = map.queryRenderedFeatures(screenPoint, SEARCH_LAYER_ID)
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

    var permissionRequestAction: (() -> Unit)? = null
    var freshLocationAction: (() -> Unit)? = null

    fun showSearchTarget(name: String, latitude: Double, longitude: Double, landmarkId: String? = null) {
        val feature = Feature.fromGeometry(Point.fromLngLat(longitude, latitude)).apply {
            addStringProperty("place_name", name)
            if (landmarkId != null) addStringProperty("landmark_id", landmarkId)
        }
        selectedTarget = PlaceSearchResult(name, latitude, longitude, landmarkId)
        routeDistanceMeters = null
        routeDurationSeconds = null
        routeMessage = ""
        mapRef?.style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(emptyArray<Feature>()))
        mapRef?.style?.getSourceAs<GeoJsonSource>(SEARCH_SOURCE_ID)?.setGeoJson(feature)
        mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 13.0))
    }

    fun showUserLocation(location: Location) {
        currentLocation = location
        val point = Point.fromLngLat(location.longitude, location.latitude)
        mapRef?.style?.getSourceAs<GeoJsonSource>(USER_SOURCE_ID)?.setGeoJson(Feature.fromGeometry(point))
        mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 14.0))
        locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "تم تحديد موقعك" else "Your location found"
    }

    fun requestFreshLocation() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            permissionRequestAction?.invoke()
            return
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        val last = providers.mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }.maxByOrNull { it.time }
        if (last != null) showUserLocation(last)
        locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "جارٍ تحديد موقعك الحالي…" else "Getting your current location…"
        activeLocationListener?.let { runCatching { manager.removeUpdates(it) } }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { showUserLocation(location) }
            @Deprecated("Deprecated by Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        activeLocationListener = listener
        var requested = false
        providers.forEach { provider ->
            try {
                manager.requestLocationUpdates(provider, 1000L, 0f, listener, Looper.getMainLooper())
                requested = true
            } catch (_: SecurityException) { } catch (_: IllegalArgumentException) { }
        }
        if (!requested && last == null) {
            locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "تعذر تحديد الموقع. تحقق من تشغيل GPS وخدمات الموقع." else "Location unavailable. Check GPS and location services."
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                if (activeLocationListener === listener && locationMessage.contains("جارٍ")) {
                    runCatching { manager.removeUpdates(listener) }
                    activeLocationListener = null
                    locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "لم يصلنا موقعك. تحقق من GPS أو خدمات الموقع ثم أعد المحاولة." else "No location fix received. Check GPS or location services and retry."
                }
            }, 15000L)
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val allowed = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) freshLocationAction?.invoke()
        else locationMessage = if (catalogueLanguage == CatalogueLanguage.AR) "اسمح للتطبيق بالوصول إلى موقعك" else "Allow location access to use this feature"
    }

    permissionRequestAction = { locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    freshLocationAction = { requestFreshLocation() }

    LaunchedEffect(searchTrigger) {
        remotePlaces = emptyList()
        val query = searchText.trim()
        if (query.length < 2) { isSearchingPlaces = false; return@LaunchedEffect }
        delay(1000)
        isSearchingPlaces = true
        remotePlaces = withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(query, "UTF-8")
                // Approximate search window covers El Bayadh Province, including El Abiodh Sidi Cheikh.
                val connection = URL("https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=jsonv2&limit=8&countrycodes=dz&viewbox=$searchViewbox&bounded=1&accept-language=ar").openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "VISTA-El-Bayadh/0.1.3 (tourism map)")
                try {
                    val array = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                    (0 until array.length()).mapNotNull { index ->
                        val item = array.getJSONObject(index)
                        val lat = item.optString("lat").toDoubleOrNull() ?: return@mapNotNull null
                        val lon = item.optString("lon").toDoubleOrNull() ?: return@mapNotNull null
                        val inside = if (wilayaPolygons.isNotEmpty()) isInsideWilaya(lat, lon, wilayaPolygons)
                            else lat in 30.7017..34.4482 && lon in -0.4159..2.3603
                        if (!inside) return@mapNotNull null
                        PlaceSearchResult(item.optString("display_name"), lat, lon)
                    }
                } finally { connection.disconnect() }
            }.getOrDefault(emptyList())
        }
        isSearchingPlaces = false
    }

    LaunchedEffect(routeTrigger) {
        if (routeTrigger == 0) return@LaunchedEffect
        val origin = currentLocation
        val destination = selectedTarget
        if (origin == null || destination == null) {
            routeMessage = if (catalogueLanguage == CatalogueLanguage.AR) "حدد موقعك ووجهتك أولاً" else "Choose your location and destination first"
            return@LaunchedEffect
        }
        isRouting = true
        routeMessage = if (catalogueLanguage == CatalogueLanguage.AR) "جارٍ حساب المسار عبر الطرق…" else "Calculating road route…"
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://router.project-osrm.org/route/v1/driving/" +
                    "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}" +
                    "?overview=full&geometries=geojson&steps=true"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 20000
                connection.setRequestProperty("User-Agent", "VISTA-El-Bayadh/0.2 (regional tourism map)")
                try {
                    val payload = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    if (payload.optString("code") != "Ok") error(payload.optString("message", "No route found"))
                    val route = payload.getJSONArray("routes").getJSONObject(0)
                    val coordinates = route.getJSONObject("geometry").getJSONArray("coordinates")
                    val points = (0 until coordinates.length()).mapNotNull { index ->
                        val coordinate = coordinates.getJSONArray(index)
                        if (coordinate.length() < 2) null
                        else Point.fromLngLat(coordinate.getDouble(0), coordinate.getDouble(1))
                    }
                    if (points.size < 2) error("Route geometry is empty")
                    RoadRouteResult(
                        Feature.fromGeometry(LineString.fromLngLats(points)),
                        route.getDouble("distance"),
                        route.getDouble("duration")
                    )
                } finally {
                    connection.disconnect()
                }
            }
        }
        isRouting = false
        result.onSuccess { route ->
            routeDistanceMeters = route.distanceMeters
            routeDurationSeconds = route.durationSeconds
            routeMessage = if (catalogueLanguage == CatalogueLanguage.AR) "تم حساب المسار عبر شبكة الطرق" else "Road route calculated"
            mapRef?.style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.setGeoJson(route.feature)
            mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(destination.latitude, destination.longitude), 11.5
            ))
        }.onFailure {
            routeDistanceMeters = null
            routeDurationSeconds = null
            routeMessage = if (catalogueLanguage == CatalogueLanguage.AR)
                "تعذر حساب المسار الآن. تحقق من الإنترنت أو جرّب لاحقاً."
            else "Unable to calculate a route. Check your connection and try again."
            mapRef?.style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(emptyArray<Feature>()))
        }
    }

    DisposableEffect(context) {
        onDispose {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            activeLocationListener?.let { runCatching { manager.removeUpdates(it) } }
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
                        onValueChange = {
                            searchText = it
                            selectedTarget = null
                            mapRef?.style?.getSourceAs<GeoJsonSource>(SEARCH_SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(emptyArray<Feature>()))
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (catalogueLanguage == CatalogueLanguage.AR) "ابحث عن معلم أو منطقة..." else "Search landmarks and places...", color = MapIvory.copy(alpha = 0.65f), fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { searchTrigger += 1 }),
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
                    IconButton(onClick = { searchTrigger += 1 }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search places", tint = MapGold)
                    }
                    IconButton(onClick = {
                        searchText = ""
                        remotePlaces = emptyList()
                        selectedTarget = null
                        mapRef?.style?.getSourceAs<GeoJsonSource>(SEARCH_SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(emptyArray<Feature>()))
                    }) {
                        Text("×", color = MapIvory, fontSize = 22.sp)
                    }
                }
            }

            if (searchText.isNotBlank() && selectedTarget == null) {
                Surface(shape = RoundedCornerShape(16.dp), color = MapNavy.copy(alpha = 0.97f)) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        if (matchingLandmarks.isEmpty() && matchingMapPlaces.isEmpty() && remotePlaces.isEmpty() && !isSearchingPlaces) {
                            Text(if (catalogueLanguage == CatalogueLanguage.AR) "لا توجد نتائج بعد" else "No results yet", color = MapIvory, modifier = Modifier.padding(10.dp))
                        }
                        if (isSearchingPlaces) Text(if (catalogueLanguage == CatalogueLanguage.AR) "جارٍ البحث عن المدن والقرى…" else "Searching places…", color = MapGold, modifier = Modifier.padding(8.dp))
                        matchingLandmarks.forEach { landmark ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val lat = landmark.latitude
                                    val lon = landmark.longitude
                                    if (lat != null && lon != null) {
                                        showSearchTarget(landmark.name(catalogueLanguage), lat, lon, landmark.id)
                                    }
                                }.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(landmark.name(catalogueLanguage), color = MapIvory, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                Text(if (landmark.latitude != null && landmark.longitude != null) "على الخريطة" else "الموقع قيد التحديد", color = MapGold, fontSize = 10.sp)
                            }
                        }
                        matchingMapPlaces.forEach { place ->
                            Row(Modifier.fillMaxWidth().clickable {
                                showSearchTarget(place.displayName(catalogueLanguage), place.latitude, place.longitude)
                            }.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(place.displayName(catalogueLanguage), color = MapIvory, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                Text(place.category, color = MapGold, fontSize = 10.sp)
                            }
                        }
                        remotePlaces.forEach { place ->
                            Row(Modifier.fillMaxWidth().clickable {
                                showSearchTarget(place.name, place.latitude, place.longitude)
                            }.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(place.name, color = MapIvory, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 2)
                                Text(if (catalogueLanguage == CatalogueLanguage.AR) "انتقال" else "Go", color = MapGold, fontSize = 10.sp)
                            }
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
            MapControlButton(Icons.Filled.MyLocation, "My location") { requestFreshLocation() }
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

        val target = selectedTarget
        val user = currentLocation
        if (target != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp)
                    .clickable { if (user != null && !isRouting) routeTrigger += 1 else if (user == null) requestFreshLocation() },
                shape = RoundedCornerShape(12.dp),
                color = MapNavy.copy(alpha = 0.97f),
                tonalElevation = 5.dp
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (isRouting) "جارٍ حساب المسار…" else if (user == null)
                            "اضغط لتحديد موقعك وحساب المسار إلى ${target.name.substringBefore(",")}"
                        else if (routeDistanceMeters != null)
                            "المسار عبر الطرق: ${String.format(java.util.Locale.US, "%.1f", routeDistanceMeters!! / 1000.0)} كم • ${(routeDurationSeconds!! / 60.0).toInt()} دقيقة"
                        else "احسب المسار عبر الطرق إلى ${target.name.substringBefore(",")}",
                        color = MapIvory, fontSize = 12.sp
                    )
                    if (routeMessage.isNotBlank()) Text(routeMessage, color = MapGold, fontSize = 10.sp)
                    Text(
                        if (routeDistanceMeters != null) "اضغط لإعادة حساب المسار" else "توجيه عبر الإنترنت • اضغط للبدء",
                        color = MapGold, fontSize = 10.sp
                    )
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
