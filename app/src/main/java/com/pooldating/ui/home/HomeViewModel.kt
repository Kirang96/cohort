package com.pooldating.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pooldating.data.model.Pool
import com.pooldating.data.model.PoolMembership
import com.pooldating.data.model.User
import com.pooldating.data.repository.CreditRepository
import com.pooldating.data.repository.UserRepository
import com.pooldating.data.repository.PoolRepository
import com.pooldating.utils.Result
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter

class HomeViewModel : ViewModel() {

    private val poolRepository = PoolRepository()
    private val userRepository = UserRepository()
    private val creditRepository = CreditRepository()

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _activePool = MutableLiveData<Pool?>()
    val activePool: LiveData<Pool?> = _activePool

    private val _myMembership = MutableLiveData<PoolMembership?>()
    val myMembership: LiveData<PoolMembership?> = _myMembership

    private val _joinState = MutableLiveData<Result<Boolean>>()
    val joinState: LiveData<Result<Boolean>> = _joinState

    private val _signOutState = MutableLiveData<Boolean>()
    val signOutState: LiveData<Boolean> = _signOutState

    private val _missedSummary = MutableLiveData<String?>()
    val missedSummary: LiveData<String?> = _missedSummary

    // Credit balance - always fetched from server, never cached
    private val _creditBalance = MutableLiveData<Int>(0)
    val creditBalance: LiveData<Int> = _creditBalance

    private val _purchaseState = MutableLiveData<Result<CreditRepository.PurchaseResult>?>()
    val purchaseState: LiveData<Result<CreditRepository.PurchaseResult>?> = _purchaseState

    // Current pool user is waiting in (active membership)
    private val _membershipPool = MutableLiveData<Pool?>()
    val membershipPool: LiveData<Pool?> = _membershipPool

    // Completed pool for viewing past matches (persists even after joining new pool)
    private val _completedPool = MutableLiveData<Pool?>()
    val completedPool: LiveData<Pool?> = _completedPool

    private var activeMembershipJob: Job? = null
    private var completedMembershipJob: Job? = null
    private var activePoolJob: Job? = null
    private var membershipPoolJob: Job? = null
    private var completedPoolJob: Job? = null

    init {
        android.util.Log.d("HomeViewModel", "init block started")
        loadUserAndPools()
        loadCreditBalance()
    }

    fun loadUserData() {
        android.util.Log.d("HomeViewModel", "loadUserData called")
        loadUserAndPools()
        loadCreditBalance()
    }

