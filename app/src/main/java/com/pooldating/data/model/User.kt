package com.pooldating.data.model

import com.google.firebase.Timestamp

data class User(
    val user_id: String = "",
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val bio: String? = null,
    val interests: String? = null,
    val city: String? = null,
    val status: String? = "active", // active, inactive, suspended
    val created_at: Timestamp? = null,
    val last_logout_at: Timestamp? = null,
    val last_login_at: Timestamp? = null,
    val trust_score: Int = 100,
    val trust_flags: List<String> = emptyList(),
    val restriction_level: String = "none", // none, limited, blocked
    val restriction_reason: String? = null,
    val restriction_expires_at: Timestamp? = null
)
