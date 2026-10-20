package com.theylor.prisma.data.repository

import androidx.compose.runtime.mutableStateMapOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class WatchState {
    NONE,
    WATCHED,
    PENDING
}

class UserMovieRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance()

    val userMovieStatuses = mutableStateMapOf<String, WatchState>()

    suspend fun loadUserMovieStatuses() {
        val userId = auth.currentUser?.uid ?: "guest_user"
        try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("user_movies")
                .get()
                .await()

            userMovieStatuses.clear()
            for (doc in snapshot.documents) {
                val movieId = doc.id
                val statusStr = doc.getString("status") ?: "NONE"
                val status = when (statusStr) {
                    "WATCHED" -> WatchState.WATCHED
                    "PENDING" -> WatchState.PENDING
                    else -> WatchState.NONE
                }
                if (status != WatchState.NONE) {
                    userMovieStatuses[movieId] = status
                }
            }
        } catch (_: Exception) {
            // Memory state remains intact if offline
        }
    }

    suspend fun setMovieStatus(movieId: String, status: WatchState) {
        if (status == WatchState.NONE) {
            userMovieStatuses.remove(movieId)
        } else {
            userMovieStatuses[movieId] = status
        }

        val userId = auth.currentUser?.uid ?: "guest_user"
        val cleanKey = movieId.replace(Regex("[.#$\\[\\]]"), "_")

        try {
            // Guardar en Firebase Realtime Database (el link console.firebase.google.com/.../database)
            val userMoviesRtdbRef = rtdb.getReference("users").child(userId).child("user_movies").child(cleanKey)
            if (status == WatchState.NONE) {
                userMoviesRtdbRef.removeValue()
            } else {
                userMoviesRtdbRef.setValue(
                    mapOf(
                        "movieId" to movieId,
                        "status" to status.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }

            // Guardar en Firestore
            val docRef = db.collection("users")
                .document(userId)
                .collection("user_movies")
                .document(movieId)

            if (status == WatchState.NONE) {
                docRef.delete().await()
            } else {
                val data = mapOf(
                    "status" to status.name,
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data).await()
            }
        } catch (_: Exception) {
            // Kept in local state
        }
    }

    fun getStatus(movieId: String): WatchState {
        return userMovieStatuses[movieId] ?: WatchState.NONE
    }
}
