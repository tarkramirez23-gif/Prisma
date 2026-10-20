package com.theylor.prisma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.DocumentSnapshot
import com.theylor.prisma.data.model.Movie
import com.theylor.prisma.data.model.MovieReview
import com.theylor.prisma.data.repository.MovieRepository
import com.theylor.prisma.data.repository.ReviewRepository
import com.theylor.prisma.data.repository.UserMovieRepository
import com.theylor.prisma.data.repository.WatchState
import com.theylor.prisma.ui.theme.PrismaCardBackground
import com.theylor.prisma.ui.theme.PrismaDarkBackground
import com.theylor.prisma.ui.theme.PrismaOrange
import com.theylor.prisma.ui.theme.PrismaRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Peliculas(
    userMovieRepository: UserMovieRepository = remember { UserMovieRepository() },
    reviewRepository: ReviewRepository = remember { ReviewRepository() },
    currentTipoPerfil: String = "Cinefilo",
    onProfileTabClick: () -> Unit = {},
    onCommunityTabClick: () -> Unit = {},
    onListsTabClick: () -> Unit = {}
) {
    val repository = remember { MovieRepository() }
    val scope = rememberCoroutineScope()

    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var lastDoc by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPaginating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }

    var selectedMovie by remember { mutableStateOf<Movie?>(null) }

    // Estados de Búsqueda y Filtros
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var selectedDirector by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedDuration by remember { mutableStateOf<String?>(null) }

    var showFilterSheet by remember { mutableStateOf(false) }

    val allAvailableMovies = remember(movies) {
        (movies + repository.defaultCatalog).distinctBy { it.title }
    }
    val availableGenres = remember(allAvailableMovies) {
        allAvailableMovies.flatMap { it.genres }.distinct().sorted()
    }
    val availableCountries = remember(allAvailableMovies) {
        allAvailableMovies.map { it.country }.distinct().sorted()
    }
    val availableDirectors = remember(allAvailableMovies) {
        allAvailableMovies.map { it.director }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val availableYears = remember(allAvailableMovies) {
        allAvailableMovies.map { it.releaseYear }.distinct().sortedDescending()
    }
    val availableDurations = listOf("< 2 horas", ">= 2 horas")

    fun loadInitialMovies() {
        scope.launch {
            isLoading = true
            errorMessage = null
            lastDoc = null
            val result = repository.getMoviesPaginated(lastDocument = null, pageSize = 4, offsetIndex = 0)
            result.onSuccess { (newMovies, snapshot) ->
                movies = newMovies
                lastDoc = snapshot
                hasMore = newMovies.size >= 4
                isLoading = false
            }.onFailure { error ->
                errorMessage = error.localizedMessage ?: "Error al cargar las películas de Firestore"
                isLoading = false
            }
        }
    }

    fun loadMoreMovies() {
        if (isPaginating || !hasMore) return
        scope.launch {
            isPaginating = true
            val currentCount = movies.size
            val result = repository.getMoviesPaginated(lastDocument = lastDoc, pageSize = 4, offsetIndex = currentCount)
            result.onSuccess { (newMovies, snapshot) ->
                if (newMovies.isNotEmpty()) {
                    movies = movies + newMovies
                    lastDoc = snapshot
                    hasMore = newMovies.size >= 4
                } else {
                    hasMore = false
                }
                isPaginating = false
            }.onFailure {
                isPaginating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        userMovieRepository.loadUserMovieStatuses()
        loadInitialMovies()
    }

    val filteredMovies = remember(
        allAvailableMovies, searchQuery, selectedGenre, selectedCountry, selectedDirector, selectedYear, selectedDuration
    ) {
        val cleanQuery = searchQuery.normalizeForSearch()
        allAvailableMovies.filter { movie ->
            val matchesSearch = cleanQuery.isBlank() ||
                    movie.title.normalizeForSearch().contains(cleanQuery) ||
                    movie.director.normalizeForSearch().contains(cleanQuery) ||
                    movie.country.normalizeForSearch().contains(cleanQuery) ||
                    movie.synopsis.normalizeForSearch().contains(cleanQuery) ||
                    movie.genres.any { it.normalizeForSearch().contains(cleanQuery) } ||
                    movie.cast.any { it.normalizeForSearch().contains(cleanQuery) }

            val matchesGenre = selectedGenre == null || movie.genres.contains(selectedGenre)
            val matchesCountry = selectedCountry == null || movie.country.equals(selectedCountry, ignoreCase = true)
            val matchesDirector = selectedDirector == null || movie.director.equals(selectedDirector, ignoreCase = true)
            val matchesYear = selectedYear == null || movie.releaseYear == selectedYear
            val matchesDuration = when (selectedDuration) {
                "< 2 horas" -> !movie.duration.startsWith("2h") && !movie.duration.startsWith("3h")
                ">= 2 horas" -> movie.duration.startsWith("2h") || movie.duration.startsWith("3h")
                else -> true
            }

            matchesSearch && matchesGenre && matchesCountry && matchesDirector && matchesYear && matchesDuration
        }
    }

    val hasActiveFilters = searchQuery.isNotBlank() || selectedGenre != null || selectedCountry != null ||
            selectedDirector != null || selectedYear != null || selectedDuration != null

    fun clearAllFilters() {
        searchQuery = ""
        selectedGenre = null
        selectedCountry = null
        selectedDirector = null
        selectedYear = null
        selectedDuration = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrismaDarkBackground,
                        Color.Black
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PeliculasTopBar()
            },
            bottomBar = {
                PeliculasBottomBar(
                    onProfileClick = onProfileTabClick,
                    onCommunityClick = onCommunityTabClick,
                    onListsClick = onListsTabClick
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrismaRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cargando catálogo...",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = PrismaRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se pudieron cargar las películas",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { loadInitialMovies() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrismaRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reintentar", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val featuredMovie = movies.firstOrNull { it.isFeatured } ?: movies.firstOrNull()

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Buscar título, director o género...", color = Color.Gray, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = PrismaRed,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedContainerColor = PrismaCardBackground,
                                        unfocusedContainerColor = PrismaCardBackground
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { showFilterSheet = true },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(if (hasActiveFilters) PrismaRed else PrismaCardBackground, RoundedCornerShape(16.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filtros",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        if (hasActiveFilters) {
                            item(span = { GridItemSpan(2) }) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (selectedGenre != null) {
                                        item { RemovableFilterChip(label = "Género: $selectedGenre") { selectedGenre = null } }
                                    }
                                    if (selectedCountry != null) {
                                        item { RemovableFilterChip(label = "País: $selectedCountry") { selectedCountry = null } }
                                    }
                                    if (selectedDirector != null) {
                                        item { RemovableFilterChip(label = "Director: $selectedDirector") { selectedDirector = null } }
                                    }
                                    if (selectedYear != null) {
                                        item { RemovableFilterChip(label = "Año: $selectedYear") { selectedYear = null } }
                                    }
                                    if (selectedDuration != null) {
                                        item { RemovableFilterChip(label = "Duración: $selectedDuration") { selectedDuration = null } }
                                    }
                                    item {
                                        TextButton(onClick = { clearAllFilters() }) {
                                            Text("Limpiar todo", color = PrismaRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (featuredMovie != null && !hasActiveFilters) {
                            item(span = { GridItemSpan(2) }) {
                                val featuredWatchState = userMovieRepository.getStatus(featuredMovie.title)
                                MovieFeatureBanner(
                                    movie = featuredMovie,
                                    watchState = featuredWatchState,
                                    onMovieClick = { selectedMovie = featuredMovie }
                                )
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = "Catálogo",
                                        tint = PrismaRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (hasActiveFilters) "Resultados (${filteredMovies.size})" else "Catálogo de Películas",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (filteredMovies.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Sin resultados",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No encontramos películas que coincidan con la búsqueda o los filtros seleccionados.",
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Button(
                                            onClick = { clearAllFilters() },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrismaRed),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Restablecer Filtros", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredMovies) { movie ->
                                val watchState = userMovieRepository.getStatus(movie.title)
                                MovieGridCard(
                                    movie = movie,
                                    watchState = watchState,
                                    onClick = { selectedMovie = movie }
                                )
                            }
                        }

                        if (!hasActiveFilters && filteredMovies.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isPaginating) {
                                        CircularProgressIndicator(
                                            color = PrismaRed,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Cargando más películas...", color = Color.Gray, fontSize = 12.sp)
                                    } else if (hasMore) {
                                        OutlinedButton(
                                            onClick = { loadMoreMovies() },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Cargar más películas")
                                        }
                                    } else {
                                        Text(
                                            text = "Has llegado al final del catálogo",
                                            color = Color.DarkGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = PrismaDarkBackground,
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtros Avanzados",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { clearAllFilters() }) {
                            Text("Limpiar todo", color = PrismaRed, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FilterSectionTitle("Género")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableGenres) { genre ->
                            FilterSelectableChip(
                                label = genre,
                                isSelected = selectedGenre == genre,
                                onClick = { selectedGenre = if (selectedGenre == genre) null else genre }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FilterSectionTitle("País")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableCountries) { country ->
                            FilterSelectableChip(
                                label = country,
                                isSelected = selectedCountry == country,
                                onClick = { selectedCountry = if (selectedCountry == country) null else country }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FilterSectionTitle("Director")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableDirectors) { director ->
                            FilterSelectableChip(
                                label = director,
                                isSelected = selectedDirector == director,
                                onClick = { selectedDirector = if (selectedDirector == director) null else director }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FilterSectionTitle("Año")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableYears) { year ->
                            FilterSelectableChip(
                                label = year.toString(),
                                isSelected = selectedYear == year,
                                onClick = { selectedYear = if (selectedYear == year) null else year }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FilterSectionTitle("Duración")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableDurations) { duration ->
                            FilterSelectableChip(
                                label = duration,
                                isSelected = selectedDuration == duration,
                                onClick = { selectedDuration = if (selectedDuration == duration) null else duration }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismaRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ver Resultados (${filteredMovies.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (selectedMovie != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedMovie = null },
                containerColor = PrismaDarkBackground,
                contentColor = Color.White
            ) {
                MovieDetailSheet(
                    movie = selectedMovie!!,
                    userMovieRepository = userMovieRepository,
                    reviewRepository = reviewRepository,
                    currentTipoPerfil = currentTipoPerfil,
                    onClose = { selectedMovie = null }
                )
            }
        }
    }
}

@Composable
fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        color = Color.LightGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun FilterSelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) PrismaRed else PrismaCardBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun RemovableFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(PrismaRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remover",
            tint = Color.White,
            modifier = Modifier
                .size(14.dp)
                .clickable { onRemove() }
        )
    }
}

@Composable
fun MovieFeatureBanner(
    movie: Movie,
    watchState: WatchState = WatchState.NONE,
    onMovieClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E222F), Color(0xFF0F111A))
                )
            )
            .clickable { onMovieClick() }
    ) {
        AsyncImage(
            model = movie.bannerUrl.ifEmpty { movie.posterUrl },
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.55f
        )

        if (watchState == WatchState.WATCHED) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("✓ VISTA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else if (watchState == WatchState.PENDING) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color(0xFF1565C0), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("🔖 PENDIENTE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                movie.genres.take(2).forEach { genre ->
                    MovieChip(text = genre)
                }
                Row(
                    modifier = Modifier
                        .background(Color(0xFF2A2E3D), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = movie.rating.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onMovieClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrismaRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ver Detalle",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MovieGridCard(
    movie: Movie,
    watchState: WatchState = WatchState.NONE,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = PrismaCardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF1E202B)),
                contentAlignment = Alignment.Center
            ) {
                if (movie.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (watchState == WatchState.WATCHED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("✓ VISTA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (watchState == WatchState.PENDING) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFF1565C0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("🔖 PENDIENTE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = movie.rating.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${movie.releaseYear} • ${movie.country}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MovieDetailSheet(
    movie: Movie,
    userMovieRepository: UserMovieRepository,
    reviewRepository: ReviewRepository = remember { ReviewRepository() },
    currentTipoPerfil: String = "Cinefilo",
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentStatus = userMovieRepository.getStatus(movie.title)

    var userRating by remember { mutableIntStateOf(reviewRepository.userRatings[movie.title] ?: 0) }
    var reviewsList by remember { mutableStateOf<List<MovieReview>>(emptyList()) }
    var isReviewsLoading by remember { mutableStateOf(true) }

    var reviewText by remember { mutableStateOf("") }
    var reviewError by remember { mutableStateOf<String?>(null) }
    var reviewSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    val calculatedAvgRating = remember(reviewsList, userRating) {
        if (reviewsList.isNotEmpty()) {
            val total = reviewsList.sumOf { it.rating } + if (userRating > 0 && reviewsList.none { it.userName.contains("PRISMA") }) userRating else 0
            val count = reviewsList.size + if (userRating > 0 && reviewsList.none { it.userName.contains("PRISMA") }) 1 else 0
            val avg = total.toDouble() / count.coerceAtLeast(1)
            String.format("%.1f", avg).toDoubleOrNull() ?: movie.rating
        } else {
            if (userRating > 0) userRating.toDouble() else movie.rating
        }
    }

    LaunchedEffect(movie.title) {
        val loadedRating = reviewRepository.loadUserRating(movie.title)
        if (loadedRating > 0) {
            userRating = loadedRating
        }
        val loadedReviews = reviewRepository.loadReviewsForMovie(movie.title, movie.title)
        reviewsList = loadedReviews
        isReviewsLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = movie.bannerUrl.ifEmpty { movie.posterUrl },
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${movie.releaseYear}", color = Color.LightGray, fontSize = 13.sp)
            Text(text = "•", color = Color.Gray, fontSize = 13.sp)
            Text(text = movie.country, color = Color.LightGray, fontSize = 13.sp)
            Text(text = "•", color = Color.Gray, fontSize = 13.sp)
            Text(text = movie.duration.ifEmpty { "1h 50m" }, color = Color.LightGray, fontSize = 13.sp)
            Text(text = "•", color = Color.Gray, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$calculatedAvgRating (Promedio)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card de Calificación por Estrellas (1-5)
        Card(
            colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tu Calificación (1 a 5 Estrellas)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { starIndex ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $starIndex",
                            tint = if (starIndex <= userRating) Color(0xFFFFB300) else Color.DarkGray,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    userRating = starIndex
                                    scope.launch {
                                        reviewRepository.saveUserRating(movie.title, starIndex)
                                    }
                                }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (userRating > 0) {
                        Text(
                            text = "$userRating / 5 ★",
                            color = PrismaOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Toca las estrellas",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sección Marcar Película (Mi Biblioteca)
        Card(
            colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Estado en Mi Biblioteca",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterSelectableChip(
                        label = "✓ Vista",
                        isSelected = currentStatus == WatchState.WATCHED,
                        onClick = {
                            val newStatus = if (currentStatus == WatchState.WATCHED) WatchState.NONE else WatchState.WATCHED
                            scope.launch { userMovieRepository.setMovieStatus(movie.title, newStatus) }
                        }
                    )
                    FilterSelectableChip(
                        label = "🔖 Pendiente",
                        isSelected = currentStatus == WatchState.PENDING,
                        onClick = {
                            val newStatus = if (currentStatus == WatchState.PENDING) WatchState.NONE else WatchState.PENDING
                            scope.launch { userMovieRepository.setMovieStatus(movie.title, newStatus) }
                        }
                    )
                    if (currentStatus != WatchState.NONE) {
                        FilterSelectableChip(
                            label = "✕ Sin marcar",
                            isSelected = false,
                            onClick = {
                                scope.launch { userMovieRepository.setMovieStatus(movie.title, WatchState.NONE) }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            movie.genres.forEach { genre ->
                MovieChip(text = genre)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("FICHA TÉCNICA", color = PrismaRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                if (movie.director.isNotEmpty()) {
                    Text("Director: ${movie.director}", color = Color.White, fontSize = 13.sp)
                }
                Text("País de Origen: ${movie.country}", color = Color.White, fontSize = 13.sp)
                if (movie.cast.isNotEmpty()) {
                    Text("Elenco: ${movie.cast.joinToString(", ")}", color = Color.LightGray, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sinopsis",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.synopsis.ifEmpty { "Sin sinopsis disponible." },
            color = Color.LightGray,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Card Publicar Reseña Extendida (Perfil Cinéfilo)
        Card(
            colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Escribir Reseña Extendida",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Perfil activo: $currentTipoPerfil",
                    color = PrismaOrange,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (currentTipoPerfil.equals("Cinefilo", ignoreCase = true)) {
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = {
                            reviewText = it
                            reviewError = null
                            reviewSuccessMessage = null
                        },
                        placeholder = {
                            Text("Escribe tu análisis crítico sobre la película (mínimo 20 caracteres)...", color = Color.Gray, fontSize = 13.sp)
                        },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrismaRed,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${reviewText.trim().length} caracteres",
                            color = if (reviewText.trim().isNotEmpty()) Color.Green else Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    reviewError?.let {
                        Text(it, color = PrismaRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    reviewSuccessMessage?.let {
                        Text(it, color = Color.Green, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isPublishing = true
                                val ratingToUse = if (userRating > 0) userRating else 5
                                val result = reviewRepository.addReview(
                                    movieId = movie.title,
                                    movieTitle = movie.title,
                                    rating = ratingToUse,
                                    reviewText = reviewText,
                                    userProfileType = currentTipoPerfil
                                )
                                result.onSuccess {
                                    reviewText = ""
                                    reviewSuccessMessage = "¡Reseña publicada con éxito!"
                                    reviewError = null
                                    reviewsList = reviewRepository.movieReviewsMap[movie.title] ?: emptyList()
                                    isPublishing = false
                                }.onFailure { err ->
                                    reviewError = err.localizedMessage ?: "Error al publicar la reseña"
                                    isPublishing = false
                                }
                            }
                        },
                        enabled = !isPublishing && reviewText.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismaRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Publicar Reseña", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "Tu perfil actual es Espectador. Cambia a 'Cinéfilo' en la configuración de perfil para publicar reseñas extendidas.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lista de Reseñas de la Comunidad
        Text(
            text = "Reseñas de la Comunidad (${reviewsList.size})",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isReviewsLoading) {
            CircularProgressIndicator(color = PrismaRed, modifier = Modifier.size(24.dp))
        } else if (reviewsList.isEmpty()) {
            Text("Aún no hay reseñas para esta película. ¡Sé el primero en escribir una!", color = Color.Gray, fontSize = 13.sp)
        } else {
            reviewsList.forEach { rev ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rev.userName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row {
                                repeat(5) { starIndex ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (starIndex < rev.rating) PrismaOrange else Color.DarkGray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(rev.reviewText, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrismaRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cerrar Detalle", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MovieChip(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF2A2E3D), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PeliculasTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )

        Text(
            text = "PRISMA",
            color = PrismaRed,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PeliculasBottomBar(
    onProfileClick: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onListsClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = PrismaCardBackground,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrismaRed,
                selectedTextColor = PrismaRed,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onCommunityClick,
            icon = { Icon(Icons.Default.People, contentDescription = "Community") },
            label = { Text("Community", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onListsClick,
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lists") },
            label = { Text("Lists", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onProfileClick,
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

fun String.normalizeForSearch(): String {
    val unaccented = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return unaccented.trim().lowercase()
}

@Preview
@Composable
fun PeliculasPreview() {
    Peliculas()
}