    private fun loadUserAndPools() {
        android.util.Log.d("HomeViewModel", "loadUserAndPools started")
        // 1. Get User Profile to know City
        val uid = userRepository.currentUser?.uid
        android.util.Log.d("HomeViewModel", "Current user uid: $uid")
        
        if (uid != null) {
            viewModelScope.launch {
                try {
                    android.util.Log.d("HomeViewModel", "Fetching user profile...")
                    val userResult = userRepository.getUserProfile(uid)
                    android.util.Log.d("HomeViewModel", "User profile result: $userResult")
                    
                    if (userResult is Result.Success) {
                        val user = userResult.data
                        android.util.Log.d("HomeViewModel", "User data: $user")
                        _currentUser.value = user
                        
                        if (user != null && !user.city.isNullOrEmpty()) {
                            android.util.Log.d("HomeViewModel", "User city: ${user.city}")
                            
                            // 2. Ensure a pool exists for this city (AWAIT to ensure it completes)
                            android.util.Log.d("HomeViewModel", "Creating pool for ${user.city} if needed...")
                            try {
                                val createResult = poolRepository.triggerCreatePool(user.city)
                                if (createResult is Result.Success) {
                                    android.util.Log.d("HomeViewModel", "Pool creation check completed successfully")
                                } else if (createResult is Result.Error) {
                                    android.util.Log.e("HomeViewModel", "Pool creation failed: ${createResult.exception.message}", createResult.exception)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("HomeViewModel", "Pool creation crashed", e)
                            }
                            
                            // 3. NOW start listening to pools (pool should exist by now)
                            android.util.Log.d("HomeViewModel", "Starting pool listeners for city: ${user.city}")
                            startPoolListeners(user.city)
                            
                            // 4. Check for missed activity
                            if (user.last_logout_at != null) {
                                checkForMissedActivity(user.last_logout_at)
                            }
                        } else {
                            android.util.Log.w("HomeViewModel", "User city is null or empty, skipping pool listeners")
                        }
                    } else {
                        android.util.Log.e("HomeViewModel", "Failed to get user profile: $userResult")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "loadUserAndPools CRASHED", e)
                }
            }
        } else {
            android.util.Log.w("HomeViewModel", "No current user, skipping profile load")
        }
    }

    private fun startPoolListeners(city: String) {
        // Cancel previous listeners
        activePoolJob?.cancel()
        membershipPoolJob?.cancel()

        // 1. Listen to Active/Joining Pools in City - For TOP CARD (join new pool)
        android.util.Log.d("HomeViewModel", "Starting City Pool Listener for $city")
        activePoolJob = poolRepository.getActivePoolStream(city).onEach { result ->
            if (result is Result.Success) {
                android.util.Log.d("HomeViewModel", "City pool: ${result.data?.pool_id}, status=${result.data?.status}")
                _activePool.value = result.data
            }
        }.launchIn(viewModelScope)

        // 2. Listen to user's membership (ANY membership - active or completed)
        // This is for BOTTOM CARD (user's current/past pool)
        membershipPoolJob = poolRepository.getMyMembershipStream().onEach { result ->
            if (result is Result.Success) {
                val membership = result.data
                android.util.Log.d("HomeViewModel", "User membership: ${membership?.pool_id}, status=${membership?.status}")
                _myMembership.value = membership
                
                if (membership != null) {
                    // Get the pool details for this membership
                    poolRepository.getPoolStream(membership.pool_id).onEach { poolRes ->
                        if (poolRes is Result.Success) {
                            android.util.Log.d("HomeViewModel", "User's pool: ${poolRes.data?.pool_id}, status=${poolRes.data?.status}")
                            _membershipPool.value = poolRes.data
                        }
                    }.launchIn(viewModelScope)
                } else {
                    _membershipPool.value = null
                }
            }
        }.launchIn(viewModelScope)
    }

    fun joinPool() {
        val pool = _activePool.value
        if (pool == null) return

        _joinState.value = Result.Loading
        viewModelScope.launch {
            val result = poolRepository.joinPool(pool.pool_id)
            _joinState.value = result
            
            // Refresh credit balance after join attempt
            loadCreditBalance()
            
            // IMPORTANT: We do NOT restart listeners here.
            // The existing getMyMembershipStream listener (started in startPoolListeners)
            // will automatically receive the new membership document via Firestore real-time sync.
            // This is the industry best practice for Firestore real-time apps.
            if (result is Result.Success) {
                android.util.Log.d("HomeViewModel", "Join succeeded! Firestore listener will receive membership update.")
            }
        }
    }

    fun signOut() {
        userRepository.signOut()
        _signOutState.value = true
    }

    /**
     * Load credit balance from server.
     * Balance is ALWAYS computed server-side - never trust cached values.
     */
    fun loadCreditBalance() {
        viewModelScope.launch {
            try {
                val result = creditRepository.getCreditBalance()
                if (result is Result.Success) {
                    _creditBalance.value = result.data
                    android.util.Log.d("HomeViewModel", "Credit balance loaded: ${result.data}")
                } else if (result is Result.Error) {
                    android.util.Log.e("HomeViewModel", "Failed to load credit balance", result.exception)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading credit balance", e)
            }
        }
    }

    /**
     * Purchase credits (mock function for testing).
     */
    fun purchaseCredits(amount: Int = 5) {
        _purchaseState.value = Result.Loading
        viewModelScope.launch {
            val result = creditRepository.purchaseCredits(amount)
            _purchaseState.value = result
            // Refresh balance after purchase
            if (result is Result.Success) {
                _creditBalance.value = result.data.newBalance
            }
        }
    }

    /**
     * Trigger matchmaking (Dev/Admin function)
     */
    fun runMatchmaking() {
        val pool = _activePool.value ?: return
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "Triggering matchmaking for pool ${pool.pool_id}...")
                val result = poolRepository.runMatchmaking(pool.pool_id)
                if (result is Result.Success) {
                    android.util.Log.d("HomeViewModel", "Matchmaking triggered successfully")
                } else if (result is Result.Error) {
                    android.util.Log.e("HomeViewModel", "Matchmaking trigger failed", result.exception)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error running matchmaking", e)
            }
        }

    }
    
    // State to ensure summary is checked only once per session
    private var hasCheckedSummary = false
    private val notificationRepository = com.pooldating.data.repository.NotificationRepository()

    fun onSummaryShown() {
        _missedSummary.value = null
    }

    private fun checkForMissedActivity(lastLogout: com.google.firebase.Timestamp) {
        if (hasCheckedSummary) return
        hasCheckedSummary = true // Mark as checked immediately to prevent races
        
        // 1. Get last_login_at from currentUser (it should be updated by LoginViewModel and verified in loadUserData)
        val user = _currentUser.value
        val lastLogin = user?.last_login_at
        
        viewModelScope.launch {
            val uid = userRepository.currentUser?.uid ?: return@launch
            
            val summary = notificationRepository.getMissedActivitySummary(uid, lastLogout, lastLogin)
            if (summary != null) {
                _missedSummary.value = summary
            }
        }
    }
}
