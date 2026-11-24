package dz.vista.elbayadh

import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import java.util.concurrent.Executor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dz.vista.elbayadh.data.TripPlansStore
import dz.vista.elbayadh.data.OfflineCatalogue
import dz.vista.elbayadh.data.FavoritesStore
import dz.vista.elbayadh.data.Landmark
import dz.vista.elbayadh.data.CatalogueLanguage
import dz.vista.elbayadh.data.VerificationStatus
import dz.vista.elbayadh.recognition.OfflineRecognitionImageIndex
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider

private val Forest = Color(0xFF173D32)
private val ForestDeep = Color(0xFF102C25)
private val Gold = Color(0xFFC6A15B)
private val Ivory = Color(0xFFF7F2E8)
private val Sand = Color(0xFFE9DFCC)
private val Ink = Color(0xFF25332D)
private val Muted = Color(0xFF7B817A)

private enum class AppLanguage { Arabic, French, English }

private data class Copy(
    val home: String, val map: String, val scan: String, val favorites: String,
    val more: String, val welcome: String, val subtitle: String, val search: String,
    val discover: String, val categories: List<String>, val offline: String,
    val emptyFavorites: String, val mapNote: String, val scanNote: String,
    val moreNote: String
)

private fun copyFor(language: AppLanguage) = when (language) {
    AppLanguage.Arabic -> Copy(
        "الرئيسية", "الخريطة", "الماسح", "المفضلة", "المزيد",
        "اكتشف ولاية البيض", "دليلك لاكتشاف التراث والطبيعة", "ابحث عن معلم أو منطقة...",
        "اكتشف", listOf("معالم تاريخية", "معالم طبيعية", "القصور والواحات", "النقوش الصخرية"),
        "محتوى أساسي متاح دون إنترنت", "لم تحفظ أي معلم بعد",
        "ستظهر هنا الخريطة المحلية بعد إضافة بيانات الولاية.",
        "التعرف البصري على المعالم قيد البناء؛ لن يعتمد على QR وحده.",
        "إعدادات التطبيق واللغات ومعلومات VISTA."
    )
    AppLanguage.French -> Copy(
        "Accueil", "Carte", "Scanner", "Favoris", "Plus",
        "Découvrez El Bayadh", "Votre guide du patrimoine et de la nature", "Rechercher un site ou une région...",
        "Explorer", listOf("Sites historiques", "Sites naturels", "Ksour et oasis", "Gravures rupestres"),
        "Contenu essentiel disponible hors ligne", "Aucun site enregistré pour le moment",
        "La carte locale sera affichée après l’intégration des données de la wilaya.",
        "La reconnaissance visuelle est en cours de construction et ne dépendra pas uniquement du QR.",
        "Paramètres, langues et informations sur VISTA."
    )
    AppLanguage.English -> Copy(
        "Home", "Map", "Scanner", "Favorites", "More",
        "Discover El Bayadh", "Your guide to heritage and nature", "Search for a landmark or area...",
        "Discover", listOf("Historic sites", "Natural sites", "Ksour & oases", "Rock engravings"),
        "Core content available offline", "You have no saved places yet",
        "The local map will appear after the province data is integrated.",
        "Visual landmark recognition is being built and will not rely on QR alone.",
        "App settings, languages and information about VISTA."
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VistaApp() }
    }
}

