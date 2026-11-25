package dz.vista.elbayadh

import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.graphics.BitmapFactory
import android.net.Uri
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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
import kotlinx.coroutines.launch
import dz.vista.elbayadh.data.TripPlansStore
import dz.vista.elbayadh.data.OfflineCatalogue
import dz.vista.elbayadh.data.FavoritesStore
import dz.vista.elbayadh.data.Landmark
import dz.vista.elbayadh.data.CatalogueLanguage
import dz.vista.elbayadh.data.VerificationStatus
import dz.vista.elbayadh.recognition.OfflineRecognitionImageIndex
import dz.vista.elbayadh.recognition.MobileNetV3RecognitionEngine
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider

private val Forest = Color(0xFF182B43)
private val ForestDeep = Color(0xFF101D30)
private val Gold = Color(0xFFC39A58)
private val Ivory = Color(0xFFF5EFE5)
private val Sand = Color(0xFFE7D9C5)
private val Ink = Color(0xFF302B26)
private val Muted = Color(0xFF81796F)

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
private fun WelcomeScreen(language: AppLanguage, onStart: () -> Unit) {
    val ar = language == AppLanguage.Arabic
    val fr = language == AppLanguage.French
    Box(Modifier.fillMaxSize().background(ForestDeep)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LandmarkThumbnail("field-photos/ksar-bent-khass/architecture/1000078259.jpg", "Ksar Bent Khass", large = true, fullBleed = true)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, ForestDeep.copy(alpha = 0.96f)))))
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 26.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VISTA", color = Gold, fontSize = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
                    Text("EL BAYADH", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
                    Text("البيض فيستا", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text(if (ar) "دليلك السياحي الذكي لولاية البيض" else if (fr) "Votre guide touristique intelligent d’El Bayadh" else "Your smart tourism guide to El Bayadh", color = Color(0xFFE8DDCB), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 26.dp).height(54.dp), shape = RoundedCornerShape(28.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Gold)) {
                Text(if (ar) "ابدأ" else if (fr) "Commencer" else "Get started", color = ForestDeep, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
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
    var showWelcome by remember { mutableStateOf(!preferences.getBoolean("welcome_completed", false)) }
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
                if (showWelcome) {
                    WelcomeScreen(language, onStart = { preferences.edit().putBoolean("welcome_completed", true).apply(); showWelcome = false })
                } else androidx.compose.material3.Scaffold(
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
                                c, language,
                                searchQuery = searchQuery, onSearchChange = { searchQuery = it },
                                onOpenScanner = { selectedTab = 2 },
                                landmarks = catalogue?.landmarks.orEmpty(), photosByLandmark = catalogue?.photosByLandmark.orEmpty(), favoriteIds = favoriteIds,
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
    c: Copy, language: AppLanguage,
    searchQuery: String, onSearchChange: (String) -> Unit, onOpenScanner: () -> Unit,
    landmarks: List<Landmark>, photosByLandmark: Map<String, List<String>>,
    favoriteIds: Set<String>, onFavoriteToggle: (String) -> Unit, onOpenLandmark: (Landmark) -> Unit
) {
    val lang = when (language) { AppLanguage.Arabic -> CatalogueLanguage.AR; AppLanguage.French -> CatalogueLanguage.FR; AppLanguage.English -> CatalogueLanguage.EN }
    val ar = language == AppLanguage.Arabic
    val fr = language == AppLanguage.French
    val featured = landmarks.filter { photosByLandmark[it.id].orEmpty().isNotEmpty() || !it.image.isNullOrBlank() }
    val hero = featured.firstOrNull { it.id == "ksar-bent-khass" } ?: featured.firstOrNull()
    val results = landmarks.filter { searchQuery.isNotBlank() && (it.name(lang).contains(searchQuery.trim(), true) || it.description(lang).contains(searchQuery.trim(), true)) }
    Column(Modifier.fillMaxSize().background(Ivory).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Forest, ForestDeep)))) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("VISTA", color = Gold, fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("EL BAYADH", color = Color.White, fontSize = 11.sp, letterSpacing = 4.sp)
                        Text(if (ar) "دليلك السياحي الذكي لولاية البيض" else if (fr) "Guide touristique intelligent d’El Bayadh" else "Smart tourism guide to El Bayadh", color = Color(0xFFE8DDCB), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(c.welcome, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(c.subtitle, color = Color(0xFFE8DDCB), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery, onValueChange = onSearchChange, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(c.search, color = Muted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Forest) },
                    shape = RoundedCornerShape(18.dp), singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White, focusedBorderColor = Gold, unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        }
        if (searchQuery.isNotBlank()) {
            Text(if (ar) "نتائج البحث" else if (fr) "Résultats" else "Search results", color = Forest, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
            if (results.isEmpty()) Text(if (ar) "لا توجد نتائج مطابقة" else if (fr) "Aucun résultat" else "No matching places", color = Muted, modifier = Modifier.padding(horizontal = 20.dp))
            else results.forEach { item -> LandmarkCard(item, lang, item.id in favoriteIds, { onFavoriteToggle(item.id) }, { onOpenLandmark(item) }, Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) }
        } else {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(if (ar) "وجهة تستحق الاكتشاف" else if (fr) "À découvrir" else "Worth discovering", color = Forest, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (ar) "تراث أصيل وطبيعة استثنائية" else if (fr) "Patrimoine et paysages" else "Heritage and remarkable landscapes", color = Muted, fontSize = 11.sp)
                }
                Text("WILAYA 32", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            hero?.let { item ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onOpenLandmark(item) }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                    Column {
                        LandmarkThumbnail(photosByLandmark[item.id].orEmpty().firstOrNull() ?: item.image, item.name(lang), large = true)
                        Column(Modifier.padding(15.dp)) {
                            Text(if (ar) "اختيار VISTA" else if (fr) "SÉLECTION VISTA" else "VISTA SELECTION", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(item.name(lang), color = Forest, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                            Text(item.description(lang), color = Muted, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Text(if (ar) "استكشف حسب النوع" else if (fr) "Explorer par catégorie" else "Explore by category", color = Forest, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp))
            val cats = listOf(
                Triple(if (ar) "القصور والواحات" else if (fr) "Ksour et oasis" else "Ksour & oases", "ksour", Gold),
                Triple(if (ar) "المواقع الطبيعية" else if (fr) "Sites naturels" else "Natural sites", "natural-sites", Color(0xFF7B927E)),
                Triple(if (ar) "النقوش الصخرية" else if (fr) "Art rupestre" else "Rock art", "rock-art", Color(0xFFB18A69)),
                Triple(if (ar) "التراث الديني" else if (fr) "Patrimoine religieux" else "Religious heritage", "religious-heritage", Color(0xFF77869A))
            )
            Column(Modifier.padding(horizontal = 16.dp)) {
                cats.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { cat ->
                            val count = landmarks.count { it.categoryId == cat.second }
                            Card(Modifier.weight(1f).padding(bottom = 9.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(cat.third.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                        Icon(when (cat.second) { "ksour" -> Icons.Filled.Home; "natural-sites" -> Icons.Filled.Map; "rock-art" -> Icons.Filled.Search; else -> Icons.Filled.Language }, contentDescription = null, tint = Forest, modifier = Modifier.size(19.dp))
                                    }
                                    Spacer(Modifier.width(9.dp))
                                    Column {
                                        Text(cat.first, color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(count.toString() + if (ar) " معالم" else if (fr) " sites" else " places", color = Muted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).clickable(onClick = onOpenScanner), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = ForestDeep)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, tint = ForestDeep, modifier = Modifier.size(31.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (ar) "اكتشف من هنا" else if (fr) "Découvrez ici" else "Discover here", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (ar) "وجهات مميزة" else if (fr) "Destinations vedettes" else "Featured destinations", color = Forest, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(featured.size.toString() + if (ar) " وجهات" else if (fr) " sites" else " places", color = Muted, fontSize = 10.sp)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                featured.take(8).forEach { item ->
                    Card(Modifier.width(218.dp).clickable { onOpenLandmark(item) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column {
                            LandmarkThumbnail(photosByLandmark[item.id].orEmpty().firstOrNull() ?: item.image, item.name(lang), large = true)
                            Column(Modifier.padding(12.dp)) {
                                Text(item.name(lang), color = Forest, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(if (ar) "ولاية البيض" else if (fr) "Wilaya d’El Bayadh" else "El Bayadh Province", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp).clip(RoundedCornerShape(14.dp)).background(Sand).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Language, contentDescription = null, tint = Forest) }
                Spacer(Modifier.width(10.dp))
                Text(c.offline, color = Forest, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
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
                if (landmark.sources.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        when (language) {
                            CatalogueLanguage.AR -> "المصادر والمراجع"
                            CatalogueLanguage.FR -> "Sources et références"
                            CatalogueLanguage.EN -> "Sources and references"
                        },
                        color = Forest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    landmark.sources.forEach { source ->
                        Spacer(Modifier.height(7.dp))
                        Text(source.title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        source.publisher?.let { Text(it, color = Muted, fontSize = 11.sp) }
                        source.note?.let { Text(it, color = Muted, fontSize = 11.sp, lineHeight = 16.sp) }
                        source.url?.let { Text(it, color = Forest, fontSize = 10.sp) }
                    }
                }
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
private fun LandmarkThumbnail(assetPath: String?, description: String, large: Boolean = false, fullBleed: Boolean = false) {
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
            modifier = (if (fullBleed) Modifier.fillMaxSize() else if (large) Modifier.fillMaxWidth().height(190.dp) else Modifier.size(76.dp)).clip(RoundedCornerShape(12.dp)),
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
    val recognitionEngine = remember(context) { MobileNetV3RecognitionEngine(context) }
    var recognitionModelLoaded by remember { mutableStateOf(false) }
    var recognitionReady by remember { mutableStateOf(false) }
    var liveStatus by remember { mutableStateOf<String?>(if (isArabic) "تهيئة محرك التحليل البصري…" else if (isFrench) "Initialisation du moteur visuel…" else "Preparing visual analysis engine…") }
    var galleryStatus by remember { mutableStateOf<String?>(null) }
    var liveSuggestedLandmarks by remember { mutableStateOf<List<Landmark>>(emptyList()) }
    var gallerySuggestedLandmarks by remember { mutableStateOf<List<Landmark>>(emptyList()) }
    val recognitionScope = rememberCoroutineScope()
    LaunchedEffect(recognitionEngine) {
        recognitionReady = recognitionEngine.prepare()
        recognitionModelLoaded = recognitionEngine.isModelLoaded
        liveStatus = when {
            !recognitionModelLoaded -> if (isArabic) "تعذّر تحميل محرك التحليل البصري." else if (isFrench) "Impossible de charger le moteur visuel." else "Could not load the visual analysis engine."
            !recognitionReady -> if (isArabic) "المحرك محمّل؛ يعمل حاليًا بترشيح بصري أولي غير مؤكد." else if (isFrench) "Moteur chargé ; suggestions visuelles préliminaires non confirmées." else "Engine loaded; preliminary, unverified visual suggestions are active."
            else -> if (isArabic) "وجّه الكاميرا نحو معلم؛ يجري تحليل الصورة تلقائيًا." else if (isFrench) "Pointez la caméra vers un site ; analyse automatique." else "Point the camera at a landmark; automatic analysis is active."
        }
    }
    DisposableEffect(recognitionEngine) {
        onDispose { recognitionEngine.close() }
    }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            recognitionScope.launch {
                val selected = decodeGalleryImage(context, uri)
                if (selected == null) {
                    recognitionStatus = if (isArabic) "تعذّر فتح الصورة. جرّب صورة أخرى." else if (isFrench) "Impossible d’ouvrir cette image. Essayez-en une autre." else "Could not open this image. Try another photo."
                } else {
                    analyzeLandmarkPhoto(
                        selected, recognitionEngine, recognitionReady, landmarks, language,
                        onOpenLandmark = onOpenLandmark,
                        onSuggestions = { gallerySuggestedLandmarks = it },
                        onStatus = { galleryStatus = it }
                    )
                }
            }
        }
    }
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
        val analyzerExecutor = Executors.newSingleThreadExecutor()
        val lastAnalysisAt = AtomicLong(0L)
        val lastCandidateId = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val stableCandidateFrames = AtomicInteger(0)
        val analysisInFlight = AtomicBoolean(false)

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
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                        analysis.setAnalyzer(analyzerExecutor) { proxy ->
                            val now = SystemClock.elapsedRealtime()
                            val previousAnalysisAt = lastAnalysisAt.get()
                            val shouldAnalyze = now - previousAnalysisAt >= 1100L &&
                                lastAnalysisAt.compareAndSet(previousAnalysisAt, now) &&
                                analysisInFlight.compareAndSet(false, true)
                            val frame = if (shouldAnalyze) imageProxyToBitmap(proxy) else null
                            proxy.close()
                            if (frame != null && !disposed) {
                                recognitionScope.launch {
                                    try {
                                        val ranked = recognitionEngine.rankPreviewCandidates(frame)
                                        if (disposed) return@launch
                                        val best = ranked.firstOrNull()?.takeIf { it.score >= 0.45f }
                                        if (best == null) {
                                            stableCandidateFrames.set(0)
                                            lastCandidateId.set(null)
                                            liveStatus = if (isArabic) "تم تحليل الإطار؛ لم يظهر تشابه بصري كافٍ مع الصور المرجعية. حرّك الهاتف ببطء نحو المعلم." else if (isFrench) "Image analysée ; aucune similarité suffisante avec les références. Déplacez lentement le téléphone." else "Frame analyzed; no sufficient visual similarity to the references. Slowly move the phone toward the landmark."
                                        } else {
                                            val previous = lastCandidateId.getAndSet(best.landmarkId)
                                            val stable = if (previous == best.landmarkId) stableCandidateFrames.incrementAndGet() else {
                                                stableCandidateFrames.set(1); 1
                                            }
                                            if (stable >= 2) {
                                                liveSuggestedLandmarks = ranked
                                                    .filter { it.score >= 0.35f }
                                                    .mapNotNull { candidate -> landmarks.firstOrNull { it.id == candidate.landmarkId } }
                                                liveStatus = if (isArabic) "أقرب تطابق بصري مبدئي — ترشيح غير مؤكد، اضغط لفتح بطاقة المعلم." else if (isFrench) "Meilleure similarité visuelle préliminaire — suggestion non confirmée." else "Closest preliminary visual match — unverified suggestion; tap to open."
                                            }
                                        }
                                    } catch (_: Exception) {
                                        liveStatus = if (isArabic) "تعذّر تحليل إطار الكاميرا؛ ستتم إعادة المحاولة تلقائيًا." else if (isFrench) "Échec de l’analyse de l’image ; nouvelle tentative automatique." else "Camera frame analysis failed; retrying automatically."
                                    } finally {
                                        if (!frame.isRecycled) frame.recycle()
                                        analysisInFlight.set(false)
                                    }
                                }
                            } else if (shouldAnalyze) {
                                analysisInFlight.set(false)
                            }
                        }
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        boundProvider = provider
                        boundPreview = preview
                        boundAnalysis = analysis
                    }.onFailure {
                        if (!disposed) recognitionStatus = if (isArabic) "تعذّر تشغيل تحليل الكاميرا. أعد فتح شاشة الماسح." else if (isFrench) "Impossible de démarrer l’analyse caméra. Rouvrez le scanner." else "Could not start camera analysis. Reopen the scanner."
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
            analyzerExecutor.shutdown()
        }
    }

    Column(Modifier.fillMaxSize().background(Ivory).verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(22.dp)),
                factory = { previewView }
            )
            Spacer(Modifier.height(14.dp))
            liveStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Sand)
                ) {
                    Text(status, color = Forest, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
                }
            }
            liveSuggestedLandmarks.forEachIndexed { index, candidate ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onOpenLandmark(candidate) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(candidate.name(when (language) {
                            AppLanguage.Arabic -> CatalogueLanguage.AR
                            AppLanguage.French -> CatalogueLanguage.FR
                            AppLanguage.English -> CatalogueLanguage.EN
                        }), color = Forest, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isArabic) "اضغط لعرض تفاصيل المعلم" else if (isFrench) "Appuyez pour voir le site" else "Tap to view landmark details",
                            color = Muted, fontSize = 11.sp
                        )
                    }
                }
            }
            OutlinedButton(
                enabled = recognitionModelLoaded && localReferences.isNotEmpty(),
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(if (isArabic) "اختيار صورة من المعرض" else if (isFrench) "Choisir depuis la galerie" else "Choose from gallery", color = Forest)
            }
            galleryStatus?.let { status ->
                Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Sand)) {
                    Text(status, color = Forest, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(12.dp))
                }
            }
            gallerySuggestedLandmarks.forEach { candidate ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onOpenLandmark(candidate) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(candidate.name(when (language) {
                            AppLanguage.Arabic -> CatalogueLanguage.AR
                            AppLanguage.French -> CatalogueLanguage.FR
                            AppLanguage.English -> CatalogueLanguage.EN
                        }), color = Forest, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(if (isArabic) "اقتراح من صورة المعرض — اضغط لفتح التفاصيل" else if (isFrench) "Suggestion depuis la galerie — touchez pour ouvrir" else "Gallery suggestion — tap to open details", color = Muted, fontSize = 11.sp)
                    }
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
                        if (isArabic) "اسمح لـ VISTA باستخدام الكاميرا" else if (isFrench) "Autorisez VISTA à utiliser la caméra" else "Allow VISTA to use the camera",
                        color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Text(if (isArabic) "السماح بالكاميرا" else if (isFrench) "Autoriser la caméra" else "Allow camera", color = ForestDeep)
                    }
                }
            }
        }
    }
}

private suspend fun decodeGalleryImage(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1280) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }.getOrNull()
}

private suspend fun analyzeLandmarkPhoto(
    snapshot: Bitmap,
    engine: MobileNetV3RecognitionEngine,
    recognitionReady: Boolean,
    landmarks: List<Landmark>,
    language: AppLanguage,
    onOpenLandmark: (Landmark) -> Unit,
    onSuggestions: (List<Landmark>) -> Unit,
    onStatus: (String) -> Unit
) {
    val isArabic = language == AppLanguage.Arabic
    val isFrench = language == AppLanguage.French
    try {
        onSuggestions(emptyList())
        if (recognitionReady) {
            when (val result = engine.recognize(snapshot)) {
                is dz.vista.elbayadh.recognition.RecognitionDecision.Match -> {
                    val found = landmarks.firstOrNull { it.id == result.candidate.landmarkId }
                    if (found != null) onOpenLandmark(found)
                    else onStatus(if (isArabic) "المعلم غير موجود في الموسوعة المحلية." else if (isFrench) "Ce site est absent du catalogue local." else "The matched site is missing from the local catalogue.")
                }
                is dz.vista.elbayadh.recognition.RecognitionDecision.Ambiguous ->
                    onStatus(if (isArabic) "النتيجة غير حاسمة؛ جرّب صورة أو زاوية أخرى." else if (isFrench) "Résultat non concluant ; essayez une autre photo ou un autre angle." else "Inconclusive result; try another photo or angle.")
                dz.vista.elbayadh.recognition.RecognitionDecision.Unknown ->
                    onStatus(if (isArabic) "لم نتعرّف على هذا المشهد." else if (isFrench) "Scène non reconnue." else "Scene not recognized.")
                dz.vista.elbayadh.recognition.RecognitionDecision.ModelUnavailable ->
                    onStatus(if (isArabic) "محرك التعرّف غير جاهز." else if (isFrench) "Le moteur n’est pas prêt." else "Recognition engine is not ready.")
            }
        } else {
            val suggestions = engine.suggestCandidates(snapshot)
            val matchedLandmarks = suggestions.mapNotNull { candidate -> landmarks.firstOrNull { it.id == candidate.landmarkId } }
            onSuggestions(matchedLandmarks)
            val names = matchedLandmarks.map { landmark ->
                landmark.name(when (language) {
                    AppLanguage.Arabic -> CatalogueLanguage.AR
                    AppLanguage.French -> CatalogueLanguage.FR
                    AppLanguage.English -> CatalogueLanguage.EN
                })
            }
            onStatus(if (names.isEmpty()) {
                if (isArabic) "لم تتوفر اقتراحات لهذه الصورة." else if (isFrench) "Aucune suggestion disponible pour cette image." else "No suggestions available for this image."
            } else {
                val list = names.joinToString("، ")
                if (isArabic) "اقتراحات تجريبية غير مؤكدة: $list. هذه ليست نتيجة تعرّف موثوقة بعد."
                else if (isFrench) "Suggestions expérimentales non confirmées : $list. L’identification n’est pas encore validée."
                else "Unverified experimental suggestions: $list. This is not a validated identification."
            })
        }
    } catch (_: Exception) {
        onStatus(if (isArabic) "حدث خطأ أثناء تحليل الصورة." else if (isFrench) "Une erreur est survenue pendant l’analyse." else "An error occurred while analyzing the image.")
    } finally {
        if (!snapshot.isRecycled) snapshot.recycle()
    }
}

private fun imageProxyToBitmap(proxy: ImageProxy): Bitmap? {
    return runCatching {
        val plane = proxy.planes.first()
        val buffer = plane.buffer.duplicate()
        val width = proxy.width
        val height = proxy.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = y * rowStride + x * pixelStride
                require(offset >= 0 && offset + 3 < bytes.size) { "RGBA camera frame buffer is incomplete" }
                val r = bytes[offset].toInt() and 0xFF
                val g = bytes[offset + 1].toInt() and 0xFF
                val b = bytes[offset + 2].toInt() and 0xFF
                val a = bytes[offset + 3].toInt() and 0xFF
                pixels[y * width + x] = android.graphics.Color.argb(a, r, g, b)
            }
        }
        val raw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        raw.setPixels(pixels, 0, width, 0, 0, width, height)
        val rotation = proxy.imageInfo.rotationDegrees
        if (rotation == 0) raw else {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also { raw.recycle() }
        }
    }.getOrNull()
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
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text(if (isArabic) "حول التطبيق" else if (isFrench) "À propos" else "About VISTA", color = Forest, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(if (isArabic) "دليل سياحي ذكي للتعريف بتراث ولاية البيض ومعالمها التاريخية والطبيعية، مع محتوى أساسي متاح دون إنترنت." else if (isFrench) "Un guide touristique intelligent du patrimoine et des sites naturels et historiques d’El Bayadh, avec un contenu essentiel hors ligne." else "A smart guide to El Bayadh heritage and natural and historical sites, with essential offline content.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(12.dp))
                Text(if (isArabic) "دليل الاستخدام" else if (isFrench) "Guide d’utilisation" else "Usage guide", color = Forest, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (isArabic) "تصفّح المعالم من الرئيسية، ثم استخدم الماسح لقراءة QR أو الباركود أو التقط صورة للمعلم. افتح بطاقة المعلم لعرض معلوماته وصوره، وأضفه إلى المفضلة عند الحاجة." else if (isFrench) "Parcourez les sites depuis l’accueil. Scannez un QR/code-barres ou prenez une photo du site, puis consultez sa fiche et ses photos." else "Browse landmarks from Home. Scan a QR/barcode or photograph a landmark, then open its profile to view details and photos.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(12.dp))
                Text(if (isArabic) "عن المشروع" else if (isFrench) "Le projet" else "Project", color = Forest, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (isArabic) "يهدف VISTA إلى إبراز المقومات السياحية والتراثية والطبيعية لولاية البيض في دليل موحد." else if (isFrench) "VISTA valorise les ressources touristiques, patrimoniales et naturelles de la wilaya d’El Bayadh." else "VISTA showcases the tourism, heritage and natural assets of El Bayadh in one guide.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
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
