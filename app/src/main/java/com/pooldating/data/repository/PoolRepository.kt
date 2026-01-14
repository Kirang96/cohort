package com.pooldating.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.pooldating.data.model.Pool
import com.pooldating.data.model.PoolMembership
import com.pooldating.utils.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import android.util.Log

class PoolRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val poolsCollection = firestore.collection("pools")
    private val membershipsCollection = firestore.collection("pool_memberships")

    // Get Active Pools Stream for a City
    fun getActivePoolStream(city: String): Flow<Result<Pool?>> = callbackFlow {
        trySend(Result.Loading)
        
        val listener = poolsCollection
            .whereEqualTo("city", city)
            .whereIn("status", listOf("joining", "validating"))
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PoolRepo", "Listen failed", e)
                    trySend(Result.Error(e))
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    try {
                        val pool = snapshot.documents[0].toObject(Pool::class.java)
                        val poolWithId = pool?.copy(pool_id = snapshot.documents[0].id)
                        trySend(Result.Success(poolWithId))
                    } catch (e: Exception) {
                        Log.e("PoolRepo", "Pool deserialization error", e)
                        trySend(Result.Success(null))
                    }
                } else {
                    trySend(Result.Success(null))
                }
            }
        awaitClose { listener.remove() }
    }

    // Get Stream for Specific Pool (e.g., the one we are a member of)
    fun getPoolStream(poolId: String): Flow<Result<Pool?>> = callbackFlow {
        val listener = poolsCollection.document(poolId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.Error(e))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                     try {
                        val pool = snapshot.toObject(Pool::class.java)
                        val poolWithId = pool?.copy(pool_id = snapshot.id)
                        trySend(Result.Success(poolWithId))
                    } catch (e: Exception) {
                        trySend(Result.Error(e))
                    }
                } else {
                    trySend(Result.Success(null))
                }
            }
        awaitClose { listener.remove() }
    }

    fun getMyMembershipStream(): Flow<Result<PoolMembership?>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.Error(Exception("No user logged in")))
            close()
            return@callbackFlow
        }
        
        // Removed orderBy("joined_at") to avoid needing a composite index.
        // We fetch all memberships for the user and sort in memory.
        android.util.Log.d("PoolRepository", "Setting up membership listener for user: $uid")
        val listener = membershipsCollection
            .whereEqualTo("user_id", uid)
            .addSnapshotListener { snapshot, e ->
                android.util.Log.d("PoolRepository", "Membership snapshot received! docs=${snapshot?.documents?.size ?: 0}, error=${e?.message}")
                if (e != null) {
                    android.util.Log.e("PoolRepository", "getMyMembershipStream error: ${e.message}", e)
                    trySend(Result.Error(e))
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    try {
                        // Sort: Prioritize ACTIVE/BUFFER, then latest timestamp
                        val memberships = snapshot.documents.mapNotNull { it.toObject(PoolMembership::class.java) }
                        android.util.Log.d("PoolRepository", "Found ${memberships.size} memberships for $uid")
                        
                        // Priority 1: Active or Buffer (Current Pool)
                        val activeMembership = memberships.find { it.status == "active" || it.status == "buffer" }
                        
                        // Priority 2: Latest joined (History)
                        val latest = activeMembership ?: memberships.maxByOrNull { it.joined_at?.toDate()?.time ?: 0 }
                        
                        android.util.Log.d("PoolRepository", "Selected membership: pool=${latest?.pool_id}, status=${latest?.status} (Preferred active: ${activeMembership != null})")
                        trySend(Result.Success(latest))
                    } catch (e: Exception) {
                        android.util.Log.e("PoolRepository", "Error processing memberships: ${e.message}", e)
                        trySend(Result.Success(null))
                    }
                } else {
                    android.util.Log.d("PoolRepository", "No membership found for $uid (snapshot empty or null)")
                    trySend(Result.Success(null))
                }
            }
        awaitClose { listener.remove() }
    }

    // Get only ACTIVE membership (for determining if user is waiting in a pool)
    fun getActiveMembershipStream(): Flow<Result<PoolMembership?>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(Result.Error(Exception("No user logged in")))
        return getMembershipFlow(uid, "active")
    }
    


    // Get only COMPLETED membership (for showing past matches - persists even after joining new pool)
    fun getCompletedMembershipStream(): Flow<Result<PoolMembership?>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(Result.Error(Exception("No user logged in")))
        return getMembershipFlow(uid, "completed")
    }

    private fun getMembershipFlow(uid: String, status: String): Flow<Result<PoolMembership?>> = callbackFlow {
        val listener = membershipsCollection
            .whereEqualTo("user_id", uid)
            .whereEqualTo("status", status)
            .orderBy("joined_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1) // Get the most recent one
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("PoolRepository", "getMembershipFlow error: ${e.message}", e)
                    trySend(Result.Error(e))
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    try {
                        val membership = snapshot.documents[0].toObject(PoolMembership::class.java)
                        android.util.Log.d("PoolRepository", "Found membership: pool_id=${membership?.pool_id}, status=${membership?.status}")
                        trySend(Result.Success(membership))
                    } catch (e: Exception) {
                         trySend(Result.Success(null))
                    }
                } else {
                    android.util.Log.d("PoolRepository", "No membership found for $uid with status $status")
                    trySend(Result.Success(null))
                }
            }
        awaitClose { listener.remove() }
    }

    // Call Cloud Function: Join Pool
    suspend fun joinPool(poolId: String): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "poolId" to poolId
            )
            
            functions
                .getHttpsCallable("joinPool")
                .call(data)
                .await()
            
            Result.Success(true)
        } catch (e: Exception) {
            Log.e("PoolRepo", "Join Pool Failed", e)
            Result.Error(e)
        }
    }

    // Call Cloud Function: Create Pool (Trigger manually if needed)
    suspend fun triggerCreatePool(city: String): Result<Boolean> {
        return try {
             val data = hashMapOf(
                "city" to city
            )
            functions
                .getHttpsCallable("createPoolIfNotExists")
                .call(data)
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    // Call Cloud Function: Run Matchmaking (Dev/Admin Trigger)
    suspend fun runMatchmaking(poolId: String): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "poolId" to poolId
            )
            functions
                .getHttpsCallable("runMatchmaking")
                .call(data)
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Log.e("PoolRepo", "Matchmaking Trigger Failed", e)
            Result.Error(e)
        }
    }

    // Call Cloud Function: Add Dummy User (Admin)
    suspend fun addDummyUser(city: String, gender: String): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "city" to city,
                "gender" to gender
            )
            functions
                .getHttpsCallable("addDummyUser")
                .call(data)
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Log.e("PoolRepo", "Add Dummy User Failed", e)
             // Non-fatal for app flow, but fatal for admin action
            Result.Error(e)
        }
    }

    // Call Cloud Function: Update Pool Status (Admin)
    suspend fun updatePoolStatus(poolId: String, status: String): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "poolId" to poolId,
                "status" to status
            )
            functions
                .getHttpsCallable("updatePoolStatus")
                .call(data)
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Log.e("PoolRepo", "Update Status Failed", e)
            Result.Error(e)
        }
    }
    
    // NEW: Backfill Chat Names (Admin)
    suspend fun backfillChatNames(): Result<Boolean> {
        return try {
            functions
                .getHttpsCallable("backfillChatNames")
                .call(hashMapOf<String, Any>())
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Log.e("PoolRepo", "Backfill Names Failed", e)
            Result.Error(e)
        }
    }
    // Check if user is in an active (locked) pool
    suspend fun checkActivePoolLock(uid: String): Boolean {
        return try {
            val membershipSnapshot = membershipsCollection
                .whereEqualTo("user_id", uid)
                .whereIn("status", listOf("buffer", "active"))
                .limit(1)
                .get()
                .await()
            
            if (membershipSnapshot.isEmpty) return false
            
            val poolId = membershipSnapshot.documents[0].getString("pool_id") ?: return false
            val poolSnapshot = poolsCollection.document(poolId).get().await()
            val poolStatus = poolSnapshot.getString("status")
            
            poolStatus == "joining" || poolStatus == "validating"
        } catch (e: Exception) {
            Log.e("PoolRepo", "Check lock failed", e)
            false
        }
    }
    
    // NEW: Force Complete Pool (Admin)
    suspend fun forceCompletePool(poolId: String): Result<Boolean> {
        return try {
            functions.getHttpsCallable("forceCompletePool")
                .call(hashMapOf("poolId" to poolId))
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // NEW: Fast Forward Pool (Admin)
    suspend fun fastForwardPool(poolId: String, targetStage: String): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "poolId" to poolId,
                "targetStage" to targetStage
            )
            functions.getHttpsCallable("fastForwardPool")
                .call(data)
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // NEW: Trigger Lifecycle Checks Manual (Admin)
    suspend fun triggerLifecycleChecks(): Result<Boolean> {
        return try {
            functions.getHttpsCallable("triggerLifecycleChecks")
                .call(hashMapOf<String, Any>())
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
