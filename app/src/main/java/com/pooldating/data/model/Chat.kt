package com.pooldating.data.model

import com.google.firebase.Timestamp

data class Chat(
    val match_id: String = "",
    val pool_id: String = "",
    val user_a: String = "",
    val user_b: String = "",
    val user_a_name: String = "",
    val user_b_name: String = "",
    val status: String = "", // active, expired, continued
    val expires_at: Timestamp? = null,
    val continued_by: List<String> = emptyList(),
    val created_at: Timestamp? = null,
    val last_message_at: Timestamp? = null,
    val unread_count_a: Int = 0,
    val unread_count_b: Int = 0,
    val blocked_by: List<String> = emptyList()
)

data class Message(
    val sender_id: String = "",
    val text: String? = null,  // Can be null if image-only message
    val sent_at: Timestamp? = null,
    val isPending: Boolean = false,  // For optimistic UI
    val isFailed: Boolean = false,   // For failed sends
    // Iteration 8 Ext: Image support
    val image_url: String? = null,
    val image_width: Int? = null,
    val image_height: Int? = null
)