@Composable
private fun VistaApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("vista_preferences", Context.MODE_PRIVATE) }
    var language by remember {
        mutableStateOf(
            when (preferences.getString("language", "ar")) {
                "fr" -> AppLanguage.French
                "en" -> AppLanguage.English
                else -> AppLanguage.Arabic
            }
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tripStore = remember { TripPlansStore(context) }
    val favoritesStore = remember { FavoritesStore(context) }
    val catalogue = remember { runCatching { OfflineCatalogue(context).load() }.getOrNull() }
    var favoriteIds by remember { mutableStateOf(favoritesStore.allIds()) }
    fun toggleFavorite(id: String) { favoritesStore.toggle(id); favoriteIds = favoritesStore.allIds() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLandmark by remember { mutableStateOf<Landmark?>(null) }
    fun changeLanguage(next: AppLanguage) {
        language = next
        preferences.edit().putString("language", when (next) {
            AppLanguage.Arabic -> "ar"
            AppLanguage.French -> "fr"
            AppLanguage.English -> "en"
        }).apply()
    }
    val c = copyFor(language)
    val direction = if (language == AppLanguage.Arabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            Surface(modifier = Modifier.fillMaxSize(), color = Ivory) {
                androidx.compose.material3.Scaffold(
                    containerColor = Ivory,
                    bottomBar = {
                        NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                            val items = listOf(
                                Icons.Filled.Home to c.home,
                                Icons.Filled.Map to c.map,
                                Icons.Filled.CenterFocusStrong to c.scan,
                                Icons.Filled.Favorite to c.favorites,
                                Icons.Filled.MoreHoriz to c.more
                            )
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = { Icon(item.first, contentDescription = item.second) },
                                    label = { Text(item.second, fontSize = 10.sp, maxLines = 1) },
                                    colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                        selectedIconColor = Gold,
                                        selectedTextColor = Forest,
                                        indicatorColor = Sand
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                c, language, onLanguage = ::changeLanguage,
                                searchQuery = searchQuery, onSearchChange = { searchQuery = it },
                                onOpenScanner = { selectedTab = 2 },
                                landmarks = catalogue?.landmarks.orEmpty(), favoriteIds = favoriteIds,
                                onFavoriteToggle = ::toggleFavorite,
                                onOpenLandmark = { selectedLandmark = it }
                            )
                            1 -> InfoScreen(c.map, c.mapNote, Icons.Filled.Map)
                            2 -> ScannerScreen(language, catalogue?.landmarks.orEmpty(), onOpenLandmark = { selectedLandmark = it })
                            3 -> FavoritesScreen(
                                title = c.favorites,
                                landmarks = catalogue?.landmarks.orEmpty().filter { it.id in favoriteIds },
                                language = language,
                                onFavoriteToggle = ::toggleFavorite,
                                onOpenLandmark = { selectedLandmark = it }
                            )
                            else -> MoreScreen(c, language, onLanguage = ::changeLanguage, tripStore = tripStore, landmarks = catalogue?.landmarks.orEmpty())
                        }
                    }
                }
                selectedLandmark?.let { landmark ->
                    LandmarkDetailDialog(
                        landmark = landmark,
                        language = when (language) {
                            AppLanguage.Arabic -> CatalogueLanguage.AR
                            AppLanguage.French -> CatalogueLanguage.FR
                            AppLanguage.English -> CatalogueLanguage.EN
                        },
                        onDismiss = { selectedLandmark = null },
                        isFavorite = landmark.id in favoriteIds,
                        photos = catalogue?.photosByLandmark?.get(landmark.id).orEmpty(),
                        onFavoriteToggle = { toggleFavorite(landmark.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    c: Copy,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenScanner: () -> Unit,
    landmarks: List<Landmark>,
    favoriteIds: Set<String>,
    onFavoriteToggle: (String) -> Unit,
    onOpenLandmark: (Landmark) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Ivory).verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier.fillMaxWidth().height(250.dp)
                .background(Brush.verticalGradient(listOf(Forest, ForestDeep))),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("VISTA", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("EL BAYADH", color = Color.White, fontSize = 12.sp, letterSpacing = 4.sp)
                    }
                    LanguagePicker(language, onLanguage)
                }
                Spacer(Modifier.height(28.dp))
                Text(c.welcome, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(c.subtitle, color = Color(0xFFE9DFC9), fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(c.search, color = Muted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Forest) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White
                    )
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                .clickable(onClick = onOpenScanner),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Forest)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Gold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, tint = ForestDeep, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (language == AppLanguage.Arabic) "تعرّف على المعلم أمامك"
                        else if (language == AppLanguage.French) "Identifiez le site devant vous"
                        else "Identify the landmark in front of you",
                        color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (language == AppLanguage.Arabic) "وجّه الكاميرا نحو المعلم لبدء التعرّف"
                        else if (language == AppLanguage.French) "Pointez la caméra vers le site pour commencer"
                        else "Point your camera at a landmark to get started",
                        color = Color(0xFFE9DFC9), fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        }
        Text(
            if (language == AppLanguage.Arabic) "أو ابحث عن معلم بالاسم"
            else if (language == AppLanguage.French) "Ou recherchez un site par son nom"
            else "Or search for a landmark by name",
            color = Forest, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        val catalogueLanguage = when (language) {
            AppLanguage.Arabic -> CatalogueLanguage.AR
            AppLanguage.French -> CatalogueLanguage.FR
            AppLanguage.English -> CatalogueLanguage.EN
        }
        val matchingLandmarks = landmarks.filter { landmark ->
            searchQuery.isNotBlank() && (
                landmark.name(catalogueLanguage).contains(searchQuery.trim(), ignoreCase = true) ||
                landmark.description(catalogueLanguage).contains(searchQuery.trim(), ignoreCase = true)
            )
        }
        if (searchQuery.isNotBlank()) {
            if (matchingLandmarks.isEmpty()) {
                Text(
                    if (language == AppLanguage.Arabic) "لا توجد نتائج مطابقة"
                    else if (language == AppLanguage.French) "Aucun résultat correspondant"
                    else "No matching landmarks",
                    color = Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            } else {
                matchingLandmarks.forEach { landmark ->
                    LandmarkCard(
                        landmark = landmark,
                        language = catalogueLanguage,
                        isFavorite = landmark.id in favoriteIds,
                        onFavoriteToggle = { onFavoriteToggle(landmark.id) },
                        onOpen = { onOpenLandmark(landmark) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFFEDE5D5)).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Language, contentDescription = null, tint = Forest)
            }
            Spacer(Modifier.width(10.dp))
            Text(c.offline, color = Forest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryCard(title: String, brush: Brush, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Forest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(Modifier.fillMaxSize().background(brush).padding(14.dp), contentAlignment = Alignment.BottomStart) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LanguagePicker(language: AppLanguage, onLanguage: (AppLanguage) -> Unit) {
    Row(
        modifier = Modifier.clip(CircleShape).background(Color(0x33222222)).padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(AppLanguage.Arabic to "ع", AppLanguage.French to "FR", AppLanguage.English to "EN").forEach { (lang, label) ->
            Box(
                modifier = Modifier.clip(CircleShape)
                    .background(if (language == lang) Gold else Color.Transparent)
                    .clickable { onLanguage(lang) }.padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(label, color = if (language == lang) ForestDeep else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LandmarkCard(
    landmark: Landmark,
    language: CatalogueLanguage,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onOpen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(landmark.name(language), color = Forest, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                if (landmark.description(language).isNotBlank()) {
                    Text(landmark.description(language), color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
                val status = when (landmark.verification) {
                    VerificationStatus.VERIFIED -> when (language) {
                        CatalogueLanguage.AR -> "معلومة موثقة"
                        CatalogueLanguage.FR -> "Information vérifiée"
                        CatalogueLanguage.EN -> "Verified information"
                    }
                    VerificationStatus.ORAL_HISTORY -> when (language) {
                        CatalogueLanguage.AR -> "رواية شفوية"
                        CatalogueLanguage.FR -> "Tradition orale"
                        CatalogueLanguage.EN -> "Oral history"
                    }
                    VerificationStatus.UNDER_REVIEW -> when (language) {
                        CatalogueLanguage.AR -> "قيد التحقيق"
                        CatalogueLanguage.FR -> "En cours de vérification"
                        CatalogueLanguage.EN -> "Under review"
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(status, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            LandmarkThumbnail(landmark.image, landmark.name(language))
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) Color(0xFFB24E4E) else Muted
                )
            }
        }
    }
}


@Composable
private fun LandmarkDetailDialog(
    landmark: Landmark,
    language: CatalogueLanguage,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    photos: List<String>,
    onFavoriteToggle: () -> Unit
) {
    val title = landmark.name(language)
    val description = landmark.description(language)
    val status = when (landmark.verification) {
        VerificationStatus.VERIFIED -> when (language) {
            CatalogueLanguage.AR -> "معلومة موثقة"
            CatalogueLanguage.FR -> "Information vérifiée"
            CatalogueLanguage.EN -> "Verified information"
        }
        VerificationStatus.ORAL_HISTORY -> when (language) {
            CatalogueLanguage.AR -> "رواية شفوية"
            CatalogueLanguage.FR -> "Tradition orale"
            CatalogueLanguage.EN -> "Oral history"
        }
        VerificationStatus.UNDER_REVIEW -> when (language) {
            CatalogueLanguage.AR -> "المعلومات قيد التحقيق"
            CatalogueLanguage.FR -> "Informations en cours de vérification"
            CatalogueLanguage.EN -> "Information under review"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Forest, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LandmarkThumbnail(landmark.image, title, large = true)
                photos.filter { it != landmark.image }.forEachIndexed { index, photo ->
                    Spacer(Modifier.height(10.dp))
                    LandmarkThumbnail(photo, "$title ${index + 2}", large = true)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    description.ifBlank {
                        when (language) {
                            CatalogueLanguage.AR -> "نعمل على إعداد بطاقة هذا المعلم بمعلومات وصور ميدانية موثوقة."
                            CatalogueLanguage.FR -> "La fiche de ce site sera complétée avec des informations et des photos de terrain vérifiées."
                            CatalogueLanguage.EN -> "This landmark's profile is being prepared with verified information and field photographs."
                        }
                    },
                    color = Ink, fontSize = 14.sp, lineHeight = 22.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(status, color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                if (landmark.latitude != null && landmark.longitude != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("%.5f, %.5f".format(landmark.latitude, landmark.longitude), color = Muted, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFavoriteToggle) {
                Text(
                    if (isFavorite) {
                        when (language) {
                            CatalogueLanguage.AR -> "إزالة من المفضلة"
                            CatalogueLanguage.FR -> "Retirer des favoris"
                            CatalogueLanguage.EN -> "Remove from favorites"
                        }
                    } else {
                        when (language) {
                            CatalogueLanguage.AR -> "أضف إلى المفضلة"
                            CatalogueLanguage.FR -> "Ajouter aux favoris"
                            CatalogueLanguage.EN -> "Add to favorites"
                        }
                    },
                    color = Forest
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(when (language) {
                    CatalogueLanguage.AR -> "إغلاق"
                    CatalogueLanguage.FR -> "Fermer"
                    CatalogueLanguage.EN -> "Close"
                }, color = Muted)
            }
        },
        containerColor = Ivory
    )
}

@Composable
private fun LandmarkThumbnail(assetPath: String?, description: String, large: Boolean = false) {
    val context = LocalContext.current
    var bitmap by remember(assetPath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(assetPath) {
        bitmap = if (assetPath.isNullOrBlank() || assetPath.contains("://")) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.assets.open(assetPath).use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        null
                    } else {
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 480) {
                            sample *= 2
                        }
                        val options = BitmapFactory.Options().apply { inSampleSize = sample }
                        context.assets.open(assetPath).use {
                            BitmapFactory.decodeStream(it, null, options)
                        }
                    }
                }.getOrNull()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = description,
            modifier = (if (large) Modifier.fillMaxWidth().height(190.dp) else Modifier.size(76.dp)).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ScannerScreen(language: AppLanguage, landmarks: List<Landmark>, onOpenLandmark: (Landmark) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isArabic = language == AppLanguage.Arabic
    val isFrench = language == AppLanguage.French
    val localReferences = remember(context) {
        runCatching { OfflineRecognitionImageIndex(context).load() }.getOrDefault(emptyList())
    }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var qrMessage by remember { mutableStateOf<String?>(null) }
    var qrMatchedLandmark by remember { mutableStateOf<Landmark?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(previewView, lifecycleOwner, hasCameraPermission) {
        var disposed = false
        var boundProvider: ProcessCameraProvider? = null
        var boundPreview: Preview? = null
        var boundAnalysis: ImageAnalysis? = null
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
        val analyzerExecutor = Executors.newSingleThreadExecutor()
        val processing = AtomicBoolean(false)

        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                if (!disposed) {
                    runCatching {
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(analyzerExecutor) { proxy ->
                            analyzeVistaQr(proxy, scanner, processing, landmarks,
                                onFound = { found ->
                                    if (!disposed && qrMatchedLandmark?.id != found.id) {
                                        qrMatchedLandmark = found
                                        qrMessage = null
                                    }
                                },
                                onUnknown = { if (!disposed) qrMessage = it }
                            )
                        }
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        boundProvider = provider
                        boundPreview = preview
                        boundAnalysis = analysis
                    }
                }
            }, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            disposed = true
            boundAnalysis?.clearAnalyzer()
            val provider = boundProvider
            val preview = boundPreview
            val analysis = boundAnalysis
            if (provider != null) {
                val useCases = mutableListOf<androidx.camera.core.UseCase>()
                preview?.let(useCases::add)
                analysis?.let(useCases::add)
                if (useCases.isNotEmpty()) runCatching { provider.unbind(*useCases.toTypedArray()) }
            }
            scanner.close()
            analyzerExecutor.shutdown()
        }
    }

    Column(Modifier.fillMaxSize().background(Ivory).padding(16.dp)) {
        qrMatchedLandmark?.let { found ->
            AlertDialog(
                onDismissRequest = { qrMatchedLandmark = null },
                title = { Text(found.name(when (language) {
                    AppLanguage.Arabic -> CatalogueLanguage.AR
                    AppLanguage.French -> CatalogueLanguage.FR
                    AppLanguage.English -> CatalogueLanguage.EN
                })) },
                text = { Text(if (isArabic) "تمت قراءة رمز هذا المعلم." else if (isFrench) "Le QR de ce site a été lu." else "This landmark's QR code was read.") },
                confirmButton = {
                    TextButton(onClick = {
                        qrMatchedLandmark = null
                        onOpenLandmark(found)
                    }) { Text(if (isArabic) "اكتشف المعلم" else if (isFrench) "Découvrir" else "Discover") }
                },
                dismissButton = {
                    TextButton(onClick = { qrMatchedLandmark = null }) {
                        Text(if (isArabic) "إغلاق" else if (isFrench) "Fermer" else "Close")
                    }
                }
            )
        }
        Text(
            if (isArabic) "الماسح المعرف" else if (isFrench) "Scanner intelligent" else "Smart landmark scanner",
            color = Forest, fontSize = 23.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isArabic) "وجّه الكاميرا نحو المعلم. التعرّف البصري على المعالم سيُفعّل بعد إضافة نموذج الولاية واختباره."
            else if (isFrench) "Pointez la caméra vers le site. L’identification visuelle sera activée après l’intégration et le test du modèle local."
            else "Point the camera at the landmark. Visual identification will be enabled after the local province model is integrated and tested.",
            color = Muted, fontSize = 13.sp, lineHeight = 20.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            if (isArabic) "المسح الاحتياطي: QR (اختياري)" else if (isFrench) "Lecture de secours : QR (facultatif)" else "Fallback scan: optional QR",
            color = Forest, fontWeight = FontWeight.SemiBold, fontSize = 12.sp
        )
        qrMessage?.let { Text(it, color = Muted, fontSize = 11.sp) }
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)),
                factory = { previewView }
            )
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Sand)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        if (isArabic) "الكاميرا جاهزة" else if (isFrench) "Caméra prête" else "Camera ready",
                        color = Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isArabic) "صور مرجعية محلية: ${localReferences.size}"
                        else if (isFrench) "Images de référence locales : ${localReferences.size}"
                        else "Local reference images: ${localReferences.size}",
                        color = Forest, fontSize = 12.sp
                    )
                    localReferences.forEach { reference ->
                        Text(
                            when (language) {
                                AppLanguage.Arabic -> reference.labelAr
                                AppLanguage.French -> reference.labelFr.ifBlank { reference.labelAr }
                                AppLanguage.English -> reference.labelEn.ifBlank { reference.labelAr }
                            },
                            color = Ink, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Text(
                        if (isArabic) "محرك المطابقة البصرية لم يُدمج بعد؛ لن نعرض نتيجة تعرّف قبل تشغيل نموذج حقيقي واختباره."
                        else if (isFrench) "Le moteur de reconnaissance visuelle n’est pas encore intégré. Aucun résultat ne sera affiché avant l’ajout et le test d’un vrai modèle."
                        else "The visual matching engine is not integrated yet. No identification result will be shown until a real model is added and tested.",
                        color = Muted, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(ForestDeep),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, tint = Gold, modifier = Modifier.size(58.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (isArabic) "اسمح لـ VISTA باستخدام الكاميرا"
                        else if (isFrench) "Autorisez VISTA à utiliser la caméra"
                        else "Allow VISTA to use the camera",
                        color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Text(
                            if (isArabic) "السماح بالكاميرا" else if (isFrench) "Autoriser la caméra" else "Allow camera",
                            color = ForestDeep
                        )
                    }
                }
            }
        }
    }
}

private fun analyzeVistaQr(
    proxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    processing: AtomicBoolean,
    landmarks: List<Landmark>,
    onFound: (Landmark) -> Unit,
    onUnknown: (String) -> Unit
) {
    val mediaImage = proxy.image
    if (mediaImage == null || !processing.compareAndSet(false, true)) {
        proxy.close()
        return
    }
    val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { codes ->
            val raw = codes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue ?: return@addOnSuccessListener
            val id = raw.substringAfterLast('/').removePrefix("vista:landmark:").substringBefore('?').trim()
            val found = landmarks.firstOrNull { it.id == id }
            if (found != null) onFound(found)
            else onUnknown("QR detected, but it does not contain a known VISTA landmark ID.")
        }
        .addOnFailureListener { onUnknown("QR scan failed. Try holding the camera steady.") }
        .addOnCompleteListener {
            processing.set(false)
            proxy.close()
        }
}

@Composable
private fun FavoritesScreen(
    title: String,
    landmarks: List<Landmark>,
    language: AppLanguage,
    onFavoriteToggle: (String) -> Unit,
    onOpenLandmark: (Landmark) -> Unit
) {
    val catalogueLanguage = when (language) {
        AppLanguage.Arabic -> CatalogueLanguage.AR
        AppLanguage.French -> CatalogueLanguage.FR
        AppLanguage.English -> CatalogueLanguage.EN
    }
    val empty = when (language) {
        AppLanguage.Arabic -> "لم تحفظ أي معلم بعد. أضف المعالم إلى المفضلة من صفحة الاستكشاف."
        AppLanguage.French -> "Aucun favori. Enregistrez des sites depuis la page d’accueil."
        AppLanguage.English -> "No favorites yet. Save places from the home screen."
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 18.dp)) {
        Text(title, color = Forest, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        if (landmarks.isEmpty()) {
            Text(empty, color = Muted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 20.dp))
        } else {
            landmarks.forEach { landmark ->
                LandmarkCard(
                    landmark = landmark,
                    language = catalogueLanguage,
                    isFavorite = true,
                    onFavoriteToggle = { onFavoriteToggle(landmark.id) },
                    onOpen = { onOpenLandmark(landmark) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoScreen(title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(76.dp).clip(CircleShape).background(Sand), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Forest, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(message, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
    }
}

@Composable
private fun MoreScreen(
    c: Copy,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    tripStore: TripPlansStore,
    landmarks: List<Landmark>
) {
    var tripTitle by remember { mutableStateOf("") }
    var trips by remember { mutableStateOf(tripStore.all()) }
    val isArabic = language == AppLanguage.Arabic
    val isFrench = language == AppLanguage.French
    val tripsTitle = if (isArabic) "رحلاتي" else if (isFrench) "Mes voyages" else "My trips"
    val tripHint = if (isArabic) "اسم الرحلة الجديدة" else if (isFrench) "Nom du nouveau voyage" else "New trip name"
    val createLabel = if (isArabic) "إنشاء رحلة" else if (isFrench) "Créer un voyage" else "Create trip"
    val emptyLabel = if (isArabic) "لم تنشئ أي رحلة بعد." else if (isFrench) "Aucun voyage pour le moment." else "No trips created yet."
    val stationsLabel = if (isArabic) "محطات" else if (isFrench) "étapes" else "stops"
    val deleteLabel = if (isArabic) "حذف" else if (isFrench) "Supprimer" else "Delete"
    val noPlaces = if (isArabic) "لا توجد معالم مضافة بعد. ستتمكن من اختيار المعالم عند إدراجها في الموسوعة." else if (isFrench) "Aucun site n’est encore disponible. Vous pourrez ajouter des étapes lorsque le catalogue sera renseigné." else "No landmarks are available yet. You can add stops once the catalogue is populated."
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text(c.more, color = Forest, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(c.moreNote, color = Muted, fontSize = 15.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(22.dp))
        Text(tripsTitle, color = Forest, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = tripTitle,
            onValueChange = { tripTitle = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tripHint) },
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (tripTitle.isNotBlank()) {
                    tripStore.create(tripTitle)
                    trips = tripStore.all()
                    tripTitle = ""
                }
            },
            enabled = tripTitle.isNotBlank(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Forest)
        ) { Text(createLabel, color = Color.White) }
        Spacer(Modifier.height(12.dp))
        if (trips.isEmpty()) {
            Text(emptyLabel, color = Muted, fontSize = 14.sp)
        } else {
            trips.forEach { trip ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(trip.title, color = Forest, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${trip.landmarkIds.size} $stationsLabel", color = Muted, fontSize = 12.sp)
                        if (landmarks.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (isArabic) "أضف محطات إلى رحلتك" else if (isFrench) "Ajouter des étapes" else "Add stops to your trip",
                                color = Forest, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                            )
                            landmarks.forEach { landmark ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        landmark.name(when (language) {
                                            AppLanguage.Arabic -> CatalogueLanguage.AR
                                            AppLanguage.French -> CatalogueLanguage.FR
                                            AppLanguage.English -> CatalogueLanguage.EN
                                        }),
                                        color = Ink, fontSize = 12.sp, modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = {
                                        tripStore.addLandmark(trip.id, landmark.id)
                                        trips = tripStore.all()
                                    }) {
                                        Text(
                                            if (landmark.id in trip.landmarkIds) {
                                                if (isArabic) "مضاف" else if (isFrench) "Ajouté" else "Added"
                                            } else {
                                                if (isArabic) "إضافة" else if (isFrench) "Ajouter" else "Add"
                                            },
                                            color = if (landmark.id in trip.landmarkIds) Muted else Forest
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(noPlaces, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = {
                                tripStore.delete(trip.id)
                                trips = tripStore.all()
                            }) { Text(deleteLabel, color = Color(0xFF9B5145)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("Language / Langue / اللغة", color = Forest, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(AppLanguage.Arabic to "العربية", AppLanguage.French to "Français", AppLanguage.English to "English").forEach { (lang, label) ->
                TextButton(onClick = { onLanguage(lang) }) { Text(label, color = if (language == lang) Gold else Forest) }
            }
        }
    }
}
