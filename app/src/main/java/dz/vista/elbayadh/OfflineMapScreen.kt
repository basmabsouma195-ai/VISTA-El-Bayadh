package dz.vista.elbayadh

import android.content.Context
import org.json.JSONObject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import dz.vista.elbayadh.data.OfflineMapPin
import dz.vista.elbayadh.data.OfflineMapPinStore

private val MapNavy = Color(0xFF071B2D)
private val MapGold = Color(0xFFC39A58)
private val MapLight = Color(0xFFF6F0E5)
private val MapMuted = Color(0xFFB8C5D1)

private data class OfflinePlace(
    val name: String,
    val arabicName: String?,
    val frenchName: String?,
    val englishName: String?,
    val category: String,
    val latitude: Double,
    val longitude: Double
) {
    fun matches(query: String): Boolean {
        val q = normalizePlaceText(query)
        if (q.isBlank()) return false
        val names = listOfNotNull(name, arabicName, frenchName, englishName)
        if (names.any { normalizePlaceText(it).contains(q) }) return true
        val abiodhAliases = listOf("الأبيض سيدي الشيخ", "الابيض سيدي الشيخ", "el abiodh sidi cheikh", "el abiodh sidi chikh", "el bayadh sidi cheikh")
        return abiodhAliases.any { normalizePlaceText(it).contains(q) || q.contains(normalizePlaceText(it)) } &&
            names.any { normalizePlaceText(it).contains("abiodh") || normalizePlaceText(it).contains("ابيض") }
    }
}

private fun normalizePlaceText(value: String): String = value
    .lowercase()
    .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
    .replace("ى", "ي").replace("ة", "ه")
    .replace(Regex("[ًٌٍَُِّْـ]"), "")
    .trim()

@Composable
internal fun OfflineMapScreen(language: String) {
    val context = LocalContext.current
    val isArabic = language == "Arabic"
    val isFrench = language == "French"
    val mapFile = remember(context) { bundledMapFile(context) }
    val places = remember(context) { loadOfflinePlaces(context) }
    val pinStore = remember(context) { OfflineMapPinStore(context) }
    val pins = remember(context) {
        mutableStateListOf<OfflineMapPin>().apply { addAll(pinStore.all()) }
    }
    val latestPins = rememberUpdatedState(pins)
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<OfflinePlace?>(null) }

    val mapSetup = remember(mapFile) {
        if (mapFile == null) {
            Result.failure<MapView>(IllegalStateException("Offline map file is missing."))
        } else {
            runCatching { createOfflineMapView(context, mapFile, pinStore, latestPins) }
        }
    }
    val mapView = mapSetup.getOrNull()
    val setupError = mapSetup.exceptionOrNull()?.localizedMessage
    val searchResults = remember(places, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else places.asSequence().filter { it.matches(searchQuery) }.take(30).toList()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MapNavy).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (isArabic) "خريطة ولاية البيض" else if (isFrench) "Carte de la wilaya d’El Bayadh" else "El Bayadh Wilaya map",
            color = MapLight
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; selectedPlace = null },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = {
                Text(
                    if (isArabic) "ابحث عن مدينة أو قرية أو معلم..." else if (isFrench) "Ville, village ou site..." else "Search a city, village or landmark...",
                    color = MapMuted
                )
            },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = MapLight,
                unfocusedTextColor = MapLight,
                focusedContainerColor = Color(0xFF102B43),
                unfocusedContainerColor = Color(0xFF102B43),
                focusedBorderColor = MapGold,
                unfocusedBorderColor = Color(0xFF31506A),
                cursorColor = MapGold
            )
        )

        if (searchQuery.isNotBlank()) {
            Text(
                if (isArabic) "نتائج البحث: ${searchResults.size}" else if (isFrench) "Résultats : ${searchResults.size}" else "Results: ${searchResults.size}",
                color = MapGold
            )
            if (searchResults.isEmpty()) {
                Text(
                    if (isArabic) "لا توجد نتيجة في فهرس الأماكن المحلي." else if (isFrench) "Aucun résultat dans l’index local." else "No match in the local places index.",
                    color = MapMuted
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 176.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchResults) { place ->
                        TextButton(
                            onClick = {
                                selectedPlace = place
                                mapView?.setCenter(LatLong(place.latitude, place.longitude))
                                mapView?.setZoomLevel(13.toByte())
                                mapView?.layerManager?.redrawLayers()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(place.arabicName?.takeIf { isArabic && it.isNotBlank() } ?: place.name, color = MapLight)
                                Text(place.category, color = MapMuted)
                            }
                        }
                    }
                }
            }
        }

        selectedPlace?.let { place ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF153650))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(place.arabicName?.takeIf { isArabic && it.isNotBlank() } ?: place.name, color = MapLight)
                    Text("${place.latitude}, ${place.longitude}", color = MapMuted)
                    TextButton(onClick = { selectedPlace = null; searchQuery = "" }) {
                        Text(if (isArabic) "إغلاق التفاصيل" else if (isFrench) "Fermer" else "Close details", color = MapGold)
                    }
                }
            }
        }

        if (mapView != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isArabic) "علاماتك المحفوظة: ${pins.size}" else if (isFrench) "Repères enregistrés : ${pins.size}" else "Saved pins: ${pins.size}",
                    color = MapLight
                )
                TextButton(
                    onClick = { pinStore.clear(); pins.clear() },
                    enabled = pins.isNotEmpty()
                ) {
                    Text(if (isArabic) "مسح العلامات" else if (isFrench) "Effacer" else "Clear pins", color = MapGold)
                }
            }

            LaunchedEffect(mapView, pins.size) {
                val layers = mapView.layerManager.layers
                layers.filterIsInstance<Marker>().toList().forEach { marker ->
                    layers.remove(marker)
                    marker.onDestroy()
                }
                pins.toList().forEach { pin ->
                    val drawable = context.getDrawable(R.drawable.ic_map_pin) ?: return@forEach
                    val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)
                    layers.add(Marker(LatLong(pin.latitude, pin.longitude), bitmap, 0, -bitmap.height / 2))
                }
                mapView.layerManager.redrawLayers()
            }

            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().weight(1f))
            DisposableEffect(mapView) {
                onDispose { runCatching { mapView.destroyAll() } }
            }
        } else {
            val message = when {
                mapFile == null && isArabic -> "ملف الخريطة المحلية غير موجود داخل التطبيق."
                mapFile == null && isFrench -> "Le fichier de carte hors ligne est absent de l’application."
                mapFile == null -> "The offline map file is not bundled in the app."
                isArabic -> "تعذر تشغيل الخريطة: ${setupError ?: "خطأ غير معروف"}"
                isFrench -> "Impossible d’ouvrir la carte : ${setupError ?: "erreur inconnue"}"
                else -> "The map could not be opened: ${setupError ?: "Unknown error"}"
            }
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2A45))
            ) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(message, color = MapLight)
                }
            }
        }
    }
}

