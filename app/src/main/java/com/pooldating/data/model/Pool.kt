package com.pooldating.data.model

import com.google.firebase.Timestamp

data class Pool(
    val pool_id: String = "",
    val city: String? = null,
    val status: String? = null, // joining, validating, matching, completed, cancelled
    val male_count: Int? = 0,
    val female_count: Int? = 0,
    val buffer_m_count: Int? = 0,
    val buffer_f_count: Int? = 0,
    val created_at: Timestamp? = null,
    val join_deadline: Timestamp? = null,
    val match_deadline: Timestamp? = null
)

data class PoolMembership(
    val pool_id: String = "",
    val user_id: String = "",
    val gender: String = "",
    val joined_at: Timestamp? = null,
    val status: String = "" // active, rolled_over
)
