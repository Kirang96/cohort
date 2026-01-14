package com.pooldating.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getMissedActivitySummary(uid: String, lastLogout: Timestamp, lastLogin: Timestamp?): String? {
        try {
            // Fetch ALL matches/chats where I am user_a OR user_b (Separate queries to avoid Index error)
            
            // Matches
            val matchesA = db.collection("matches").whereEqualTo("user_a", uid).get().await()
            val matchesB = db.collection("matches").whereEqualTo("user_b", uid).get().await()
            
            val newMatchesCount = (matchesA.documents + matchesB.documents)
                .mapNotNull { it.getTimestamp("created_at") }
                .count { created -> 
                    val afterLogout = created > lastLogout
                    val beforeLogin = lastLogin == null || created < lastLogin
                    afterLogout && beforeLogin
                }

            // Chats
            val chatsA = db.collection("chats").whereEqualTo("user_a", uid).get().await()
            val chatsB = db.collection("chats").whereEqualTo("user_b", uid).get().await()
            
            // For chats, we also check UNREAD count > 0 to filter out self-sent messages
            // And use last_message_at
            
            // We need full chat object logic or just manual check
            val newChatsCount = (chatsA.documents + chatsB.documents)
                .count { doc ->
                    val lastMessageAt = doc.getTimestamp("last_message_at")
                    val userA = doc.getString("user_a")
                    val unreadA = doc.getLong("unread_count_a")?.toInt() ?: 0
                    val unreadB = doc.getLong("unread_count_b")?.toInt() ?: 0
                    
                    if (lastMessageAt == null) return@count false
                    
                    val afterLogout = lastMessageAt > lastLogout
                    val beforeLogin = lastLogin == null || lastMessageAt < lastLogin
                    
                    // Also check UNREAD status (Self-sent messages have 0 unread for ME)
                    // If I am user_a, I check unread_count_a.
                    val myUnread = if (uid == userA) unreadA else unreadB
                    val hasUnread = myUnread > 0
                    
                    // Logic: 
                    // 1. Time Window: strictly between logout and login.
                    // 2. OR Unread: If I have unread messages, I missed them.
                    // Actually, "beforeLogin" cap handles the "self-sent while online" case?
                    // If I send a message, it is AFTER login. So excluded by time check.
                    // If I receive a message while online, it is AFTER login. Excluded by time check.
                    // So Time Window is sufficient?
                    // YES.
                    // BUT "unread" check is safer if time skew exists?
                    // Let's use Time Window primarily.
                    
                    afterLogout && beforeLogin
                }
            
            if (newMatchesCount > 0 || newChatsCount > 0) {
                return "While you were away: ${if (newMatchesCount > 0) "$newMatchesCount new matches " else ""}${if (newChatsCount > 0) "$newChatsCount new messages" else ""}"
            }
            return null
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Failed to check missed activity", e)
            return null
        }
    }
}
