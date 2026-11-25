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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import java.io.FileInputStream

private val MapNavy = Color(0xFF071B2D)
private val MapGold = Color(0xFFC39A58)
private val MapLight = Color(0xFFF6F0E5)

@Composable
internal fun OfflineMapScreen(language: AppLanguage) {
    val context = LocalContext.current
    val isArabic = language == AppLanguage.Arabic
    val isFrench = language == AppLanguage.French
    val mapFile = remember(context) { bundledMapFile(context) }
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
                    val layer = TileRendererLayer(cache, store, model.mapViewPosition, AndroidGraphicFactory.INSTANCE)
                    layer.setXmlRenderTheme(MapsforgeThemes.DEFAULT)
                    layerManager.layers.add(layer)
                    setCenter(LatLong(33.68, 1.02))
                    setZoomLevel(8.toByte())
                }
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
