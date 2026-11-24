package dz.vista.elbayadh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
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
    var searchQuery by remember { mutableStateOf("") }
    var favoriteIds by remember {
        mutableStateOf(preferences.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }
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
                                searchQuery = searchQuery, onSearchChange = { searchQuery = it }
                            )
                            1 -> InfoScreen(c.map, c.mapNote, Icons.Filled.Map)
                            2 -> InfoScreen(c.scan, c.scanNote, Icons.Filled.CenterFocusStrong)
                            3 -> InfoScreen(c.favorites, if (favoriteIds.isEmpty()) c.emptyFavorites else
                                if (language == AppLanguage.Arabic) "عدد العناصر المحفوظة: ${favoriteIds.size}" else
                                if (language == AppLanguage.French) "Sites enregistrés : ${favoriteIds.size}" else
                                "Saved places: ${favoriteIds.size}", Icons.Filled.Favorite)
                            else -> MoreScreen(c, language, onLanguage = ::changeLanguage)
                        }
                    }
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
    onSearchChange: (String) -> Unit
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
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(c.discover, color = Forest, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("VISTA", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        val visibleCategories = c.categories.filter {
            searchQuery.isBlank() || it.contains(searchQuery.trim(), ignoreCase = true)
        }
        if (visibleCategories.isEmpty()) {
            Text(
                if (language == AppLanguage.Arabic) "لا توجد تصنيفات مطابقة" else
                    if (language == AppLanguage.French) "Aucune catégorie correspondante" else "No matching categories",
                color = Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            visibleCategories.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { colIndex, title ->
                        val palette = listOf(
                            Brush.linearGradient(listOf(Color(0xFF315D4C), Color(0xFF173D32))),
                            Brush.linearGradient(listOf(Color(0xFF8B6A3F), Color(0xFF59452D))),
                            Brush.linearGradient(listOf(Color(0xFF68745A), Color(0xFF344A39))),
                            Brush.linearGradient(listOf(Color(0xFF8C7358), Color(0xFF5A4938)))
                        )
                        CategoryCard(title, palette[(rowIndex * 2 + colIndex) % palette.size], Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
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
private fun MoreScreen(c: Copy, language: AppLanguage, onLanguage: (AppLanguage) -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Text(c.more, color = Forest, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text(c.moreNote, color = Muted, fontSize = 15.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(24.dp))
        Text("Language / Langue / اللغة", color = Forest, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(AppLanguage.Arabic to "العربية", AppLanguage.French to "Français", AppLanguage.English to "English").forEach { (lang, label) ->
                TextButton(onClick = { onLanguage(lang) }) { Text(label, color = if (language == lang) Gold else Forest) }
            }
        }
    }
}
