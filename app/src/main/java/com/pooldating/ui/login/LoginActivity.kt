package com.pooldating.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pooldating.R
import com.pooldating.ui.home.HomeActivity
import com.pooldating.ui.profile.ProfileSetupActivity
import com.pooldating.utils.Result

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient
    
    private val signInLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Google Sign-In Error: ID Token is NULL", Toast.LENGTH_LONG).show()
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                // Common codes: 10 = Developer Error (Fingerprint/Package mismatch), 12500 = Sign in failed
                Toast.makeText(this, "Google Sign-In API Error: Code ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } else {
             Toast.makeText(this, "Sign-In Result Not OK: ${result.resultCode}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Auto-Login Check
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Check profile existence and redirect
            viewModel.checkUserProfile { exists ->
                if (exists) {
                     startActivity(Intent(this, HomeActivity::class.java))
                } else {
                     startActivity(Intent(this, ProfileSetupActivity::class.java))
                }
                finish()
            }
            return // Skip UI setup if logged in (although async check might race, simple enough for now)
        }
        
        // Configure Google Sign In
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

        findViewById<Button>(R.id.btnGoogleSignIn).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        // Dev Login Wiring
        val etEmail = findViewById<android.widget.EditText>(R.id.etEmail)
        val etPassword = findViewById<android.widget.EditText>(R.id.etPassword)
        val btnEmailLogin = findViewById<Button>(R.id.btnEmailLogin)

        btnEmailLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()
            if (email.isNotBlank() && pass.isNotBlank()) {
                if (pass.length >= 6) {
                    viewModel.devLogin(email, pass)
                } else {
                    Toast.makeText(this, "Password must be >= 6 chars", Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(this, "Email and Password required", Toast.LENGTH_SHORT).show()
            }
        }
        
        observeViewModel()
    }

    private fun observeViewModel() {
        android.util.Log.d("LoginActivity", "Setting up observers")
        
        viewModel.loginState.observe(this) { result ->
            android.util.Log.d("LoginActivity", "loginState changed: $result")
            
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            val btnSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
            
            when (result) {
                is Result.Loading -> {
                    android.util.Log.d("LoginActivity", "State: Loading")
                    progressBar.visibility = View.VISIBLE
                    btnSignIn.isEnabled = false
                }
                is Result.Success -> {
                    android.util.Log.d("LoginActivity", "State: Success, data=${result.data}")
                    progressBar.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    
                    if (result.data) { // Logged in
                        android.util.Log.d("LoginActivity", "Checking user profile...")
                        try {
                            viewModel.checkUserProfile { exists ->
                                android.util.Log.d("LoginActivity", "Profile check result: exists=$exists")
                                if (isFinishing || isDestroyed) return@checkUserProfile
                                
                                try {
                                    if (exists) {
                                        startActivity(Intent(this, HomeActivity::class.java))
                                    } else {
                                        startActivity(Intent(this, ProfileSetupActivity::class.java))
                                    }
                                    finish()
                                } catch (e: Exception) {
                                    android.util.Log.e("LoginActivity", "Navigation error", e)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LoginActivity", "checkUserProfile error", e)
                        }
                    }
                }
                is Result.Error -> {
                    android.util.Log.e("LoginActivity", "State: Error", result.exception)
                    progressBar.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    
                    val msg = result.exception.message ?: "Unknown error"
                    if (msg.contains("The email address is already in use")) {
                         Toast.makeText(this, "Email in use. Try logging in.", Toast.LENGTH_LONG).show()
                    } else if (msg.contains("This operation is not allowed")) {
                         Toast.makeText(this, "Enable 'Email/Password' in Firebase Console!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
