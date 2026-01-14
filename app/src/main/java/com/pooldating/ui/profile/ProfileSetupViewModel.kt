package com.pooldating.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pooldating.data.model.User
import com.pooldating.data.repository.UserRepository
import com.pooldating.utils.Result
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp

class ProfileSetupViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _saveState = MutableLiveData<Result<Boolean>>()
    val saveState: LiveData<Result<Boolean>> = _saveState

    // Predefined list of cities - Kochi-only for initial launch
    val cities = listOf("Kochi")
    
    val genders = listOf("Male", "Female")

    fun saveProfile(name: String, age: String, gender: String, bio: String, interests: String, city: String) {
        if (name.isBlank() || age.isBlank() || gender.isBlank() || city.isBlank()) {
            _saveState.value = Result.Error(Exception("Please fill all required fields"))
            return
        }

        val ageInt = age.toIntOrNull()
        if (ageInt == null || ageInt < 18) {
            _saveState.value = Result.Error(Exception("Invalid age (must be 18+)"))
            return
        }

        val user = repository.currentUser ?: return
        
        _saveState.value = Result.Loading

        val newUser = User(
            user_id = user.uid,
            name = name,
            age = ageInt,
            gender = gender,
            bio = bio,
            interests = interests,
            city = city,
            status = "active",
            created_at = Timestamp.now()
        )

        viewModelScope.launch {
            _saveState.value = repository.createUserProfile(newUser)
        }
    }
}
