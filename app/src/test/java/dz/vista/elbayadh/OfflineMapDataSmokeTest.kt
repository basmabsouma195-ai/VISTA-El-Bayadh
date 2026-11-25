package dz.vista.elbayadh

import org.junit.Assert.assertTrue
import org.junit.Test
import org.mapsforge.core.model.Tile
import org.mapsforge.map.reader.MapFile
import java.io.File
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan
import kotlin.math.cos

class OfflineMapDataSmokeTest {
    private fun tile(latitude: Double, longitude: Double, zoom: Int): Tile {
        val scale = 1 shl zoom
        val x = floor((longitude + 180.0) / 360.0 * scale).toLong()
        val latRad = Math.toRadians(latitude)
        val y = floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * scale).toLong()
        return Tile(x, y, zoom.toByte(), 256)
    }

    @Test
    fun bundledMapContainsRenderableRoadDataAtElBayadhCity() {
        val file = File("src/main/assets/maps/el-bayadh.map")
        assertTrue("Bundled Mapsforge map is missing or empty", file.isFile && file.length() > 100_000L)

        MapFile(file).use { map ->
            val info = map.mapFileInfo
            assertTrue("Map file version must be supported", info.fileVersion in 3..5)
            assertTrue("Map tile size must be 256", info.tilePixelSize == 256)
            assertTrue("Map bounding box must include El Bayadh city", info.boundingBox.contains(33.68, 1.02))

            val zoomLevels = listOf(8, 10, 12, 14)
            val readableWays = zoomLevels.sumOf { zoom ->
                val result = map.readMapData(tile(33.68, 1.02, zoom))
                result.ways.size
            }
            assertTrue(
                "The map file opens but contains no readable ways at El Bayadh city across zoom levels $zoomLevels",
                readableWays > 0
            )
        }
    }
}
