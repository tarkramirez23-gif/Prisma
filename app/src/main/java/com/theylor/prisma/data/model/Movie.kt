package com.theylor.prisma.data.model

data class Movie(
    val id: String = "",
    val title: String = "",
    val synopsis: String = "",
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val rating: Double = 0.0,
    val genres: List<String> = emptyList(),
    val releaseYear: Int = 2024,
    val duration: String = "",
    val director: String = "",
    val country: String = "EE.UU.",
    val cast: List<String> = emptyList(),
    val isFeatured: Boolean = false
)
