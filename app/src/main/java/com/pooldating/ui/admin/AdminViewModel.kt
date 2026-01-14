package com.pooldating.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pooldating.data.model.Pool
import com.pooldating.data.repository.PoolRepository
import com.pooldating.utils.Result
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val poolRepository = PoolRepository()

    private val _pool = MutableLiveData<Pool?>()
    val pool: LiveData<Pool?> = _pool

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadPoolInfo(city: String) {
        _isLoading.value = true
        viewModelScope.launch {
            poolRepository.getActivePoolStream(city).collect { result ->
                _isLoading.value = false
                if (result is Result.Success) {
                    _pool.value = result.data
                } else if (result is Result.Error) {
                    _message.value = "Error loading pool: ${result.exception.message}"
                }
            }
        }
    }
    
    fun addDummyUser(city: String, gender: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = poolRepository.addDummyUser(city, gender)
            _isLoading.value = false
            
            if (result is Result.Success) {
                _message.value = "Added 1 $gender to $city"
                // The stream listener (loadPoolInfo) should auto-update the counts if we keep it active.
                // Or we can manually refresh. For now, rely on stream or manual reload.
            } else if (result is Result.Error) {
                _message.value = "Failed to add dummy: ${result.exception.message}"
            }
        }
    }
    
    fun addDummyUsers(city: String, gender: String, count: Int) {
         viewModelScope.launch {
             var successCount = 0
             _isLoading.value = true
             repeat(count) {
                 val result = poolRepository.addDummyUser(city, gender)
                 if (result is Result.Success) successCount++
             }
             _isLoading.value = false
             _message.value = "Added $successCount / $count $gender(s) to $city"
         }
    }
    
    // NEW: Update Pool Status Helper
    fun setPoolStatus(poolId: String, status: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = poolRepository.updatePoolStatus(poolId, status)
            _isLoading.value = false
            if (result is Result.Success) {
                _message.value = "Pool status set to: $status"
                // Refresh pool info if city is known?
                val p = _pool.value
                if (p != null) loadPoolInfo(p.city ?: "")
            } else if (result is Result.Error) {
                _message.value = "Failed to update status: ${result.exception.message}"
            }
        }
    }
    
    // NEW: Run Matchmaking Helper
    fun runMatchmaking(poolId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = poolRepository.runMatchmaking(poolId)
            _isLoading.value = false
            if (result is Result.Success) {
                _message.value = "Matchmaking triggered successfully!"
                 val p = _pool.value
                if (p != null) loadPoolInfo(p.city ?: "")
            } else if (result is Result.Error) {
                _message.value = "Matchmaking failed: ${result.exception.message}"
            }
        }
    }
    
    // NEW: Force Complete Pool
    fun forceCompletePool(poolId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = poolRepository.forceCompletePool(poolId)
            _isLoading.value = false
            if (result is Result.Success) {
                _message.value = "Pool Force Completed!"
                val p = _pool.value
                if (p != null) loadPoolInfo(p.city ?: "")
            } else if (result is Result.Error) {
                _message.value = "Force Complete failed: ${result.exception.message}"
            }
        }
    }
    
    fun fastForwardPool(poolId: String, targetStage: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = poolRepository.fastForwardPool(poolId, targetStage)
            _isLoading.value = false
            if (result is Result.Success) {
                _message.value = "Fast Forwarded to $targetStage"
                val p = _pool.value
                if (p != null) loadPoolInfo(p.city ?: "")
            } else if (result is Result.Error) {
                _message.value = "FF Failed: ${result.exception.message}"
            }
        }
    }
    
    fun triggerLifecycleChecks() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = poolRepository.triggerLifecycleChecks()
            _isLoading.value = false
            if (result is Result.Success) _message.value = "Checks Triggered"
            else _message.value = "Checks Failed"
        }
    }
    fun backfillChatNames() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = poolRepository.backfillChatNames()
            _isLoading.value = false
            if (result is Result.Success) {
                _message.value = "Backfill complete!"
            } else if (result is Result.Error) {
                _message.value = "Backfill failed: ${result.exception.message}"
            }
        }
    }
    
    // ============================================
    // ITERATION 8: ADMIN INSPECTION FUNCTIONS
    // ============================================
    
    private val _inspectResult = MutableLiveData<String>()
    val inspectResult: LiveData<String> = _inspectResult
    
    private val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
    
    fun inspectCurrentUser() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        inspectUser(uid)
    }
    
    /**
     * Force refresh token to ensure admin claim is picked up, then call function.
     */
    private fun withFreshToken(onReady: () -> Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _inspectResult.value = "Error: Not logged in"
            return
        }
        user.getIdToken(true).addOnSuccessListener {
            onReady()
        }.addOnFailureListener { e ->
            _inspectResult.value = "Token refresh failed: ${e.message}"
        }
    }
    
    fun inspectUser(userId: String) {
        _isLoading.value = true
        withFreshToken {
            functions.getHttpsCallable("inspectUser")
                .call(hashMapOf("userId" to userId))
                .addOnSuccessListener { result ->
                    _isLoading.value = false
                    _inspectResult.value = "User Inspect:\n${formatResult(result.data)}"
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _inspectResult.value = "Inspect failed: ${e.message}"
                }
        }
    }
    
    fun inspectPoolById(poolId: String) {
        _isLoading.value = true
        withFreshToken {
            functions.getHttpsCallable("inspectPool")
                .call(hashMapOf("poolId" to poolId))
                .addOnSuccessListener { result ->
                    _isLoading.value = false
                    _inspectResult.value = "Pool Inspect:\n${formatResult(result.data)}"
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _inspectResult.value = "Inspect failed: ${e.message}"
                }
        }
    }
    
    fun inspectCredits(userId: String) {
        _isLoading.value = true
        withFreshToken {
            functions.getHttpsCallable("inspectCredits")
                .call(hashMapOf("userId" to userId))
                .addOnSuccessListener { result ->
                    _isLoading.value = false
                    _inspectResult.value = "Credits:\n${formatResult(result.data)}"
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _inspectResult.value = "Inspect failed: ${e.message}"
                }
        }
    }
    
    fun verifyInvariants() {
        _isLoading.value = true
        withFreshToken {
            functions.getHttpsCallable("verifySystemInvariants")
                .call(hashMapOf("fix" to false))
                .addOnSuccessListener { result ->
                    _isLoading.value = false
                    _inspectResult.value = "System Check:\n${formatResult(result.data)}"
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _inspectResult.value = "Check failed: ${e.message}"
                }
        }
    }
    
    private fun formatResult(data: Any?): String {
        return when (data) {
            is Map<*, *> -> data.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            else -> data.toString()
        }
    }
    
    /**
     * Bootstrap admin - sets the current user as admin (one-time setup)
     */
    fun bootstrapAdmin() {
        _isLoading.value = true
        functions.getHttpsCallable("bootstrapAdmin")
            .call(hashMapOf<String, Any>())
            .addOnSuccessListener { result ->
                _isLoading.value = false
                _inspectResult.value = "✅ Admin claim set! Please log out and log back in to activate."
                _message.value = "Admin claim set successfully!"
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _inspectResult.value = "Bootstrap failed: ${e.message}"
            }
    }
}

class AdminViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
