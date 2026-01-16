package com.pooldating.data.repository

import com.pooldating.data.model.Circle
import com.pooldating.data.model.CircleMembership
import com.pooldating.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * CircleRepository - Renamed interface for Circle management
 * Wraps PoolRepository methods with Circle terminology
 * Firestore collection remains 'pools' for backward compatibility
 */
class CircleRepository {
    
    private val poolRepository = PoolRepository()
    
    // ===== STREAMS =====
    
    fun getActiveCircleStream(city: String): Flow<Result<Circle?>> {
        return poolRepository.getActivePoolStream(city)
    }
    
    fun getCircleStream(circleId: String): Flow<Result<Circle?>> {
        return poolRepository.getPoolStream(circleId)
    }
    
    fun getMyMembershipStream(): Flow<Result<CircleMembership?>> {
        return poolRepository.getMyMembershipStream()
    }
    
    fun getActiveMembershipStream(): Flow<Result<CircleMembership?>> {
        return poolRepository.getActiveMembershipStream()
    }
    
    fun getCompletedMembershipStream(): Flow<Result<CircleMembership?>> {
        return poolRepository.getCompletedMembershipStream()
    }
    
    // ===== ACTIONS =====
    
    suspend fun joinCircle(circleId: String): Result<Boolean> {
        return poolRepository.joinPool(circleId)
    }
    
    suspend fun triggerCreateCircle(city: String): Result<Boolean> {
        return poolRepository.triggerCreatePool(city)
    }
    
    suspend fun runMatchmaking(circleId: String): Result<Boolean> {
        return poolRepository.runMatchmaking(circleId)
    }
    
    suspend fun addDummyUser(city: String, gender: String): Result<Boolean> {
        return poolRepository.addDummyUser(city, gender)
    }
    
    suspend fun updateCircleStatus(circleId: String, status: String): Result<Boolean> {
        return poolRepository.updatePoolStatus(circleId, status)
    }
    
    suspend fun backfillChatNames(): Result<Boolean> {
        return poolRepository.backfillChatNames()
    }
    
    suspend fun checkActiveCircleLock(uid: String): Boolean {
        return poolRepository.checkActivePoolLock(uid)
    }
    
    suspend fun forceCompleteCircle(circleId: String): Result<Boolean> {
        return poolRepository.forceCompletePool(circleId)
    }
    
    suspend fun fastForwardCircle(circleId: String, targetStage: String): Result<Boolean> {
        return poolRepository.fastForwardPool(circleId, targetStage)
    }
    
    suspend fun triggerLifecycleChecks(): Result<Boolean> {
        return poolRepository.triggerLifecycleChecks()
    }
}
