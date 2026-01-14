package com.pooldating.data.model

import com.google.firebase.Timestamp

data class Match(
    val match_id: String = "",
    val pool_id: String = "",
    val user_a: String = "",
    val user_b: String = "",
    val user_a_name: String = "Unknown",
    val user_b_name: String = "Unknown",
    val compatibility_score: Int = 0,
    val created_at: Timestamp? = null
)

data class MatchWithUser(
    val match: Match,
    val otherUser: User
)
