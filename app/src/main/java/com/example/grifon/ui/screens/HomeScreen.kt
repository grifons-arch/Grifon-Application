package com.example.grifon.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.grifon.R
import com.example.grifon.core.AppLanguage
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.Product
import com.example.grifon.ui.theme.GrifonGold
import com.example.grifon.viewmodel.CategoryIconItem
import com.example.grifon.viewmodel.HomeViewModel
import java.util.Locale

private val HomeBackground = Color(0xFFF6F5F1)
private val HomeCard = Color.White
private val HomeBlue = Color(0xFF001489)
private val HomeBlueDark = Color(0xFF001064)
private val HomeText = Color(0xFF1C2433)
private val HomeMuted = Color(0xFF6A7280)
private val HomeBorder = Color(0xFFE2E6EE)
private val HomeGold = Color(0xFFE2C283)
private val HomeSoftBlue = Color(0xFFEAF0FF)
private val HomeSoftGray = Color(0xFFF1F3F7)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (String) -> Unit,
    onSearch: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onOpenAccount: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenCart: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val canDisplayPrices = LocalCanDisplayPrices.current
    val isLoggedIn = LocalIsLoggedIn.current
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = HomeBackground,
    ) {
        when (val state = uiState) {
            UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = HomeBlue)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = Color(0xFFB3261E),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            is UiState.Success -> {
                val data = state.data
                val menuCategories = remember(viewModel.staticCategoryIcons, data.categories) {
                    viewModel.staticCategoryIcons.filter { it.categoryId != null }
                }
                val promoCategories = remember {
                    homePromoCategories()
                }
                val productTabs = remember(data.featuredProducts, data.allProducts) {
                    buildHomeProductTabs(
                        featuredProducts = data.featuredProducts,
                        allProducts = data.allProducts,
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 108.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        MobileWholesaleStrip(canDisplayPrices = canDisplayPrices)
                    }
                    item {
                        UtilityLinksSection()
                    }
                    item {
                        MobileHeaderSection(
                            isLoggedIn = isLoggedIn,
                            canDisplayPrices = canDisplayPrices,
                            onSearch = onSearch,
                            onOpenAccount = onOpenAccount,
                            onOpenFavorites = onOpenFavorites,
                            onOpenCart = onOpenCart,
                        )
                    }
                    item {
                        PrimaryMenuSection(
                            onCategoryClick = onCategoryClick,
                            onOpenExternal = { uriHandler.openUri(it) },
                        )
                    }
                    item {
                        CategoryCarouselSection(
                            promoCategories = promoCategories,
                            onCategoryClick = onCategoryClick,
                        )
                    }
                    item {
                        CategoryMenuSection(
                            menuCategories = menuCategories,
                            categoryModels = data.categories,
                            onCategoryClick = onCategoryClick,
                        )
                    }
                    item {
                        FeatureHighlightsSection()
                    }
                    item {
                        CustomOrdersSection(
                            canDisplayPrices = canDisplayPrices,
                            onCategoryClick = onCategoryClick,
                            onOpenExternal = { uriHandler.openUri(it) },
                        )
                    }
                    item {
                        VisitUsSection(
                            onOpenExternal = { uriHandler.openUri(it) },
                        )
                    }
                    item {
                        HomeProductTabsSection(
                            tabs = productTabs,
                            favoriteIds = data.favoriteIds,
                            onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                            onProductClick = onProductClick,
                        )
                    }
                    item {
                        NewsletterSection()
                    }
                    item {
                        FooterLinksSection(
                            onCategoryClick = onCategoryClick,
                            onOpenExternal = { uriHandler.openUri(it) },
                        )
                    }
                    item {
                        SupportSection()
                    }
                    item {
                        FooterCopyrightSection()
                    }
                }
            }
        }
    }

}

@Composable
private fun MobileWholesaleStrip(canDisplayPrices: Boolean) {
    val message = if (canDisplayPrices) {
        localizedText(
            greek = "Η πρόσβαση χονδρικής είναι ενεργή. Μπορείτε να δείτε τιμές και να προχωρήσετε σε παραγγελία.",
            english = "Wholesale access is active. You can view prices and proceed with your order.",
            swedish = "Grossiståtkomst är aktiv. Du kan se priser och fortsätta med beställningen.",
        )
    } else {
        localizedText(
            greek = "Για να μπορείτε να παραγγείλετε ή να δείτε τιμές, θα πρέπει πρώτα να δημιουργήσετε λογαριασμό ή να συνδεθείτε με τον υπάρχοντα λογαριασμό σας.",
            english = "To place orders or view prices, you first need to create an account or sign in.",
            swedish = "För att beställa eller se priser behöver du först skapa ett konto eller logga in.",
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canDisplayPrices) Color(0xFF0F7A4E) else HomeBlue,
        ),
    ) {
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun UtilityLinksSection() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickLinkChip(
                label = localizedText(
                    greek = "Σουηδικό κατάστημα",
                    english = "Swedish store",
                    swedish = "Svensk butik",
                ),
                onClick = { uriHandler.openUri("https://replica.grifon.gr/sv/") },
            )
            QuickLinkChip(
                label = localizedText(
                    greek = "Καταστήματα λιανικής",
                    english = "Retail stores",
                    swedish = "Butiker",
                ),
                onClick = { uriHandler.openUri("https://replica.grifon.gr/katastimata") },
            )
            QuickLinkChip(
                label = localizedText(
                    greek = "Παραγγελίες",
                    english = "Orders",
                    swedish = "Beställningar",
                ),
                onClick = { uriHandler.openUri("https://replica.grifon.gr/istoriko-apo-tis-paraggelies-sas") },
            )
        }
    }
}

