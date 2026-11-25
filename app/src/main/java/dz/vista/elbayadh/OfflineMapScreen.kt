package dz.vista.elbayadh

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import java.io.FileInputStream
import dz.vista.elbayadh.data.OfflineMapPin
import dz.vista.elbayadh.data.OfflineMapPinStore

private val MapNavy = Color(0xFF071B2D)
private val MapGold = Color(0xFFC39A58)
private val MapLight = Color(0xFFF6F0E5)

@Composable
internal fun OfflineMapScreen(language: String) {
    val context = LocalContext.current
    val isArabic = language == "Arabic"
    val isFrench = language == "French"
    val mapFile = remember(context) { bundledMapFile(context) }
    val pinStore = remember(context) { OfflineMapPinStore(context) }
    val pins = remember(context) { mutableStateListOf<OfflineMapPin>().apply { addAll(pinStore.all()) } }
    val latestPins = rememberUpdatedState(pins)
    val status = if (mapFile != null) {
        if (isArabic) "الخريطة المحلية جاهزة للعرض." else if (isFrench) "La carte locale est prête." else "The local map is ready."
    } else {
        if (isArabic) "ملف خريطة ولاية البيض لم يُضمّن بعد. لن نعرض خريطة وهمية أو نستخدم الإنترنت بدلًا منها." else if (isFrench) "Le fichier cartographique d’El Bayadh n’est pas encore intégré. Aucune carte fictive ni source Internet ne sera utilisée." else "The El Bayadh map file is not bundled yet. No placeholder map or online map source will be shown."
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MapNavy).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isArabic) "خريطة ولاية البيض" else if (isFrench) "Carte de la wilaya d’El Bayadh" else "El Bayadh Wilaya map",
            color = MapLight
        )
        if (mapFile != null) {
            val mapView = remember(mapFile) {
                AndroidGraphicFactory.createInstance(context.applicationContext)
                MapView(context).apply {
                    setBuiltInZoomControls(true)
                    mapScaleBar.isVisible = true
                    val cache = AndroidUtil.createTileCache(
                        context,
                        "vista-el-bayadh-map",
                        model.displayModel.tileSize,
                        1f,
                        model.frameBufferModel.overdrawFactor
                    )
                    val store = MapFile(FileInputStream(mapFile))
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
            Text(
                if (isArabic) "اضغط على الخريطة لإضافة علامة وحفظها على جهازك." else if (isFrench) "Touchez la carte pour ajouter et enregistrer un repère sur cet appareil." else "Tap the map to add a pin and save it on this device.",
                color = MapLight
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isArabic) "العلامات المحفوظة: ${pins.size}" else if (isFrench) "Repères enregistrés : ${pins.size}" else "Saved pins: ${pins.size}",
                    color = MapLight
                )
                TextButton(
                    onClick = {
                        pinStore.clear()
                        pins.clear()
                    },
                    enabled = pins.isNotEmpty()
                ) {
                    Text(if (isArabic) "مسح العلامات" else if (isFrench) "Effacer" else "Clear pins", color = MapGold)
                }
            }
            LaunchedEffect(mapView, pins.size) {
                val layers = mapView.layerManager.layers
                layers.filterIsInstance<Marker>().forEach { marker ->
                    layers.remove(marker)
                    marker.onDestroy()
                }
                pins.forEach { pin ->
                    val drawable = context.getDrawable(R.drawable.ic_map_pin) ?: return@forEach
                    val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)
                    layers.add(Marker(LatLong(pin.latitude, pin.longitude), bitmap, 0, -bitmap.height / 2))
                }
                mapView.layerManager.redrawLayers()
            }
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            DisposableEffect(mapView) {
                onDispose { mapView.destroyAll() }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2A45))
            ) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(status, color = MapLight)
                }
            }
        }
    }
}

private fun bundledMapFile(context: Context): File? {
    val assetPath = "maps/el-bayadh.map"
    val exists = runCatching { context.assets.open(assetPath).use { true } }.getOrDefault(false)
    if (!exists) return null
    val destination = File(context.filesDir, "el-bayadh.map")
    if (!destination.exists() || destination.length() == 0L) {
        context.assets.open(assetPath).use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return destination.takeIf { it.exists() && it.length() > 0L }
}
