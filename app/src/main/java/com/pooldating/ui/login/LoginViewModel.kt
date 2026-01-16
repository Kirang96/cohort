package com.pooldating.ui.login

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.pooldating.data.repository.UserRepository
import com.pooldating.utils.Result
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginViewModel : ViewModel() {

    private val repository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val _loginState = MutableLiveData<Result<Boolean>>()
    val loginState: LiveData<Result<Boolean>> = _loginState
    
    private val _otpSent = MutableLiveData<Boolean>()
    val otpSent: LiveData<Boolean> = _otpSent
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Phone OTP Flow
    fun sendOtp(phoneNumber: String, activity: Activity) {
        _loginState.value = Result.Loading
        _error.value = null
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification (instant verification)
                android.util.Log.d("LoginViewModel", "Auto-verification completed")
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                android.util.Log.e("LoginViewModel", "Verification failed", e)
                _loginState.value = Result.Error(e)
                _error.value = e.message ?: "Verification failed"
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                android.util.Log.d("LoginViewModel", "OTP sent, verificationId: $verificationId")
                this@LoginViewModel.verificationId = verificationId
                this@LoginViewModel.resendToken = token
                _otpSent.value = true
            }
        }
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    fun verifyOtp(otp: String) {
        val vId = verificationId
        if (vId == null) {
            _error.value = "Please request OTP first"
            return
        }
        
        _loginState.value = Result.Loading
        val credential = PhoneAuthProvider.getCredential(vId, otp)
        signInWithPhoneCredential(credential)
    }
    
    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("LoginViewModel", "Phone auth success")
                    _loginState.value = Result.Success(true)
                } else {
                    android.util.Log.e("LoginViewModel", "Phone auth failed", task.exception)
                    _loginState.value = Result.Error(task.exception ?: Exception("Phone auth failed"))
                    _error.value = task.exception?.message ?: "Verification failed"
                }
            }
    }

    // Google Sign In (kept for future use)
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
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
        }
    }

    // Dev helper (kept for testing)
    fun devLogin(email: String, pass: String) {
        _loginState.value = Result.Loading
        viewModelScope.launch {
            val signInResult = repository.signInWithEmail(email, pass)
            if (signInResult is Result.Success) {
                _loginState.value = signInResult
            } else {
                android.util.Log.d("LoginViewModel", "Sign in failed, trying sign up...")
                val signUpResult = repository.signUpWithEmail(email, pass)
                _loginState.value = signUpResult
            }
        }
    }
}