@Composable
private fun MobileHeaderSection(
    isLoggedIn: Boolean,
    canDisplayPrices: Boolean,
    onSearch: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenCart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Grifon",
                    modifier = Modifier
                        .height(44.dp)
                        .width(126.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedText(
                            greek = "Είδη δώρων & τουριστικών",
                            english = "Gift & souvenir wholesale",
                            swedish = "Present- och souvenirgrossist",
                        ),
                        color = HomeText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = localizedText(
                            greek = "Mobile απόδοση της αρχικής του replica.grifon.gr",
                            english = "Mobile rendering of the replica.grifon.gr homepage",
                            swedish = "Mobil version av startsidan på replica.grifon.gr",
                        ),
                        color = HomeMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            FauxSearchBar(onClick = { onSearch("") })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeaderActionPill(
                    icon = Icons.Filled.AccountCircle,
                    label = if (isLoggedIn) {
                        localizedText(
                            greek = "Ο λογαριασμός μου",
                            english = "My account",
                            swedish = "Mitt konto",
                        )
                    } else {
                        localizedText(
                            greek = "Σύνδεση",
                            english = "Sign in",
                            swedish = "Logga in",
                        )
                    },
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAccount,
                )
                HeaderActionPill(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = localizedText(
                        greek = "Wishlist",
                        english = "Wishlist",
                        swedish = "Önskelista",
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFavorites,
                )
                HeaderActionPill(
                    icon = Icons.Filled.ShoppingCart,
                    label = if (canDisplayPrices) {
                        localizedText(
                            greek = "Καλάθι",
                            english = "Cart",
                            swedish = "Varukorg",
                        )
                    } else {
                        localizedText(
                            greek = "Χονδρική",
                            english = "Wholesale",
                            swedish = "Grossist",
                        )
                    },
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCart,
                )
            }
        }
    }
}

@Composable
private fun CategoryCarouselSection(
    promoCategories: List<HomePromoCategory>,
    onCategoryClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(
            eyebrow = localizedText(
                greek = "box-catecarosel",
                english = "box-catecarosel",
                swedish = "box-catecarosel",
            ),
            title = localizedText(
                greek = "Κατηγορίες αρχικής",
                english = "Homepage categories",
                swedish = "Kategorier på startsidan",
            ),
            subtitle = localizedText(
                greek = "Οι βασικές κατηγορίες της live αρχικής σε mobile carousel.",
                english = "The main live-homepage categories in a mobile carousel.",
                swedish = "De viktigaste kategorierna från live-hemsidan i en mobil karusell.",
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(promoCategories) { category ->
                PromoCategoryCard(
                    modifier = Modifier.width(262.dp),
                    category = category,
                    onClick = { onCategoryClick(category.categoryId) },
                )
            }
        }
    }
}

