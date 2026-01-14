package com.pooldating.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pooldating.data.model.User
import com.pooldating.data.repository.UserRepository
import com.pooldating.data.repository.PoolRepository
import com.pooldating.utils.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileViewModel : ViewModel() {

    private val repository = UserRepository()
    private val poolRepository = PoolRepository()

    private val _userData = MutableLiveData<Result<User?>>()
    val userData: LiveData<Result<User?>> = _userData

    private val _updateState = MutableLiveData<Result<Boolean>>()
    val updateState: LiveData<Result<Boolean>> = _updateState
    
    private val _hasActiveMembership = MutableLiveData<Boolean>(false)
    val hasActiveMembership: LiveData<Boolean> = _hasActiveMembership
    
    val cities = listOf("Kochi") // Kochi-only for initial launch

    fun loadUserData() {
        val user = repository.currentUser
        if (user != null) {
            _userData.value = Result.Loading
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
        
        _updateState.value = Result.Loading

        val updates = mapOf(
            "bio" to bio,
            "interests" to interests,
            "city" to city
        )

        viewModelScope.launch {
            _updateState.value = repository.updateUserProfile(user.uid, updates)
        }
    }
}
