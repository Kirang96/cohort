package com.pooldating.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Filter

import com.google.firebase.functions.FirebaseFunctions
import com.pooldating.utils.Result
import com.pooldating.data.model.Chat
import com.pooldating.data.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    fun getMyChatsStream(): Flow<Result<List<Chat>>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(Result.Error(Exception("Not logged in")))
            close()
            return@callbackFlow
        }

        val uid = currentUser.uid
        val collection = db.collection("chats")
        
        val query = collection.where(
            Filter.or(
                Filter.equalTo("user_a", uid),
                Filter.equalTo("user_b", uid)
            )
        )

        val registration = query.addSnapshotListener { snapshot, e ->
             if (e != null) {
                 trySend(Result.Error(e))
                 return@addSnapshotListener
             }
             if (snapshot != null) {
                 val chats = snapshot.documents.mapNotNull { doc ->
                     val data = doc.toObject(Chat::class.java)
                     if (data != null) {
                         data.copy(match_id = doc.id)
                     } else null
                 }
                 trySend(Result.Success(chats))
             }
        }
        
        awaitClose { registration.remove() }
    }

    // Optimized Single Chat Stream
    fun getChatStream(matchId: String): Flow<Result<Chat>> = callbackFlow {
        val docRef = db.collection("chats").document(matchId)
        val registration = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Error(e))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val chat = snapshot.toObject(Chat::class.java)?.copy(match_id = snapshot.id)
                if (chat != null) {
                    trySend(Result.Success(chat))
                }
            }
        }
        awaitClose { registration.remove() }
    }
    
    fun getMessagesStream(matchId: String): Flow<Result<List<Message>>> = callbackFlow {
        val query = db.collection("chats").document(matchId).collection("messages")
            .orderBy("sent_at", Query.Direction.ASCENDING)
            
        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Error(e))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                 val msgs = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                 trySend(Result.Success(msgs))
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(matchId: String, text: String): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "matchId" to matchId,
                "text" to text
            )
            functions.getHttpsCallable("sendMessage").call(data).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun requestChatContinuation(matchId: String): Result<Boolean> {
        return try {
            val data = hashMapOf("matchId" to matchId)
            functions.getHttpsCallable("requestChatContinuation").call(data).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun markChatRead(matchId: String): Result<Boolean> {
        return try {
            val data = hashMapOf("matchId" to matchId)
            functions.getHttpsCallable("markChatRead").call(data).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun blockUser(blockedId: String, reason: String?, matchId: String? = null): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "blockedId" to blockedId,
                "reason" to reason,
                "matchId" to matchId
            )
            functions.getHttpsCallable("blockUser").call(data).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun reportUser(reportedId: String, reason: String, context: String? = null, matchId: String? = null): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "reportedId" to reportedId,
                "reason" to reason,
                "context" to context,
                "matchId" to matchId
            )
            functions.getHttpsCallable("reportUser").call(data).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun unblockUser(blockedId: String, matchId: String? = null): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "blockedId" to blockedId,
                "matchId" to matchId
            )
            functions.getHttpsCallable("unblockUser").call(data).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    // ============================================
    // ITERATION 8 EXT: IMAGE SUPPORT
    // ============================================
    
    /**
     * Request a signed upload URL for sending an image in chat.
     * Returns: uploadUrl, downloadUrl, messageId
     */
    suspend fun requestImageUploadUrl(matchId: String): Result<ImageUploadInfo> {
        return try {
            val data = hashMapOf("matchId" to matchId)
            val result = functions.getHttpsCallable("requestImageUploadUrl").call(data).await()
            val resultData = result.data as? Map<*, *>
            
            if (resultData != null) {
                Result.Success(ImageUploadInfo(
                    uploadUrl = resultData["uploadUrl"] as? String ?: "",
                    downloadUrl = resultData["downloadUrl"] as? String ?: "",
                    messageId = resultData["messageId"] as? String ?: "",
                    storagePath = resultData["storagePath"] as? String ?: ""
                ))
            } else {
                Result.Error(Exception("Invalid response"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Send a message with an image.
     */
    suspend fun sendImageMessage(
        matchId: String, 
        imageUrl: String, 
        imageWidth: Int? = null, 
        imageHeight: Int? = null,
        text: String? = null
    ): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "matchId" to matchId,
                "image_url" to imageUrl,
                "image_width" to imageWidth,
                "image_height" to imageHeight,
                "text" to text
            )
            functions.getHttpsCallable("sendMessage").call(data).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

/**
 * Data class for image upload information
 */
data class ImageUploadInfo(
    val uploadUrl: String = "", // No longer used, kept for backward compat
    val downloadUrl: String,
    val messageId: String,
    val storagePath: String
)
