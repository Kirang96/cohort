package com.pooldating.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pooldating.data.model.Match
import com.pooldating.data.model.User
import com.pooldating.data.model.MatchWithUser
import com.pooldating.utils.Result
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class MatchesRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getMatchesForPool(poolId: String): Result<List<MatchWithUser>> = coroutineScope {
        val userId = auth.currentUser?.uid ?: return@coroutineScope Result.Error(Exception("No user logged in"))

        try {
            // Firestore "OR" query requires Composite Index if determining order, but basic OR is supported in recent SDKs.
            // Safe approach: Two simple queries.
            
            val matchesA = db.collection("matches")
                .whereEqualTo("pool_id", poolId)
                .whereEqualTo("user_a", userId)
                .get()
                .await()
                .toObjects(Match::class.java)

            val matchesB = db.collection("matches")
                .whereEqualTo("pool_id", poolId)
                .whereEqualTo("user_b", userId)
                .get()
                .await()
                .toObjects(Match::class.java)

            val allMatches = (matchesA + matchesB).distinctBy { it.match_id.ifEmpty { it.toString() } } // Fallback distinct if ID missing

            if (allMatches.isEmpty()) {
                return@coroutineScope Result.Success(emptyList())
            }

            // Fetch Profiles for "Other User"
            // We can do this in parallel
            val results = allMatches.map { match ->
                async {
                    val otherUserId = if (match.user_a == userId) match.user_b else match.user_a
                    val userDoc = db.collection("users").document(otherUserId).get().await()
                    val otherUser = userDoc.toObject(User::class.java)
                    
                    if (otherUser != null) {
                         MatchWithUser(match, otherUser)
                    } else {
                         null // Should not happen if data integrity is kept
                    }
                }
            }.awaitAll().filterNotNull()

            Result.Success(results)

        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
