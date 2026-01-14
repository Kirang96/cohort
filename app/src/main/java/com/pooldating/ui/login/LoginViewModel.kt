package com.pooldating.ui.login

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.pooldating.data.repository.UserRepository
import com.pooldating.utils.Result
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _loginState = MutableLiveData<Result<Boolean>>()
    val loginState: LiveData<Result<Boolean>> = _loginState

    fun signInWithGoogle(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        repository.signInWithCredential(credential).onEach { result ->
            _loginState.value = result
        }.launchIn(viewModelScope)
    }
    
    fun checkUserProfile(onResult: (Boolean) -> Unit) {
         val user = repository.currentUser
         if (user != null) {
             viewModelScope.launch {
                 val result = repository.getUserProfile(user.uid)
                 if (result is Result.Success && result.data != null) {
                     repository.updateLastLogin(user.uid)
                     onResult(true) // Profile exists
                 } else {
                     onResult(false) // Profile does not exist
                 }
             }
         }
    }

    /**
     * Dev helper: Try to login, if fails, try to signup.
     */
    fun devLogin(email: String, pass: String) {
        _loginState.value = Result.Loading
        viewModelScope.launch {
            // 1. Try Sign In
            val signInResult = repository.signInWithEmail(email, pass)
            if (signInResult is Result.Success) {
                _loginState.value = signInResult
            } else {
                // 2. Try Sign Up (assuming sign in failed because user doesn't exist)
                // Note: Real app should handle errors better, but this is for dev testing.
                android.util.Log.d("LoginViewModel", "Sign in failed, trying sign up...")
                val signUpResult = repository.signUpWithEmail(email, pass)
                _loginState.value = signUpResult
            }
        }
    }
}