@Composable
private fun PrimaryMenuSection(
    onCategoryClick: (String) -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    val menuGroups = remember { primaryMenuGroups() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = localizedText(
                    greek = "Δίκτυο Grifon και Πληροφορίες",
                    english = "Grifon Network and Information",
                    swedish = "Grifon-nätverk och information",
                ),
                color = HomeText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = localizedText(
                    greek = "Mobile εκδοχή του οριζόντιου menu της αρχικής, με τα sections που λείπουν από το header.",
                    english = "Mobile version of the homepage horizontal menu, including the sections missing from the header.",
                    swedish = "Mobil version av startsidans horisontella meny med sektionerna som saknades i headern.",
                ),
                color = HomeMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            menuGroups.forEach { group ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = HomeSoftGray),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = group.title,
                            color = HomeBlue,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        group.items.forEachIndexed { index, item ->
                            FooterLinkRow(
                                item = item,
                                onCategoryClick = onCategoryClick,
                                onOpenExternal = onOpenExternal,
                            )
                            if (index != group.items.lastIndex) {
                                HorizontalDivider(color = HomeBorder)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryMenuSection(
    menuCategories: List<CategoryIconItem>,
    categoryModels: List<Category>,
    onCategoryClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = localizedText(
                    greek = "Ψωνίστε ανά κατηγορία",
                    english = "Shop by category",
                    swedish = "Handla efter kategori",
                ),
                color = HomeText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = localizedText(
                    greek = "Mobile εκδοχή του κατακόρυφου category menu της live αρχικής.",
                    english = "Mobile version of the vertical category menu from the live homepage.",
                    swedish = "Mobil version av den vertikala kategorimenyn från live-hemsidan.",
                ),
                color = HomeMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            menuCategories.forEachIndexed { index, item ->
                val categoryId = item.categoryId ?: return@forEachIndexed
                CategoryMenuRow(
                    iconRes = item.resId,
                    title = categoryDisplayName(categoryId, categoryModels),
                    onClick = { onCategoryClick(categoryId) },
                )
                if (index != menuCategories.lastIndex) {
                    HorizontalDivider(color = HomeBorder)
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightsSection() {
    val features = remember { homeFeatureHighlights() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(
            eyebrow = localizedText(
                greek = "box-clotyabn",
                english = "box-clotyabn",
                swedish = "box-clotyabn",
            ),
            title = localizedText(
                greek = "Γιατί Grifon",
                english = "Why Grifon",
                swedish = "Varför Grifon",
            ),
            subtitle = localizedText(
                greek = "Τα βασικά service blocks της αρχικής του website, αποδομένα σε mobile cards.",
                english = "The key service blocks from the website homepage, rendered as mobile cards.",
                swedish = "De viktigaste serviceblocken från webbplatsens startsida i mobilkort.",
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(features) { feature ->
                FeatureCard(
                    modifier = Modifier.width(210.dp),
                    feature = feature,
                )
            }
        }
    }
}

@Composable
private fun CustomOrdersSection(
    canDisplayPrices: Boolean,
    onCategoryClick: (String) -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = localizedText(
                    greek = "Κατασκευάζουμε προϊόντα δικής σας έμπνευσης",
                    english = "We manufacture products inspired by your own ideas",
                    swedish = "Vi tillverkar produkter inspirerade av dina egna idéer",
                ),
                color = HomeText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = localizedText(
                    greek = "Η αρχική του website δίνει έμφαση στις ειδικές κατασκευές, στα custom projects και στην 360° παρουσίαση προϊόντων. Εδώ το αποδίδουμε σε compact mobile blocks.",
                    english = "The website homepage emphasizes special constructions, custom projects, and 360° product presentation. Here that is translated into compact mobile blocks.",
                    swedish = "Webbplatsens startsida lyfter fram specialtillverkningar, kundanpassade projekt och 360° produktvisning. Här översätts det till kompakta mobilblock.",
                ),
                color = HomeMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            PromoImageCard(
                title = localizedText(
                    greek = "Ειδικές παραγγελίες και custom collections",
                    english = "Special orders and custom collections",
                    swedish = "Specialbeställningar och anpassade kollektioner",
                ),
                subtitle = localizedText(
                    greek = "Από τουριστικά είδη μέχρι εταιρικά δώρα και θεματικές κατασκευές.",
                    english = "From tourist products to corporate gifts and thematic constructions.",
                    swedish = "Från turistprodukter till företagsgåvor och tematiska konstruktioner.",
                ),
                imageRes = R.drawable.diakosmitika_keramikago,
                onClick = { onCategoryClick("4000") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompactInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.ViewInAr,
                    title = localizedText(
                        greek = "360° Προβολή",
                        english = "360° View",
                        swedish = "360° vy",
                    ),
                    body = localizedText(
                        greek = "Στο website υπάρχει 360 viewer. Στο mobile app κρατάμε το αντίστοιχο section ως προωθητικό block.",
                        english = "The website includes a 360 viewer. In the mobile app we preserve it as a promotional block.",
                        swedish = "Webbplatsen har en 360-visare. I mobilappen behåller vi det som ett kampanjblock.",
                    ),
                    onClick = { onOpenExternal("https://360.vfigures.gr/360/400030-1.01.html") },
                )
                CompactInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Storefront,
                    title = localizedText(
                        greek = if (canDisplayPrices) "Wholesale ενεργό" else "Wholesale πρόσβαση",
                        english = if (canDisplayPrices) "Wholesale active" else "Wholesale access",
                        swedish = if (canDisplayPrices) "Grossist aktiv" else "Grossiståtkomst",
                    ),
                    body = if (canDisplayPrices) {
                        localizedText(
                            greek = "Οι τιμές και οι παραγγελίες είναι διαθέσιμες στον λογαριασμό σας.",
                            english = "Prices and ordering are available on your account.",
                            swedish = "Priser och beställning är tillgängliga på ditt konto.",
                        )
                    } else {
                        localizedText(
                            greek = "Μετά την έγκριση λογαριασμού ξεκλειδώνουν τιμές και παραγγελίες.",
                            english = "Prices and ordering unlock after account approval.",
                            swedish = "Priser och beställning låses upp efter kontogodkännande.",
                        )
                    },
                    onClick = { onOpenExternal("https://replica.grifon.gr/o-logargiasmos-mou") },
                )
            }
        }
    }
}

@Composable
private fun VisitUsSection(
    onOpenExternal: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = HomeSoftGray),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = localizedText(
                    greek = "Επισκεφτείτε μας από κοντά",
                    english = "Visit us in person",
                    swedish = "Besök oss på plats",
                ),
                color = HomeText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = localizedText(
                    greek = "Το HTML της αρχικής περιλαμβάνει YouTube section και χάρτη. Στη mobile έκδοση του app τα αποδίδουμε ως γρήγορα info cards.",
                    english = "The homepage HTML includes a YouTube section and map. In the app mobile version they are rendered as quick info cards.",
                    swedish = "Startsidans HTML innehåller en YouTube-sektion och karta. I appens mobilversion visas de som snabba infokort.",
                ),
                color = HomeMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompactInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.PlayCircle,
                    title = "YouTube",
                    body = localizedText(
                        greek = "Video παρουσίαση showroom και προϊόντων.",
                        english = "Video presentation of the showroom and products.",
                        swedish = "Videopresentation av showroom och produkter.",
                    ),
                    onClick = { onOpenExternal("https://replica.grifon.gr/content/gia-emas") },
                )
                CompactInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.LocationOn,
                    title = localizedText(
                        greek = "Χάρτης",
                        english = "Map",
                        swedish = "Karta",
                    ),
                    body = localizedText(
                        greek = "Ηράκλειο Κρήτης, στοιχεία επικοινωνίας και εύκολη πρόσβαση.",
                        english = "Heraklion, Crete, with contact details and easy access.",
                        swedish = "Heraklion, Kreta, med kontaktuppgifter och enkel åtkomst.",
                    ),
                    onClick = { onOpenExternal("https://replica.grifon.gr/epikinoniste-mazi-mas") },
                )
            }
        }
    }
}

