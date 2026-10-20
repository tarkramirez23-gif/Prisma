package com.theylor.prisma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.theylor.prisma.data.repository.MovieRepository
import com.theylor.prisma.ui.theme.PrismaCardBackground
import com.theylor.prisma.ui.theme.PrismaDarkBackground
import com.theylor.prisma.ui.theme.PrismaOrange
import com.theylor.prisma.ui.theme.PrismaRed
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Resenas(
    onHomeTabClick: () -> Unit = {},
    onProfileTabClick: () -> Unit = {},
    onCommunityTabClick: () -> Unit = {},
    onListsTabClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val movieRepository = remember { MovieRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var reviewsList by remember { mutableStateOf<List<ReviewData>>(sampleReviews) }
    var isLoadingReviews by remember { mutableStateOf(true) }

    var showWriteSheet by remember { mutableStateOf(false) }

    // Formulario de reseña
    var selectedMovieTitle by remember { mutableStateOf(movieRepository.defaultCatalog.first().title) }
    var selectedMovieGenre by remember { mutableStateOf(movieRepository.defaultCatalog.first().genres.firstOrNull() ?: "Cine") }
    var userRating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    var reviewError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val catalogMovies = movieRepository.defaultCatalog

    // Cargar reseñas de Firestore
    LaunchedEffect(Unit) {
        try {
            val snapshot = withTimeoutOrNull(2500) {
                db.collection("community_reviews")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val remoteReviews = snapshot.documents.mapNotNull { doc ->
                    val userName = doc.getString("userName") ?: "Cinéfilo PRISMA"
                    val timeStr = doc.getString("timeStr") ?: "Hace un momento"
                    val rating = doc.getLong("rating")?.toInt() ?: 5
                    val text = doc.getString("reviewText") ?: ""
                    val title = doc.getString("movieTitle") ?: "Cine"
                    val genre = doc.getString("movieGenre") ?: "Cine"
                    ReviewData(doc.id, userName, timeStr, rating, text, title, genre)
                }
                reviewsList = remoteReviews + sampleReviews
            }
        } catch (_: Exception) {
            // Keep default sample reviews
        } finally {
            isLoadingReviews = false
        }
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
                ResenasTopBar()
            },
            bottomBar = {
                ResenasBottomBar(
                    onHomeClick = onHomeTabClick,
                    onProfileClick = onProfileTabClick,
                    onCommunityClick = onCommunityTabClick,
                    onListsClick = onListsTabClick
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reseñas Recientes",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showWriteSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrismaRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Escribir",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingReviews) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrismaRed)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(reviewsList) { review ->
                            ReviewCard(review)
                        }
                    }
                }
            }
        }

        // Modal Sheet para escribir nueva reseña
        if (showWriteSheet) {
            ModalBottomSheet(
                onDismissRequest = { showWriteSheet = false },
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
                            text = "Publicar Reseña",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showWriteSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seleccionar Película
                    Text("Selecciona una Película", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(catalogMovies) { movie ->
                            val isSelected = movie.title == selectedMovieTitle
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) PrismaRed else PrismaCardBackground,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedMovieTitle = movie.title
                                        selectedMovieGenre = movie.genres.firstOrNull() ?: "Cine"
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = movie.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seleccionar Calificación (1-5 estrellas)
                    Text("Tu Calificación", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { starIndex ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (starIndex <= userRating) Color(0xFFFFB300) else Color.DarkGray,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { userRating = starIndex }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$userRating / 5 ★",
                            color = PrismaOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo de Texto de Reseña
                    Text("Reseña o Crítica", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = {
                            reviewText = it
                            reviewError = null
                        },
                        placeholder = {
                            Text("Escribe tu opinión sobre la película...", color = Color.Gray, fontSize = 13.sp)
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrismaRed,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedContainerColor = PrismaCardBackground,
                            unfocusedContainerColor = PrismaCardBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    reviewError?.let {
                        Text(it, color = PrismaRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val trimmed = reviewText.trim()
                            if (trimmed.isEmpty()) {
                                reviewError = "Por favor escribe tu reseña antes de publicar."
                                return@Button
                            }

                            scope.launch {
                                isSubmitting = true
                                val user = auth.currentUser
                                val authorName = user?.displayName?.ifEmpty { "Cinéfilo " + (user.email?.substringBefore("@") ?: "PRISMA") }
                                    ?: "Cinéfilo PRISMA"

                                val newReviewData = ReviewData(
                                    id = System.currentTimeMillis().toString(),
                                    userName = authorName,
                                    timestamp = "Hace un momento",
                                    rating = userRating,
                                    reviewText = trimmed,
                                    movieTitle = selectedMovieTitle,
                                    movieGenre = selectedMovieGenre
                                )

                                reviewsList = listOf(newReviewData) + reviewsList

                                try {
                                    val firestoreMap = mapOf(
                                        "userName" to authorName,
                                        "timeStr" to "Hace un momento",
                                        "rating" to userRating,
                                        "reviewText" to trimmed,
                                        "movieTitle" to selectedMovieTitle,
                                        "movieGenre" to selectedMovieGenre,
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    // Guardar en Realtime Database (el link console.firebase.google.com/.../database)
                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("community_reviews").push().setValue(firestoreMap)

                                    withTimeoutOrNull(2500) {
                                        db.collection("community_reviews").add(firestoreMap).await()
                                    }
                                } catch (_: Exception) {
                                    // Local state preserved
                                }

                                reviewText = ""
                                isSubmitting = false
                                showWriteSheet = false
                            }
                        },
                        enabled = !isSubmitting && reviewText.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrismaRed,
                            disabledContainerColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Publicar Reseña", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResenasTopBar() {
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
fun ResenasBottomBar(
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onListsClick: () -> Unit = {}
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
            selected = true,
            onClick = onCommunityClick,
            icon = { Icon(Icons.Default.People, contentDescription = "Community") },
            label = { Text("Community", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrismaRed,
                selectedTextColor = PrismaRed,
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

@Composable
fun ReviewCard(review: ReviewData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PrismaCardBackground
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = review.userName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = review.timestamp,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) PrismaOrange else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = review.reviewText,
                color = Color.LightGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrismaDarkBackground, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = PrismaRed,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = review.movieTitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = review.movieGenre,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

data class ReviewData(
    val id: String = "",
    val userName: String = "",
    val timestamp: String = "",
    val rating: Int = 5,
    val reviewText: String = "",
    val movieTitle: String = "",
    val movieGenre: String = ""
)

val sampleReviews = listOf(
    ReviewData(
        id = "s1",
        userName = "Carlos Ruiz",
        timestamp = "Hace 2 horas",
        rating = 5,
        reviewText = "Una obra maestra absoluta de la ciencia ficción moderna. La fotografía y la banda sonora te sumergen por completo en una atmósfera inigualable. Totalmente recomendada.",
        movieTitle = "Neon Horizon",
        movieGenre = "Sci-Fi / Cyberpunk"
    ),
    ReviewData(
        id = "s2",
        userName = "Elena M.",
        timestamp = "Hace 5 horas",
        rating = 4,
        reviewText = "Un thriller psicológico brillante. Te mantiene al borde del asiento durante las dos horas completas con giros inesperados. Las actuaciones son soberbias.",
        movieTitle = "El Abismo",
        movieGenre = "Misterio / Thriller"
    ),
    ReviewData(
        id = "s3",
        userName = "Diego F.",
        timestamp = "Ayer",
        rating = 3,
        reviewText = "Visualmente es espectacular, pero el guion se siente un poco flojo en el segundo acto. Aún así, vale la pena verla en la pantalla más grande posible.",
        movieTitle = "The Ebb Tide",
        movieGenre = "Aventura / Fantasía"
    ),
    ReviewData(
        id = "s4",
        userName = "Laura V.",
        timestamp = "Hace 2 días",
        rating = 5,
        reviewText = "Hacía tiempo que una película no me conmovía tanto. Una historia profundamente humana expresada con una delicadeza y una belleza visual increíbles.",
        movieTitle = "Eco del Viento",
        movieGenre = "Romance / Música"
    )
)

@Preview
@Composable
fun ResenasPreview() {
    Resenas()
}
