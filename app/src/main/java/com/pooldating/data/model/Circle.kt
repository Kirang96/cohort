package com.pooldating.data.model

import com.google.firebase.Timestamp

/**
 * Circle data model (formerly Pool)
 * Represents a dating circle that users can join for matching
 */
data class Circle(
    val pool_id: String = "", // kept as pool_id for Firestore compatibility
    val city: String? = null,
    val status: String? = null, // joining, validating, matching, completed, cancelled
    val male_count: Int? = 0,
    val female_count: Int? = 0,
    val buffer_m_count: Int? = 0,
    val buffer_f_count: Int? = 0,
    val created_at: Timestamp? = null,
    val join_deadline: Timestamp? = null,
    val match_deadline: Timestamp? = null
) {
    // Alias for cleaner code
    val circleId: String get() = pool_id
}

/**
 * Circle Membership (formerly PoolMembership)
 * Represents a user's membership in a circle
 */
data class CircleMembership(
    val pool_id: String = "", // kept as pool_id for Firestore compatibility
    val user_id: String = "",
    val gender: String = "",
    val joined_at: Timestamp? = null,
    val status: String = "" // active, rolled_over
) {
    // Alias for cleaner code
    val circleId: String get() = pool_id
}

// Type aliases for backward compatibility
typealias Pool = Circle
typealias PoolMembership = CircleMembership