@Composable
private fun HomeProductTabsSection(
    tabs: List<HomeProductTab>,
    favoriteIds: Set<String>,
    onToggleFavorite: (Product) -> Unit,
    onProductClick: (String) -> Unit,
) {
    if (tabs.isEmpty()) return

    var selectedTabIndex by remember(tabs) { mutableIntStateOf(0) }
    val safeIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    val selectedTab = tabs[safeIndex]

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(
            eyebrow = localizedText(
                greek = "LeoProductTab",
                english = "LeoProductTab",
                swedish = "LeoProductTab",
            ),
            title = localizedText(
                greek = "Προϊόντα αρχικής",
                english = "Homepage products",
                swedish = "Produkter på startsidan",
            ),
            subtitle = localizedText(
                greek = "Τα tabs της live αρχικής μεταφέρονται εδώ με πραγματικά προϊόντα του app.",
                english = "The tabs from the live homepage are carried here with real app products.",
                swedish = "Flikarna från live-hemsidan visas här med riktiga produkter från appen.",
            ),
        )

        ScrollableTabRow(
            selectedTabIndex = safeIndex,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = HomeBlue,
            divider = {},
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = safeIndex == index,
                    onClick = { selectedTabIndex = index },
                    selectedContentColor = HomeBlue,
                    unselectedContentColor = HomeMuted,
                    text = {
                        Text(
                            text = tab.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        if (selectedTab.products.isEmpty()) {
            EmptyProductsState()
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(selectedTab.products, key = { it.id }) { product ->
                    WebsiteProductCard(
                        modifier = Modifier.width(220.dp),
                        product = product,
                        isFavorite = favoriteIds.contains(product.id),
                        onToggleFavorite = { onToggleFavorite(product) },
                        onClick = { onProductClick(product.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun NewsletterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(HomeBlue)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = localizedText(
                greek = "Εγγραφείτε στο ενημερωτικό μας δελτίο και λάβετε…",
                english = "Subscribe to our newsletter and receive…",
                swedish = "Prenumerera på vårt nyhetsbrev och få…",
            ),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = localizedText(
                greek = "Εγγραφείτε τώρα στο ενημερωτικό δελτίο για να λαμβάνετε ενημερώσεις σχετικά με καινούργια είδη, προσφορές και τα εκπτωτικά σας κουπόνια.",
                english = "Subscribe now to receive updates about new items, offers, and discount coupons.",
                swedish = "Prenumerera nu för att få uppdateringar om nya produkter, erbjudanden och rabattkuponger.",
            ),
            color = Color.White.copy(alpha = 0.84f),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            placeholder = {
                Text(
                    text = localizedText(
                        greek = "Η ηλεκτρονική σας διεύθυνση",
                        english = "Your email address",
                        swedish = "Din e-postadress",
                    ),
                    color = Color.White.copy(alpha = 0.72f),
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            ),
            singleLine = true,
        )
    }
}

@Composable
fun WholesaleLoginBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF3FF)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = localizedText(
                    greek = "Οι wholesale τιμές είναι διαθέσιμες μόνο μετά από έγκριση.",
                    english = "Wholesale prices become available after approval.",
                    swedish = "Grossistpriser blir tillgängliga efter godkännande.",
                ),
                color = HomeBlue,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = localizedText(
                    greek = "Μετά την ενεργοποίηση του λογαριασμού σας μπορείτε να δείτε τιμές και να προχωρήσετε σε παραγγελία.",
                    english = "After your account is activated, you can view prices and proceed with orders.",
                    swedish = "När ditt konto har aktiverats kan du se priser och fortsätta med beställningar.",
                ),
                color = HomeText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FooterLinksSection(
    onCategoryClick: (String) -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    val linkGroups = remember { footerLinkGroups() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        linkGroups.forEach { group ->
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HomeCard),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = group.title,
                        color = HomeText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    group.items.forEachIndexed { index, item ->
                        FooterLinkRow(
                            item = item,
                            onCategoryClick = onCategoryClick,
                            onOpenExternal = onOpenExternal,
                        )
                        if (index != group.items.lastIndex) {
                            HorizontalDivider(color = HomeBorder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFF0F2F6))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = localizedText(
                greek = "Χρειάζεστε βοήθεια? Καλέστε μας:",
                english = "Need help? Call us:",
                swedish = "Behöver du hjälp? Ring oss:",
            ),
            color = HomeText,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = "(0030) 2810 821627 · 2810 821730 · 6936545855",
            color = HomeBlue,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SupportBadge(label = "WhatsApp", modifier = Modifier.weight(1f))
            SupportBadge(label = "Viber", modifier = Modifier.weight(1f))
            SupportBadge(label = "Skype", modifier = Modifier.weight(1f))
            SupportBadge(label = "Telegram", modifier = Modifier.weight(1f))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Email, contentDescription = null, tint = HomeBlue)
            Text(
                text = "info@grifon.gr",
                color = HomeBlue,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Phone, contentDescription = null, tint = HomeMuted)
            Text(
                text = localizedText(
                    greek = "Δευτέρα – Σάββατο: 8:00 – 16:00 / Κυριακή: κλειστά",
                    english = "Monday – Saturday: 8:00 – 16:00 / Sunday: closed",
                    swedish = "Måndag – lördag: 8:00 – 16:00 / Söndag: stängt",
                ),
                color = HomeMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FooterCopyrightSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(color = HomeBorder)
        Text(
            text = "© Grifon – ${localizedText("Όλα τα δικαιώματα διατηρούνται.", "All rights reserved.", "Alla rättigheter förbehållna.")}",
            color = HomeMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun SectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = eyebrow,
            color = HomeGold,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = title,
            color = HomeText,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = subtitle,
            color = HomeMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PromoCategoryCard(
    category: HomePromoCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
    ) {
        Column {
            Image(
                painter = painterResource(id = category.imageRes),
                contentDescription = category.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = category.title,
                    color = HomeText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = category.description,
                    color = HomeMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = localizedText(
                            greek = "Άνοιγμα κατηγορίας",
                            english = "Open category",
                            swedish = "Öppna kategori",
                        ),
                        color = HomeBlue,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HomeBlue,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: HomeFeature,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(HomeSoftBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = HomeBlue,
                )
            }
            Text(
                text = feature.title,
                color = HomeText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = feature.description,
                color = HomeMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PromoImageCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HomeBlueDark),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clickable(onClick = onClick),
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.62f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CompactInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HomeBlue,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = title,
                color = HomeText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = body,
                color = HomeMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WebsiteProductCard(
    product: Product,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canDisplayPrices = LocalCanDisplayPrices.current
    val isLoggedIn = LocalIsLoggedIn.current

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(182.dp)
                    .background(Color.White),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentScale = ContentScale.Fit,
                )
                if (isLoggedIn) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, HomeBorder, CircleShape),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFD14A4A) else HomeText,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = product.title,
                    color = HomeText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = localizedText(
                        greek = "Κωδικός: ${productReference(product)}",
                        english = "Code: ${productReference(product)}",
                        swedish = "Kod: ${productReference(product)}",
                    ),
                    color = HomeMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (canDisplayPrices && product.price != null) {
                        "${product.price} ${product.currency}"
                    } else if (canDisplayPrices) {
                        stringResource(R.string.price_unavailable)
                    } else {
                        localizedText(
                            greek = "Τιμές μόνο για πελάτες χονδρικής",
                            english = "Prices visible to wholesale customers only",
                            swedish = "Priser visas endast för grossistkunder",
                        )
                    },
                    color = if (canDisplayPrices && product.price != null) HomeBlue else HomeMuted,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyProductsState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = localizedText(
                    greek = "Δεν βρέθηκαν προϊόντα για προβολή.",
                    english = "No products were found to display.",
                    swedish = "Inga produkter hittades att visa.",
                ),
                color = HomeMuted,
            )
        }
    }
}

@Composable
private fun QuickLinkChip(
    label: String,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(HomeSoftBlue)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = HomeBlue,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun HeaderActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF7F8FB))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HomeBlue,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = HomeText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
        )
    }
}

@Composable
private fun FauxSearchBar(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF7F8FB))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Category,
            contentDescription = null,
            tint = HomeBlue,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = localizedText(
                    greek = "Αναζήτηση ανά κατηγορία ή κωδικό",
                    english = "Search by category or code",
                    swedish = "Sök efter kategori eller kod",
                ),
                color = HomeText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = localizedText(
                    greek = "Όπως το search block του PrestaShop mobile header",
                    english = "Like the search block in the PrestaShop mobile header",
                    swedish = "Som sökblocket i PrestaShops mobila header",
                ),
                color = HomeMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = HomeBlue,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FooterLinkRow(
    item: FooterLinkItem,
    onCategoryClick: (String) -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
                    item.categoryId != null -> onCategoryClick(item.categoryId)
                    item.url != null -> onOpenExternal(item.url)
                }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = item.label,
            color = HomeMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = HomeBlue,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CategoryMenuRow(
    iconRes: Int,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(HomeSoftBlue),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = HomeText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = HomeBlue,
        )
    }
}

