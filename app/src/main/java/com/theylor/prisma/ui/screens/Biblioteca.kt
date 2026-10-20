package com.theylor.prisma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theylor.prisma.data.model.Movie
import com.theylor.prisma.data.repository.MovieRepository
import com.theylor.prisma.data.repository.UserMovieRepository
import com.theylor.prisma.data.repository.WatchState
import com.theylor.prisma.ui.theme.PrismaCardBackground
import com.theylor.prisma.ui.theme.PrismaDarkBackground
import com.theylor.prisma.ui.theme.PrismaRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Biblioteca(
    userMovieRepository: UserMovieRepository,
    movieRepository: MovieRepository = remember { MovieRepository() },
    onHomeTabClick: () -> Unit = {},
    onCommunityTabClick: () -> Unit = {},
    onProfileTabClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Vistas, 1: Pendientes
    val scope = rememberCoroutineScope()

    val allCatalogMovies = movieRepository.defaultCatalog

    LaunchedEffect(Unit) {
        userMovieRepository.loadUserMovieStatuses()
    }

    val watchedMovies = remember(userMovieRepository.userMovieStatuses.toMap()) {
        allCatalogMovies.filter { movie ->
            userMovieRepository.getStatus(movie.title) == WatchState.WATCHED ||
                    userMovieRepository.getStatus(movie.id) == WatchState.WATCHED
        }
    }

    val pendingMovies = remember(userMovieRepository.userMovieStatuses.toMap()) {
        allCatalogMovies.filter { movie ->
            userMovieRepository.getStatus(movie.title) == WatchState.PENDING ||
                    userMovieRepository.getStatus(movie.id) == WatchState.PENDING
        }
    }

    val displayedMovies = if (selectedTab == 0) watchedMovies else pendingMovies

    var selectedMovieForDetail by remember { mutableStateOf<Movie?>(null) }

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
                BibliotecaTopBar()
            },
            bottomBar = {
                BibliotecaBottomBar(
                    onHomeClick = onHomeTabClick,
                    onCommunityClick = onCommunityTabClick,
                    onProfileClick = onProfileTabClick
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Mi Biblioteca",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Pestañas (Vistas / Pendientes)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PrismaCardBackground,
                    contentColor = PrismaRed,
                    modifier = Modifier.background(PrismaCardBackground, RoundedCornerShape(16.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Vistas (${watchedMovies.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) PrismaRed else Color.Gray
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Pendientes (${pendingMovies.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) PrismaRed else Color.Gray
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (displayedMovies.isEmpty()) {
                    // Estado Vacío
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.CheckCircleOutline else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == 0) "No has marcado películas como vistas" else "No tienes películas pendientes",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Explora el catálogo y marca tus películas favoritas para verlas guardadas aquí.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onHomeTabClick,
                                colors = ButtonDefaults.buttonColors(containerColor = PrismaRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Explorar Catálogo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedMovies) { movie ->
                            val currentWatchState = userMovieRepository.getStatus(movie.title)
                            LibraryMovieCard(
                                movie = movie,
                                watchState = currentWatchState,
                                onClick = { selectedMovieForDetail = movie },
                                onToggleStatus = {
                                    scope.launch {
                                        val newStatus = if (selectedTab == 0) WatchState.NONE else WatchState.WATCHED
                                        userMovieRepository.setMovieStatus(movie.title, newStatus)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (selectedMovieForDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedMovieForDetail = null },
                containerColor = PrismaDarkBackground,
                contentColor = Color.White
            ) {
                MovieDetailSheet(
                    movie = selectedMovieForDetail!!,
                    userMovieRepository = userMovieRepository,
                    onClose = { selectedMovieForDetail = null }
                )
            }
        }
    }
}

@Composable
fun LibraryMovieCard(
    movie: Movie,
    watchState: WatchState,
    onClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
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

                // Badge de Estado Vista / Pendiente
                if (watchState == WatchState.WATCHED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("✓ VISTA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (watchState == WatchState.PENDING) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFF1565C0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🔖 PENDIENTE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
fun BibliotecaTopBar() {
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
fun BibliotecaBottomBar(
    onHomeClick: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = PrismaCardBackground,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
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
            selected = true,
            onClick = { },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lists") },
            label = { Text("Lists", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrismaRed,
                selectedTextColor = PrismaRed,
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
