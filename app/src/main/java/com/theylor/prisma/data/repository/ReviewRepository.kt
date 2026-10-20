package com.theylor.prisma.data.repository

import androidx.compose.runtime.mutableStateMapOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.theylor.prisma.data.model.MovieReview
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class ReviewRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val userRatings = mutableStateMapOf<String, Int>()
    val movieReviewsMap = mutableStateMapOf<String, List<MovieReview>>()

    suspend fun loadUserRating(movieId: String): Int {
        val userId = auth.currentUser?.uid ?: "guest_user"
        return try {
            val doc = withTimeoutOrNull(2000) {
                db.collection("peliculas")
                    .document(movieId)
                    .collection("ratings")
                    .document(userId)
                    .get()
                    .await()
            }
            val rating = doc?.getLong("rating")?.toInt() ?: 0
            if (rating > 0) {
                userRatings[movieId] = rating
            }
            rating
        } catch (_: Exception) {
            userRatings[movieId] ?: 0
        }
    }

    suspend fun saveUserRating(movieId: String, rating: Int) {
        userRatings[movieId] = rating
        val userId = auth.currentUser?.uid ?: "guest_user"

        try {
            withTimeoutOrNull(2000) {
                db.collection("peliculas")
                    .document(movieId)
                    .collection("ratings")
                    .document(userId)
                    .set(
                        mapOf(
                            "rating" to rating,
                            "userId" to userId,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
            }
        } catch (_: Exception) {
            // Memory state preserved
        }
    }

    suspend fun loadReviewsForMovie(movieId: String, movieTitle: String): List<MovieReview> {
        return try {
            val snapshot = withTimeoutOrNull(2500) {
                db.collection("peliculas")
                    .document(movieId)
                    .collection("reviews")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
            }

            val remoteReviews = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(MovieReview::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            val combinedReviews = if (remoteReviews.isNotEmpty()) {
                remoteReviews
            } else {
                movieReviewsMap[movieId] ?: getSampleReviewsForMovie(movieTitle)
            }

            movieReviewsMap[movieId] = combinedReviews
            combinedReviews
        } catch (_: Exception) {
            val fallback = movieReviewsMap[movieId] ?: getSampleReviewsForMovie(movieTitle)
            movieReviewsMap[movieId] = fallback
            fallback
        }
    }

    suspend fun addReview(
        movieId: String,
        movieTitle: String,
        rating: Int,
        reviewText: String,
        userProfileType: String = "Cinefilo"
    ): Result<MovieReview> {
        val trimmedText = reviewText.trim()
        if (trimmedText.isEmpty()) {
            return Result.failure(Exception("Escribe tu reseña antes de publicar."))
        }
        if (trimmedText.length > 1000) {
            return Result.failure(Exception("La reseña no puede exceder los 1000 caracteres."))
        }

        val currentUser = auth.currentUser
        val userName = currentUser?.displayName?.ifEmpty { "Cinéfilo " + (currentUser.email?.substringBefore("@") ?: "PRISMA") } ?: "Cinéfilo PRISMA"
        val userId = currentUser?.uid ?: "guest_user"

        val newReview = MovieReview(
            movieId = movieId,
            movieTitle = movieTitle,
            userId = userId,
            userName = userName,
            rating = rating,
            reviewText = trimmedText,
            timestamp = System.currentTimeMillis(),
            userProfileType = userProfileType
        )

        val currentList = movieReviewsMap[movieId] ?: emptyList()
        movieReviewsMap[movieId] = listOf(newReview) + currentList

        saveUserRating(movieId, rating)

        try {
            withTimeoutOrNull(2500) {
                db.collection("peliculas")
                    .document(movieId)
                    .collection("reviews")
                    .add(newReview)
                    .await()
            }
        } catch (_: Exception) {
            // Local state preserved
        }

        return Result.success(newReview)
    }

    private fun getSampleReviewsForMovie(movieTitle: String): List<MovieReview> {
        return listOf(
            MovieReview(
                id = "sample_1",
                movieId = movieTitle,
                movieTitle = movieTitle,
                userName = "Carlos Ruiz (Cinéfilo)",
                rating = 5,
                reviewText = "Una obra cinematográfica magistral. La composición visual y la profundidad de los personajes superan todas las expectativas.",
                timestamp = System.currentTimeMillis() - 7200000
            ),
            MovieReview(
                id = "sample_2",
                movieId = movieTitle,
                movieTitle = movieTitle,
                userName = "Elena M. (Cinéfilo)",
                rating = 4,
                reviewText = "Excelente dirección artística y ritmo vertiginoso. Definitivamente una de las producciones más destacadas del año.",
                timestamp = System.currentTimeMillis() - 86400000
            )
        )
    }
}