@Composable
private fun SupportBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, HomeBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = HomeBlue,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

private fun homePromoCategories(): List<HomePromoCategory> {
    return listOf(
        HomePromoCategory(
            categoryId = "4000",
            imageRes = R.drawable.diakosmitika_keramikago,
            title = "Διακοσμητικά Κεραμικά",
            description = "Κεραμικά κρεμαστά, πλάκες, μινωικά και ελληνικά διακοσμητικά για τουριστικά σημεία και gift shops.",
        ),
        HomePromoCategory(
            categoryId = "4000",
            imageRes = R.drawable.kersmiks_diskodmhtiks,
            title = "Φανάρια, Καντήλια",
            description = "Κεραμικά και διακοσμητικά φωτιστικά στοιχεία για εσωτερικό ή εξωτερικό χώρο.",
        ),
        HomePromoCategory(
            categoryId = "4500",
            imageRes = R.drawable.veroza,
            title = "Βερονέζ",
            description = "Μυθολογικά, ιστορικά και μοντέρνα αγαλματίδια υψηλής αισθητικής.",
        ),
        HomePromoCategory(
            categoryId = "4500",
            imageRes = R.drawable.polwesterika,
            title = "Πολυεστερικά",
            description = "Διακοσμητικά πολυεστερικά σχέδια για τουριστικά, θεματικά και gift προϊόντα.",
        ),
        HomePromoCategory(
            categoryId = "4500",
            imageRes = R.drawable.mproytzinna,
            title = "Μπρούτζινα",
            description = "Μπρούτζινα διακοσμητικά και αγαλματίδια για premium σημεία πώλησης.",
        ),
        HomePromoCategory(
            categoryId = "5000",
            imageRes = R.drawable.fvthsthka,
            title = "Φωτιστικά",
            description = "Φωτιστικά και decorative pieces που θυμίζουν τη live homepage βιτρίνα.",
        ),
        HomePromoCategory(
            categoryId = "7000",
            imageRes = R.drawable.skaki_tabli,
            title = "Τάβλι, Σκάκι",
            description = "Κλασικά παιχνίδια, τουριστικές σκακιέρες και χειροποίητα set.",
        ),
        HomePromoCategory(
            categoryId = "7000",
            imageRes = R.drawable.paixnidia_rouytrina,
            title = "Παιχνίδια, Λούτρινα",
            description = "Επιλεγμένα παιχνίδια και λούτρινα για gift corners και τουριστικά καταστήματα.",
        ),
        HomePromoCategory(
            categoryId = "7500",
            imageRes = R.drawable.sapounia,
            title = "Σαπούνια",
            description = "Ελαιόλαδο, φυσικές ύλες και χρηστικά είδη που ταιριάζουν στη boutique λογική του καταλόγου.",
        ),
        HomePromoCategory(
            categoryId = "8000",
            imageRes = R.drawable.yfasmatina,
            title = "Υφασμάτινα",
            description = "Τσάντες, υφάσματα και αξεσουάρ για τουριστικά καταστήματα και δώρο.",
        ),
    )
}

