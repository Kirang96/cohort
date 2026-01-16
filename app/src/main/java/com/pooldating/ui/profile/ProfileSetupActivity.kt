package com.pooldating.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.pooldating.R
import com.pooldating.ui.home.HomeActivity
import com.pooldating.utils.Result

class ProfileSetupActivity : AppCompatActivity() {

    private val viewModel: ProfileSetupViewModel by viewModels()

    private lateinit var etName: EditText
    private lateinit var etAge: EditText
    private lateinit var btnMale: TextView
    private lateinit var btnFemale: TextView
    private lateinit var etBio: EditText
    private lateinit var tvBioCounter: TextView
    private lateinit var flexboxInterests: FlexboxLayout
    private lateinit var btnSave: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var selectedGender: String? = null
    private val selectedInterests = mutableSetOf<String>()
    
    private val interestOptions = listOf(
        "Reading", "Music", "Travel", "Cooking", "Photography",
        "Art", "Movies", "Fitness", "Technology", "Nature",
        "Writing", "Gaming", "Coffee", "Hiking", "Dancing"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        initViews()
        setupGenderToggle()
        setupBioCounter()
        setupInterestChips()
        observeViewModel()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etAge = findViewById(R.id.etAge)
        btnMale = findViewById(R.id.btnMale)
        btnFemale = findViewById(R.id.btnFemale)
        etBio = findViewById(R.id.etBio)
        tvBioCounter = findViewById(R.id.tvBioCounter)
        flexboxInterests = findViewById(R.id.flexboxInterests)
        btnSave = findViewById(R.id.btnSaveProfile)
        progressBar = findViewById(R.id.progressBar)

        // Back button
        findViewById<android.widget.ImageView>(R.id.btnBack).setOnClickListener {
            // Sign out and go back to login
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            startActivity(android.content.Intent(this, com.pooldating.ui.login.LoginActivity::class.java))
            finish()
        }

        // Add text watchers for validation
        val validationWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateForm() }
        }
        etName.addTextChangedListener(validationWatcher)
        etAge.addTextChangedListener(validationWatcher)

        btnSave.setOnClickListener { saveProfile() }
    }

    private fun setupGenderToggle() {
        btnMale.setOnClickListener {
            selectedGender = "male"
            updateGenderUI()
            validateForm()
        }
        btnFemale.setOnClickListener {
            selectedGender = "female"
            updateGenderUI()
            validateForm()
        }
    }

    private fun updateGenderUI() {
        if (selectedGender == "male") {
            btnMale.setBackgroundResource(R.drawable.bg_gender_selected)
            btnMale.setTextColor(ContextCompat.getColor(this, R.color.primary_foreground))
            btnFemale.setBackgroundResource(R.drawable.bg_gender_unselected)
            btnFemale.setTextColor(ContextCompat.getColor(this, R.color.foreground))
        } else if (selectedGender == "female") {
            btnFemale.setBackgroundResource(R.drawable.bg_gender_selected)
            btnFemale.setTextColor(ContextCompat.getColor(this, R.color.primary_foreground))
            btnMale.setBackgroundResource(R.drawable.bg_gender_unselected)
            btnMale.setTextColor(ContextCompat.getColor(this, R.color.foreground))
        }
    }

    private fun setupBioCounter() {
        etBio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tvBioCounter.text = "${s?.length ?: 0}/150"
            }
        })
    }

    private fun setupInterestChips() {
        interestOptions.forEach { interest ->
            val chip = createInterestChip(interest)
            flexboxInterests.addView(chip)
        }
    }

    private fun createInterestChip(interest: String): TextView {
        val chip = TextView(this).apply {
            text = interest
            textSize = 14f
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            setBackgroundResource(R.drawable.bg_chip_unselected)
            setTextColor(ContextCompat.getColor(context, R.color.foreground))
            
            val params = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, dpToPx(8), dpToPx(8))
            layoutParams = params
            
            setOnClickListener { toggleChip(this, interest) }
        }
        return chip
    }

    private fun toggleChip(chip: TextView, interest: String) {
        if (selectedInterests.contains(interest)) {
            selectedInterests.remove(interest)
            chip.setBackgroundResource(R.drawable.bg_chip_unselected)
            chip.setTextColor(ContextCompat.getColor(this, R.color.foreground))
            chip.text = interest
        } else {
            if (selectedInterests.size < 5) {
                selectedInterests.add(interest)
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.primary_foreground))
                chip.text = "$interest ✕"
            } else {
                Toast.makeText(this, "Maximum 5 interests allowed", Toast.LENGTH_SHORT).show()
            }
        }
        validateForm()
    }

    private fun validateForm() {
        val nameValid = etName.text.toString().length >= 2
        val ageValid = etAge.text.toString().toIntOrNull()?.let { it in 18..99 } ?: false
        val genderValid = selectedGender != null
        val interestsValid = selectedInterests.size in 2..5

        btnSave.isEnabled = nameValid && ageValid && genderValid && interestsValid
    }

    private fun saveProfile() {
        val name = etName.text.toString()
        val age = etAge.text.toString()
        val gender = selectedGender ?: return
        val bio = etBio.text.toString()
        val interests = selectedInterests.joinToString(",")
        val city = "Kochi" // Hardcoded for Kochi-only launch

        viewModel.saveProfile(name, age, gender, bio, interests, city)
    }

    private fun observeViewModel() {
        viewModel.saveState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnSave.isEnabled = false
                }
                is Result.Success -> {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Profile Created!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    validateForm()
                    Toast.makeText(this, "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
