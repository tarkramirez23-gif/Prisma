package com.theylor.prisma.data.model

data class MovieReview(
    val id: String = "",
    val movieId: String = "",
    val movieTitle: String = "",
    val userId: String = "",
    val userName: String = "Cinéfilo Anon",
    val rating: Int = 5,
    val reviewText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userProfileType: String = "Cinefilo"
)
