package com.pooldating.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.pooldating.R
import com.pooldating.ui.home.HomeActivity
import com.pooldating.ui.profile.ProfileSetupActivity
import com.pooldating.utils.Result

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    
    // UI Elements
    private lateinit var btnSignUp: TextView
    private lateinit var btnLogIn: TextView
    private lateinit var layoutPhoneInput: LinearLayout
    private lateinit var layoutOtpInput: LinearLayout
    private lateinit var etPhone: EditText
    private lateinit var etOtp: EditText
    private lateinit var btnContinue: MaterialButton
    private lateinit var btnVerify: MaterialButton
    private lateinit var btnChangeNumber: TextView
    private lateinit var tvOtpSentTo: TextView
    private lateinit var progressBar: ProgressBar
    
    private var isSignUpMode = true  // Default to Sign up

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Auto-Login Check
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.checkUserProfile { exists ->
                if (exists) {
                    startActivity(Intent(this, HomeActivity::class.java))
                } else {
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                }
                finish()
            }
            return
        }
        
        initViews()
        setupToggle()
        setupPhoneInput()
        setupOtpInput()
        observeViewModel()
    }
    
    private fun initViews() {
        btnSignUp = findViewById(R.id.btnSignUp)
        btnLogIn = findViewById(R.id.btnLogIn)
        layoutPhoneInput = findViewById(R.id.layoutPhoneInput)
        layoutOtpInput = findViewById(R.id.layoutOtpInput)
        etPhone = findViewById(R.id.etPhone)
        etOtp = findViewById(R.id.etOtp)
        btnContinue = findViewById(R.id.btnContinue)
        btnVerify = findViewById(R.id.btnVerify)
        btnChangeNumber = findViewById(R.id.btnChangeNumber)
        tvOtpSentTo = findViewById(R.id.tvOtpSentTo)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupToggle() {
        updateToggleUI()
        
        btnSignUp.setOnClickListener {
            isSignUpMode = true
            updateToggleUI()
        }
        
        btnLogIn.setOnClickListener {
            isSignUpMode = false
            updateToggleUI()
        }
    }
    
    private fun updateToggleUI() {
        if (isSignUpMode) {
            btnSignUp.setBackgroundResource(R.drawable.bg_toggle_selected)
            btnSignUp.setTextColor(ContextCompat.getColor(this, R.color.primary_foreground))
            btnLogIn.background = null
            btnLogIn.setTextColor(ContextCompat.getColor(this, R.color.muted_foreground))
            btnContinue.text = "Continue"
        } else {
            btnLogIn.setBackgroundResource(R.drawable.bg_toggle_selected)
            btnLogIn.setTextColor(ContextCompat.getColor(this, R.color.primary_foreground))
            btnSignUp.background = null
            btnSignUp.setTextColor(ContextCompat.getColor(this, R.color.muted_foreground))
            btnContinue.text = "Log in"
        }
    }
    
    private fun setupPhoneInput() {
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnContinue.isEnabled = s?.length == 10
            }
        })
        
        btnContinue.setOnClickListener {
            val phone = etPhone.text.toString()
            if (phone.length == 10) {
                showLoading(true)
                viewModel.sendOtp("+91$phone", this)
            }
        }
    }
    
    private fun setupOtpInput() {
        etOtp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnVerify.isEnabled = s?.length == 6
            }
        })
        
        btnVerify.setOnClickListener {
            val otp = etOtp.text.toString()
            if (otp.length == 6) {
                showLoading(true)
                viewModel.verifyOtp(otp)
            }
        }
        
        btnChangeNumber.setOnClickListener {
            showPhoneInput()
        }
    }
    
    private fun showOtpInput() {
        val phone = etPhone.text.toString()
        tvOtpSentTo.text = "Sent to +91 $phone"
        layoutPhoneInput.visibility = View.GONE
        layoutOtpInput.visibility = View.VISIBLE
        etOtp.requestFocus()
    }
    
    private fun showPhoneInput() {
        layoutOtpInput.visibility = View.GONE
        layoutPhoneInput.visibility = View.VISIBLE
        etOtp.text?.clear()
        etPhone.requestFocus()
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnContinue.isEnabled = !show && etPhone.text?.length == 10
        btnVerify.isEnabled = !show && etOtp.text?.length == 6
    }

    private fun observeViewModel() {
        viewModel.otpSent.observe(this) { sent ->
            showLoading(false)
            if (sent) {
                showOtpInput()
            }
        }
        
        viewModel.loginState.observe(this) { result ->
            when (result) {
                is Result.Loading -> showLoading(true)
                is Result.Success -> {
                    showLoading(false)
                    if (result.data) {
                        viewModel.checkUserProfile { exists ->
                            if (isFinishing || isDestroyed) return@checkUserProfile
                            if (exists) {
                                startActivity(Intent(this, HomeActivity::class.java))
                            } else {
                                startActivity(Intent(this, ProfileSetupActivity::class.java))
                            }
                            finish()
                        }
                    }
                }
                is Result.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                showLoading(false)
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