private fun homeFeatureHighlights(): List<HomeFeature> {
    return listOf(
        HomeFeature(
            icon = Icons.Filled.Storefront,
            title = "Δημιουργήστε",
            description = "Συνθέστε τη δική σας προϊοντική πρόταση με βάση το concept του καταστήματός σας.",
        ),
        HomeFeature(
            icon = Icons.Filled.LocalShipping,
            title = "Δωρεάν μεταφορά σε όλη την Κρήτη",
            description = "Για επιλεγμένες παραγγελίες και σταθερούς συνεργάτες εντός Κρήτης.",
        ),
        HomeFeature(
            icon = Icons.Filled.Category,
            title = "Για πελάτες εκτός Κρήτης",
            description = "Συνδυασμοί αποστολής και εξυπηρέτησης για την υπόλοιπη Ελλάδα.",
        ),
        HomeFeature(
            icon = Icons.Filled.Public,
            title = "Διεθνείς αποστολές",
            description = "Υποστηρίζουμε συνεργασίες και αποστολές για πελάτες εκτός Ελλάδας.",
        ),
        HomeFeature(
            icon = Icons.Filled.WorkspacePremium,
            title = "Premium δίκτυο Grifon",
            description = "Χρόνια εμπειρίας σε δώρα, τουριστικά και ειδικές θεματικές συλλογές.",
        ),
    )
}

