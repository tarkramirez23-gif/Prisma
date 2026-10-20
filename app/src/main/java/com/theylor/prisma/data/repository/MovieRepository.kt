package com.theylor.prisma.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.theylor.prisma.data.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class MovieRepository {
    private val db = FirebaseFirestore.getInstance()
    private val peliculasCollection = db.collection("peliculas")
    private val rtdbRef = FirebaseDatabase.getInstance().getReference("peliculas")

    val defaultCatalog = listOf(
        Movie(
            title = "Neon Horizon",
            synopsis = "En un futuro distópico dominado por luces de neón y megacorporaciones, un detective cibernético descubre una conspiración que amenaza la existencia de la humanidad.",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80",
            rating = 9.8,
            genres = listOf("Sci-Fi", "Acción"),
            releaseYear = 2025,
            duration = "2h 18m",
            director = "Katsuhiro Otomo",
            country = "Japón",
            cast = listOf("Akira Sato", "Elena Rostova", "Marcus Vance"),
            isFeatured = true
        ),
        Movie(
            title = "El Abismo",
            synopsis = "Una tripulación de exploración abisal se adentra en las profundidades de la fosa de las Marianas, desencadenando secretos ancestrales ocultos bajo el océano.",
            posterUrl = "https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
            rating = 8.9,
            genres = listOf("Misterio", "Thriller"),
            releaseYear = 2024,
            duration = "1h 55m",
            director = "Guillermo del Toro",
            country = "México",
            cast = listOf("Lucas Miller", "Sofia Chen", "David O'Connor"),
            isFeatured = false
        ),
        Movie(
            title = "Noche de Fuego",
            synopsis = "En un pequeño pueblo rodeado de conflictos, tres jóvenes aprenden el valor de la amistad mientras luchan por preservar sus sueños.",
            posterUrl = "https://images.unsplash.com/photo-1509248961158-e54f6934749c?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=800&q=80",
            rating = 8.5,
            genres = listOf("Drama", "Suspenso"),
            releaseYear = 2023,
            duration = "1h 48m",
            director = "Tatiana Huezo",
            country = "México",
            cast = listOf("Ana Cristina Ordóñez", "Mayra Batalla", "Norma Pablo"),
            isFeatured = false
        ),
        Movie(
            title = "The Ebb Tide",
            synopsis = "Un marinero solitario naufraga en una isla aparentemente desierta donde la marea revela ruinas antiguas llenas de enigmas insondables.",
            posterUrl = "https://images.unsplash.com/photo-1439405326854-014607f694d7?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80",
            rating = 8.2,
            genres = listOf("Aventura", "Fantasía"),
            releaseYear = 2024,
            duration = "2h 05m",
            director = "Christopher Nolan",
            country = "Reino Unido",
            cast = listOf("Christian Bale", "Cillian Murphy", "Florence Pugh"),
            isFeatured = false
        ),
        Movie(
            title = "Forma y Luz",
            synopsis = "Un viaje visual deslumbrante que explora la arquitectura moderna, las sombras de las metrópolis y la interacción humana con el espacio urbano.",
            posterUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
            rating = 9.0,
            genres = listOf("Documental", "Arte"),
            releaseYear = 2024,
            duration = "1h 30m",
            director = "Denis Villeneuve",
            country = "Canadá",
            cast = listOf("Narrado por Morgan Freeman"),
            isFeatured = false
        ),
        Movie(
            title = "Estrenos: Prisma Red",
            synopsis = "Una producción original de PRISMA que redefine el cine independiente con giros dramáticos e innovadores efectos cinematográficos.",
            posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80",
            rating = 9.4,
            genres = listOf("Acción", "Thriller"),
            releaseYear = 2025,
            duration = "2h 10m",
            director = "Theylor R.",
            country = "España",
            cast = listOf("Emma Stone", "Oscar Isaac", "Pedro Pascal"),
            isFeatured = false
        ),
        Movie(
            title = "Código Estelar",
            synopsis = "Una estación espacial pierde contacto con la Tierra tras recibir una misteriosa señal proveniente del centro de la Vía Láctea.",
            posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=800&q=80",
            rating = 9.3,
            genres = listOf("Sci-Fi", "Espacio"),
            releaseYear = 2025,
            duration = "2h 25m",
            director = "Ridley Scott",
            country = "EE.UU.",
            cast = listOf("Jessica Chastain", "Matt Damon", "Michael Fassbender"),
            isFeatured = false
        ),
        Movie(
            title = "La Sombra del Rey",
            synopsis = "Estrategias políticas, traiciones y batallas campales en la Europa medieval mientras un joven recluta busca vengar la caída de su dinastía.",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=800&q=80",
            rating = 8.7,
            genres = listOf("Histórico", "Drama"),
            releaseYear = 2023,
            duration = "2h 30m",
            director = "Peter Jackson",
            country = "Reino Unido",
            cast = listOf("Viggo Mortensen", "Cate Blanchett", "Orlando Bloom"),
            isFeatured = false
        ),
        Movie(
            title = "Eco del Viento",
            synopsis = "Dos músicos ambulantes cruzan los paisajes montañosos de los Andes componiendo la melodía que cambiará sus vidas para siempre.",
            posterUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80",
            rating = 8.1,
            genres = listOf("Romance", "Música"),
            releaseYear = 2024,
            duration = "1h 42m",
            director = "Alfonso Cuarón",
            country = "México",
            cast = listOf("Gael García Bernal", "Diego Luna", "Marina de Tavira"),
            isFeatured = false
        ),
        Movie(
            title = "El Laberinto de Cristal",
            synopsis = "Un grupo de arqueólogos descubre una ciudad subterránea construida enteramente de cuarzo refractario que distorsiona el tiempo.",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=800&q=80",
            rating = 8.8,
            genres = listOf("Fantasía", "Misterio"),
            releaseYear = 2022,
            duration = "2h 02m",
            director = "Steven Spielberg",
            country = "EE.UU.",
            cast = listOf("Harrison Ford", "Karen Allen", "John Rhys-Davies"),
            isFeatured = false
        ),
        Movie(
            title = "Sombras en la Nieve",
            synopsis = "Un detective retirado debe resolver un crimen imposible en una cabaña aislada durante la tormenta del siglo en los Alpes.",
            posterUrl = "https://images.unsplash.com/photo-1483921020237-2ff51e8e4b22?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80",
            rating = 8.6,
            genres = listOf("Suspenso", "Policial"),
            releaseYear = 2024,
            duration = "1h 50m",
            director = "David Fincher",
            country = "Francia",
            cast = listOf("Jake Gyllenhaal", "Mark Ruffalo", "Rooney Mara"),
            isFeatured = false
        ),
        Movie(
            title = "Odisea Cuántica",
            synopsis = "Un experimento con aceleradores de partículas abre portales a universos paralelos donde cada decisión humana crea una realidad distinta.",
            posterUrl = "https://images.unsplash.com/photo-1507499739999-097706ad8914?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80",
            rating = 9.6,
            genres = listOf("Sci-Fi", "Filosofía"),
            releaseYear = 2025,
            duration = "2h 40m",
            director = "Lana Wachowski",
            country = "EE.UU.",
            cast = listOf("Keanu Reeves", "Carrie-Anne Moss", "Yahya Abdul-Mateen II"),
            isFeatured = false
        ),
        Movie(
            title = "Crónicas del Desierto",
            synopsis = "Nómadas de las dunas luchan por el control de la última fuente de agua purificada en un planeta abrasado por dos soles.",
            posterUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1473580044384-7ba9967e16a0?auto=format&fit=crop&w=800&q=80",
            rating = 8.4,
            genres = listOf("Aventura", "Acción"),
            releaseYear = 2023,
            duration = "2h 15m",
            director = "George Miller",
            country = "Australia",
            cast = listOf("Tom Hardy", "Charlize Theron", "Nicholas Hoult"),
            isFeatured = false
        ),
        Movie(
            title = "Horizonte Infinito",
            synopsis = "Un recorrido cinematográfico por las fronteras más extremas del planeta Tierra y el cosmos, narrado con la última tecnología de filmación IMAX.",
            posterUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80",
            rating = 9.1,
            genres = listOf("Documental", "Ciencia"),
            releaseYear = 2024,
            duration = "1h 38m",
            director = "Ron Fricke",
            country = "EE.UU.",
            cast = listOf("Narrado por David Attenborough"),
            isFeatured = false
        ),
        Movie(
            title = "Ritmo Urbano",
            synopsis = "En los barrios bravos de una gran metrópoli, un grupo de jóvenes encuentra en el hip-hop y el baile callejero una vía de escape y redención.",
            posterUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80",
            rating = 8.3,
            genres = listOf("Música", "Drama"),
            releaseYear = 2023,
            duration = "1h 45m",
            director = "Spike Lee",
            country = "EE.UU.",
            cast = listOf("Anthony Ramos", "Corey Hawkins", "Leslie Grace"),
            isFeatured = false
        ),
        Movie(
            title = "La Última Estación",
            synopsis = "Pasajeros atascados en un tren nocturno durante una niebla espesa descubren que el vehículo no se detiene en ninguna estación conocida.",
            posterUrl = "https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=800&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
            rating = 8.0,
            genres = listOf("Terror", "Suspenso"),
            releaseYear = 2024,
            duration = "1h 40m",
            director = "Bong Joon-ho",
            country = "Corea del Sur",
            cast = listOf("Song Kang-ho", "Choi Woo-shik", "Park So-dam"),
            isFeatured = false
        )
    )

    suspend fun getMoviesPaginated(
        lastDocument: DocumentSnapshot? = null,
        pageSize: Long = 4,
        offsetIndex: Int = 0
    ): Result<Pair<List<Movie>, DocumentSnapshot?>> {
        CoroutineScope(Dispatchers.IO).launch {
            seedSampleMoviesIfEmpty()
        }

        return try {
            val resultPair = withTimeoutOrNull(2500) {
                var query = peliculasCollection
                    .orderBy("releaseYear", Query.Direction.DESCENDING)
                    .limit(pageSize)

                if (lastDocument != null) {
                    query = query.startAfter(lastDocument)
                }

                val snapshot = query.get().await()
                val movies = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Movie::class.java)?.copy(id = doc.id)
                }
                val newLastDocument = snapshot.documents.lastOrNull()
                Pair(movies, newLastDocument)
            }

            if (resultPair != null && resultPair.first.isNotEmpty()) {
                Result.success(resultPair)
            } else {
                val localPage = defaultCatalog.drop(offsetIndex).take(pageSize.toInt())
                Result.success(Pair(localPage, null))
            }
        } catch (_: Exception) {
            val localPage = defaultCatalog.drop(offsetIndex).take(pageSize.toInt())
            Result.success(Pair(localPage, null))
        }
    }

    private fun seedSampleMoviesIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (movie in defaultCatalog) {
                    val cleanDocId = movie.title.replace(Regex("[.#$\\[\\]/]"), "_")
                    // Escribe directamente en Firestore
                    peliculasCollection.document(cleanDocId).set(movie)
                    // Escribe directamente en Realtime Database
                    rtdbRef.child(cleanDocId).setValue(movie)
                }
            } catch (_: Exception) {}
        }
    }
}