private fun loadOfflinePlaces(context: Context): List<OfflinePlace> = runCatching {
    val json = context.assets.open("maps/el-bayadh-pois.json").bufferedReader().use { it.readText() }
    val root = JSONObject(json)
    val array = root.getJSONArray("items")
    buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val lat = item.optDouble("latitude", Double.NaN)
            val lon = item.optDouble("longitude", Double.NaN)
            val name = item.optString("name").trim()
            if (name.isNotBlank() && lat.isFinite() && lon.isFinite()) {
                add(
                    OfflinePlace(
                        name = name,
                        arabicName = item.optString("name_ar").takeIf { it.isNotBlank() },
                        frenchName = item.optString("name_fr").takeIf { it.isNotBlank() },
                        englishName = item.optString("name_en").takeIf { it.isNotBlank() },
                        category = item.optString("category", "place"),
                        latitude = lat,
                        longitude = lon
                    )
                )
            }
        }
    }
}.getOrDefault(emptyList())

private fun createOfflineMapView(
    context: Context,
    mapFile: File,
    pinStore: OfflineMapPinStore,
    latestPins: androidx.compose.runtime.State<MutableList<OfflineMapPin>>
): MapView {
    if (AndroidGraphicFactory.INSTANCE == null) {
        AndroidGraphicFactory.createInstance(context.applicationContext)
    }
    return MapView(context).apply {
        setBuiltInZoomControls(true)
        mapScaleBar.isVisible = true
        val cache = AndroidUtil.createTileCache(
            context,
            "vista-el-bayadh-map",
            model.displayModel.tileSize,
            1f,
            model.frameBufferModel.overdrawFactor
        )
        val store = MapFile(mapFile)
        val layer = object : TileRendererLayer(cache, store, model.mapViewPosition, AndroidGraphicFactory.INSTANCE) {
            override fun onTap(tapLatLong: LatLong, layerXY: Point, tapXY: Point): Boolean {
                val nextPin = OfflineMapPin(
                    latitude = tapLatLong.latitude,
                    longitude = tapLatLong.longitude,
                    title = "Saved place ${latestPins.value.size + 1}"
                )
                pinStore.add(nextPin)
                latestPins.value.add(nextPin)
                return true
            }
        }
        layer.setXmlRenderTheme(MapsforgeThemes.DEFAULT)
        layerManager.layers.add(layer)
        setCenter(LatLong(33.68, 1.02))
        setZoomLevel(8.toByte())
    }
}

private fun bundledMapFile(context: Context): File? = runCatching {
    context.assets.open("maps/el-bayadh.map").use { input ->
        val destination = File(context.filesDir, "el-bayadh.map")
        if (!destination.exists() || destination.length() == 0L) {
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        destination.takeIf { it.exists() && it.length() > 0L }
    }
}.getOrNull()