private fun footerLinkGroups(): List<FooterLinkGroup> {
    return listOf(
        FooterLinkGroup(
            title = "Κεραμικά",
            items = listOf(
                FooterLinkItem(label = "Διακοσμητικά Κεραμικά", categoryId = "4000"),
                FooterLinkItem(label = "Φανάρια, Κεριά", categoryId = "4000"),
            ),
        ),
        FooterLinkGroup(
            title = "Αγαλματίδια κ.λπ.",
            items = listOf(
                FooterLinkItem(label = "Βερονέζ", categoryId = "4500"),
                FooterLinkItem(label = "Μπρούντζινα", categoryId = "4500"),
                FooterLinkItem(label = "Πολυεστέρικα", categoryId = "4500"),
                FooterLinkItem(label = "Γύψινα, Πωρόλιθος, Μαρμάρινα", categoryId = "4500"),
            ),
        ),
        FooterLinkGroup(
            title = "Περισσότερα",
            items = listOf(
                FooterLinkItem(label = "Τάβλι, Σκάκι", categoryId = "7000"),
                FooterLinkItem(label = "Σαπούνια", categoryId = "7500"),
                FooterLinkItem(label = "Ύφασμα και τσάντες", categoryId = "8000"),
                FooterLinkItem(label = "Όροι χρήσης", url = "https://replica.grifon.gr/content/oroi-kai-proipotheseis"),
                FooterLinkItem(label = "Πολιτική Απορρήτου", url = "https://replica.grifon.gr/content/genikos-kanonismos-prostasias-dedomenon-gdpr"),
                FooterLinkItem(label = "Σχετικά με εμάς", url = "https://replica.grifon.gr/content/gia-emas"),
            ),
        ),
    )
}

private fun primaryMenuGroups(): List<FooterLinkGroup> {
    return listOf(
        FooterLinkGroup(
            title = "Δίκτυο Grifon",
            items = listOf(
                FooterLinkItem(label = "Χονδρικής", url = "https://replica.grifon.gr/content/pelates-xondrikis"),
                FooterLinkItem(label = "Προμηθευτές", url = "https://replica.grifon.gr/content/Promitheutes-kai-sinergasies"),
                FooterLinkItem(label = "Θ. Εργασίας", url = "https://replica.grifon.gr/content/theseis-ergasias"),
            ),
        ),
        FooterLinkGroup(
            title = "Πληροφορίες",
            items = listOf(
                FooterLinkItem(label = "Γνωρίστε μας", url = "https://replica.grifon.gr/content/gia-emas"),
                FooterLinkItem(label = "Όροι και προϋποθέσεις", url = "https://replica.grifon.gr/content/oroi-kai-proipotheseis"),
                FooterLinkItem(label = "Παραγγελίες & Τρόποι αποστολής", url = "https://replica.grifon.gr/content/paragelies-kai-tropoi-apostolis"),
                FooterLinkItem(label = "Τρόποι πληρωμής", url = "https://replica.grifon.gr/content/Tropoi-pliromis"),
                FooterLinkItem(label = "Πολιτική επιστροφών", url = "https://replica.grifon.gr/content/Politiki-epistrofon"),
                FooterLinkItem(label = "GDPR", url = "https://replica.grifon.gr/content/genikos-kanonismos-prostasias-dedomenon-gdpr"),
                FooterLinkItem(label = "Επικοινωνήστε μαζί μας", url = "https://replica.grifon.gr/epikinoniste-mazi-mas"),
            ),
        ),
    )
}

private fun buildHomeProductTabs(
    featuredProducts: List<Product>,
    allProducts: List<Product>,
): List<HomeProductTab> {
    val sourceProducts = (featuredProducts + allProducts).distinctBy { it.id }
    if (sourceProducts.isEmpty()) return emptyList()

    fun matches(product: Product, vararg keywords: String): Boolean {
        val haystack = buildString {
            append(product.title)
            append(' ')
            append(product.brand)
            append(' ')
            product.attributesMap.forEach { (key, values) ->
                append(key)
                append(' ')
                values.forEach {
                    append(it)
                    append(' ')
                }
            }
        }.normalizeSearchField()

        return keywords.any { keyword -> haystack.contains(keyword.normalizeSearchField()) }
    }

    fun fallbackProducts(offset: Int): List<Product> {
        return sourceProducts.drop(offset).ifEmpty { sourceProducts }.take(10)
    }

    val veroneseProducts = sourceProducts.filter {
        matches(it, "veronese", "βερον", "βερoν")
    }.take(10)
    val ceramicProducts = sourceProducts.filter {
        matches(it, "κεραμ", "πλακ", "φαιστ", "κνωσ", "μινω", "diskos", "keram")
    }.take(10)
    val gameProducts = sourceProducts.filter {
        matches(it, "σκακι", "σκάκι", "τάβλι", "ταβλι", "backgammon", "chess")
    }.take(10)

    return listOf(
        HomeProductTab(
            title = localizedText(
                greek = "Νέα Προϊόντα",
                english = "New Products",
                swedish = "Nya produkter",
            ),
            products = featuredProducts.ifEmpty { fallbackProducts(0) }.take(10),
        ),
        HomeProductTab(
            title = "Βερονέζ",
            products = veroneseProducts.ifEmpty { fallbackProducts(2) },
        ),
        HomeProductTab(
            title = localizedText(
                greek = "Διακοσμητικά Κεραμικά",
                english = "Decorative Ceramics",
                swedish = "Dekorativ keramik",
            ),
            products = ceramicProducts.ifEmpty { fallbackProducts(4) },
        ),
        HomeProductTab(
            title = localizedText(
                greek = "Τάβλι, Σκάκι",
                english = "Backgammon, Chess",
                swedish = "Backgammon, schack",
            ),
            products = gameProducts.ifEmpty { fallbackProducts(6) },
        ),
    )
}

