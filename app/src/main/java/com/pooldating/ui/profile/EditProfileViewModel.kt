package com.pooldating.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pooldating.data.model.User
import com.pooldating.data.repository.UserRepository
import com.pooldating.data.repository.PoolRepository
import com.pooldating.utils.Result as AppResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.collect

class EditProfileViewModel : ViewModel() {

    private val repository = UserRepository()
    private val poolRepository = PoolRepository()

    private val _userData = MutableLiveData<AppResult<User?>>()
    val userData: LiveData<AppResult<User?>> = _userData

    private val _updateState = MutableLiveData<AppResult<Boolean>>()
    val updateState: LiveData<AppResult<Boolean>> = _updateState
    
    private val _hasActiveMembership = MutableLiveData<Boolean>(false)
    val hasActiveMembership: LiveData<Boolean> = _hasActiveMembership
    
    val cities = listOf("Kochi") // Kochi-only for initial launch

    fun loadUserData() {
        val user = repository.currentUser
        if (user != null) {
            _userData.value = AppResult.Loading
            viewModelScope.launch {
                _userData.value = repository.getUserProfile(user.uid)
            }
            
            // Check for active pool membership
            checkActiveMembership()
        }
    }
    
    private fun checkActiveMembership() {
        val uid = repository.currentUser?.uid ?: return
        viewModelScope.launch {
            _hasActiveMembership.postValue(poolRepository.checkActivePoolLock(uid))
        }
    }

    fun updateProfile(bio: String, interests: String, city: String) {
        val user = repository.currentUser ?: return
        
        _updateState.value = AppResult.Loading

        val updates = mapOf(
            "bio" to bio,
            "interests" to interests,
            "city" to city
        )

        viewModelScope.launch {
            _updateState.value = repository.updateUserProfile(user.uid, updates)
        }
    }
    
    fun triggerAdminMatchmaking() {
        val userResult = _userData.value
        val user = if (userResult is AppResult.Success) userResult.data else null
        
        if (user == null) return
        val city = user.city ?: return
        
        viewModelScope.launch {
            // 1. Get active pool ID
            poolRepository.getActivePoolStream(city).take(1).collect { result ->
                 // Check type
                 if (result is AppResult.Success) {
                    // Force cast to access 'data' property
                    val success = result as AppResult.Success<*>
                    val pool = success.data as? com.pooldating.data.model.Pool
                    
                    if (pool != null) {
                        // 2. Trigger Matchmaking
                        poolRepository.runMatchmaking(pool.pool_id)
                    }
                }
            }
        }
    }
}
