package com.pooldating.data.repository

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pooldating.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import com.pooldating.utils.Result

class UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    val currentUser get() = auth.currentUser

    fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential): Flow<Result<Boolean>> = callbackFlow {
        trySend(Result.Loading)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(Result.Success(true))
                } else {
                    trySend(Result.Error(task.exception ?: Exception("Authentication failed")))
                }
                close()
            }
        awaitClose { }
    }

    suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (snapshot.exists()) {
                try {
                    Result.Success(snapshot.toObject(User::class.java))
                } catch (e: Exception) {
                    android.util.Log.e("UserRepo", "Deserialization error", e)
                    Result.Success(null) 
                }
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createUserProfile(user: User): Result<Boolean> {
        return try {
            android.util.Log.d("UserRepo", "Attempting to create user profile for: ${user.user_id}")
            kotlinx.coroutines.withTimeout(10000L) {
                usersCollection.document(user.user_id).set(user).await()
            }
            android.util.Log.d("UserRepo", "Profile created successfully")
            Result.Success(true)
        } catch (e: Exception) {
            android.util.Log.e("UserRepo", "Error creating profile", e)
            Result.Error(e)
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Boolean> {
        return try {
            android.util.Log.d("UserRepo", "Attempting to update profile for: $uid with $updates")
            kotlinx.coroutines.withTimeout(10000L) {
                usersCollection.document(uid).set(updates, SetOptions.merge()).await()
            }
            android.util.Log.d("UserRepo", "Profile updated successfully")
            Result.Success(true)
        } catch (e: Exception) {
            android.util.Log.e("UserRepo", "Error updating profile", e)
            Result.Error(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<Boolean> {
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    suspend fun updateLastLogin(uid: String) {
        try {
            usersCollection.document(uid).update("last_login_at", com.google.firebase.Timestamp.now()).await()
        } catch (e: Exception) {
            android.util.Log.w("UserRepo", "Failed to update last_login_at", e)
        }
    }
}