private fun categoryDisplayName(categoryId: String, categories: List<Category>): String {
    return categories.firstOrNull { it.id == categoryId }?.name ?: homeCategoryLabel(categoryId)
}

private fun productReference(product: Product): String {
    val referenceKey = product.attributesMap.entries.firstOrNull { (key, _) ->
        val normalized = key.normalizeSearchField()
        normalized.contains("sku") ||
            normalized.contains("reference") ||
            normalized.contains("kodik") ||
            normalized.contains("code")
    }
    val referenceValue = referenceKey?.value?.firstOrNull()?.takeIf { it.isNotBlank() }
    return referenceValue ?: product.id
}

private fun String.normalizeSearchField(): String {
    return lowercase(Locale.ROOT)
        .replace('ά', 'α')
        .replace('έ', 'ε')
        .replace('ή', 'η')
        .replace('ί', 'ι')
        .replace('ϊ', 'ι')
        .replace('ΐ', 'ι')
        .replace('ό', 'ο')
        .replace('ύ', 'υ')
        .replace('ϋ', 'υ')
        .replace('ΰ', 'υ')
        .replace('ώ', 'ω')
        .replace(Regex("[^a-z0-9α-ω]+"), " ")
        .trim()
}

private fun localizedText(greek: String, english: String, swedish: String): String {
    return when (AppLanguage.currentLanguage()) {
        "en" -> english
        "sv" -> swedish
        else -> greek
    }
}

private fun homeCategoryLabel(categoryId: String?): String {
    return when (AppLanguage.currentLanguage()) {
        "sv" -> when (categoryId) {
            null -> "Alla produkter"
            "4000" -> "Keramik"
            "4500" -> "Figuriner"
            "5000" -> "Dekor"
            "7500" -> "Bruksföremål"
            "7000" -> "Hobby och spel"
            "8000" -> "Accessoarer"
            else -> "Kategori"
        }

        "en" -> when (categoryId) {
            null -> "All Products"
            "4000" -> "Ceramics"
            "4500" -> "Figurines"
            "5000" -> "Decor"
            "7500" -> "Everyday Use"
            "7000" -> "Hobby & Games"
            "8000" -> "Accessories"
            else -> "Category"
        }

        else -> when (categoryId) {
            null -> "Όλα τα Προϊόντα"
            "4000" -> "Κεραμικά"
            "4500" -> "Αγαλματίδια κ.α."
            "5000" -> "Διακοσμητικά"
            "7500" -> "Για χρήση"
            "7000" -> "Χόμπι και παιχνίδια"
            "8000" -> "Αξεσουάρ"
            else -> "Κατηγορία"
        }
    }
}

data class HomePromoCategory(
    val categoryId: String,
    val imageRes: Int,
    val title: String,
    val description: String,
)

data class HomeFeature(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
)

data class FooterLinkGroup(
    val title: String,
    val items: List<FooterLinkItem>,
)

data class FooterLinkItem(
    val label: String,
    val categoryId: String? = null,
    val url: String? = null,
)

data class HomeProductTab(
    val title: String,
    val products: List<Product>,
)

@Composable
fun ProductCard(
    product: Product,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    val canDisplayPrices = LocalCanDisplayPrices.current
    val isLoggedIn = LocalIsLoggedIn.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.White.copy(alpha = 0.02f)),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentScale = ContentScale.Fit,
                )
                if (isLoggedIn) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.94f))
                            .border(1.5.dp, GrifonGold, CircleShape),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite_products),
                            tint = if (isFavorite) Color(0xFFE05050) else Color(0xFF3B3120),
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    minLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (canDisplayPrices && product.price != null) {
                    Text(
                        text = "${product.price} €",
                        color = Color(0xFFC5A059),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (canDisplayPrices) R.string.price_unavailable else R.string.wholesale_prices_only
                        ),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
